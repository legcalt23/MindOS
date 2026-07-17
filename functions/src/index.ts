/**
 * index.ts — Cloud Functions entry point for MindOS v3
 *
 * All callable functions follow the same security envelope:
 *   requireAppCheck → requireAuth → validateInput → checkRateLimit → business logic
 *
 * Admin SDK bypasses Firestore Rules.  Every function in this file that
 * writes to Firestore MUST enforce auth, input schema, and rate limits
 * independently — Firestore Rules are irrelevant for Admin SDK writes.
 *
 * Exported callables:
 *   verifyClientIntegrity   — App Check smoke-test
 *   followUser              — follow a public account
 *   unfollowUser            — unfollow
 *   sendFollowRequest       — request to follow a private account
 *   cancelFollowRequest     — requester cancels their pending request
 *   acceptFollowRequest     — target accepts incoming request
 *   denyFollowRequest       — target denies incoming request
 *   blockUser               — block; cascades unfollow + invalidate requests + counter update
 *   unblockUser             — unblock
 *   recordProfileVisit      — deduplicated profile visit with block check
 *   createPost              — create post (author identity sourced from DB)
 *   deletePost              — delete post (ownership enforced server-side)
 *
 * Exported triggers:
 *   enforceStorageQuotas    — Storage onFinalize: quota + rate limit + suspend
 *   reduceStorageQuotasOnDelete — Storage onDelete: decrement ledger
 *   onPostDeleted           — Firestore onDelete: purge attachments + feed entries
 *   reconcileProfileCardMedia — Pub/Sub schedule: reclaim orphaned profile-card media
 */

import * as admin from 'firebase-admin';

admin.initializeApp();

// ── Callables ────────────────────────────────────────────────────────────────

export { verifyClientIntegrity } from './appCheck';
export {
  followUser,
  unfollowUser,
  sendFollowRequest,
  cancelFollowRequest,
  acceptFollowRequest,
  denyFollowRequest,
  blockUser,
  unblockUser,
} from './socialGraph';

// Firestore trigger: materialize the counters/{uid} read-model from the sharded write path (A1).
export { aggregateCounterShards } from './socialGraph';
export { recordProfileVisit } from './profileVisits';
export { createPost, deletePost } from './posts';

// Home feed: fan-out-on-read over the viewer's followees (A2). feeds/ has no writer;
// this callable computes the feed on demand with a per-instance best-effort cache.
export { loadHomeFeed } from './homeFeed';

// ── Account lifecycle ──────────────────────────────────────────────────────────

// deleteAccount (callable) enqueues a durable purge job; processAccountDeletion (Cloud Tasks
// worker) runs it with a long timeout, idempotent + resumable (A3 / GDPR Art. 17).
export { deleteAccount, processAccountDeletion } from './accountDeletion';

// ── Wallet callables ─────────────────────────────────────────────────────────

export { getWalletBalance, spendCoins, getWalletTransactions, initiatePurchase, verifyPurchase } from './walletFunctions';

// ── Marketplace callables ───────────────────────────────────────────────────

export { claimProduct, publishProfileCard } from './marketplaceFunctions';

// ── Admin callables (Phase 6 — maker/checker adjustments) ───────────────────

export {
  adminRequestWalletAdjustment,
  adminApproveWalletAdjustment,
} from './adminFunctions';

// ── Payment webhook handlers ──────────────────────────────────────────────────

export { webhookGooglePlay, webhookAppStore, webhookStripe } from './webhooks';

// ── Storage triggers ─────────────────────────────────────────────────────────

export { enforceStorageQuotas, reduceStorageQuotasOnDelete } from './storage';

// ── Firestore triggers ───────────────────────────────────────────────────────

export { onPostDeleted } from './posts';

// ── Scheduled triggers ─────────────────────────────────────────────────────────

export { reconcileProfileCardMedia } from './profileCardGc';

// Voice speech broker: authenticated/App-Checked mobile audio -> NVIDIA Speech NIM.
export { setVoiceCloudSpeechConsent, speechAsrRecognize } from './speechBroker';
