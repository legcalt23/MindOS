/**
 * posts.ts
 * Callable Cloud Functions: createPost, deletePost
 * Trigger:          onPostDeleted — purge attachments + feed copies
 *
 * SECURITY CONTRACT (createPost):
 *  1. requireAppCheck       — reject unverified clients
 *  2. requireAuth           — reject unauthenticated
 *  3. validateCreatePostInput — strict schema + attachment limits
 *  4. requireNotStorageSuspended — suspended users can't post
 *  5. checkRateLimit         — 10 posts/min per UID
 *  6. ownership enforcement  — data.userId MUST equal callerUid
 *  7. Admin SDK write        — writes to posts/ and user_posts/{uid}/posts/
 *
 * SECURITY CONTRACT (deletePost):
 *  1–2. requireAppCheck + requireAuth
 *  3. validate postId param
 *  4. ownership check        — resource.data.userId must equal callerUid
 *  5. rate limit
 *  6. Admin SDK delete       — deletes posts/ and user_posts/{uid}/posts/ atomically
 *
 * onPostDeleted Trigger:
 *  • Purges Storage attachments under post_attachments/{userId}/{postId}/
 *  • Purges materialized feed copies at feeds/{viewerId}/posts/{postId}
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth, requireSelf, requireNotStorageSuspended } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';
import { validateCreatePostInput, validateRequestActionInput } from './lib/validators';

const db      = () => admin.firestore();
const storage = () => admin.storage();
const FieldValue = admin.firestore.FieldValue;

// ─────────────────────────────────────────────────────────────────────────────
// createPost
// ─────────────────────────────────────────────────────────────────────────────

export const createPost = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const input = validateCreatePostInput(data);

  // Suspended users cannot create posts
  await requireNotStorageSuspended(callerUid);

  // Ownership: postType payload must be for the calling user
  // (the caller provides postId; userId is inferred from auth — NOT trusted from client)
  await checkRateLimit(callerUid, 'createPost');

  const postRef      = db().collection('posts').doc(input.postId);
  const userPostRef  = db().collection('user_posts').doc(callerUid)
    .collection('posts').doc(input.postId);
  const now          = FieldValue.serverTimestamp();

  // Fetch author identity from trusted source (not from client)
  const visualSnap = await db().collection('profile_visuals').doc(callerUid).get();
  if (!visualSnap.exists) {
    throw new functions.https.HttpsError('not-found', 'Author profile not found.');
  }
  const visualData = visualSnap.data()!;

  await db().runTransaction(async (tx) => {
    const existingPost = await tx.get(postRef);
    if (existingPost.exists) {
      throw new functions.https.HttpsError('already-exists', 'Post ID already exists.');
    }

    const postDoc = {
      id:              input.postId,
      userId:          callerUid,                           // always from auth, not client
      authorUsername:  visualData['username']  as string,  // always from DB, not client
      authorDisplayName: visualData['displayName'] as string,
      postType:        input.postType,
      attachments:     input.attachments,
      text:            input.text    ?? null,
      description:     input.description ?? null,
      createdAt:       now,
      updatedAt:       now,
    };

    tx.set(postRef,     postDoc);
    tx.set(userPostRef, postDoc);
  });

  functions.logger.info(`createPost: uid=${callerUid} postId=${input.postId}`);
  return { success: true, postId: input.postId };
});

// ─────────────────────────────────────────────────────────────────────────────
// deletePost
// ─────────────────────────────────────────────────────────────────────────────

export const deletePost = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { requestId: postId } = validateRequestActionInput(data); // requestId reused as postId
  await checkRateLimit(callerUid, 'deletePost');

  const postRef     = db().collection('posts').doc(postId);
  const userPostRef = db().collection('user_posts').doc(callerUid)
    .collection('posts').doc(postId);

  await db().runTransaction(async (tx) => {
    const snap = await tx.get(postRef);
    if (!snap.exists) return; // idempotent

    // Cross-user write protection
    requireSelf(callerUid, snap.data()!['userId'] as string, 'post');

    tx.delete(postRef);
    tx.delete(userPostRef);
  });

  // Kick off attachment purge asynchronously (best-effort — retried by trigger)
  purgePostAttachments(callerUid, postId).catch((e) =>
    functions.logger.error('purgePostAttachments error', e),
  );

  return { success: true };
});

// ─────────────────────────────────────────────────────────────────────────────
// onPostDeleted Firestore trigger
// Purges Storage files + materialized feed entries
// ─────────────────────────────────────────────────────────────────────────────

export const onPostDeleted = functions.firestore
  .document('posts/{postId}')
  .onDelete(async (snap, context) => {
    const postId = context.params['postId'] as string;
    const data   = snap.data();
    if (!data) return;

    const userId = data['userId'] as string | undefined;
    if (!userId) {
      functions.logger.warn(`onPostDeleted: missing userId for postId=${postId}`);
      return;
    }

    await Promise.all([
      purgePostAttachments(userId, postId),
      purgeFeedEntries(postId),
    ]);
  });

/** Deletes all files under post_attachments/{userId}/{postId}/ */
async function purgePostAttachments(userId: string, postId: string): Promise<void> {
  if (!isSafeStorageSegment(userId) || !isSafeStorageSegment(postId)) {
    functions.logger.warn('purgePostAttachments skipped unsafe storage segment');
    return;
  }
  const prefix = `post_attachments/${userId}/${postId}/`;
  try {
    const bucket    = storage().bucket();
    const [files]   = await bucket.getFiles({ prefix });
    await Promise.all(files.map((f) => f.delete()));
    functions.logger.info(`purgePostAttachments: deleted ${files.length} files for postId=${postId}`);
  } catch (e) {
    functions.logger.error(`purgePostAttachments failed for postId=${postId}:`, e);
  }
}

/** Deletes materialized feed entries for a deleted post (best-effort, paginated). */
async function purgeFeedEntries(postId: string): Promise<void> {
  if (!isSafeStorageSegment(postId)) {
    functions.logger.warn('purgeFeedEntries skipped unsafe postId');
    return;
  }
  const BATCH_SIZE = 100;
  try {
    const query = db().collectionGroup('posts')
      .where('postId', '==', postId)
      .limit(BATCH_SIZE);

    let snaps = await query.get();
    while (!snaps.empty) {
      const batch = db().batch();
      snaps.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
      if (snaps.size < BATCH_SIZE) break;
      snaps = await query.get();
    }
  } catch (e) {
    functions.logger.error(`purgeFeedEntries failed for postId=${postId}:`, e);
  }
}

function isSafeStorageSegment(value: string): boolean {
  return /^[A-Za-z0-9_-]{3,128}$/.test(value) && !value.includes('..') && !value.includes('/') && !value.includes('\\');
}
