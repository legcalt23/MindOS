/**
 * walletRepository.ts
 * Firestore data-access layer for wallet collections.
 *
 * Security contract:
 *  - No method mutates ledger_transactions or ledger_entries (append-only).
 *  - No method accepts userId from external input; callers pass verified uid.
 *  - Wallet and snapshot reads inside a Firestore transaction use tx.get()
 *    to participate in optimistic locking.
 *  - All writes use Admin SDK (bypasses Firestore Rules as expected).
 */

import * as admin from 'firebase-admin';
import * as crypto from 'crypto';
import {
  COLLECTIONS,
  SCHEMA_VERSION,
  WalletDocument,
  WalletStatus,
  BalanceSnapshotDocument,
  LedgerTransactionDocument,
  LedgerTransactionType,
  LedgerTransactionStatus,
  LedgerEntryDocument,
  LedgerBucket,
  LedgerEntryType,
  IdempotencyKeyDocument,
  IdempotencyStatus,
  PurchaseDocument,
  PurchaseProvider,
  PurchaseStatus,
  PurchaseTokenIndexDocument,
  WalletAuditLogDocument,
  AuditActorType,
  FraudSignalDocument,
  FraudSignalSeverity,
  FraudSignalSource,
  AdminActionDocument,
  AdminActionStatus,
} from './walletSchema';

const db = () => admin.firestore();

// ── Helpers ───────────────────────────────────────────────────────────────────

function now(): number {
  return Date.now();
}

function idempotencyDocId(walletId: string, key: string): string {
  return `${walletId}_${key}`;
}

function purchaseTokenIndexDocId(provider: PurchaseProvider, tokenHash: string): string {
  return `${provider}_${tokenHash}`;
}

export function hashPurchaseToken(rawToken: string): string {
  return crypto.createHash('sha256').update(rawToken).digest('hex');
}

// ── Wallet Repository ─────────────────────────────────────────────────────────

export const WalletRepository = {

  ref(walletId: string) {
    return db().collection(COLLECTIONS.WALLETS).doc(walletId);
  },

  async findById(walletId: string): Promise<WalletDocument | null> {
    const snap = await WalletRepository.ref(walletId).get();
    return snap.exists ? (snap.data() as WalletDocument) : null;
  },

  async findByIdInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
  ): Promise<WalletDocument | null> {
    const snap = await tx.get(WalletRepository.ref(walletId));
    return snap.exists ? (snap.data() as WalletDocument) : null;
  },

  async create(walletId: string): Promise<WalletDocument> {
    const ts = now();
    const doc: WalletDocument = {
      walletId,
      userId: walletId,
      status: 'active',
      createdAt: ts,
      updatedAt: ts,
      schemaVersion: SCHEMA_VERSION,
    };
    await WalletRepository.ref(walletId).set(doc);
    return doc;
  },

  async updateStatusInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    status: WalletStatus,
  ): Promise<void> {
    tx.update(WalletRepository.ref(walletId), { status, updatedAt: now() });
  },
};

// ── Balance Snapshot Repository ───────────────────────────────────────────────

export const SnapshotRepository = {

  ref(walletId: string) {
    return db().collection(COLLECTIONS.BALANCE_SNAPSHOTS).doc(walletId);
  },

  async findByWalletId(walletId: string): Promise<BalanceSnapshotDocument | null> {
    const snap = await SnapshotRepository.ref(walletId).get();
    return snap.exists ? (snap.data() as BalanceSnapshotDocument) : null;
  },

  async findByWalletIdInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
  ): Promise<BalanceSnapshotDocument | null> {
    const snap = await tx.get(SnapshotRepository.ref(walletId));
    return snap.exists ? (snap.data() as BalanceSnapshotDocument) : null;
  },

  async create(walletId: string): Promise<BalanceSnapshotDocument> {
    const doc: BalanceSnapshotDocument = {
      walletId,
      availableBalance: 0,
      purchasedBalance: 0,
      promotionalBalance: 0,
      earnedBalance: 0,
      version: 0,
      lastTransactionId: null,
      updatedAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    await SnapshotRepository.ref(walletId).set(doc);
    return doc;
  },

  updateInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    delta: {
      purchasedDelta?: number;
      promotionalDelta?: number;
      earnedDelta?: number;
    },
    currentSnapshot: BalanceSnapshotDocument,
    lastTransactionId: string,
  ): void {
    const purchased    = (currentSnapshot.purchasedBalance    + (delta.purchasedDelta    ?? 0));
    const promotional  = (currentSnapshot.promotionalBalance  + (delta.promotionalDelta  ?? 0));
    const earned       = (currentSnapshot.earnedBalance       + (delta.earnedDelta       ?? 0));
    const available    = purchased + promotional + earned;

    if (purchased < 0 || promotional < 0 || earned < 0 || available < 0) {
      throw new Error('INSUFFICIENT_BALANCE');
    }

    tx.update(SnapshotRepository.ref(walletId), {
      purchasedBalance:   purchased,
      promotionalBalance: promotional,
      earnedBalance:      earned,
      availableBalance:   available,
      version:            currentSnapshot.version + 1,
      lastTransactionId,
      updatedAt:          now(),
    });
  },
};

// ── Ledger Transaction Repository ─────────────────────────────────────────────

export const LedgerTransactionRepository = {

  ref(transactionId: string) {
    return db().collection(COLLECTIONS.LEDGER_TRANSACTIONS).doc(transactionId);
  },

  createInTx(
    tx: admin.firestore.Transaction,
    doc: Omit<LedgerTransactionDocument, 'schemaVersion' | 'createdAt'>,
  ): void {
    const full: LedgerTransactionDocument = {
      ...doc,
      createdAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    tx.set(LedgerTransactionRepository.ref(doc.transactionId), full);
  },

  async findById(transactionId: string): Promise<LedgerTransactionDocument | null> {
    const snap = await LedgerTransactionRepository.ref(transactionId).get();
    return snap.exists ? (snap.data() as LedgerTransactionDocument) : null;
  },

  async listByWalletId(
    walletId: string,
    limit: number,
    startAfter?: string,
  ): Promise<LedgerTransactionDocument[]> {
    let q = db()
      .collection(COLLECTIONS.LEDGER_TRANSACTIONS)
      .where('walletId', '==', walletId)
      .where('status', '==', 'confirmed' as LedgerTransactionStatus)
      .orderBy('createdAt', 'desc')
      .limit(limit);

    if (startAfter) {
      const cursorSnap = await LedgerTransactionRepository.ref(startAfter).get();
      if (cursorSnap.exists) q = q.startAfter(cursorSnap);
    }

    const snaps = await q.get();
    return snaps.docs.map((d) => d.data() as LedgerTransactionDocument);
  },
};

// ── Ledger Entry Repository ───────────────────────────────────────────────────

export const LedgerEntryRepository = {

  ref(entryId: string) {
    return db().collection(COLLECTIONS.LEDGER_ENTRIES).doc(entryId);
  },

  createInTx(
    tx: admin.firestore.Transaction,
    doc: Omit<LedgerEntryDocument, 'schemaVersion' | 'createdAt'>,
  ): void {
    if (doc.amount === 0) throw new Error('Ledger entry amount must not be zero.');
    const full: LedgerEntryDocument = {
      ...doc,
      createdAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    tx.set(LedgerEntryRepository.ref(doc.entryId), full);
  },

  async listByTransactionId(transactionId: string): Promise<LedgerEntryDocument[]> {
    const snaps = await db()
      .collection(COLLECTIONS.LEDGER_ENTRIES)
      .where('transactionId', '==', transactionId)
      .get();
    return snaps.docs.map((d) => d.data() as LedgerEntryDocument);
  },

  async listByWalletId(walletId: string): Promise<LedgerEntryDocument[]> {
    const snaps = await db()
      .collection(COLLECTIONS.LEDGER_ENTRIES)
      .where('walletId', '==', walletId)
      .where('amount', '!=', 0)
      .orderBy('amount')
      .orderBy('createdAt', 'asc')
      .get();
    return snaps.docs.map((d) => d.data() as LedgerEntryDocument);
  },
};

// ── Idempotency Key Repository ────────────────────────────────────────────────

export const IdempotencyRepository = {

  ref(walletId: string, key: string) {
    return db().collection(COLLECTIONS.IDEMPOTENCY_KEYS).doc(idempotencyDocId(walletId, key));
  },

  async find(walletId: string, key: string): Promise<IdempotencyKeyDocument | null> {
    const snap = await IdempotencyRepository.ref(walletId, key).get();
    return snap.exists ? (snap.data() as IdempotencyKeyDocument) : null;
  },

  async findInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    key: string,
  ): Promise<IdempotencyKeyDocument | null> {
    const snap = await tx.get(IdempotencyRepository.ref(walletId, key));
    return snap.exists ? (snap.data() as IdempotencyKeyDocument) : null;
  },

  createInProgressInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    key: string,
    action: string,
    requestHash: string,
  ): void {
    const ts = now();
    const doc: IdempotencyKeyDocument = {
      docId: idempotencyDocId(walletId, key),
      key,
      walletId,
      action,
      requestHash,
      status: 'in_progress',
      responsePayload: null,
      transactionId: null,
      createdAt: ts,
      expiresAt: ts + 24 * 60 * 60 * 1000,
      schemaVersion: SCHEMA_VERSION,
    };
    tx.set(IdempotencyRepository.ref(walletId, key), doc);
  },

  completeInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    key: string,
    transactionId: string,
    responsePayload: unknown,
  ): void {
    tx.update(IdempotencyRepository.ref(walletId, key), {
      status: 'completed' as IdempotencyStatus,
      transactionId,
      responsePayload,
    });
  },

  failInTx(
    tx: admin.firestore.Transaction,
    walletId: string,
    key: string,
  ): void {
    tx.update(IdempotencyRepository.ref(walletId, key), {
      status: 'failed' as IdempotencyStatus,
    });
  },
};

// ── Purchase Repository ───────────────────────────────────────────────────────

export const PurchaseRepository = {

  ref(purchaseId: string) {
    return db().collection(COLLECTIONS.PURCHASES).doc(purchaseId);
  },

  tokenIndexRef(provider: PurchaseProvider, tokenHash: string) {
    return db()
      .collection(COLLECTIONS.PURCHASE_TOKEN_INDEX)
      .doc(purchaseTokenIndexDocId(provider, tokenHash));
  },

  async findById(purchaseId: string): Promise<PurchaseDocument | null> {
    const snap = await PurchaseRepository.ref(purchaseId).get();
    return snap.exists ? (snap.data() as PurchaseDocument) : null;
  },

  async findByIdInTx(
    tx: admin.firestore.Transaction,
    purchaseId: string,
  ): Promise<PurchaseDocument | null> {
    const snap = await tx.get(PurchaseRepository.ref(purchaseId));
    return snap.exists ? (snap.data() as PurchaseDocument) : null;
  },

  async findTokenIndex(
    tx: admin.firestore.Transaction,
    provider: PurchaseProvider,
    tokenHash: string,
  ): Promise<PurchaseTokenIndexDocument | null> {
    const snap = await tx.get(PurchaseRepository.tokenIndexRef(provider, tokenHash));
    return snap.exists ? (snap.data() as PurchaseTokenIndexDocument) : null;
  },

  createInTx(
    tx: admin.firestore.Transaction,
    doc: Omit<PurchaseDocument, 'schemaVersion' | 'createdAt' | 'updatedAt'>,
  ): void {
    const ts = now();
    const full: PurchaseDocument = {
      ...doc,
      createdAt: ts,
      updatedAt: ts,
      schemaVersion: SCHEMA_VERSION,
    };
    tx.set(PurchaseRepository.ref(doc.purchaseId), full);

    const tokenIndex: PurchaseTokenIndexDocument = {
      purchaseId: doc.purchaseId,
      provider: doc.provider,
      tokenHash: doc.providerPurchaseTokenHash,
      createdAt: ts,
    };
    tx.set(PurchaseRepository.tokenIndexRef(doc.provider, doc.providerPurchaseTokenHash), tokenIndex);
  },

  updateStatusInTx(
    tx: admin.firestore.Transaction,
    purchaseId: string,
    status: PurchaseStatus,
    ledgerTransactionId?: string,
  ): void {
    const update: Record<string, unknown> = { status, updatedAt: now() };
    if (ledgerTransactionId) update['ledgerTransactionId'] = ledgerTransactionId;
    tx.update(PurchaseRepository.ref(purchaseId), update);
  },
};

// ── Audit Log Repository ──────────────────────────────────────────────────────

export const AuditLogRepository = {

  async append(
    actorId: string,
    actorType: AuditActorType,
    action: string,
    targetWalletId: string,
    correlationId: string,
    details: Record<string, unknown>,
  ): Promise<void> {
    const logId = crypto.randomUUID();
    const doc: WalletAuditLogDocument = {
      logId,
      actorId,
      actorType,
      action,
      targetWalletId,
      correlationId,
      details,
      createdAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    await db().collection(COLLECTIONS.WALLET_AUDIT_LOGS).doc(logId).set(doc);
  },

  appendInTx(
    tx: admin.firestore.Transaction,
    actorId: string,
    actorType: AuditActorType,
    action: string,
    targetWalletId: string,
    correlationId: string,
    details: Record<string, unknown>,
  ): void {
    const logId = crypto.randomUUID();
    const doc: WalletAuditLogDocument = {
      logId,
      actorId,
      actorType,
      action,
      targetWalletId,
      correlationId,
      details,
      createdAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    tx.set(db().collection(COLLECTIONS.WALLET_AUDIT_LOGS).doc(logId), doc);
  },
};

// ── Fraud Signal Repository ───────────────────────────────────────────────────

export const FraudSignalRepository = {

  /**
   * Appends a fraud signal document.
   * PII sanitization (hashing) must be performed by the caller (FraudSignalService)
   * before reaching this layer.
   * Returns the signalId of the created document.
   */
  async append(
    walletId: string,
    userId: string,
    signalType: string,
    severity: FraudSignalSeverity,
    riskScore: number,
    source: FraudSignalSource,
    metadata: Record<string, unknown>,
  ): Promise<string> {
    const signalId = crypto.randomUUID();
    const doc: FraudSignalDocument = {
      signalId,
      walletId,
      userId,
      signalType,
      severity,
      riskScore,
      source,
      metadata,
      createdAt: now(),
      schemaVersion: SCHEMA_VERSION,
    };
    await db().collection(COLLECTIONS.FRAUD_SIGNALS).doc(signalId).set(doc);
    return signalId;
  },

  /**
   * Returns fraud signals for a wallet created within the last [windowMs] milliseconds.
   * Used by RiskScoringService for signal-history velocity checks.
   */
  async listRecentByWalletId(
    walletId: string,
    windowMs: number,
    limit: number,
  ): Promise<FraudSignalDocument[]> {
    const since = Date.now() - windowMs;
    const snaps = await db()
      .collection(COLLECTIONS.FRAUD_SIGNALS)
      .where('walletId', '==', walletId)
      .where('createdAt', '>=', since)
      .orderBy('createdAt', 'desc')
      .limit(limit)
      .get();
    return snaps.docs.map((d) => d.data() as FraudSignalDocument);
  },
};

// ── Admin Action Repository ───────────────────────────────────────────────────

export const AdminActionRepository = {

  ref(actionId: string) {
    return db().collection(COLLECTIONS.ADMIN_ACTIONS).doc(actionId);
  },

  /**
   * Reads an admin action document by its ID. Returns null if not found.
   * Non-transactional. For maker/checker flows the read inside the approval
   * transaction must use findByIdInTx for optimistic locking.
   */
  async findById(actionId: string): Promise<AdminActionDocument | null> {
    const snap = await AdminActionRepository.ref(actionId).get();
    return snap.exists ? (snap.data() as AdminActionDocument) : null;
  },

  /** Transactional read — use inside runTransaction for optimistic locking. */
  async findByIdInTx(
    tx: admin.firestore.Transaction,
    actionId: string,
  ): Promise<AdminActionDocument | null> {
    const snap = await tx.get(AdminActionRepository.ref(actionId));
    return snap.exists ? (snap.data() as AdminActionDocument) : null;
  },

  /**
   * Creates a new pending admin action.
   * Caller (service layer) is responsible for setting status='pending' and
   * for ensuring adminId !== approverId at approval time (NOT here).
   */
  async create(
    doc: Omit<AdminActionDocument, 'schemaVersion' | 'createdAt' | 'resolvedAt'> & {
      resolvedAt?: number | null;
    },
  ): Promise<AdminActionDocument> {
    const ts = now();
    const full: AdminActionDocument = {
      ...doc,
      resolvedAt:    doc.resolvedAt ?? null,
      createdAt:     ts,
      schemaVersion: SCHEMA_VERSION,
    };
    await AdminActionRepository.ref(doc.actionId).set(full);
    return full;
  },

  /**
   * Updates status and approverId in a transaction.
   * Use ONLY from the approval/execution code path (atomic with ledger writes).
   */
  updateStatusInTx(
    tx: admin.firestore.Transaction,
    actionId: string,
    status: AdminActionStatus,
    approverId: string | null,
  ): void {
    tx.update(AdminActionRepository.ref(actionId), {
      status,
      approverId,
      resolvedAt: now(),
    });
  },
};

// Re-export types used by other modules
export type {
  LedgerTransactionType, LedgerTransactionStatus, LedgerBucket, LedgerEntryType,
  PurchaseProvider, PurchaseStatus, FraudSignalSeverity, FraudSignalSource,
  AdminActionStatus,
};
