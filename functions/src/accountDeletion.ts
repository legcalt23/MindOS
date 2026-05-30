/**
 * accountDeletion.ts — `deleteAccount` callable (GDPR Art. 17 / store requirement)
 *
 * Envelope: requireAppCheck → requireAuth → checkRateLimit → purge → delete Auth user.
 *
 * Design (see docs/security/ACCOUNT_DELETION_DECISION.md):
 *  - PURGE the user's PII across Firestore + Storage.
 *  - RETAIN the financial ledger/wallet/purchases UNTOUCHED — legal obligation (the coins are
 *    sold for real money, D5=yes). No email/name lives there; `uid` is an opaque key, so
 *    retention leaks no PII beyond what profile deletion already removes. Deeper uid→hash
 *    pseudonymization is a documented follow-up (needs ledger-schema review) and is deliberately
 *    NOT attempted here to avoid corrupting the double-entry ledger / reconciliation.
 *  - DMs: NOT modified (D4). Deleting the user's profile already anonymizes them (their uid no
 *    longer resolves to a name/avatar); the other participant keeps the conversation. dm_media is
 *    shared and retained.
 *  - The Auth user is deleted LAST, so a mid-run failure leaves the user able to retry (idempotent).
 *
 * Idempotent: every step tolerates already-deleted state, so the client may safely retry.
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';

const db = () => admin.firestore();

/** Firestore commits cap at 500 writes; stay safely under. */
const DELETE_BATCH_SIZE = 400;

/** Deletes every document returned by [query], in batches. Returns the count deleted. */
async function deleteByQuery(query: admin.firestore.Query): Promise<number> {
  const snapshot = await query.get();
  let batch = db().batch();
  let pending = 0;
  let deleted = 0;
  for (const doc of snapshot.docs) {
    batch.delete(doc.ref);
    pending += 1;
    deleted += 1;
    if (pending >= DELETE_BATCH_SIZE) {
      await batch.commit();
      batch = db().batch();
      pending = 0;
    }
  }
  if (pending > 0) {
    await batch.commit();
  }
  return deleted;
}

/** Deletes all docs in [collection] where [field] == [uid]. */
function deleteWhereField(collection: string, field: string, uid: string): Promise<number> {
  return deleteByQuery(db().collection(collection).where(field, '==', uid));
}

/** Best-effort Storage prefix cleanup — never blocks account deletion. */
async function deleteStoragePrefix(prefix: string): Promise<void> {
  try {
    await admin.storage().bucket().deleteFiles({ prefix });
  } catch (error) {
    functions.logger.warn(`deleteAccount: storage cleanup failed for prefix=${prefix}`, error);
  }
}

/**
 * Purges all of the user's PII (Firestore + Storage) and then deletes the Auth user.
 * Exported separately from the callable wrapper so it is unit-testable without the
 * App Check / auth / rate-limit envelope. Idempotent.
 */
export async function executeAccountDeletion(uid: string): Promise<void> {
  functions.logger.info(`deleteAccount: start uid=${uid}`);

  // 1) Owned single-doc records.
  await db().collection('profiles').doc(uid).delete();
  await db().collection('profile_visuals').doc(uid).delete();
  await db().collection('privacy').doc(uid).delete();
  await db().collection('counters').doc(uid).delete();

  // 2) Account record → soft-deleted (schema reserves hard-delete to nobody).
  await db().collection('users').doc(uid).set(
    {
      status: 'Deleted',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  // 3) Username claim(s) → freed.
  await deleteWhereField('usernames', 'uid', uid);

  // 4) Social graph (bidirectional — Firestore has no OR, so two queries each).
  await deleteWhereField('follows', 'followerId', uid);
  await deleteWhereField('follows', 'followeeId', uid);
  await deleteWhereField('observations', 'observerUserId', uid);
  await deleteWhereField('observations', 'observedUserId', uid);
  await deleteWhereField('followRequests', 'requesterId', uid);
  await deleteWhereField('followRequests', 'targetId', uid);
  await deleteWhereField('blocks', 'blockerId', uid);
  await deleteWhereField('blocks', 'blockedId', uid);

  // 5) Behavioral data.
  await deleteWhereField('profile_visits', 'viewerUserId', uid);
  await deleteWhereField('profile_visits', 'profileOwnerId', uid);
  await deleteWhereField('notifications', 'actorUserId', uid);
  await deleteWhereField('notifications', 'recipientUserId', uid);

  // 6) Posts. Deleting top-level posts/{id} fires onPostDeleted (purges attachments + feed
  //    fan-out). Then drop the per-user subtree (own posts mirror + materialized feed).
  await deleteWhereField('posts', 'userId', uid);
  await deleteByQuery(db().collection('user_posts').doc(uid).collection('posts'));
  await deleteByQuery(db().collection('feeds').doc(uid).collection('posts'));

  // 7) Storage — user-owned media. dm_media is shared (D4) → retained.
  await deleteStoragePrefix(`profile_visuals/${uid}/`);
  await deleteStoragePrefix(`post_attachments/${uid}/`);

  // 8) Wallet/ledger/purchases: RETAINED untouched (legal obligation, D5=yes). See header.

  // 9) Delete the Auth user LAST. Tolerate already-deleted (idempotent retry).
  try {
    await admin.auth().deleteUser(uid);
  } catch (error) {
    const code = (error as { code?: string } | null)?.code;
    if (code !== 'auth/user-not-found') {
      throw error;
    }
  }

  functions.logger.info(`deleteAccount: done uid=${uid}`);
}

export const deleteAccount = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = requireAuth(context);
  await checkRateLimit(uid, 'deleteAccount');

  await executeAccountDeletion(uid);

  return { deleted: true, uid };
});
