/**
 * accountDeletion.ts - `deleteAccount` callable (GDPR Art. 17 / store requirement)
 *
 * Envelope: requireAppCheck -> requireAuth -> recent reauth -> rate limit -> durable purge job
 * -> pseudonymize retained financial records -> delete Auth user.
 *
 * Durable contract:
 *  - account_deletion_jobs/{deleted_<hmac(uid)>} records progress and failures without storing
 *    the raw UID.
 *  - users/{uid} is marked DeletionPending before destructive work starts.
 *  - Storage cleanup is blocking. If Storage purge fails, Auth is not deleted and the job stays
 *    retryable.
 *  - Wallet/ledger/purchase records are retained for legal/accounting purposes, but walletId/userId
 *    fields and raw UID document IDs are migrated to a server-side pseudonym.
 *  - The Auth user is deleted last. The flow is idempotent so the client may retry.
 */

import * as crypto from 'crypto';
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';
import { COLLECTIONS } from './lib/walletSchema';
import { logSafeRef, logSafeStoragePrefix } from './lib/privacyLogging';
import { isProductionFirebaseProject } from './lib/projectEnvironment';

const db = () => admin.firestore();

/** Firestore commits cap at 500 writes; stay safely under. */
const DELETE_BATCH_SIZE = 400;
const ACCOUNT_DELETION_REAUTH_WINDOW_MS = 10 * 60 * 1000;

interface AccountDeletionResult {
  deleted: true;
  deletionJobId: string;
  correlationId: string;
}

function nowField(): admin.firestore.FieldValue {
  return admin.firestore.FieldValue.serverTimestamp();
}

/**
 * Local-only fallback secret. Used ONLY outside a production Firebase project
 * (emulator, CI, local tests) so unit tests need no env setup. In production a
 * missing secret is fail-closed — see deletionPseudonymSecret — so the
 * pseudonyms protecting retained financial records can never be derived from a
 * publicly-known constant.
 */
const LOCAL_FALLBACK_PSEUDONYM_SECRET = 'mindos-account-deletion-local-fallback';

/**
 * Resolves the HMAC secret used to pseudonymize retained records on deletion.
 * Fail-closed in production: if no real secret is configured, refuse to run
 * rather than emit predictable (reversible) pseudonyms that would defeat the
 * GDPR pseudonymization guarantee.
 */
function deletionPseudonymSecret(): string {
  const configured = process.env['ACCOUNT_DELETION_HMAC_SECRET']
    || process.env['PII_LOG_HMAC_SECRET']
    || process.env['LOG_REDACTION_SECRET'];
  if (configured) return configured;
  if (isProductionFirebaseProject()) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Account deletion pseudonymization secret is not configured. '
        + 'Set ACCOUNT_DELETION_HMAC_SECRET (or PII_LOG_HMAC_SECRET / LOG_REDACTION_SECRET).',
    );
  }
  return LOCAL_FALLBACK_PSEUDONYM_SECRET;
}

function deletionPseudonym(uid: string): string {
  const digest = crypto.createHmac('sha256', deletionPseudonymSecret())
    .update(uid)
    .digest('hex')
    .slice(0, 32);
  return `deleted_${digest}`;
}

function authTimeToMillis(authTime: unknown): number | null {
  if (typeof authTime !== 'number' || !Number.isFinite(authTime) || authTime <= 0) return null;
  return authTime > 10_000_000_000 ? authTime : authTime * 1000;
}

export function assertRecentAuthForAccountDeletion(
  context: functions.https.CallableContext,
  nowMs = Date.now(),
  maxAgeMs = ACCOUNT_DELETION_REAUTH_WINDOW_MS,
): void {
  const authTimeMs = authTimeToMillis(context.auth?.token?.auth_time);
  if (authTimeMs == null || nowMs - authTimeMs > maxAgeMs || authTimeMs > nowMs + 60_000) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Recent sign-in is required before deleting the account.',
    );
  }
}

function jobRefForPseudonym(pseudonym: string): admin.firestore.DocumentReference {
  return db().collection('account_deletion_jobs').doc(pseudonym);
}

async function markJob(
  jobRef: admin.firestore.DocumentReference,
  patch: Record<string, unknown>,
): Promise<void> {
  await jobRef.set({ ...patch, updatedAt: nowField() }, { merge: true });
}

async function markStep(
  jobRef: admin.firestore.DocumentReference,
  step: string,
  status: 'completed' | 'failed',
  extra: Record<string, unknown> = {},
): Promise<void> {
  await markJob(jobRef, {
    [`steps.${step}`]: {
      status,
      ...extra,
      at: nowField(),
    },
  });
}

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

/** Blocking Storage prefix cleanup. Failure keeps the deletion job retryable. */
async function deleteStoragePrefix(prefix: string): Promise<void> {
  await admin.storage().bucket().deleteFiles({ prefix });
}

async function moveSingleDoc(
  collection: string,
  oldId: string,
  newId: string,
  rewrite: Record<string, unknown>,
): Promise<boolean> {
  const oldRef = db().collection(collection).doc(oldId);
  const oldSnap = await oldRef.get();
  if (!oldSnap.exists) return false;
  const newRef = db().collection(collection).doc(newId);
  await db().runTransaction(async (tx) => {
    const current = await tx.get(oldRef);
    if (!current.exists) return;
    tx.set(newRef, {
      ...current.data(),
      ...rewrite,
      pseudonymizedAt: nowField(),
    }, { merge: true });
    tx.delete(oldRef);
  });
  return true;
}

function rewriteUidFields(
  data: Record<string, unknown>,
  uid: string,
  pseudonym: string,
): Record<string, unknown> {
  const update: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(data)) {
    if (value === uid && (
      key === 'uid'
      || key === 'userId'
      || key === 'walletId'
      || key === 'actorId'
      || key === 'targetWalletId'
      || key === 'adminId'
      || key === 'approverId'
    )) {
      update[key] = pseudonym;
    }
  }
  return update;
}

async function updateQueryDocs(
  query: admin.firestore.Query,
  uid: string,
  pseudonym: string,
): Promise<number> {
  const snapshot = await query.get();
  let batch = db().batch();
  let pending = 0;
  let updated = 0;

  for (const doc of snapshot.docs) {
    const rewrite = rewriteUidFields(doc.data(), uid, pseudonym);
    if (Object.keys(rewrite).length === 0) continue;
    batch.update(doc.ref, { ...rewrite, pseudonymizedAt: nowField() });
    pending += 1;
    updated += 1;
    if (pending >= DELETE_BATCH_SIZE) {
      await batch.commit();
      batch = db().batch();
      pending = 0;
    }
  }

  if (pending > 0) await batch.commit();
  return updated;
}

async function moveIdempotencyDocs(uid: string, pseudonym: string): Promise<number> {
  const snapshot = await db()
    .collection(COLLECTIONS.IDEMPOTENCY_KEYS)
    .where('walletId', '==', uid)
    .get();
  let moved = 0;
  for (const doc of snapshot.docs) {
    const data = doc.data();
    const key = typeof data['key'] === 'string' ? data['key'] : doc.id.replace(`${uid}_`, '');
    const newDocId = `${pseudonym}_${key}`;
    await db().runTransaction(async (tx) => {
      const current = await tx.get(doc.ref);
      if (!current.exists) return;
      tx.set(db().collection(COLLECTIONS.IDEMPOTENCY_KEYS).doc(newDocId), {
        ...current.data(),
        docId: newDocId,
        walletId: pseudonym,
        pseudonymizedAt: nowField(),
      }, { merge: true });
      tx.delete(doc.ref);
    });
    moved += 1;
  }
  return moved;
}

async function pseudonymizeRetainedFinancialRecords(
  uid: string,
  pseudonym: string,
): Promise<Record<string, number | boolean>> {
  const result: Record<string, number | boolean> = {};

  result.walletMoved = await moveSingleDoc(COLLECTIONS.WALLETS, uid, pseudonym, {
    walletId: pseudonym,
    userId: pseudonym,
  });
  result.snapshotMoved = await moveSingleDoc(COLLECTIONS.BALANCE_SNAPSHOTS, uid, pseudonym, {
    walletId: pseudonym,
  });
  result.idempotencyMoved = await moveIdempotencyDocs(uid, pseudonym);

  result.ledgerTransactionsUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.LEDGER_TRANSACTIONS).where('walletId', '==', uid),
    uid,
    pseudonym,
  );
  result.ledgerEntriesUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.LEDGER_ENTRIES).where('walletId', '==', uid),
    uid,
    pseudonym,
  );
  result.purchasesByWalletUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.PURCHASES).where('walletId', '==', uid),
    uid,
    pseudonym,
  );
  result.purchasesByUserUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.PURCHASES).where('userId', '==', uid),
    uid,
    pseudonym,
  );
  result.fraudSignalsByWalletUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.FRAUD_SIGNALS).where('walletId', '==', uid),
    uid,
    pseudonym,
  );
  result.fraudSignalsByUserUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.FRAUD_SIGNALS).where('userId', '==', uid),
    uid,
    pseudonym,
  );
  result.auditTargetsUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.WALLET_AUDIT_LOGS).where('targetWalletId', '==', uid),
    uid,
    pseudonym,
  );
  result.auditActorsUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.WALLET_AUDIT_LOGS).where('actorId', '==', uid),
    uid,
    pseudonym,
  );
  result.adminActionsUpdated = await updateQueryDocs(
    db().collection(COLLECTIONS.ADMIN_ACTIONS).where('targetWalletId', '==', uid),
    uid,
    pseudonym,
  );

  return result;
}

/**
 * Purges all of the user's PII and deletes the Auth user after critical purge completes.
 * Exported separately from the callable wrapper so it is unit-testable without the
 * App Check / auth / rate-limit envelope. Idempotent.
 */
export async function executeAccountDeletion(
  uid: string,
  correlationId: string = crypto.randomUUID(),
): Promise<AccountDeletionResult> {
  const pseudonym = deletionPseudonym(uid);
  const jobRef = jobRefForPseudonym(pseudonym);

  functions.logger.info('deleteAccount: start', {
    correlationId,
    uidRef: logSafeRef(uid, 'uid'),
    deletionJobId: pseudonym,
  });

  await markJob(jobRef, {
    jobId: pseudonym,
    correlationId,
    uidRef: logSafeRef(uid, 'uid'),
    status: 'running',
    startedAt: nowField(),
  });

  try {
    // 1) Mark the account before destructive work starts. Raw UID is only the document path
    // needed while Auth still exists; final tombstone is stored under the pseudonymized job ID.
    await db().collection('users').doc(uid).set(
      {
        status: 'DeletionPending',
        deletionJobId: pseudonym,
        deletionCorrelationId: correlationId,
        updatedAt: nowField(),
      },
      { merge: true },
    );
    await markStep(jobRef, 'account_pending', 'completed');

    // 2) Owned single-doc records.
    await db().collection('profiles').doc(uid).delete();
    await db().collection('profile_visuals').doc(uid).delete();
    await db().collection('privacy').doc(uid).delete();
    await db().collection('counters').doc(uid).delete();
    await db().collection('profile_cards_private').doc(uid).delete();
    await db().collection('profile_cards_public').doc(uid).delete();
    await markStep(jobRef, 'single_docs_deleted', 'completed');

    // 3) Username claim(s) -> freed.
    const usernamesDeleted = await deleteWhereField('usernames', 'uid', uid);

    // 4) Social graph (bidirectional - Firestore has no OR, so two queries each).
    const followsFromDeleted = await deleteWhereField('follows', 'followerId', uid);
    const followsToDeleted = await deleteWhereField('follows', 'followeeId', uid);
    const observationsFromDeleted = await deleteWhereField('observations', 'observerUserId', uid);
    const observationsToDeleted = await deleteWhereField('observations', 'observedUserId', uid);
    const followRequestsFromDeleted = await deleteWhereField('followRequests', 'requesterId', uid);
    const followRequestsToDeleted = await deleteWhereField('followRequests', 'targetId', uid);
    const blocksFromDeleted = await deleteWhereField('blocks', 'blockerId', uid);
    const blocksToDeleted = await deleteWhereField('blocks', 'blockedId', uid);
    await markStep(jobRef, 'social_graph_deleted', 'completed', {
      usernamesDeleted,
      followsFromDeleted,
      followsToDeleted,
      observationsFromDeleted,
      observationsToDeleted,
      followRequestsFromDeleted,
      followRequestsToDeleted,
      blocksFromDeleted,
      blocksToDeleted,
    });

    // 5) Behavioral data.
    const visitsByViewerDeleted = await deleteWhereField('profile_visits', 'viewerUserId', uid);
    const visitsByOwnerDeleted = await deleteWhereField('profile_visits', 'profileOwnerId', uid);
    const notificationsByActorDeleted = await deleteWhereField('notifications', 'actorUserId', uid);
    const notificationsByRecipientDeleted = await deleteWhereField('notifications', 'recipientUserId', uid);
    await markStep(jobRef, 'behavioral_data_deleted', 'completed', {
      visitsByViewerDeleted,
      visitsByOwnerDeleted,
      notificationsByActorDeleted,
      notificationsByRecipientDeleted,
    });

    // 6) Posts. Deleting top-level posts/{id} fires onPostDeleted (purges attachments + feed
    // fan-out). Then drop the per-user subtree (own posts mirror + materialized feed).
    const postsDeleted = await deleteWhereField('posts', 'userId', uid);
    const userPostsDeleted = await deleteByQuery(db().collection('user_posts').doc(uid).collection('posts'));
    const feedPostsDeleted = await deleteByQuery(db().collection('feeds').doc(uid).collection('posts'));
    await markStep(jobRef, 'posts_deleted', 'completed', {
      postsDeleted,
      userPostsDeleted,
      feedPostsDeleted,
    });

    // 7) Storage - user-owned media. dm_media is shared (D4) and retained.
    // Card media is reclaimed lazily by reconcileProfileCardMedia (durable backstop); purging it
    // here makes right-to-erasure immediate. Blocking + retryable on failure like the rest.
    await deleteStoragePrefix(`profile_visuals/${uid}/`);
    await deleteStoragePrefix(`post_attachments/${uid}/`);
    await deleteStoragePrefix(`profile_cards_private/${uid}/`);
    await deleteStoragePrefix(`profile_cards_public/${uid}/`);
    await markStep(jobRef, 'storage_deleted', 'completed', {
      profileVisualsPrefixRef: logSafeStoragePrefix(`profile_visuals/${uid}/`),
      postAttachmentsPrefixRef: logSafeStoragePrefix(`post_attachments/${uid}/`),
      profileCardsPrivatePrefixRef: logSafeStoragePrefix(`profile_cards_private/${uid}/`),
      profileCardsPublicPrefixRef: logSafeStoragePrefix(`profile_cards_public/${uid}/`),
    });

    // 8) Retained financial records are pseudonymized before Auth deletion.
    const retained = await pseudonymizeRetainedFinancialRecords(uid, pseudonym);
    await markStep(jobRef, 'retained_financial_records_pseudonymized', 'completed', retained);

    // 9) Remove the pending user account doc; the deletion job is the final tombstone.
    await db().collection('users').doc(uid).delete();
    await markStep(jobRef, 'user_doc_deleted', 'completed');

    // 10) Delete the Auth user LAST. Tolerate already-deleted (idempotent retry).
    try {
      await admin.auth().deleteUser(uid);
    } catch (error) {
      const code = (error as { code?: string } | null)?.code;
      if (code !== 'auth/user-not-found') {
        throw error;
      }
    }
    await markStep(jobRef, 'auth_deleted', 'completed');

    await markJob(jobRef, {
      status: 'completed',
      completedAt: nowField(),
    });

    functions.logger.info('deleteAccount: done', {
      correlationId,
      uidRef: logSafeRef(uid, 'uid'),
      deletionJobId: pseudonym,
    });

    return { deleted: true, deletionJobId: pseudonym, correlationId };
  } catch (error) {
    await markJob(jobRef, {
      status: 'failed',
      failureCode: (error as { code?: string } | null)?.code ?? 'unknown',
      failedAt: nowField(),
    });
    functions.logger.error('deleteAccount: failed', {
      correlationId,
      uidRef: logSafeRef(uid, 'uid'),
      deletionJobId: pseudonym,
      errorCode: (error as { code?: string } | null)?.code ?? 'unknown',
    });
    throw error;
  }
}

export const deleteAccount = functions.https.onCall(async (_data, context) => {
  requireAppCheck(context);
  const uid = requireAuth(context);
  assertRecentAuthForAccountDeletion(context);
  await checkRateLimit(uid, 'deleteAccount');

  return executeAccountDeletion(uid);
});
