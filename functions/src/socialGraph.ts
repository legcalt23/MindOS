/**
 * socialGraph.ts
 * Callable Cloud Functions for: follows, followRequests, blocks, counters.
 *
 * SECURITY CONTRACT (per function):
 *  1. requireAppCheck  — reject unverified clients
 *  2. requireAuth      — reject unauthenticated callers
 *  3. validate input   — strict schema validation
 *  4. requireNotSelf   — prevent self-targeting
 *  5. checkRateLimit   — per-UID per-action rate limiting
 *  6. requireNoBlock   — (where applicable) prevent action across blocks
 *  7. Admin SDK write  — all writes performed server-side in a transaction
 *  8. Idempotency      — check pre-conditions before writing
 *
 * Counters are NEVER trusted from the client. Fan-in counts (followersCount,
 * incomingPendingRequestsCount) are written to a per-uid shard subcollection with
 * FieldValue.increment inside the same transaction as the social-graph document
 * (audit A1 — avoids hot single-document write contention); single-writer counts stay on
 * counters/{uid} directly. The client read-model counters/{uid} is kept eventually
 * consistent by the aggregateCounterShards trigger. See lib/counterShards.ts.
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth, requireNotSelf, requireNoBlock } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';
import {
  validateFollowInput,
  validateFollowRequestInput,
  validateRequestActionInput,
  validateBlockInput,
} from './lib/validators';
import { logSafeRef } from './lib/privacyLogging';
import {
  applyShardedCounterDelta,
  incrementParentCounterFields,
  readCounterMigration,
  aggregateShardedCounterFields,
} from './lib/counterShards';

const db = () => admin.firestore();
const FieldValue = admin.firestore.FieldValue;

// ─────────────────────────────────────────────────────────────────────────────
// followUser
// Creates: follows/{callerId}_{targetId}
// Updates: counters/{callerId}.followingCount + counters/{targetId}.followersCount
// ─────────────────────────────────────────────────────────────────────────────

export const followUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { targetUserId } = validateFollowInput(data);

  requireNotSelf(callerUid, targetUserId, 'follow');
  await checkRateLimit(callerUid, 'follow');
  await requireNoBlock(callerUid, targetUserId);

  // Check target is public (private accounts require a follow request instead)
  const privacySnap = await db().collection('privacy').doc(targetUserId).get();
  if (!privacySnap.exists) {
    throw new functions.https.HttpsError('not-found', 'Target user not found.');
  }
  if (privacySnap.data()!['profileVisibility'] !== 'Public') {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Target account is private. Send a follow request instead.',
    );
  }

  const relationId  = `${callerUid}_${targetUserId}`;
  const followRef   = db().collection('follows').doc(relationId);
  const now         = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    // Reads first (Firestore requires all reads before writes).
    const followSnap = await tx.get(followRef);
    if (followSnap.exists) return; // idempotent: already following
    const targetMigration = await readCounterMigration(tx, targetUserId);

    tx.set(followRef, {
      followerId: callerUid,
      followeeId: targetUserId,
      createdAt:  now,
    });

    // followersCount fans in on the target (viral) → sharded; the caller's own
    // followingCount is single-writer → atomic parent increment.
    applyShardedCounterDelta(tx, targetUserId, { followersCount: 1 }, targetMigration);
    incrementParentCounterFields(tx, callerUid, { followingCount: 1 });
  });

  functions.logger.info('followUser: complete', {
    callerRef: logSafeRef(callerUid),
    targetRef: logSafeRef(targetUserId),
  });
  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// unfollowUser
// Deletes: follows/{callerId}_{targetId}
// Updates: counters
// ─────────────────────────────────────────────────────────────────────────────

export const unfollowUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { targetUserId } = validateFollowInput(data);

  requireNotSelf(callerUid, targetUserId, 'unfollow');
  await checkRateLimit(callerUid, 'unfollow');

  const relationId = `${callerUid}_${targetUserId}`;
  const followRef  = db().collection('follows').doc(relationId);

  await db().runTransaction(async (tx) => {
    // Reads first.
    const followSnap = await tx.get(followRef);
    if (!followSnap.exists) return; // idempotent: not following
    const targetMigration = await readCounterMigration(tx, targetUserId);

    tx.delete(followRef);
    // Decrement only runs because the follow existed → counters stay >= 0.
    applyShardedCounterDelta(tx, targetUserId, { followersCount: -1 }, targetMigration);
    incrementParentCounterFields(tx, callerUid, { followingCount: -1 });
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// sendFollowRequest  (private accounts)
// Creates: followRequests/{requestId}
// Updates: counters/{callerId}.outgoingPendingRequestsCount
//          counters/{targetId}.incomingPendingRequestsCount
// ─────────────────────────────────────────────────────────────────────────────

export const sendFollowRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { targetUserId } = validateFollowRequestInput(data);

  requireNotSelf(callerUid, targetUserId, 'follow-request');
  await checkRateLimit(callerUid, 'followRequest');
  await requireNoBlock(callerUid, targetUserId);

  const requestId  = `${callerUid}_${targetUserId}`;
  const reqRef     = db().collection('followRequests').doc(requestId);
  const targetPrivacyRef = db().collection('privacy').doc(targetUserId);
  const now        = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    // Reads first.
    const [reqSnap, targetPrivacySnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(targetPrivacyRef),
    ]);

    if (!targetPrivacySnap.exists) {
      throw new functions.https.HttpsError('not-found', 'Target user not found.');
    }
    if (targetPrivacySnap.data()?.['profileVisibility'] !== 'Private') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Target account is not private. Follow public accounts directly.',
      );
    }

    // Idempotency: pending request already exists
    if (reqSnap.exists && reqSnap.data()!['status'] === 'Pending') return;

    const targetMigration = await readCounterMigration(tx, targetUserId);

    tx.set(reqRef, {
      id:          requestId,
      requesterId: callerUid,
      targetId:    targetUserId,
      status:      'Pending',
      createdAt:   now,
      resolvedAt:  null,
    });

    // incomingPendingRequestsCount fans in on the target (viral) → sharded; the caller's
    // own outgoingPendingRequestsCount is single-writer → atomic parent increment.
    applyShardedCounterDelta(tx, targetUserId, { incomingPendingRequestsCount: 1 }, targetMigration);
    incrementParentCounterFields(tx, callerUid, { outgoingPendingRequestsCount: 1 });
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// cancelFollowRequest — caller cancels their own outgoing request
// ─────────────────────────────────────────────────────────────────────────────

export const cancelFollowRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { requestId } = validateRequestActionInput(data);
  await checkRateLimit(callerUid, 'cancelRequest');

  const reqRef = db().collection('followRequests').doc(requestId);
  const now    = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const reqSnap = await tx.get(reqRef);
    if (!reqSnap.exists) return; // idempotent

    const reqData = reqSnap.data()!;
    // Only the requester can cancel
    if (reqData['requesterId'] !== callerUid) {
      throw new functions.https.HttpsError('permission-denied', 'Not your request.');
    }
    if (reqData['status'] !== 'Pending') return; // already resolved

    const targetId = reqData['targetId'] as string;
    const targetMigration = await readCounterMigration(tx, targetId);

    tx.update(reqRef, { status: 'Canceled', resolvedAt: now });

    // Decrement only runs because a Pending request existed → counters stay >= 0.
    applyShardedCounterDelta(tx, targetId, { incomingPendingRequestsCount: -1 }, targetMigration);
    incrementParentCounterFields(tx, callerUid, { outgoingPendingRequestsCount: -1 });
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// acceptFollowRequest — target accepts an incoming request
// Creates: follows/{requesterId}_{callerId}
// ─────────────────────────────────────────────────────────────────────────────

export const acceptFollowRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { requestId } = validateRequestActionInput(data);
  await checkRateLimit(callerUid, 'acceptRequest');

  const reqRef = db().collection('followRequests').doc(requestId);
  const now    = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const reqSnap = await tx.get(reqRef);
    if (!reqSnap.exists) throw new functions.https.HttpsError('not-found', 'Follow request not found.');

    const reqData = reqSnap.data()!;
    if (reqData['targetId'] !== callerUid) {
      throw new functions.https.HttpsError('permission-denied', 'This request is not addressed to you.');
    }
    if (reqData['status'] !== 'Pending') return; // already resolved — idempotent

    const requesterId = reqData['requesterId'] as string;

    // Check block hasn't appeared since request was sent
    const followRef = db().collection('follows').doc(`${requesterId}_${callerUid}`);
    const [blockAB, blockBA, followSnap] = await Promise.all([
      tx.get(db().collection('blocks').doc(`${requesterId}_${callerUid}`)),
      tx.get(db().collection('blocks').doc(`${callerUid}_${requesterId}`)),
      tx.get(followRef),
    ]);
    if (blockAB.exists || blockBA.exists) {
      throw new functions.https.HttpsError('permission-denied', 'Cannot accept due to an existing block.');
    }

    // The acceptor (caller) gains a follower and drops an incoming request → both viral → sharded.
    const callerMigration = await readCounterMigration(tx, callerUid);

    tx.update(reqRef, { status: 'Accepted', resolvedAt: now });

    if (!followSnap.exists) {
      tx.set(followRef, { followerId: requesterId, followeeId: callerUid, createdAt: now });

      applyShardedCounterDelta(tx, callerUid, {
        followersCount: 1,
        incomingPendingRequestsCount: -1,
      }, callerMigration);
      // The requester's own following/outgoing are single-writer → atomic parent increments.
      incrementParentCounterFields(tx, requesterId, {
        followingCount: 1,
        outgoingPendingRequestsCount: -1,
      });
    }
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// denyFollowRequest — target denies an incoming request
// ─────────────────────────────────────────────────────────────────────────────

export const denyFollowRequest = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { requestId } = validateRequestActionInput(data);
  await checkRateLimit(callerUid, 'denyRequest');

  const reqRef = db().collection('followRequests').doc(requestId);
  const now    = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const reqSnap = await tx.get(reqRef);
    if (!reqSnap.exists) return;

    const reqData = reqSnap.data()!;
    if (reqData['targetId'] !== callerUid) {
      throw new functions.https.HttpsError('permission-denied', 'This request is not addressed to you.');
    }
    if (reqData['status'] !== 'Pending') return;

    const requesterId = reqData['requesterId'] as string;
    const callerMigration = await readCounterMigration(tx, callerUid);

    tx.update(reqRef, { status: 'Denied', resolvedAt: now });

    // The caller's incomingPendingRequestsCount is viral → sharded; the requester's own
    // outgoingPendingRequestsCount is single-writer → atomic parent increment.
    applyShardedCounterDelta(tx, callerUid, { incomingPendingRequestsCount: -1 }, callerMigration);
    incrementParentCounterFields(tx, requesterId, { outgoingPendingRequestsCount: -1 });
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// blockUser
// Creates: blocks/{callerId}_{targetId}
// Side-effects:
//   • Cancels any follow in either direction
//   • Invalidates pending follow requests in either direction
//   • Updates counters atomically
// ─────────────────────────────────────────────────────────────────────────────

export const blockUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { targetUserId } = validateBlockInput(data);

  requireNotSelf(callerUid, targetUserId, 'block');
  await checkRateLimit(callerUid, 'block');

  const blockRef    = db().collection('blocks').doc(`${callerUid}_${targetUserId}`);
  // Relationships that may need cleanup
  const followABRef = db().collection('follows').doc(`${callerUid}_${targetUserId}`);
  const followBARef = db().collection('follows').doc(`${targetUserId}_${callerUid}`);
  const reqABRef    = db().collection('followRequests').doc(`${callerUid}_${targetUserId}`);
  const reqBARef    = db().collection('followRequests').doc(`${targetUserId}_${callerUid}`);
  const now         = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    // Reads first (all reads before any write).
    const [blockSnap, followAB, followBA, reqAB, reqBA] = await Promise.all([
      tx.get(blockRef),
      tx.get(followABRef),
      tx.get(followBARef),
      tx.get(reqABRef),
      tx.get(reqBARef),
    ]);

    // Idempotency
    if (blockSnap.exists) return;

    const callerMigration = await readCounterMigration(tx, callerUid);
    const targetMigration = await readCounterMigration(tx, targetUserId);

    // Compute counter deltas from the relationship snapshots (pure — no writes yet).
    const removeFollowAB  = followAB.exists;
    const removeFollowBA  = followBA.exists;
    const invalidateReqAB = reqAB.exists && reqAB.data()!['status'] === 'Pending';
    const invalidateReqBA = reqBA.exists && reqBA.data()!['status'] === 'Pending';

    let callerFollowingDelta = 0;
    let callerFollowersDelta = 0;
    let callerOutgoingDelta  = 0;
    let callerIncomingDelta  = 0;
    let targetFollowingDelta = 0;
    let targetFollowersDelta = 0;
    let targetOutgoingDelta  = 0;
    let targetIncomingDelta  = 0;

    if (removeFollowAB)  { callerFollowingDelta -= 1; targetFollowersDelta -= 1; }
    if (removeFollowBA)  { targetFollowingDelta -= 1; callerFollowersDelta -= 1; }
    if (invalidateReqAB) { callerOutgoingDelta  -= 1; targetIncomingDelta  -= 1; }
    if (invalidateReqBA) { targetOutgoingDelta  -= 1; callerIncomingDelta  -= 1; }

    // Writes.
    tx.set(blockRef, {
      blockerId: callerUid,
      blockedId: targetUserId,
      createdAt: now,
    });
    if (removeFollowAB)  tx.delete(followABRef);
    if (removeFollowBA)  tx.delete(followBARef);
    if (invalidateReqAB) tx.update(reqABRef, { status: 'InvalidatedByBlock', resolvedAt: now });
    if (invalidateReqBA) tx.update(reqBARef, { status: 'InvalidatedByBlock', resolvedAt: now });

    // Fan-in fields (followers/incoming) on BOTH parties → sharded (D-03). Single-writer
    // fields (following/outgoing) and blockedUsersCount (written only by the blocker) →
    // atomic parent increment. Decrements only run when the relationship existed → >= 0.
    applyShardedCounterDelta(tx, callerUid, {
      followersCount: callerFollowersDelta,
      incomingPendingRequestsCount: callerIncomingDelta,
    }, callerMigration);
    applyShardedCounterDelta(tx, targetUserId, {
      followersCount: targetFollowersDelta,
      incomingPendingRequestsCount: targetIncomingDelta,
    }, targetMigration);
    incrementParentCounterFields(tx, callerUid, {
      blockedUsersCount: 1,
      followingCount: callerFollowingDelta,
      outgoingPendingRequestsCount: callerOutgoingDelta,
    });
    incrementParentCounterFields(tx, targetUserId, {
      followingCount: targetFollowingDelta,
      outgoingPendingRequestsCount: targetOutgoingDelta,
    });
  });

  functions.logger.info('blockUser: complete', {
    callerRef: logSafeRef(callerUid),
    targetRef: logSafeRef(targetUserId),
  });
  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// unblockUser
// Deletes: blocks/{callerId}_{targetId}
// Updates: counters/{callerId}.blockedUsersCount
// ─────────────────────────────────────────────────────────────────────────────

export const unblockUser = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { targetUserId } = validateBlockInput(data);

  requireNotSelf(callerUid, targetUserId, 'unblock');
  await checkRateLimit(callerUid, 'unblock');

  const blockRef   = db().collection('blocks').doc(`${callerUid}_${targetUserId}`);

  await db().runTransaction(async (tx) => {
    const blockSnap = await tx.get(blockRef);

    if (!blockSnap.exists) return; // idempotent

    // Verify it's the caller's own block (cross-user write prevention)
    if (blockSnap.data()!['blockerId'] !== callerUid) {
      throw new functions.https.HttpsError('permission-denied', 'You cannot remove another user\'s block.');
    }

    tx.delete(blockRef);
    // blockedUsersCount is single-writer (only the blocker) → atomic parent decrement; runs
    // only because the block existed → stays >= 0.
    incrementParentCounterFields(tx, callerUid, { blockedUsersCount: -1 });
  });

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// aggregateCounterShards — Firestore trigger: materialize the counters/{uid} read-model
// ─────────────────────────────────────────────────────────────────────────────
// Keeps the CLIENT read-model counters/{uid} eventually consistent from the sharded
// write path (A1). Idempotent: on every shard write it re-sums the shards and merges the
// coerced (>= 0) totals of the fan-in fields (followersCount, incomingPendingRequestsCount)
// onto counters/{uid}, preserving the single-writer parent fields. It writes the PARENT
// (never a shard) so it never re-fires itself, and no-ops on an empty shard set so account
// deletion (which drops the shards before the parent) cannot resurrect a deleted counter.

export const aggregateCounterShards = functions.firestore
  .document('counters/{uid}/shards/{shardId}')
  .onWrite(async (_change, context) => {
    const uid = context.params['uid'] as string;
    const totals = await aggregateShardedCounterFields(uid);
    if (totals === null) return; // shard set empty — nothing to materialize
    await db().collection('counters').doc(uid).set(totals, { merge: true });
  });
