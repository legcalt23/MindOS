/**
 * idempotencyService.ts
 * Idempotency enforcement for all economic operations.
 *
 * State machine for idempotency keys:
 *   not_found       → proceed (create in_progress)
 *   in_progress     → reject (concurrent request)
 *   completed + same hash  → replay (return cached response)
 *   completed + diff hash  → conflict (same key, different request body)
 *   failed + same hash     → proceed (safe retry)
 *   failed + diff hash     → conflict (key reuse with different body)
 *
 * Security: requestHash is SHA-256 of canonical (key-sorted) JSON so body
 * order differences from the same client do not cause false conflicts.
 */

import * as crypto from 'crypto';
import * as admin from 'firebase-admin';
import { IdempotencyRepository } from './walletRepository';
import { IdempotencyKeyDocument } from './walletSchema';
import { walletError } from './walletErrors';

// ── Canonical request hashing ─────────────────────────────────────────────────

function sortKeys(obj: unknown): unknown {
  if (obj === null || typeof obj !== 'object') return obj;
  if (Array.isArray(obj)) return obj.map(sortKeys);
  const sorted: Record<string, unknown> = {};
  for (const key of Object.keys(obj as object).sort()) {
    sorted[key] = sortKeys((obj as Record<string, unknown>)[key]);
  }
  return sorted;
}

/**
 * Produces a stable SHA-256 hex digest regardless of key insertion order.
 * Strip the idempotency key itself before calling (it is the scope, not the body).
 */
export function hashRequest(data: unknown): string {
  const normalized = sortKeys(data);
  return crypto.createHash('sha256').update(JSON.stringify(normalized)).digest('hex');
}

// ── Pure state-machine decision ───────────────────────────────────────────────

export type IdempotencyDecision =
  | { kind: 'proceed' }
  | { kind: 'concurrent' }
  | { kind: 'conflict' }
  | { kind: 'replay'; response: unknown; transactionId: string };

/**
 * Pure function — no I/O; fully unit-testable.
 * Determines what the system should do given the current state of the idempotency key.
 */
export function resolveIdempotencyDecision(
  existing: IdempotencyKeyDocument | null,
  incomingRequestHash: string,
): IdempotencyDecision {
  if (!existing) return { kind: 'proceed' };

  if (existing.status === 'in_progress') return { kind: 'concurrent' };

  if (existing.requestHash !== incomingRequestHash) return { kind: 'conflict' };

  if (existing.status === 'completed') {
    return {
      kind: 'replay',
      response: existing.responsePayload,
      transactionId: existing.transactionId!,
    };
  }

  // failed + same hash → safe retry
  return { kind: 'proceed' };
}

// ── Pre-transaction check (fast path) ────────────────────────────────────────

export type PreTxCheckResult =
  | { isReplay: false }
  | { isReplay: true; response: unknown; transactionId: string };

/**
 * Non-transactional pre-check — used to short-circuit obvious replays or
 * conflicts before entering an expensive Firestore transaction.
 *
 * IMPORTANT: This is advisory only. The authoritative check must happen inside
 * the business transaction via applyIdempotencyInTx().
 */
export async function checkIdempotencyPreTx(
  walletId: string,
  key: string,
  requestHash: string,
  correlationId: string,
): Promise<PreTxCheckResult> {
  const existing = await IdempotencyRepository.find(walletId, key);
  const decision = resolveIdempotencyDecision(existing, requestHash);

  switch (decision.kind) {
    case 'conflict':
      throw walletError('IDEMPOTENCY_KEY_CONFLICT', correlationId);
    case 'concurrent':
      throw walletError('DUPLICATE_IDEMPOTENCY_KEY', correlationId);
    case 'replay':
      return { isReplay: true, response: decision.response, transactionId: decision.transactionId };
    case 'proceed':
      return { isReplay: false };
  }
}

// ── Within-transaction enforcement ───────────────────────────────────────────

export type InTxApplyResult =
  | { isReplay: false }
  | { isReplay: true; response: unknown; transactionId: string };

/**
 * Authoritative idempotency enforcement inside a Firestore transaction.
 *
 * Call this AFTER all tx.get() reads and BEFORE any tx.set()/tx.update() writes,
 * except when the result is a replay (in which case writes are skipped entirely).
 *
 * On 'proceed': writes the in_progress record so concurrent transactions conflict.
 * Returns { isReplay: false } so the caller can proceed with business writes then
 * call IdempotencyRepository.completeInTx() at the end.
 */
export function applyIdempotencyInTx(
  tx: admin.firestore.Transaction,
  existing: IdempotencyKeyDocument | null,
  walletId: string,
  key: string,
  action: string,
  requestHash: string,
  correlationId: string,
): InTxApplyResult {
  const decision = resolveIdempotencyDecision(existing, requestHash);

  switch (decision.kind) {
    case 'conflict':
      throw walletError('IDEMPOTENCY_KEY_CONFLICT', correlationId);
    case 'concurrent':
      throw walletError('DUPLICATE_IDEMPOTENCY_KEY', correlationId);
    case 'replay':
      return { isReplay: true, response: decision.response, transactionId: decision.transactionId };
    case 'proceed':
      IdempotencyRepository.createInProgressInTx(tx, walletId, key, action, requestHash);
      return { isReplay: false };
  }
}
