/**
 * walletSchema.ts
 * Firestore schema constants and TypeScript types for all wallet collections.
 *
 * This is the single source of truth for collection names, document shapes,
 * and runtime type guards used by all wallet Cloud Functions.
 *
 * There are no SQL migrations in Firestore. Schema is enforced by:
 *   1. Cloud Functions writing documents that match these types.
 *   2. Firestore Rules (firestore.rules) denying all client writes.
 *   3. Type guards in this file used at read time.
 *
 * schemaVersion is included in every document for future evolution.
 */

// ── Collection names ──────────────────────────────────────────────────────────

export const COLLECTIONS = {
  WALLETS:                 'wallets',
  BALANCE_SNAPSHOTS:       'balance_snapshots',
  LEDGER_TRANSACTIONS:     'ledger_transactions',
  LEDGER_ENTRIES:          'ledger_entries',
  IDEMPOTENCY_KEYS:        'idempotency_keys',
  PURCHASES:               'purchases',
  PURCHASE_TOKEN_INDEX:    'purchase_token_index',
  PAYMENT_PROVIDER_EVENTS: 'payment_provider_events',
  FRAUD_SIGNALS:           'fraud_signals',
  WALLET_AUDIT_LOGS:       'wallet_audit_logs',
  ADMIN_ACTIONS:           'admin_actions',
} as const;

export const SCHEMA_VERSION = 1;

// ── Wallet ────────────────────────────────────────────────────────────────────

export type WalletStatus = 'active' | 'frozen' | 'suspended';

export interface WalletDocument {
  walletId: string;       // == userId (one wallet per user)
  userId: string;
  status: WalletStatus;
  createdAt: number;      // millis
  updatedAt: number;
  schemaVersion: 1;
}

// ── Balance Snapshot ──────────────────────────────────────────────────────────

export interface BalanceSnapshotDocument {
  walletId: string;
  availableBalance: number;     // integer >= 0 — total spendable
  purchasedBalance: number;     // integer >= 0
  promotionalBalance: number;   // integer >= 0
  earnedBalance: number;        // integer >= 0
  version: number;              // monotonically increasing — optimistic lock
  lastTransactionId: string | null;
  updatedAt: number;
  schemaVersion: 1;
}

// ── Ledger Transaction ────────────────────────────────────────────────────────

export type LedgerTransactionType =
  | 'purchase_credit'
  | 'spend'
  | 'refund'
  | 'admin_adjustment'
  | 'promotional_credit';

export type LedgerTransactionStatus = 'confirmed' | 'failed' | 'reversed';

export interface LedgerTransactionDocument {
  transactionId: string;        // UUID v4
  walletId: string;
  type: LedgerTransactionType;
  idempotencyKey: string;
  status: LedgerTransactionStatus;
  metadata: Record<string, unknown>;  // type-specific context
  createdAt: number;
  schemaVersion: 1;
}

// ── Ledger Entry ──────────────────────────────────────────────────────────────

export type LedgerEntryType = 'credit' | 'debit';
export type LedgerBucket = 'purchased' | 'promotional' | 'earned';

export interface LedgerEntryDocument {
  entryId: string;              // UUID v4
  transactionId: string;
  walletId: string;
  entryType: LedgerEntryType;
  bucket: LedgerBucket;
  amount: number;               // integer != 0; positive=credit, negative=debit
  runningBalance: number;       // balance in this bucket AFTER this entry
  createdAt: number;
  schemaVersion: 1;
}

// ── Idempotency Key ───────────────────────────────────────────────────────────

export type IdempotencyStatus = 'in_progress' | 'completed' | 'failed';

export interface IdempotencyKeyDocument {
  docId: string;                // == `${walletId}_${key}`
  key: string;
  walletId: string;
  action: string;
  requestHash: string;          // SHA-256 of canonical request body
  status: IdempotencyStatus;
  responsePayload: unknown | null;
  transactionId: string | null;
  createdAt: number;
  expiresAt: number;            // millis; TTL for cleanup (24h)
  schemaVersion: 1;
}

// ── Purchase ──────────────────────────────────────────────────────────────────

export type PurchaseProvider = 'sandbox' | 'google_play' | 'apple_iap' | 'stripe';
export type PurchaseStatus = 'initiated' | 'verified' | 'credited' | 'failed' | 'refunded';

export interface PurchaseDocument {
  purchaseId: string;           // UUID v4
  walletId: string;
  userId: string;
  provider: PurchaseProvider;
  providerPurchaseTokenHash: string;  // SHA-256; UNIQUE via purchase_token_index
  providerTransactionId: string | null;
  packId: string;
  coinsAmount: number;          // integer; from server catalog, NOT client
  priceCents: number;           // integer; 0 for sandbox
  currency: string;             // "SANDBOX" for sandbox
  status: PurchaseStatus;
  ledgerTransactionId: string | null;
  createdAt: number;
  updatedAt: number;
  schemaVersion: 1;
}

// Purchase token uniqueness index — docId = `${provider}_${tokenHash}`
export interface PurchaseTokenIndexDocument {
  purchaseId: string;
  provider: PurchaseProvider;
  tokenHash: string;
  createdAt: number;
}

// ── Payment Provider Event ────────────────────────────────────────────────────

export interface PaymentProviderEventDocument {
  eventId: string;              // == docId = `${provider}_${providerEventId}`
  provider: PurchaseProvider;
  providerEventId: string;
  purchaseId: string | null;
  eventType: string;
  sanitizedPayload: Record<string, unknown>;  // no secrets stored
  processedAt: number;
  schemaVersion: 1;
}

// ── Fraud Signal ──────────────────────────────────────────────────────────────

/**
 * Four-tier severity scale aligned with standard risk frameworks.
 * Enforcement actions are keyed to this, not the raw numeric score.
 */
export type FraudSignalSeverity = 'low' | 'medium' | 'high' | 'critical';

/**
 * Origin of the fraud signal.
 * PII-bearing sources (ip, device) must be hashed before storage.
 */
export type FraudSignalSource =
  | 'velocity'
  | 'device'
  | 'ip'
  | 'behavior'
  | 'attestation'
  | 'manual';

export interface FraudSignalDocument {
  signalId: string;
  walletId: string;
  userId: string;
  signalType: string;
  /** Four-tier severity derived from the numeric riskScore. */
  severity: FraudSignalSeverity;
  /** 0-100 integer. Never computed client-side; always server-assigned. */
  riskScore: number;
  /** Origin of the signal. PII fields (ip, deviceId, etc.) are SHA-256 hashed. */
  source: FraudSignalSource;
  metadata: Record<string, unknown>;
  createdAt: number;
  schemaVersion: 1;
}

// ── Wallet Audit Log ──────────────────────────────────────────────────────────

export type AuditActorType = 'user' | 'admin' | 'system';

export interface WalletAuditLogDocument {
  logId: string;
  actorId: string;
  actorType: AuditActorType;
  action: string;
  targetWalletId: string;
  correlationId: string;
  details: Record<string, unknown>;
  createdAt: number;
  schemaVersion: 1;
}

// ── Admin Action ──────────────────────────────────────────────────────────────

export type AdminActionStatus = 'pending' | 'approved' | 'executed' | 'rejected';

export interface AdminActionDocument {
  actionId: string;
  adminId: string;
  action: string;
  targetWalletId: string;
  amount: number | null;
  justification: string;
  status: AdminActionStatus;
  approverId: string | null;
  createdAt: number;
  resolvedAt: number | null;
  schemaVersion: 1;
}

// ── Runtime guards (used at read time) ───────────────────────────────────────

export function isValidWalletStatus(s: unknown): s is WalletStatus {
  return s === 'active' || s === 'frozen' || s === 'suspended';
}

export function isValidLedgerTransactionType(t: unknown): t is LedgerTransactionType {
  return [
    'purchase_credit', 'spend', 'refund', 'admin_adjustment', 'promotional_credit',
  ].includes(t as string);
}

export function isValidBucket(b: unknown): b is LedgerBucket {
  return b === 'purchased' || b === 'promotional' || b === 'earned';
}

export function isValidPurchaseProvider(p: unknown): p is PurchaseProvider {
  return p === 'sandbox' || p === 'google_play' || p === 'apple_iap' || p === 'stripe';
}

export function isValidPurchaseStatus(s: unknown): s is PurchaseStatus {
  return ['initiated', 'verified', 'credited', 'failed', 'refunded'].includes(s as string);
}

export function isValidFraudSignalSeverity(s: unknown): s is FraudSignalSeverity {
  return s === 'low' || s === 'medium' || s === 'high' || s === 'critical';
}

export function isValidFraudSignalSource(s: unknown): s is FraudSignalSource {
  return (
    s === 'velocity' ||
    s === 'device'   ||
    s === 'ip'       ||
    s === 'behavior' ||
    s === 'attestation' ||
    s === 'manual'
  );
}
