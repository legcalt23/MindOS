/**
 * walletFunctions.ts
 * Wallet callable Cloud Functions.
 *
 * Security envelope for every callable:
 *   requireAppCheck → requireAuth → validateInput → checkRateLimitFailClosed → business logic
 *
 * Invariants:
 *  - walletId is ALWAYS context.auth.uid — never derived from client data.
 *  - Responses never include internal metadata (userId, createdAt, etc.).
 *  - Auto-created wallets start at zero balance; creation is audited.
 */

import * as crypto from 'crypto';
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import { requireAppCheck, requireAuth, requireEmailVerified } from './lib/guards';
import { checkRateLimitFailClosed } from './lib/rateLimiter';
import {
  validateGetBalanceInput,
  validateSpendCoinsInput,
  validateGetTransactionsInput,
  validateInitiatePurchaseInput,
  validateVerifyPurchaseInput,
  PACK_CATALOG,
} from './lib/walletValidators';
import { resolveCorrelationId } from './lib/correlationId';
import { walletError } from './lib/walletErrors';
import { walletLog } from './lib/walletLogger';
import {
  WalletRepository,
  SnapshotRepository,
  LedgerTransactionRepository,
  IdempotencyRepository,
  PurchaseRepository,
  hashPurchaseToken,
} from './lib/walletRepository';
import { hashRequest, checkIdempotencyPreTx, applyIdempotencyInTx } from './lib/idempotencyService';
import { LedgerService, LedgerEntrySpec } from './lib/ledgerService';
import {
  BalanceSnapshotDocument,
  LedgerBucket,
  LedgerTransactionDocument,
} from './lib/walletSchema';
import { getPaymentProvider } from './lib/paymentProviders';
import { RiskScoringService } from './lib/riskScoringService';
import { FraudSignalService } from './lib/fraudSignalService';
import { AuditLogService } from './lib/auditLogService';
import { isProductionFirebaseProject } from './lib/projectEnvironment';

const db = () => admin.firestore();

function timingSafeStringEquals(a: string, b: string): boolean {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function assertSandboxWalletAllowed(correlationId: string): void {
  if (isProductionFirebaseProject()) {
    throw walletError('PROVIDER_NOT_CONFIGURED', correlationId);
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Allocate a spend across buckets using priority: promotional → earned → purchased.
 * Throws INSUFFICIENT_BALANCE if total available is less than amount.
 */
function allocateSpend(
  amount: number,
  snapshot: BalanceSnapshotDocument,
  correlationId: string,
): LedgerEntrySpec[] {
  if (snapshot.availableBalance < amount) {
    throw walletError('INSUFFICIENT_BALANCE', correlationId);
  }

  const entries: LedgerEntrySpec[] = [];
  let remaining = amount;

  const buckets: Array<{ name: LedgerBucket; balance: number }> = [
    { name: 'promotional', balance: snapshot.promotionalBalance },
    { name: 'earned',      balance: snapshot.earnedBalance },
    { name: 'purchased',   balance: snapshot.purchasedBalance },
  ];

  for (const { name, balance } of buckets) {
    if (remaining <= 0) break;
    const take = Math.min(remaining, balance);
    if (take > 0) {
      entries.push({ bucket: name, amount: -take });
      remaining -= take;
    }
  }

  return entries;
}

// ── Fraud enforcement helper ──────────────────────────────────────────────────

/**
 * Runs risk scoring for an economic action and enforces the decision.
 *
 *  allow   → returns; caller proceeds normally.
 *  throttle → records signal; returns (caller proceeds).
 *  review   → records signal + audit; returns (caller proceeds, flagged).
 *  freeze   → auto-freezes wallet + records signal + audit; throws FRAUD_DETECTED.
 *
 * Security contract:
 *  - Score and signal details are NEVER forwarded to the client.
 *  - Freeze is audited before the HttpsError is thrown.
 *  - A single weak signal never causes freeze (score-based threshold enforced
 *    inside RiskScoringService).
 */
async function enforceRiskCheck(params: {
  walletId: string;
  userId: string;
  action: 'initiatePurchase' | 'verifyPurchase' | 'spendCoins';
  correlationId: string;
  packId?: string;
  itemId?: string;
  quantity?: number;
}): Promise<void> {
  const assessment = await RiskScoringService.evaluate({
    walletId:      params.walletId,
    userId:        params.userId,
    action:        params.action,
    correlationId: params.correlationId,
    packId:        params.packId,
    itemId:        params.itemId,
    quantity:      params.quantity,
  });

  const { decision, signals, score } = assessment;

  // Log internally (score/signals never sent to client)
  walletLog.info(
    { correlationId: params.correlationId, action: params.action, walletId: params.walletId },
    `Risk assessment: decision=${decision} score=${score} signals=${signals.length}`,
  );

  if (decision === 'allow') return;

  // Record the highest-scoring signal (or all signals for critical decisions)
  const signalRecordPromises = signals.map((sig) =>
    FraudSignalService.record({
      walletId:      params.walletId,
      userId:        params.userId,
      signalType:    sig.type,
      severity:      sig.severity,
      riskScore:     sig.contribution,
      source:        sig.source,
      metadata:      { detail: sig.detail, action: params.action, aggregateScore: score },
      correlationId: params.correlationId,
    }),
  );

  if (decision === 'throttle') {
    // Record signals; proceed — actual throttle is handled by rate limiter.
    await Promise.allSettled(signalRecordPromises);
    walletLog.warn(
      { correlationId: params.correlationId, action: params.action, walletId: params.walletId },
      `Risk throttle signal recorded (score=${score})`,
    );
    return;
  }

  if (decision === 'review') {
    // Record signals + audit; proceed — flagged for manual review.
    await Promise.allSettled(signalRecordPromises);
    await AuditLogService.record({
      actorId:        'system',
      actorType:      'system',
      action:         'fraud_review_flagged',
      targetWalletId: params.walletId,
      correlationId:  params.correlationId,
      details:        { action: params.action, score, signalCount: signals.length },
    });
    walletLog.warn(
      { correlationId: params.correlationId, action: params.action, walletId: params.walletId },
      `Risk review flag raised (score=${score})`,
    );
    return;
  }

  // decision === 'freeze'
  // Auto-freeze wallet + full audit BEFORE throwing.
  await Promise.allSettled(signalRecordPromises);

  // Freeze the wallet (simple non-transactional update — status change is idempotent)
  await db().collection('wallets').doc(params.walletId).update({
    status: 'frozen' as const,
    updatedAt: Date.now(),
  });

  await AuditLogService.record({
    actorId:        'system',
    actorType:      'system',
    action:         'wallet_auto_frozen_fraud',
    targetWalletId: params.walletId,
    correlationId:  params.correlationId,
    details:        { action: params.action, score, signalCount: signals.length },
  });

  walletLog.warn(
    { correlationId: params.correlationId, action: params.action, walletId: params.walletId },
    `Wallet auto-frozen by fraud enforcement (score=${score})`,
  );

  // Throw opaque error — score/signals never reach the client
  throw walletError('FRAUD_DETECTED', params.correlationId);
}

// ── getWalletBalance ──────────────────────────────────────────────────────────

/**
 * Returns the caller's wallet balance from the server-authoritative snapshot.
 * Auto-creates the wallet on first access (idempotent — same data either way).
 * Allows read for active and frozen wallets; balance is always visible.
 *
 * Response fields — no internal IDs or timestamps beyond lastTransactionId:
 *   availableBalance, purchasedBalance, promotionalBalance, earnedBalance,
 *   status, version, lastTransactionId, correlationId
 */
export const getWalletBalance = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);

  const input = validateGetBalanceInput(data);
  const correlationId = resolveCorrelationId(input);

  // getBalance is not an economic action — fails open (logs warning) if rate-limiter unavailable
  await checkRateLimitFailClosed(callerUid, 'getBalance');

  const walletId = callerUid; // NEVER from client

  try {
    let [wallet, snapshot] = await Promise.all([
      WalletRepository.findById(walletId),
      SnapshotRepository.findByWalletId(walletId),
    ]);

    if (!wallet) {
      walletLog.info({ correlationId, action: 'getWalletBalance', walletId }, 'Auto-creating wallet on first access');
      await AuditLogService.record({
        actorId:        callerUid,
        actorType:      'system',
        action:         'wallet_auto_created',
        targetWalletId: walletId,
        correlationId,
        details:        {},
      });
      wallet = await WalletRepository.create(walletId);
    }

    if (!snapshot) {
      snapshot = await SnapshotRepository.create(walletId);
    }

    if (wallet.status === 'suspended') {
      throw walletError('WALLET_SUSPENDED', correlationId);
    }

    return {
      availableBalance:   snapshot.availableBalance,
      purchasedBalance:   snapshot.purchasedBalance,
      promotionalBalance: snapshot.promotionalBalance,
      earnedBalance:      snapshot.earnedBalance,
      status:             wallet.status,
      version:            snapshot.version,
      lastTransactionId:  snapshot.lastTransactionId,
      correlationId,
    };
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    walletLog.error({ correlationId, action: 'getWalletBalance', walletId }, 'getWalletBalance unexpected error', { err });
    throw walletError('WALLET_NOT_FOUND', correlationId);
  }
});

// ── spendCoins ────────────────────────────────────────────────────────────────

/**
 * Deduct coins from the caller's wallet inside a Firestore transaction.
 *
 * Security properties:
 *  - walletId = callerUid (NEVER from client)
 *  - amount/quantity validated server-side (1–1000); no final balance accepted from client
 *  - Frozen/suspended wallets are rejected before any write
 *  - Full idempotency: same key+body → cached response; same key+different body → conflict
 *  - Double-spend protection: Firestore optimistic lock via snapshot.version
 *  - All writes are atomic in one Firestore transaction
 *  - Audit log appended inside the same transaction
 *
 * Spend priority: promotional → earned → purchased (promo/earned burned first)
 */
export const spendCoins = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  requireEmailVerified(context);

  const input = validateSpendCoinsInput(data);
  const correlationId = resolveCorrelationId(input);

  // spendCoins is an economic action — fails CLOSED if rate-limiter infrastructure unavailable
  await checkRateLimitFailClosed(callerUid, 'spendCoins');

  const walletId = callerUid; // NEVER from client

  // Fraud risk check — must run before idempotency pre-check and DB transaction
  await enforceRiskCheck({
    walletId,
    userId:        callerUid,
    action:        'spendCoins',
    correlationId,
    itemId:        input.itemId,
    quantity:      input.quantity,
  });

  const requestHash = hashRequest({ itemId: input.itemId, quantity: input.quantity });

  // Fast pre-check: obvious replays and conflicts before touching the DB transaction
  const preCheck = await checkIdempotencyPreTx(walletId, input.idempotencyKey, requestHash, correlationId);
  if (preCheck.isReplay) {
    return preCheck.response;
  }

  try {
    const response = await db().runTransaction(async (tx) => {
      // ── Phase 1: All reads ───────────────────────────────────────────────────
      const [existingIdempotency, wallet, snapshot] = await Promise.all([
        IdempotencyRepository.findInTx(tx, walletId, input.idempotencyKey),
        WalletRepository.findByIdInTx(tx, walletId),
        SnapshotRepository.findByWalletIdInTx(tx, walletId),
      ]);

      // ── Phase 2: All writes ──────────────────────────────────────────────────

      // Idempotency guard — creates in_progress or returns replay
      const idempResult = applyIdempotencyInTx(
        tx, existingIdempotency, walletId, input.idempotencyKey,
        'spendCoins', requestHash, correlationId,
      );
      if (idempResult.isReplay) return idempResult.response;

      // Wallet state validation
      if (!wallet) throw walletError('WALLET_NOT_FOUND', correlationId);
      if (wallet.status === 'frozen')    throw walletError('WALLET_FROZEN', correlationId);
      if (wallet.status === 'suspended') throw walletError('WALLET_SUSPENDED', correlationId);
      if (!snapshot) throw walletError('WALLET_NOT_FOUND', correlationId);

      // Allocate spend across buckets (throws INSUFFICIENT_BALANCE if short)
      const entries = allocateSpend(input.quantity, snapshot, correlationId);
      const transactionId = crypto.randomUUID();

      // Ledger writes (snapshot update + transaction record + entries)
      LedgerService.applyInTx(tx, walletId, snapshot, {
        transactionId,
        type:          'spend',
        idempotencyKey: input.idempotencyKey,
        entries,
        metadata:      { itemId: input.itemId, quantitySpent: input.quantity, correlationId },
      });

      // Audit log (inside transaction — atomically with ledger)
      AuditLogService.recordInTx(tx, {
        actorId:        walletId,
        actorType:      'user',
        action:         'spend_coins',
        targetWalletId: walletId,
        correlationId,
        details:        { transactionId, itemId: input.itemId, quantitySpent: input.quantity },
      });

      const txResponse = {
        transactionId,
        coinsSpent: input.quantity,
        itemId:     input.itemId,
        correlationId,
      };

      // Mark idempotency key as completed (inside transaction — atomically)
      IdempotencyRepository.completeInTx(tx, walletId, input.idempotencyKey, transactionId, txResponse);

      return txResponse;
    });

    return response;
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    walletLog.error(
      { correlationId, action: 'spendCoins', walletId },
      'spendCoins unexpected error', { err },
    );
    throw walletError('WALLET_NOT_FOUND', correlationId);
  }
});

// ── getWalletTransactions ─────────────────────────────────────────────────────

/**
 * Whitelist-filter a LedgerTransactionDocument for safe client exposure.
 * Never leaks purchase tokens, provider IDs, fraud signals, or correlation IDs.
 */
function sanitizeTransactionForClient(doc: LedgerTransactionDocument): Record<string, unknown> {
  const safeMetadata: Record<string, unknown> = {};
  const meta = doc.metadata;

  if (doc.type === 'spend') {
    if (typeof meta['itemId'] === 'string')        safeMetadata['itemId']     = meta['itemId'];
    if (typeof meta['quantitySpent'] === 'number') safeMetadata['coinsSpent'] = meta['quantitySpent'];
  } else if (doc.type === 'purchase_credit' || doc.type === 'promotional_credit') {
    if (typeof meta['packId'] === 'string') safeMetadata['packId'] = meta['packId'];
  } else if (doc.type === 'refund' || doc.type === 'admin_adjustment') {
    if (typeof meta['reversesTransactionId'] === 'string') {
      safeMetadata['reversesTransactionId'] = meta['reversesTransactionId'];
    }
  }

  return {
    transactionId: doc.transactionId,
    type:          doc.type,
    createdAt:     doc.createdAt,
    metadata:      safeMetadata,
  };
}

/**
 * Returns the caller's confirmed ledger transactions, paginated, most-recent-first.
 *
 * Security:
 *  - walletId always from callerUid
 *  - metadata sanitized: only whitelisted fields per transaction type
 *  - No purchase tokens, fraud signals, or correlation IDs leaked
 */
export const getWalletTransactions = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);

  const input = validateGetTransactionsInput(data);
  const correlationId = resolveCorrelationId(input);

  await checkRateLimitFailClosed(callerUid, 'getBalance');

  const walletId = callerUid; // NEVER from client
  const limit = input.limit ?? 20;

  try {
    const docs = await LedgerTransactionRepository.listByWalletId(walletId, limit, input.cursor);
    const items = docs.map(sanitizeTransactionForClient);
    const nextCursor = items.length === limit
      ? (items[items.length - 1]['transactionId'] as string)
      : null;

    return { items, nextCursor, correlationId };
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    walletLog.error(
      { correlationId, action: 'getWalletTransactions', walletId },
      'getWalletTransactions unexpected error', { err },
    );
    throw walletError('WALLET_NOT_FOUND', correlationId);
  }
});

// ── initiatePurchase ──────────────────────────────────────────────────────────

/**
 * Create a pending purchase record and return a sandbox token.
 *
 * Security properties:
 *  - walletId = callerUid (NEVER from client)
 *  - packId validated against server-side PACK_CATALOG; coinsAmount from catalog only
 *  - Sandbox token (UUID) generated server-side; client never controls it
 *  - Token stored as SHA-256 hash only; raw token returned once, never stored
 *  - Full idempotency: same key+body → same response; different body → conflict
 *  - Wallet status validated before any write
 */
export const initiatePurchase = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  requireEmailVerified(context);

  const input = validateInitiatePurchaseInput(data);
  const correlationId = resolveCorrelationId(input);
  assertSandboxWalletAllowed(correlationId);

  await checkRateLimitFailClosed(callerUid, 'initiatePurchase');

  const walletId = callerUid; // NEVER from client

  // Fraud risk check — before idempotency pre-check and DB transaction
  await enforceRiskCheck({
    walletId,
    userId:        callerUid,
    action:        'initiatePurchase',
    correlationId,
    packId:        input.packId,
  });

  const requestHash = hashRequest({ packId: input.packId });

  const preCheck = await checkIdempotencyPreTx(walletId, input.idempotencyKey, requestHash, correlationId);
  if (preCheck.isReplay) return preCheck.response;

  const pack = PACK_CATALOG[input.packId]; // validated by validateInitiatePurchaseInput; from server catalog
  const purchaseId   = crypto.randomUUID();
  const sandboxToken = 'sandbox_' + crypto.randomUUID();
  const tokenHash    = hashPurchaseToken(sandboxToken);

  try {
    const response = await db().runTransaction(async (tx) => {
      // ── Phase 1: All reads ───────────────────────────────────────────────────
      const [existingIdempotency, wallet] = await Promise.all([
        IdempotencyRepository.findInTx(tx, walletId, input.idempotencyKey),
        WalletRepository.findByIdInTx(tx, walletId),
      ]);

      // ── Phase 2: All writes ──────────────────────────────────────────────────

      const idempResult = applyIdempotencyInTx(
        tx, existingIdempotency, walletId, input.idempotencyKey,
        'initiatePurchase', requestHash, correlationId,
      );
      if (idempResult.isReplay) return idempResult.response;

      if (!wallet)                       throw walletError('WALLET_NOT_FOUND', correlationId);
      if (wallet.status === 'frozen')    throw walletError('WALLET_FROZEN', correlationId);
      if (wallet.status === 'suspended') throw walletError('WALLET_SUSPENDED', correlationId);

      PurchaseRepository.createInTx(tx, {
        purchaseId,
        walletId,
        userId:                    walletId,
        provider:                  'sandbox',
        providerPurchaseTokenHash: tokenHash,
        providerTransactionId:     null,
        packId:                    input.packId,
        coinsAmount:               pack.coins, // from server catalog — NEVER from client
        priceCents:                0,
        currency:                  'SANDBOX',
        status:                    'initiated',
        ledgerTransactionId:       null,
      });

      AuditLogService.recordInTx(tx, {
        actorId:        walletId,
        actorType:      'user',
        action:         'purchase_initiated',
        targetWalletId: walletId,
        correlationId,
        details:        { purchaseId, packId: input.packId, coinsAmount: pack.coins },
      });

      const txResponse = {
        purchaseId,
        sandboxToken, // raw token returned once — not stored anywhere, not logged
        packId:       input.packId,
        coinsAmount:  pack.coins,
        correlationId,
      };

      IdempotencyRepository.completeInTx(tx, walletId, input.idempotencyKey, purchaseId, txResponse);

      return txResponse;
    });

    return response;
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    walletLog.error(
      { correlationId, action: 'initiatePurchase', walletId },
      'initiatePurchase unexpected error', { err },
    );
    throw walletError('WALLET_NOT_FOUND', correlationId);
  }
});

// ── verifyPurchase ────────────────────────────────────────────────────────────

/**
 * Verify a sandbox purchase and credit coins to the caller's wallet.
 *
 * Security properties:
 *  - walletId = callerUid (NEVER from client)
 *  - Cross-user protection: purchase.walletId !== callerUid → PURCHASE_NOT_FOUND
 *  - coinsToCredit from server-side PACK_CATALOG via payment provider, never from client
 *  - Double-credit protection: purchase status must be 'initiated' to proceed
 *  - Full idempotency: same key+body → cached response
 *  - All writes atomic in one Firestore transaction
 *  - Audit log appended inside the same transaction
 */
export const verifyPurchase = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  requireEmailVerified(context);

  const input = validateVerifyPurchaseInput(data);
  const correlationId = resolveCorrelationId(input);
  assertSandboxWalletAllowed(correlationId);

  await checkRateLimitFailClosed(callerUid, 'verifyPurchase');

  const walletId = callerUid; // NEVER from client

  // Fraud risk check — before idempotency pre-check and DB transaction
  await enforceRiskCheck({
    walletId,
    userId:        callerUid,
    action:        'verifyPurchase',
    correlationId,
  });

  const requestHash = hashRequest({
    sandboxPurchaseId: input.sandboxPurchaseId,
    sandboxToken: input.sandboxToken,
  });

  const preCheck = await checkIdempotencyPreTx(walletId, input.idempotencyKey, requestHash, correlationId);
  if (preCheck.isReplay) return preCheck.response;

  // Pre-read outside transaction for fast rejection before holding a Firestore lock
  const purchase = await PurchaseRepository.findById(input.sandboxPurchaseId);
  if (!purchase || purchase.walletId !== walletId) {
    throw walletError('PURCHASE_NOT_FOUND', correlationId); // hides existence on cross-user access
  }
  if (purchase.status === 'credited') {
    throw walletError('PURCHASE_ALREADY_CREDITED', correlationId);
  }
  if (!timingSafeStringEquals(hashPurchaseToken(input.sandboxToken), purchase.providerPurchaseTokenHash)) {
    throw walletError('PURCHASE_NOT_FOUND', correlationId);
  }

  // Sandbox verification — no external network; validates packId from server catalog
  const provider = getPaymentProvider('sandbox', correlationId);
  const verifyResult = await provider.verify({
    purchaseId:            purchase.purchaseId,
    walletId,
    packId:                purchase.packId,
    providerPurchaseToken: input.sandboxToken,
    correlationId,
  });

  if (!verifyResult.isValid) {
    throw walletError('PURCHASE_NOT_FOUND', correlationId);
  }

  const transactionId = crypto.randomUUID();

  try {
    const response = await db().runTransaction(async (tx) => {
      // ── Phase 1: All reads ───────────────────────────────────────────────────
      const [existingIdempotency, wallet, snapshot, txPurchase] = await Promise.all([
        IdempotencyRepository.findInTx(tx, walletId, input.idempotencyKey),
        WalletRepository.findByIdInTx(tx, walletId),
        SnapshotRepository.findByWalletIdInTx(tx, walletId),
        PurchaseRepository.findByIdInTx(tx, input.sandboxPurchaseId),
      ]);

      // ── Phase 2: All writes ──────────────────────────────────────────────────

      const idempResult = applyIdempotencyInTx(
        tx, existingIdempotency, walletId, input.idempotencyKey,
        'verifyPurchase', requestHash, correlationId,
      );
      if (idempResult.isReplay) return idempResult.response;

      if (!wallet)                       throw walletError('WALLET_NOT_FOUND', correlationId);
      if (wallet.status === 'frozen')    throw walletError('WALLET_FROZEN', correlationId);
      if (wallet.status === 'suspended') throw walletError('WALLET_SUSPENDED', correlationId);
      if (!snapshot)                     throw walletError('WALLET_NOT_FOUND', correlationId);

      // Re-validate inside transaction — double-credit protection
      if (!txPurchase || txPurchase.walletId !== walletId) throw walletError('PURCHASE_NOT_FOUND', correlationId);
      if (txPurchase.status === 'credited') throw walletError('PURCHASE_ALREADY_CREDITED', correlationId);

      const entries: LedgerEntrySpec[] = [
        { bucket: 'purchased', amount: verifyResult.coinsToCredit },
      ];

      LedgerService.applyInTx(tx, walletId, snapshot, {
        transactionId,
        type:           'purchase_credit',
        idempotencyKey: input.idempotencyKey,
        entries,
        metadata: {
          purchaseId:   purchase.purchaseId,
          packId:       purchase.packId,
          coinsAmount:  verifyResult.coinsToCredit,
          provider:     'sandbox',
          correlationId,
        },
      });

      PurchaseRepository.updateStatusInTx(tx, purchase.purchaseId, 'credited', transactionId);

      AuditLogService.recordInTx(tx, {
        actorId:        walletId,
        actorType:      'user',
        action:         'purchase_credited',
        targetWalletId: walletId,
        correlationId,
        details:        { transactionId, purchaseId: purchase.purchaseId, coinsReceived: verifyResult.coinsToCredit },
      });

      const txResponse = {
        transactionId,
        coinsReceived: verifyResult.coinsToCredit,
        purchaseId:    purchase.purchaseId,
        correlationId,
      };

      IdempotencyRepository.completeInTx(tx, walletId, input.idempotencyKey, transactionId, txResponse);

      return txResponse;
    });

    return response;
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    walletLog.error(
      { correlationId, action: 'verifyPurchase', walletId },
      'verifyPurchase unexpected error', { err },
    );
    throw walletError('WALLET_NOT_FOUND', correlationId);
  }
});
