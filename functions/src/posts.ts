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
import { requireAppCheck, requireAuth, requireEmailVerified, requireSelf, requireNotStorageSuspended } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';
import {
  CreatePostInput,
  PostAttachmentInput,
  validateCreatePostInput,
  validateRequestActionInput,
} from './lib/validators';
import { logSafeError, logSafeRef } from './lib/privacyLogging';

const db      = () => admin.firestore();
const storage = () => admin.storage();
const FieldValue = admin.firestore.FieldValue;

// ─────────────────────────────────────────────────────────────────────────────
// createPost
// ─────────────────────────────────────────────────────────────────────────────

export const createPost = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  requireEmailVerified(context);
  const input = validateCreatePostInput(data, callerUid);

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
  const authorIdentity = requireCanonicalAuthorIdentity(visualSnap.data());

  await validatePostAttachmentObjects(callerUid, input);

  await db().runTransaction(async (tx) => {
    const existingPost = await tx.get(postRef);
    if (existingPost.exists) {
      throw new functions.https.HttpsError('already-exists', 'Post ID already exists.');
    }

    const postDoc = {
      id:              input.postId,
      userId:          callerUid,                           // always from auth, not client
      authorUsername:  authorIdentity.username,  // always from DB, not client
      authorDisplayName: authorIdentity.displayName,
      postType:        input.postType,
      attachments:     input.attachments,
      text:            input.text    ?? null,
      description:     input.description ?? null,
      textStyle:       input.textStyle ?? null,
      createdAt:       now,
      updatedAt:       now,
    };

    tx.set(postRef,     postDoc);
    tx.set(userPostRef, postDoc);
  });

  functions.logger.info('createPost: complete', {
    uidRef: logSafeRef(callerUid),
    postRef: logSafeRef(input.postId),
  });
  return { success: true, postId: input.postId };
});

function requireCanonicalAuthorIdentity(data: admin.firestore.DocumentData | undefined): {
  username: string;
  displayName: string;
} {
  const username = data?.['username'];
  const displayName = data?.['displayName'];
  const validUsername = typeof username === 'string' &&
    /^[a-z0-9._]{3,30}$/.test(username) &&
    !username.startsWith('.') &&
    !username.endsWith('.') &&
    !username.includes('..');
  const validDisplayName = typeof displayName === 'string' &&
    displayName.length > 0 &&
    displayName.length <= 80;
  if (!validUsername || !validDisplayName) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Author profile identity is incomplete or invalid.',
    );
  }
  return { username, displayName };
}

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

  // Storage and feed cleanup are owned by onPostDeleted to keep deletePost idempotent.
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
      functions.logger.warn('onPostDeleted: missing userId', { postRef: logSafeRef(postId) });
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
    functions.logger.info('purgePostAttachments: complete', {
      deletedCount: files.length,
      postRef: logSafeRef(postId),
    });
  } catch (e) {
    functions.logger.error('purgePostAttachments failed', {
      postRef: logSafeRef(postId),
      error: logSafeError(e),
    });
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
    functions.logger.error('purgeFeedEntries failed', {
      postRef: logSafeRef(postId),
      error: logSafeError(e),
    });
  }
}

function isSafeStorageSegment(value: string): boolean {
  return /^[A-Za-z0-9_-]{3,128}$/.test(value) && !value.includes('..') && !value.includes('/') && !value.includes('\\');
}

type StorageObjectMetadata = {
  contentType?: string;
  size?: string | number;
  metadata?: Record<string, string | undefined>;
};

async function validatePostAttachmentObjects(ownerUid: string, input: CreatePostInput): Promise<void> {
  await Promise.all(input.attachments.map(async (attachment) => {
    await validatePostAttachmentObject(ownerUid, input.postId, attachment);
    await validatePostThumbnailObject(ownerUid, input.postId, attachment);
  }));
}

async function validatePostAttachmentObject(
  ownerUid: string,
  postId: string,
  attachment: PostAttachmentInput,
): Promise<void> {
  let metadata: StorageObjectMetadata;
  try {
    const [objectMetadata] = await storage().bucket().file(attachment.storagePath).getMetadata();
    metadata = objectMetadata as unknown as StorageObjectMetadata;
  } catch (error) {
    functions.logger.warn('createPost attachment object missing', {
      uidRef: logSafeRef(ownerUid, 'uid'),
      postRef: logSafeRef(postId),
      attachmentRef: logSafeRef(attachment.id, 'attachment'),
      error: logSafeError(error),
    });
    throw new functions.https.HttpsError('failed-precondition', 'Attachment file is not available.');
  }

  const custom = metadata.metadata ?? {};
  const size = Number(metadata.size ?? 0);
  const metadataMatches =
    custom['ownerUid'] === ownerUid &&
    custom['postId'] === postId &&
    custom['visibility'] === 'private';
  const payloadMatches =
    contentTypeMatches(metadata.contentType, attachment.mimeType) &&
    Number.isFinite(size) &&
    size === attachment.sizeBytes;

  if (!metadataMatches || !payloadMatches) {
    functions.logger.warn('createPost attachment object metadata mismatch', {
      uidRef: logSafeRef(ownerUid, 'uid'),
      postRef: logSafeRef(postId),
      attachmentRef: logSafeRef(attachment.id, 'attachment'),
      expectedType: attachment.mimeType,
      actualType: metadata.contentType ?? 'unknown',
    });
    throw new functions.https.HttpsError('failed-precondition', 'Attachment file does not match post metadata.');
  }
}

async function validatePostThumbnailObject(
  ownerUid: string,
  postId: string,
  attachment: PostAttachmentInput,
): Promise<void> {
  const thumbnailPath = attachment.thumbnailUrl;
  if (!thumbnailPath) return;

  let metadata: StorageObjectMetadata;
  try {
    const [objectMetadata] = await storage().bucket().file(thumbnailPath).getMetadata();
    metadata = objectMetadata as unknown as StorageObjectMetadata;
  } catch (error) {
    functions.logger.warn('createPost thumbnail object missing', {
      uidRef: logSafeRef(ownerUid, 'uid'),
      postRef: logSafeRef(postId),
      attachmentRef: logSafeRef(attachment.id, 'attachment'),
      error: logSafeError(error),
    });
    throw new functions.https.HttpsError('failed-precondition', 'Attachment thumbnail is not available.');
  }

  const custom = metadata.metadata ?? {};
  const size = Number(metadata.size ?? 0);
  const metadataMatches =
    custom['ownerUid'] === ownerUid &&
    custom['postId'] === postId &&
    custom['visibility'] === 'private';
  const payloadMatches =
    metadata.contentType === 'image/jpeg' &&
    Number.isFinite(size) &&
    size > 0 &&
    size <= 1_048_576;

  if (!metadataMatches || !payloadMatches) {
    functions.logger.warn('createPost thumbnail object metadata mismatch', {
      uidRef: logSafeRef(ownerUid, 'uid'),
      postRef: logSafeRef(postId),
      attachmentRef: logSafeRef(attachment.id, 'attachment'),
      actualType: metadata.contentType ?? 'unknown',
    });
    throw new functions.https.HttpsError('failed-precondition', 'Attachment thumbnail does not match post metadata.');
  }
}

function contentTypeMatches(actual: string | undefined, expected: string): boolean {
  if (expected === 'audio/wav') {
    return actual === 'audio/wav' ||
      actual === 'audio/x-wav' ||
      actual === 'audio/wave' ||
      actual === 'audio/vnd.wave';
  }
  return actual === expected;
}
