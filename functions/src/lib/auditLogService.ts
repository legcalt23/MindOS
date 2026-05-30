/**
 * auditLogService.ts
 * High-level service for emitting audit log entries.
 *
 * Security contract:
 *  - Audit logs are append-only. Enforcement layers:
 *      1. Firestore Rules deny all client writes to `wallet_audit_logs`.
 *      2. Only Cloud Functions (Admin SDK) can write — via this service.
 *      3. This service never exposes a delete / update API.
 *  - Sensitive fields in `details` (tokens, secrets, credentials) are
 *    REDACTED before storage. Hashing is NOT used here because audit logs
 *    must never be matchable back to original credentials.
 *  - `action` values are restricted to a server-side enum (AuditAction).
 *    No arbitrary strings — adds type safety AND a single grep-point for review.
 *  - All callers must go through this service. Direct AuditLogRepository
 *    usage is now considered legacy and should be migrated.
 */

import * as admin from 'firebase-admin';
import { AuditLogRepository } from './walletRepository';
import { AuditActorType } from './walletSchema';

// ── Public types ──────────────────────────────────────────────────────────────

/**
 * Closed set of permitted audit action names.
 * Add new actions here (not at call sites). One file = one place to review.
 */
export type AuditAction =
  // ── User economic actions
  | 'spend_coins'
  | 'purchase_initiated'
  | 'purchase_credited'
  | 'refund_skeleton'
  // ── System wallet operations
  | 'wallet_auto_created'
  | 'wallet_auto_frozen_fraud'
  | 'fraud_review_flagged'
  // ── Admin operations (used by Phase 6 admin endpoints)
  | 'admin_wallet_frozen'
  | 'admin_wallet_unfrozen'
  | 'admin_adjustment_requested'
  | 'admin_adjustment_approved'
  | 'admin_adjustment_rejected'
  | 'admin_adjustment_executed';

export interface AuditEvent {
  actorId: string;
  actorType: AuditActorType;
  action: AuditAction;
  targetWalletId: string;
  correlationId: string;
  /** Arbitrary context. Sensitive fields are redacted before storage. */
  details?: Record<string, unknown>;
}

// ── Sanitization ──────────────────────────────────────────────────────────────

/**
 * Substrings (lowercase, case-insensitive match) that mark a key as sensitive.
 * Any matching key has its value replaced with "<redacted>" before storage.
 */
const SENSITIVE_KEY_SUBSTRINGS = [
  'token',
  'secret',
  'password',
  'credential',
  'authorization',
  'apikey',
  'accesstoken',
  'refreshtoken',
  'idtoken',
  'sessionid',
];

function isSensitiveKey(key: string): boolean {
  const lower = key.toLowerCase();
  return SENSITIVE_KEY_SUBSTRINGS.some((pattern) => lower.includes(pattern));
}

/**
 * Recursively redacts sensitive fields from a details payload.
 *  - Keys matching SENSITIVE_KEY_SUBSTRINGS → value replaced with "<redacted>".
 *  - Nested objects → recursed.
 *  - Arrays → each item; object items are recursed.
 *  - Primitives in non-sensitive keys → passed through.
 *  - null / undefined values → preserved as-is.
 *
 * Exported for unit testing.
 */
export function sanitizeDetails(
  details: Record<string, unknown>,
): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(details)) {
    if (isSensitiveKey(key)) {
      result[key] = '<redacted>';
      continue;
    }
    if (value === null || value === undefined) {
      result[key] = value;
      continue;
    }
    if (Array.isArray(value)) {
      result[key] = value.map((item) =>
        item && typeof item === 'object' && !Array.isArray(item)
          ? sanitizeDetails(item as Record<string, unknown>)
          : item,
      );
      continue;
    }
    if (typeof value === 'object') {
      result[key] = sanitizeDetails(value as Record<string, unknown>);
      continue;
    }
    result[key] = value;
  }
  return result;
}

// ── AuditLogService ───────────────────────────────────────────────────────────

export const AuditLogService = {

  /**
   * Records an audit event outside of any Firestore transaction.
   * Use for post-hoc logging where atomicity with a mutation is not required
   * (e.g., wallet auto-creation, fraud freeze).
   */
  async record(event: AuditEvent): Promise<void> {
    const safe = sanitizeDetails(event.details ?? {});
    await AuditLogRepository.append(
      event.actorId,
      event.actorType,
      event.action,
      event.targetWalletId,
      event.correlationId,
      safe,
    );
  },

  /**
   * Records an audit event inside an active Firestore transaction.
   * Use whenever the audit must be atomic with an economic mutation
   * (spend, purchase credit, admin adjustment execution).
   *
   * Note: this returns void (not a promise) because Firestore transactions
   * batch all writes and commit them together.
   */
  recordInTx(tx: admin.firestore.Transaction, event: AuditEvent): void {
    const safe = sanitizeDetails(event.details ?? {});
    AuditLogRepository.appendInTx(
      tx,
      event.actorId,
      event.actorType,
      event.action,
      event.targetWalletId,
      event.correlationId,
      safe,
    );
  },

  /**
   * Refund skeleton — placeholder for the future refund flow.
   *
   * Phase 6 only emits an audit entry so any attempted refund is traceable
   * even though the actual ledger reversal is NOT yet implemented.
   *
   * Real refund accounting will be wired when real payment providers are
   * integrated (post-sandbox). Until then, calling this returns silently
   * after audit, so callers (e.g. admin tools) know the request was logged.
   */
  async recordRefundSkeleton(params: {
    actorId: string;
    actorType: AuditActorType;
    targetWalletId: string;
    correlationId: string;
    purchaseId?: string;
    reason: string;
  }): Promise<void> {
    await AuditLogService.record({
      actorId:        params.actorId,
      actorType:      params.actorType,
      action:         'refund_skeleton',
      targetWalletId: params.targetWalletId,
      correlationId:  params.correlationId,
      details: {
        purchaseId: params.purchaseId ?? null,
        reason:     params.reason,
        note:       'refund_not_implemented_in_sandbox_phase',
      },
    });
  },
};
