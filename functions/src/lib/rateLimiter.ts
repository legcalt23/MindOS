/**
 * rateLimiter.ts
 * Server-side rate limiting via Firestore atomic counters.
 * Each bucket: rate_limits/{uid}_{action}
 * Schema: { count: number, windowStart: number }
 */
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

const db = () => admin.firestore();

export interface RateLimitConfig {
  /** Maximum number of calls allowed within windowMs */
  maxCalls: number;
  /** Window duration in milliseconds */
  windowMs: number;
}

export const RATE_LIMITS: Record<string, RateLimitConfig> = {
  follow:           { maxCalls: 30,  windowMs: 60_000 },   // 30 follows/min
  unfollow:         { maxCalls: 30,  windowMs: 60_000 },
  followRequest:    { maxCalls: 20,  windowMs: 60_000 },   // 20 requests/min
  cancelRequest:    { maxCalls: 20,  windowMs: 60_000 },
  acceptRequest:    { maxCalls: 60,  windowMs: 60_000 },
  denyRequest:      { maxCalls: 60,  windowMs: 60_000 },
  block:            { maxCalls: 20,  windowMs: 60_000 },   // 20 blocks/min
  unblock:          { maxCalls: 20,  windowMs: 60_000 },
  profileVisit:     { maxCalls: 60,  windowMs: 60_000 },   // 60 visits/min
  createPost:       { maxCalls: 10,  windowMs: 60_000 },   // 10 posts/min
  deletePost:       { maxCalls: 30,  windowMs: 60_000 },
  claimUsername:    { maxCalls: 5,   windowMs: 3_600_000 },// 5 changes/hour
  deleteAccount:    { maxCalls: 3,   windowMs: 3_600_000 },// 3 attempts/hour (rare, irreversible)
  // ── Wallet / economic operations ────────────────────────
  getBalance:       { maxCalls: 30,  windowMs: 60_000 },   // 30 reads/min
  spendCoins:       { maxCalls: 20,  windowMs: 60_000 },   // 20 spends/min
  initiatePurchase: { maxCalls: 10,  windowMs: 60_000 },   // 10 purchases/min
  verifyPurchase:   { maxCalls: 10,  windowMs: 60_000 },   // 10 verifications/min
  // ── Admin operations ────────────────────────────────────
  // Deliberately tight: a legitimate admin should never need this throughput.
  // Compromised admin spam is the threat model.
  adminRequestWalletAdjustment: { maxCalls: 20, windowMs: 60_000 }, // 20 requests/min
  adminApproveWalletAdjustment: { maxCalls: 30, windowMs: 60_000 }, // 30 approvals/min
};

/**
 * Checks and increments a rate-limit bucket for a given (uid, action) pair.
 * Throws HttpsError('resource-exhausted') if the limit is exceeded.
 */
export async function checkRateLimit(uid: string, action: string): Promise<void> {
  const cfg = RATE_LIMITS[action];
  if (!cfg) return; // unknown actions are not rate-limited here

  const key = `${uid}_${action}`;
  const ref = db().collection('rate_limits').doc(key);

  const exceeded = await db().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const now = Date.now();

    let count = 1;
    let windowStart = now;

    if (snap.exists) {
      const data = snap.data()!;
      const elapsed = now - (data.windowStart as number);

      if (elapsed < cfg.windowMs) {
        count = (data.count as number) + 1;
        windowStart = data.windowStart as number;
      }
      // else: new window, reset to 1
    }

    if (count > cfg.maxCalls) {
      return true; // exceeded — do NOT increment
    }

    tx.set(ref, { count, windowStart, uid, action, updatedAt: now });
    return false;
  });

  if (exceeded) {
    functions.logger.warn(`Rate limit exceeded: uid=${uid} action=${action}`);
    throw new functions.https.HttpsError(
      'resource-exhausted',
      `Too many "${action}" requests. Please wait and try again.`,
    );
  }
}

// ── Fail-closed actions ───────────────────────────────────────────────────

const ECONOMIC_ACTIONS = new Set([
  'spendCoins',
  'initiatePurchase',
  'verifyPurchase',
  // Admin endpoints mutate ledger (via maker/checker) — same fail-closed
  // discipline as user-facing economic actions.
  'adminRequestWalletAdjustment',
  'adminApproveWalletAdjustment',
]);

/**
 * Like checkRateLimit, but if the rate-limit infrastructure itself fails
 * (e.g., Firestore unavailable), economic actions are denied by default.
 * Non-economic actions fall through on infrastructure failure.
 */
export async function checkRateLimitFailClosed(uid: string, action: string): Promise<void> {
  try {
    await checkRateLimit(uid, action);
  } catch (err) {
    if (err instanceof functions.https.HttpsError) throw err;
    if (ECONOMIC_ACTIONS.has(action)) {
      functions.logger.error(`Rate limiter infrastructure failure for economic action: uid=${uid} action=${action}`);
      throw new functions.https.HttpsError(
        'unavailable',
        'Service temporarily unavailable. Please try again later.',
      );
    }
    functions.logger.warn(`Rate limiter infrastructure failure (non-economic, allowing): uid=${uid} action=${action}`);
  }
}
