/**
 * riskScoringService.ts
 * Rule-based fraud risk scoring for economic operations.
 *
 * Security contract:
 *  - Score is computed server-side only; never accepted from client.
 *  - Score is never revealed to the client (enforcement throws opaque FRAUD_DETECTED).
 *  - Fail-safe is configurable per rule: fail-open (allow on query error) or
 *    fail-closed (treat as max-contribution on query error for critical rules).
 *  - A single low-severity signal never triggers freeze.
 *    Only a combined score ≥ RISK_THRESHOLDS.freeze causes enforcement.
 *  - ML / external systems are out of scope for this phase (stubs documented).
 */

import * as admin from 'firebase-admin';
import { COLLECTIONS } from './walletSchema';
import { FraudSignalSeverity, FraudSignalSource } from './walletSchema';

const db = () => admin.firestore();

// ── Public types ──────────────────────────────────────────────────────────────

/** Economic action being scored. */
export type ScoredAction = 'initiatePurchase' | 'verifyPurchase' | 'spendCoins';

/**
 * Enforcement outcome from risk assessment.
 *  - allow:    proceed normally.
 *  - throttle: record signal; proceed (rate limiter handles actual throttling).
 *  - review:   record signal + audit; proceed (flagged for manual review).
 *  - freeze:   block operation; auto-freeze wallet; append audit.
 */
export type RiskDecision = 'allow' | 'throttle' | 'review' | 'freeze';

/** A single signal contributing to the aggregate score. */
export interface RiskSignal {
  type: string;
  /** Score points contributed by this signal (0-100). */
  contribution: number;
  severity: FraudSignalSeverity;
  source: FraudSignalSource;
  detail: string;
}

/** Full result of a risk evaluation. */
export interface RiskAssessment {
  /** Aggregate score (0-100, clamped). */
  score: number;
  decision: RiskDecision;
  signals: RiskSignal[];
}

/** Context for a single risk evaluation request. */
export interface RiskContext {
  walletId: string;
  userId: string;
  action: ScoredAction;
  correlationId: string;
  /** Provided for purchase-related actions. */
  packId?: string;
  /** Provided for spend actions. */
  itemId?: string;
  quantity?: number;
}

// ── Configurable thresholds (exported for test visibility) ────────────────────

/**
 * Score thresholds that gate enforcement decisions.
 * Lower bound is inclusive.
 *   0  ≤ score < throttle → allow
 *   throttle ≤ score < review  → throttle
 *   review   ≤ score < freeze  → review
 *   freeze   ≤ score           → freeze
 */
export const RISK_THRESHOLDS = {
  throttle: 30,
  review:   60,
  freeze:   80,
} as const;

/** Velocity rule configuration. Exported for transparency in tests. */
export const VELOCITY_CONFIG = {
  /** Window for purchase velocity checks. */
  purchasesWindowMs:       10 * 60 * 1000,  // 10 min
  /** >N purchases in window → high contribution. */
  purchasesHighThreshold:  5,
  /** >N purchases in window → medium contribution. */
  purchasesMedThreshold:   3,

  /** Window for spend velocity checks. */
  spendWindowMs:           5 * 60 * 1000,   // 5 min
  /** >N spends in window → medium contribution. */
  spendHighThreshold:      10,
  /** >N spends in window → low contribution. */
  spendMedThreshold:       5,

  /** Window for repeated-pack detection. */
  repeatedPackWindowMs:    30 * 60 * 1000,  // 30 min
  /** ≥N purchases of same pack in window → signal. */
  repeatedPackThreshold:   3,

  /** Max results fetched per velocity query (cap Firestore reads). */
  velocityQueryLimit:      20,
} as const;

// ── Private helpers ───────────────────────────────────────────────────────────

function decideFromScore(score: number): RiskDecision {
  if (score >= RISK_THRESHOLDS.freeze)   return 'freeze';
  if (score >= RISK_THRESHOLDS.review)   return 'review';
  if (score >= RISK_THRESHOLDS.throttle) return 'throttle';
  return 'allow';
}

function clampScore(s: number): number {
  return Math.max(0, Math.min(100, Math.round(s)));
}

// ── Rule: velocity_purchases ──────────────────────────────────────────────────

/**
 * Counts recent purchase initiations for the wallet.
 * Fails open on query error (non-critical; rate limiter already bounds this).
 */
async function ruleVelocityPurchases(
  walletId: string,
): Promise<RiskSignal | null> {
  try {
    const since = Date.now() - VELOCITY_CONFIG.purchasesWindowMs;
    const snaps = await db()
      .collection(COLLECTIONS.PURCHASES)
      .where('walletId', '==', walletId)
      .where('createdAt', '>=', since)
      .limit(VELOCITY_CONFIG.velocityQueryLimit)
      .get();

    const count = snaps.size;

    if (count > VELOCITY_CONFIG.purchasesHighThreshold) {
      return {
        type:         'velocity_purchases',
        contribution: 40,
        severity:     'high',
        source:       'velocity',
        detail:       `${count} purchase(s) in 10-min window (threshold: ${VELOCITY_CONFIG.purchasesHighThreshold})`,
      };
    }
    if (count > VELOCITY_CONFIG.purchasesMedThreshold) {
      return {
        type:         'velocity_purchases',
        contribution: 20,
        severity:     'medium',
        source:       'velocity',
        detail:       `${count} purchase(s) in 10-min window (threshold: ${VELOCITY_CONFIG.purchasesMedThreshold})`,
      };
    }
    return null;
  } catch {
    // Fail open: velocity check failure does not block purchase
    return null;
  }
}

// ── Rule: velocity_spend ──────────────────────────────────────────────────────

/**
 * Counts recent spend transactions for the wallet.
 * Fails open on query error.
 */
async function ruleVelocitySpend(
  walletId: string,
): Promise<RiskSignal | null> {
  try {
    const since = Date.now() - VELOCITY_CONFIG.spendWindowMs;
    const snaps = await db()
      .collection(COLLECTIONS.LEDGER_TRANSACTIONS)
      .where('walletId', '==', walletId)
      .where('type', '==', 'spend')
      .where('createdAt', '>=', since)
      .limit(VELOCITY_CONFIG.velocityQueryLimit)
      .get();

    const count = snaps.size;

    if (count > VELOCITY_CONFIG.spendHighThreshold) {
      return {
        type:         'velocity_spend',
        contribution: 30,
        severity:     'medium',
        source:       'velocity',
        detail:       `${count} spend(s) in 5-min window (threshold: ${VELOCITY_CONFIG.spendHighThreshold})`,
      };
    }
    if (count > VELOCITY_CONFIG.spendMedThreshold) {
      return {
        type:         'velocity_spend',
        contribution: 15,
        severity:     'low',
        source:       'velocity',
        detail:       `${count} spend(s) in 5-min window (threshold: ${VELOCITY_CONFIG.spendMedThreshold})`,
      };
    }
    return null;
  } catch {
    return null;
  }
}

// ── Rule: repeated_pack ───────────────────────────────────────────────────────

/**
 * Detects repeated purchases of the same pack within a short window.
 * Only evaluated for initiatePurchase actions with a known packId.
 * Fails open on query error.
 */
async function ruleRepeatedPack(
  walletId: string,
  packId: string,
): Promise<RiskSignal | null> {
  try {
    const since = Date.now() - VELOCITY_CONFIG.repeatedPackWindowMs;
    const snaps = await db()
      .collection(COLLECTIONS.PURCHASES)
      .where('walletId', '==', walletId)
      .where('packId', '==', packId)
      .where('createdAt', '>=', since)
      .limit(VELOCITY_CONFIG.velocityQueryLimit)
      .get();

    const count = snaps.size;

    if (count >= VELOCITY_CONFIG.repeatedPackThreshold) {
      return {
        type:         'repeated_pack',
        contribution: 20,
        severity:     'medium',
        source:       'behavior',
        detail:       `Pack "${packId}" purchased ${count} time(s) in 30-min window`,
      };
    }
    return null;
  } catch {
    return null;
  }
}

// ── Rule stubs ────────────────────────────────────────────────────────────────

/**
 * STUB — Attestation check.
 * App Check is enforced at the callable boundary (requireAppCheck guard).
 * This stub exists as a placeholder for future device-attestation enrichment
 * (e.g., Play Integrity API score, DeviceCheck bit).
 * Currently contributes 0 to the score.
 */
function ruleAttestationMissing(): RiskSignal | null {
  // TODO(Phase 7+): integrate Play Integrity / DeviceCheck scores.
  return null;
}

/**
 * STUB — Refund history.
 * A high refund rate is a strong fraud signal for real-money operations.
 * Not applicable in sandbox (priceCents = 0, no real refunds).
 * Contributes 0 in sandbox phase.
 */
function ruleRefundHistory(): RiskSignal | null {
  // TODO(Phase 7+): query purchases WHERE status='refunded' for this wallet.
  return null;
}

/**
 * STUB — IP / device anomaly.
 * Requires device metadata from the client (IP, device fingerprint).
 * Firebase callable context does not expose raw IP server-side in all runtimes.
 * Contributes 0 until device attestation pipeline is wired.
 */
function ruleIpDeviceAnomaly(): RiskSignal | null {
  // TODO(Phase 7+): integrate with device signal service.
  return null;
}

// ── RiskScoringService ────────────────────────────────────────────────────────

export const RiskScoringService = {

  /**
   * Evaluates fraud risk for a given economic action context.
   *
   * Runs all active rules concurrently. Stub rules return null (no contribution).
   * Returns a RiskAssessment with an aggregate score, decision, and signal list.
   *
   * The score and signals are NEVER forwarded to the client.
   * The caller (enforcement layer) decides how to act on the decision.
   */
  async evaluate(ctx: RiskContext): Promise<RiskAssessment> {
    // Run all async rules in parallel; stubs run synchronously
    const asyncRules: Promise<RiskSignal | null>[] = [
      ruleVelocityPurchases(ctx.walletId),
      ruleVelocitySpend(ctx.walletId),
    ];

    // repeated_pack only makes sense for initiatePurchase
    if (ctx.action === 'initiatePurchase' && ctx.packId) {
      asyncRules.push(ruleRepeatedPack(ctx.walletId, ctx.packId));
    }

    const asyncResults = await Promise.all(asyncRules);

    // Synchronous stub rules
    const syncResults: (RiskSignal | null)[] = [
      ruleAttestationMissing(),
      ruleRefundHistory(),
      ruleIpDeviceAnomaly(),
    ];

    const signals: RiskSignal[] = [...asyncResults, ...syncResults]
      .filter((s): s is RiskSignal => s !== null);

    const rawScore = signals.reduce((acc, s) => acc + s.contribution, 0);
    const score = clampScore(rawScore);
    const decision = decideFromScore(score);

    return { score, decision, signals };
  },
};
