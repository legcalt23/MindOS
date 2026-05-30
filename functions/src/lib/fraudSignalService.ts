/**
 * fraudSignalService.ts
 * High-level service for recording fraud signals.
 *
 * Security contract:
 *  - PII fields (ip, deviceId, deviceFingerprint, userAgent) are SHA-256 hashed
 *    before storage. Raw values are never persisted.
 *  - riskScore is clamped to [0, 100] server-side regardless of caller input.
 *  - Signals are append-only (never updated or deleted by this service).
 *  - The service never reveals signal details to client callers.
 */

import * as crypto from 'crypto';
import { FraudSignalSeverity, FraudSignalSource } from './walletSchema';
import { FraudSignalRepository } from './walletRepository';
import { walletLog } from './walletLogger';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface RecordSignalParams {
  walletId: string;
  userId: string;
  /** Machine-readable signal type (e.g. "velocity_purchases", "repeated_pack"). */
  signalType: string;
  severity: FraudSignalSeverity;
  /** Numeric score contribution for this signal (0–100). Clamped server-side. */
  riskScore: number;
  source: FraudSignalSource;
  /**
   * Arbitrary metadata.
   * Fields whose keys match PII_FIELDS will be SHA-256 hashed before storage.
   */
  metadata: Record<string, unknown>;
  correlationId: string;
}

// ── Constants ─────────────────────────────────────────────────────────────────

/** Metadata keys whose values contain PII and must be hashed before storage. */
const PII_FIELDS = new Set([
  'ip',
  'deviceId',
  'deviceFingerprint',
  'userAgent',
  'email',
  'phone',
]);

// ── Private helpers ───────────────────────────────────────────────────────────

function hashField(value: string): string {
  return crypto.createHash('sha256').update(value).digest('hex');
}

/**
 * Returns a sanitized copy of metadata:
 *  - PII fields are replaced with `${key}Hash: sha256(value)`.
 *  - All other fields are passed through as-is.
 */
export function sanitizeMetadata(
  raw: Record<string, unknown>,
): Record<string, unknown> {
  const safe: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(raw)) {
    if (PII_FIELDS.has(key) && typeof value === 'string') {
      safe[`${key}Hash`] = hashField(value);
      // Raw value intentionally NOT stored
    } else {
      safe[key] = value;
    }
  }
  return safe;
}

/**
 * Clamp riskScore to [0, 100] and round to integer.
 * Server must never trust caller-supplied scores outside this range.
 */
export function clampScore(score: number): number {
  return Math.max(0, Math.min(100, Math.round(score)));
}

// ── FraudSignalService ────────────────────────────────────────────────────────

export const FraudSignalService = {

  /**
   * Records a fraud signal with sanitized metadata.
   *
   * Callers should not use the returned signalId for any business logic visible
   * to the client. It is provided for internal cross-reference only.
   */
  async record(params: RecordSignalParams): Promise<string> {
    const sanitizedMeta = sanitizeMetadata(params.metadata);
    const score = clampScore(params.riskScore);

    walletLog.info(
      {
        correlationId: params.correlationId,
        action: 'fraud_signal_record',
        walletId: params.walletId,
      },
      'Fraud signal recorded',
      {
        signalType: params.signalType,
        severity:   params.severity,
        score,
        source:     params.source,
      },
    );

    return FraudSignalRepository.append(
      params.walletId,
      params.userId,
      params.signalType,
      params.severity,
      score,
      params.source,
      sanitizedMeta,
    );
  },
};
