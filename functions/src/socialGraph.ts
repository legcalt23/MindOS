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
 * Counters are NEVER trusted from the client; they are incremented/decremented
 * atomically inside the same transaction as the social-graph document.
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

const db = () => admin.firestore();
const FieldValue = admin.firestore.FieldValue;

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: safe counter increment/decrement clamped at 0
// ─────────────────────────────────────────────────────────────────────────────

function counterRef(uid: string) {
  return db().collection('counters').doc(uid);
}

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
  const callerCRef  = counterRef(callerUid);
  const targetCRef  = counterRef(targetUserId);
  const now         = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const [followSnap, callerCSnap, targetCSnap] = await Promise.all([
      tx.get(followRef),
      tx.get(callerCRef),
      tx.get(targetCRef),
    ]);

    // Idempotency: already following → no-op
    if (followSnap.exists) return;

    tx.set(followRef, {
      followerId: callerUid,
      followeeId: targetUserId,
      createdAt:  now,
    });

    // Increment counters (create doc if missing)
    const callerFollowing = (callerCSnap.data()?.['followingCount'] ?? 0) as number;
    const targetFollowers = (targetCSnap.data()?.['followersCount'] ?? 0) as number;

    tx.set(callerCRef, { followingCount: callerFollowing + 1 }, { merge: true });
    tx.set(targetCRef, { followersCount: targetFollowers + 1 }, { merge: true });
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
  const callerCRef = counterRef(callerUid);
  const targetCRef = counterRef(targetUserId);

  await db().runTransaction(async (tx) => {
    const [followSnap, callerCSnap, targetCSnap] = await Promise.all([
      tx.get(followRef),
      tx.get(callerCRef),
      tx.get(targetCRef),
    ]);

    if (!followSnap.exists) return; // idempotent

    const callerFollowing = (callerCSnap.data()?.['followingCount'] ?? 0) as number;
    const targetFollowers = (targetCSnap.data()?.['followersCount'] ?? 0) as number;

    tx.delete(followRef);
    tx.set(callerCRef, { followingCount: Math.max(0, callerFollowing - 1) }, { merge: true });
    tx.set(targetCRef, { followersCount: Math.max(0, targetFollowers - 1) }, { merge: true });
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
  const callerCRef = counterRef(callerUid);
  const targetCRef = counterRef(targetUserId);
  const targetPrivacyRef = db().collection('privacy').doc(targetUserId);
  const now        = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const [reqSnap, callerCSnap, targetCSnap, targetPrivacySnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(callerCRef),
      tx.get(targetCRef),
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

    tx.set(reqRef, {
      id:          requestId,
      requesterId: callerUid,
      targetId:    targetUserId,
      status:      'Pending',
      createdAt:   now,
      resolvedAt:  null,
    });

    const outgoing = (callerCSnap.data()?.['outgoingPendingRequestsCount'] ?? 0) as number;
    const incoming = (targetCSnap.data()?.['incomingPendingRequestsCount'] ?? 0) as number;
    tx.set(callerCRef, { outgoingPendingRequestsCount: outgoing + 1 }, { merge: true });
    tx.set(targetCRef, { incomingPendingRequestsCount: incoming + 1 }, { merge: true });
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

    const callerCRef = counterRef(callerUid);
    const targetCRef = counterRef(reqData['targetId'] as string);
    const [callerCSnap, targetCSnap] = await Promise.all([
      tx.get(callerCRef),
      tx.get(targetCRef),
    ]);

    tx.update(reqRef, { status: 'Canceled', resolvedAt: now });

    const outgoing = (callerCSnap.data()?.['outgoingPendingRequestsCount'] ?? 0) as number;
    const incoming = (targetCSnap.data()?.['incomingPendingRequestsCount'] ?? 0) as number;
    tx.set(callerCRef, { outgoingPendingRequestsCount: Math.max(0, outgoing - 1) }, { merge: true });
    tx.set(targetCRef, { incomingPendingRequestsCount: Math.max(0, incoming - 1) }, { merge: true });
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
    const [blockAB, blockBA] = await Promise.all([
      tx.get(db().collection('blocks').doc(`${requesterId}_${callerUid}`)),
      tx.get(db().collection('blocks').doc(`${callerUid}_${requesterId}`)),
    ]);
    if (blockAB.exists || blockBA.exists) {
      throw new functions.https.HttpsError('permission-denied', 'Cannot accept due to an existing block.');
    }

    const followRef  = db().collection('follows').doc(`${requesterId}_${callerUid}`);
    const reqCRef    = counterRef(requesterId);
    const callerCRef = counterRef(callerUid);
    const [followSnap, reqCSnap, callerCSnap] = await Promise.all([
      tx.get(followRef),
      tx.get(reqCRef),
      tx.get(callerCRef),
    ]);

    tx.update(reqRef, { status: 'Accepted', resolvedAt: now });

    if (!followSnap.exists) {
      tx.set(followRef, { followerId: requesterId, followeeId: callerUid, createdAt: now });

      const reqFollowing = (reqCSnap.data()?.['followingCount'] ?? 0) as number;
      const callerFollowers = (callerCSnap.data()?.['followersCount'] ?? 0) as number;
      const outgoing = (reqCSnap.data()?.['outgoingPendingRequestsCount'] ?? 0) as number;
      const incoming = (callerCSnap.data()?.['incomingPendingRequestsCount'] ?? 0) as number;

      tx.set(reqCRef,    {
        followingCount: reqFollowing + 1,
        outgoingPendingRequestsCount: Math.max(0, outgoing - 1),
      }, { merge: true });
      tx.set(callerCRef, {
        followersCount: callerFollowers + 1,
        incomingPendingRequestsCount: Math.max(0, incoming - 1),
      }, { merge: true });
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
    const reqCRef    = counterRef(requesterId);
    const callerCRef = counterRef(callerUid);
    const [reqCSnap, callerCSnap] = await Promise.all([
      tx.get(reqCRef),
      tx.get(callerCRef),
    ]);

    tx.update(reqRef, { status: 'Denied', resolvedAt: now });

    const outgoing = (reqCSnap.data()?.['outgoingPendingRequestsCount'] ?? 0) as number;
    const incoming = (callerCSnap.data()?.['incomingPendingRequestsCount'] ?? 0) as number;
    tx.set(reqCRef,    { outgoingPendingRequestsCount: Math.max(0, outgoing - 1) }, { merge: true });
    tx.set(callerCRef, { incomingPendingRequestsCount: Math.max(0, incoming - 1) }, { merge: true });
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
  const callerCRef  = counterRef(callerUid);
  const targetCRef  = counterRef(targetUserId);
  const now         = FieldValue.serverTimestamp();

  await db().runTransaction(async (tx) => {
    const [
      blockSnap, followAB, followBA, reqAB, reqBA,
      callerCSnap, targetCSnap,
    ] = await Promise.all([
      tx.get(blockRef),
      tx.get(followABRef),
      tx.get(followBARef),
      tx.get(reqABRef),
      tx.get(reqBARef),
      tx.get(callerCRef),
      tx.get(targetCRef),
    ]);

    // Idempotency
    if (blockSnap.exists) return;

    const callerData = callerCSnap.data() ?? {};
    const targetData = targetCSnap.data() ?? {};

    let callerFollowingDelta = 0;
    let callerFollowersDelta = 0;
    let callerOutgoingDelta  = 0;
    let callerIncomingDelta  = 0;
    let targetFollowingDelta = 0;
    let targetFollowersDelta = 0;
    let targetOutgoingDelta  = 0;
    let targetIncomingDelta  = 0;

    // Create block
    tx.set(blockRef, {
      blockerId: callerUid,
      blockedId: targetUserId,
      createdAt: now,
    });

    // Remove follow caller → target
    if (followAB.exists) {
      tx.delete(followABRef);
      callerFollowingDelta -= 1;
      targetFollowersDelta -= 1;
    }

    // Remove follow target → caller
    if (followBA.exists) {
      tx.delete(followBARef);
      targetFollowingDelta -= 1;
      callerFollowersDelta -= 1;
    }

    // Invalidate pending request caller → target
    if (reqAB.exists && reqAB.data()!['status'] === 'Pending') {
      tx.update(reqABRef, { status: 'InvalidatedByBlock', resolvedAt: now });
      callerOutgoingDelta -= 1;
      targetIncomingDelta -= 1;
    }

    // Invalidate pending request target → caller
    if (reqBA.exists && reqBA.data()!['status'] === 'Pending') {
      tx.update(reqBARef, { status: 'InvalidatedByBlock', resolvedAt: now });
      targetOutgoingDelta -= 1;
      callerIncomingDelta -= 1;
    }

    // Update counters
    tx.set(callerCRef, {
      blockedUsersCount:              Math.max(0, ((callerData['blockedUsersCount'] as number) ?? 0) + 1),
      followingCount:                 Math.max(0, ((callerData['followingCount']    as number) ?? 0) + callerFollowingDelta),
      followersCount:                 Math.max(0, ((callerData['followersCount']    as number) ?? 0) + callerFollowersDelta),
      outgoingPendingRequestsCount:   Math.max(0, ((callerData['outgoingPendingRequestsCount'] as number) ?? 0) + callerOutgoingDelta),
      incomingPendingRequestsCount:   Math.max(0, ((callerData['incomingPendingRequestsCount'] as number) ?? 0) + callerIncomingDelta),
    }, { merge: true });

    tx.set(targetCRef, {
      followingCount:                 Math.max(0, ((targetData['followingCount']    as number) ?? 0) + targetFollowingDelta),
      followersCount:                 Math.max(0, ((targetData['followersCount']    as number) ?? 0) + targetFollowersDelta),
      outgoingPendingRequestsCount:   Math.max(0, ((targetData['outgoingPendingRequestsCount'] as number) ?? 0) + targetOutgoingDelta),
      incomingPendingRequestsCount:   Math.max(0, ((targetData['incomingPendingRequestsCount'] as number) ?? 0) + targetIncomingDelta),
    }, { merge: true });
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
  const callerCRef = counterRef(callerUid);

  await db().runTransaction(async (tx) => {
    const [blockSnap, callerCSnap] = await Promise.all([
      tx.get(blockRef),
      tx.get(callerCRef),
    ]);

    if (!blockSnap.exists) return; // idempotent

    // Verify it's the caller's own block (cross-user write prevention)
    if (blockSnap.data()!['blockerId'] !== callerUid) {
      throw new functions.https.HttpsError('permission-denied', 'You cannot remove another user\'s block.');
    }

    tx.delete(blockRef);
    const blocked = (callerCSnap.data()?.['blockedUsersCount'] ?? 0) as number;
    tx.set(callerCRef, { blockedUsersCount: Math.max(0, blocked - 1) }, { merge: true });
  });

  return { success: true };
});
