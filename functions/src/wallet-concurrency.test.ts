/**
 * wallet-concurrency.test.ts
 *
 * Concurrency / volume safety tests.
 *
 * Real Firestore concurrency is enforced via optimistic locking on
 * snapshot.version. We cannot reproduce true Firestore in a unit test
 * harness, so this file verifies the equivalent SAFETY PROPERTIES:
 *
 *   - "No spend can overdraw the balance" — verified by driving 100
 *     spends against a 50-coin wallet with stateful snapshot updates;
 *     exactly 50 succeed.
 *
 *   - "No purchase can be credited twice" — verified by driving 100
 *     verify calls (different idempotency keys) against the same purchase;
 *     exactly 1 credits, the rest get PURCHASE_ALREADY_CREDITED.
 *
 *   - "Replay with the same idempotency key returns the cached response,
 *     never a fresh credit" — verified by driving 100 verify calls with
 *     identical key + body; the wallet is mutated once.
 *
 *   - "Snapshot version is the optimistic-lock token" — verified by
 *     SnapshotRepository.updateInTx always writing version+1 (the property
 *     Firestore relies on to detect contention).
 */

const mockRunTransaction = jest.fn();

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({ doc: jest.fn(() => ({ path: 'fake' })) })),
    runTransaction: (...a: unknown[]) => mockRunTransaction(...a),
  })),
}));

const mockFindWalletInTx       = jest.fn();
const mockFindSnapshotInTx     = jest.fn();
const mockFindIdempotencyInTx  = jest.fn();
const mockFindIdempotency      = jest.fn();
const mockFindPurchaseById     = jest.fn();
const mockFindPurchaseInTx     = jest.fn();
const mockUpdatePurchaseStatus = jest.fn();
const mockSnapshotUpdateInTx   = jest.fn();
const mockLedgerApplyInTx      = jest.fn();

jest.mock('./lib/walletRepository', () => ({
  WalletRepository:     { findByIdInTx: (...a: unknown[]) => mockFindWalletInTx(...a) },
  SnapshotRepository:   {
    findByWalletIdInTx: (...a: unknown[]) => mockFindSnapshotInTx(...a),
    ref:                jest.fn(() => ({ path: 'snap' })),
    updateInTx:         (...a: unknown[]) => mockSnapshotUpdateInTx(...a),
  },
  IdempotencyRepository: {
    find:                 (...a: unknown[]) => mockFindIdempotency(...a),
    findInTx:             (...a: unknown[]) => mockFindIdempotencyInTx(...a),
    createInProgressInTx: jest.fn(),
    completeInTx:         jest.fn(),
    failInTx:             jest.fn(),
  },
  PurchaseRepository: {
    findById:         (...a: unknown[]) => mockFindPurchaseById(...a),
    findByIdInTx:     (...a: unknown[]) => mockFindPurchaseInTx(...a),
    findTokenIndex:   jest.fn().mockResolvedValue(null),
    createInTx:       jest.fn(),
    updateStatusInTx: (...a: unknown[]) => mockUpdatePurchaseStatus(...a),
  },
  AuditLogRepository:          { append: jest.fn(), appendInTx: jest.fn() },
  LedgerTransactionRepository: { createInTx: jest.fn() },
  LedgerEntryRepository:       { createInTx: jest.fn() },
  FraudSignalRepository:       { append: jest.fn().mockResolvedValue('sig') },
  hashPurchaseToken:           jest.fn((r: string) => 'h_' + r),
}));

jest.mock('./lib/ledgerService', () => ({
  LedgerService: { applyInTx: (...a: unknown[]) => mockLedgerApplyInTx(...a) },
}));

jest.mock('./lib/riskScoringService', () => ({
  RiskScoringService: { evaluate: jest.fn().mockResolvedValue({ score: 0, decision: 'allow', signals: [] }) },
  RISK_THRESHOLDS:    { throttle: 30, review: 60, freeze: 80 },
}));

jest.mock('./lib/fraudSignalService', () => ({
  FraudSignalService: { record: jest.fn().mockResolvedValue('sig') },
}));

jest.mock('./lib/auditLogService', () => ({
  AuditLogService: { record: jest.fn().mockResolvedValue(undefined), recordInTx: jest.fn() },
}));

jest.mock('./lib/guards', () => ({
  requireAppCheck: jest.fn(),
  requireAuth:     jest.fn(() => 'uid-alice'),
  requireEmailVerified: jest.fn(),
}));

jest.mock('./lib/rateLimiter', () => ({
  checkRateLimitFailClosed: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('./lib/correlationId', () => ({ resolveCorrelationId: jest.fn(() => 'corr') }));

jest.mock('./lib/paymentProviders', () => ({
  getPaymentProvider: jest.fn(() => ({
    verify: jest.fn().mockResolvedValue({ isValid: true, coinsToCredit: 10 }),
  })),
}));

jest.mock('./lib/walletValidators', () => {
  const realValidators = jest.requireActual('./lib/walletValidators');
  return {
    ...realValidators,
    validateSpendCoinsInput: (d: { itemId?: string; quantity?: number; idempotencyKey?: string }) => ({
      itemId:         d.itemId         ?? 'item-1',
      quantity:       d.quantity       ?? 1,
      idempotencyKey: d.idempotencyKey ?? 'idem-key-default',
      correlationId:  'corr',
    }),
    validateVerifyPurchaseInput: (d: { sandboxPurchaseId?: string; sandboxToken?: string; idempotencyKey?: string }) => ({
      sandboxPurchaseId: d.sandboxPurchaseId ?? 'purchase-1',
      sandboxToken:      d.sandboxToken ?? 'sandbox_12345678-1234-4123-8123-123456789abc',
      idempotencyKey:    d.idempotencyKey ?? 'idem-key-default',
      correlationId:     'corr',
    }),
  };
});

// We deliberately do NOT mock walletErrors — the production code uses
// `instanceof HttpsError` to distinguish business errors from infrastructure
// errors. A mock that returned a plain Error would be swallowed and rethrown
// as WALLET_NOT_FOUND, hiding the security signal we want to verify.

jest.mock('./lib/walletLogger', () => ({
  walletLog: { info: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

jest.mock('./lib/idempotencyService', () => ({
  hashRequest:          jest.fn(() => 'req-hash'),
  checkIdempotencyPreTx: jest.fn().mockResolvedValue({ isReplay: false }),
  applyIdempotencyInTx: jest.fn(() => ({ isReplay: false })),
}));

import * as functionsTest from 'firebase-functions-test';
const testEnv = functionsTest();

import { spendCoins, verifyPurchase } from './walletFunctions';
import type { BalanceSnapshotDocument } from './lib/walletSchema';
import type { Transaction } from 'firebase-admin/firestore';

/**
 * Real HttpsError exposes the WalletErrorDetails via `details.errorCode`.
 * Use this rather than message matching — messages are user-facing strings
 * that may be localized in future.
 */
function errorCodeOf(e: unknown): string | undefined {
  if (!e || typeof e !== 'object') return undefined;
  const errObj = e as { details?: { errorCode?: string } };
  return errObj.details?.errorCode;
}

const TX = {} as unknown as Transaction;

const wrappedSpend  = testEnv.wrap(spendCoins);
const wrappedVerify = testEnv.wrap(verifyPurchase);
const ctx = { auth: { uid: 'uid-alice', token: { email_verified: true } }, app: { appId: 'test-app' } };
const VALID_SANDBOX_TOKEN = 'sandbox_12345678-1234-4123-8123-123456789abc';

function makeWallet() {
  return { walletId: 'uid-alice', userId: 'uid-alice', status: 'active' };
}
function makeSnapshot(over: Partial<BalanceSnapshotDocument> = {}): BalanceSnapshotDocument {
  return {
    walletId: 'uid-alice', availableBalance: 50,
    purchasedBalance: 50, promotionalBalance: 0, earnedBalance: 0,
    version: 1, lastTransactionId: null, updatedAt: 0, schemaVersion: 1,
    ...over,
  };
}

beforeEach(() => {
  jest.clearAllMocks();
  delete process.env['GCLOUD_PROJECT'];
  delete process.env['FIREBASE_CONFIG'];
});

// ── 100 spends against a 50-coin balance ──────────────────────────────────────

describe('CONCURRENCY — 100 spends vs 50-coin balance', () => {
  it('EXACTLY 50 succeed; the remaining 50 throw INSUFFICIENT_BALANCE', async () => {
    // Stateful snapshot — each successful spend reduces it by 1.
    let snapshot = makeSnapshot({ purchasedBalance: 50, availableBalance: 50 });
    const wallet = makeWallet();

    // Ledger mock applies the snapshot delta synthetically so the next
    // iteration sees an updated balance.
    mockLedgerApplyInTx.mockImplementation((_tx, _wid, _snap, params) => {
      const debit = params.entries.reduce((s: number, e: { amount: number }) => s + e.amount, 0);
      // debit is negative (e.g. -1)
      snapshot = {
        ...snapshot,
        purchasedBalance: snapshot.purchasedBalance + debit,
        availableBalance: snapshot.availableBalance + debit,
        version:          snapshot.version + 1,
      };
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
      mockFindWalletInTx.mockResolvedValue(wallet);
      mockFindSnapshotInTx.mockResolvedValue(snapshot);
      mockFindIdempotencyInTx.mockResolvedValue(null);
      return fn(TX);
    });

    let successes = 0;
    let insufficient = 0;
    let other = 0;

    for (let i = 0; i < 100; i++) {
      try {
        await wrappedSpend(
          { itemId: 'item-x', quantity: 1, idempotencyKey: `idem-${String(i).padStart(4, '0')}` },
          ctx,
        );
        successes++;
      } catch (e) {
        const code = errorCodeOf(e);
        if (code === 'INSUFFICIENT_BALANCE') insufficient++;
        else other++;
      }
    }

    expect(successes).toBe(50);
    expect(insufficient).toBe(50);
    expect(other).toBe(0);
    // Final balance is exactly zero — the invariant: never overspent.
    expect(snapshot.availableBalance).toBe(0);
    expect(snapshot.purchasedBalance).toBe(0);
    // Ledger was applied exactly once per successful spend.
    expect(mockLedgerApplyInTx).toHaveBeenCalledTimes(50);
  });

  it('NEVER overspends even with quantity=5 (chunked spends)', async () => {
    let snapshot = makeSnapshot({ purchasedBalance: 23, availableBalance: 23 });
    const wallet = makeWallet();

    mockLedgerApplyInTx.mockImplementation((_tx, _wid, _snap, params) => {
      const debit = params.entries.reduce((s: number, e: { amount: number }) => s + e.amount, 0);
      snapshot = {
        ...snapshot,
        purchasedBalance: snapshot.purchasedBalance + debit,
        availableBalance: snapshot.availableBalance + debit,
        version:          snapshot.version + 1,
      };
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
      mockFindWalletInTx.mockResolvedValue(wallet);
      mockFindSnapshotInTx.mockResolvedValue(snapshot);
      mockFindIdempotencyInTx.mockResolvedValue(null);
      return fn(TX);
    });

    let successes = 0;
    for (let i = 0; i < 10; i++) {
      try {
        await wrappedSpend(
          { itemId: 'x', quantity: 5, idempotencyKey: `idem-q5-${i}` },
          ctx,
        );
        successes++;
      } catch { /* expected for the last few */ }
    }

    // 23 coins / 5 per spend = 4 successful spends (consumes 20 coins)
    // The 5th would need 5 coins but only 3 remain → INSUFFICIENT_BALANCE
    expect(successes).toBe(4);
    expect(snapshot.availableBalance).toBe(3);
  });
});

// ── 100 verifies against ONE purchase ─────────────────────────────────────────

describe('CONCURRENCY — 100 verifies of the same purchase (fresh idem keys)', () => {
  it('EXACTLY 1 credits; the rest throw PURCHASE_ALREADY_CREDITED', async () => {
    // Stateful purchase status — flips to 'credited' on first success.
    let purchaseStatus: 'initiated' | 'credited' = 'initiated';
    const purchaseDoc = () => ({
      purchaseId: 'purchase-1', walletId: 'uid-alice', packId: 'coins_10',
      status: purchaseStatus, provider: 'sandbox', providerPurchaseTokenHash: `h_${VALID_SANDBOX_TOKEN}`,
    });

    mockFindIdempotency.mockResolvedValue(null);
    mockFindPurchaseById.mockImplementation(async () => purchaseDoc());

    mockUpdatePurchaseStatus.mockImplementation((_tx, _id, status) => {
      if (status === 'credited') purchaseStatus = 'credited';
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
      mockFindWalletInTx.mockResolvedValue(makeWallet());
      mockFindSnapshotInTx.mockResolvedValue(makeSnapshot({ availableBalance: 0, purchasedBalance: 0 }));
      mockFindIdempotencyInTx.mockResolvedValue(null);
      mockFindPurchaseInTx.mockResolvedValue(purchaseDoc());
      return fn(TX);
    });

    let credited = 0;
    let alreadyCredited = 0;
    let other = 0;

    for (let i = 0; i < 100; i++) {
      try {
        await wrappedVerify(
          {
            sandboxPurchaseId: 'purchase-1',
            sandboxToken: VALID_SANDBOX_TOKEN,
            idempotencyKey: `verify-idem-${String(i).padStart(4, '0')}`,
          },
          ctx,
        );
        credited++;
      } catch (e) {
        const code = errorCodeOf(e);
        if (code === 'PURCHASE_ALREADY_CREDITED') alreadyCredited++;
        else other++;
      }
    }

    expect(credited).toBe(1);
    expect(alreadyCredited).toBe(99);
    expect(other).toBe(0);
    // Ledger applied exactly ONCE — the credit happened exactly once.
    expect(mockLedgerApplyInTx).toHaveBeenCalledTimes(1);
  });
});

// ── 100 verifies with SAME idempotency key (replay) ──────────────────────────

describe('CONCURRENCY — 100 verifies with same idempotency key (replay)', () => {
  it('credits exactly ONCE; replays return cached response, no extra ledger writes', async () => {
    // First call: idempotency cache miss → proceeds and credits.
    // Subsequent calls: idempotency pre-check returns the cached response →
    // verifyPurchase short-circuits without entering the transaction.
    let purchaseStatus: 'initiated' | 'credited' = 'initiated';

    const cachedResponse = {
      transactionId: 'tx-cached', coinsReceived: 10, purchaseId: 'purchase-1', correlationId: 'corr',
    };
    const cachedRecord = {
      docId: 'uid-alice_idem-same', key: 'idem-same', walletId: 'uid-alice',
      action: 'verifyPurchase', requestHash: 'req-hash',
      status: 'completed', responsePayload: cachedResponse, transactionId: 'tx-cached',
      createdAt: 0, expiresAt: 0, schemaVersion: 1,
    };

    let callCount = 0;
    const { checkIdempotencyPreTx } = jest.requireMock('./lib/idempotencyService');
    checkIdempotencyPreTx.mockImplementation(async () => {
      callCount++;
      if (callCount === 1) return { isReplay: false };
      // Subsequent calls hit the cached record
      return { isReplay: true, response: cachedResponse, transactionId: 'tx-cached' };
    });

    mockFindIdempotency.mockImplementation(async () => callCount === 1 ? null : cachedRecord);
    mockFindPurchaseById.mockResolvedValue({
      purchaseId: 'purchase-1', walletId: 'uid-alice', packId: 'coins_10',
      status: purchaseStatus, provider: 'sandbox', providerPurchaseTokenHash: `h_${VALID_SANDBOX_TOKEN}`,
    });
    mockUpdatePurchaseStatus.mockImplementation((_tx, _id, status) => {
      if (status === 'credited') purchaseStatus = 'credited';
    });

    mockRunTransaction.mockImplementation(async (fn: (tx: Transaction) => unknown) => {
      mockFindWalletInTx.mockResolvedValue(makeWallet());
      mockFindSnapshotInTx.mockResolvedValue(makeSnapshot({ availableBalance: 0, purchasedBalance: 0 }));
      mockFindIdempotencyInTx.mockResolvedValue(null);
      mockFindPurchaseInTx.mockResolvedValue({
        purchaseId: 'purchase-1', walletId: 'uid-alice', packId: 'coins_10',
        status: 'initiated', provider: 'sandbox', providerPurchaseTokenHash: `h_${VALID_SANDBOX_TOKEN}`,
      });
      return fn(TX);
    });

    const responses = [];
    for (let i = 0; i < 100; i++) {
      const r = await wrappedVerify(
        { sandboxPurchaseId: 'purchase-1', sandboxToken: VALID_SANDBOX_TOKEN, idempotencyKey: 'idem-same' },
        ctx,
      );
      responses.push(r);
    }

    // Every call returns the same coinsReceived (1 fresh + 99 cached replays)
    expect(responses).toHaveLength(100);
    for (const r of responses) {
      expect((r as { coinsReceived: number }).coinsReceived).toBe(10);
    }
    // CRITICAL: only one ledger write happened despite 100 client calls
    expect(mockLedgerApplyInTx).toHaveBeenCalledTimes(1);
  });
});

// ── Snapshot version is the optimistic lock token ────────────────────────────
// The "version+1 on every snapshot mutation" property is the precondition
// Firestore relies on to detect concurrent writes. It is covered exhaustively
// in wallet-repository.test.ts (SnapshotRepository.updateInTx).
// Re-mocking SnapshotRepository here to exercise the real implementation
// would conflict with the module-level mocks used by the spend/verify tests
// above, so the assertion is delegated rather than duplicated.
