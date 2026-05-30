/**
 * guards.ts
 * Shared security guards: requireAuth, requireAppCheck, cross-user write prevention,
 * block checks, and privacy checks via Admin SDK reads.
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

const db = () => admin.firestore();

// ── Auth & App Check ───────────────────────────────────────────────────────

/** Ensures the caller is authenticated. Returns the verified UID. */
export function requireAuth(context: functions.https.CallableContext): string {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'You must be signed in to call this function.',
    );
  }
  return context.auth.uid;
}

/**
 * Ensures the call comes from a Firebase App-Check-verified client.
 * If your project has App Check in "enforcement" mode this is a double-check;
 * keeping it here ensures the callable layer also validates independently.
 */
export function requireAppCheck(context: functions.https.CallableContext): void {
  if (context.app === undefined) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called from a verified App Check client.',
    );
  }
}

// ── Cross-user write protection ────────────────────────────────────────────

/**
 * Verifies that the caller is not attempting to write data that belongs
 * to a different user. Throws 'permission-denied' if callerUid !== ownerUid.
 */
export function requireSelf(callerUid: string, ownerUid: string, context = 'resource'): void {
  if (callerUid !== ownerUid) {
    throw new functions.https.HttpsError(
      'permission-denied',
      `You cannot modify another user's ${context}.`,
    );
  }
}

/**
 * Verifies that the caller is NOT targeting themselves.
 * (e.g. you cannot follow/block yourself)
 */
export function requireNotSelf(callerUid: string, targetUid: string, action: string): void {
  if (callerUid === targetUid) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      `You cannot ${action} yourself.`,
    );
  }
}

// ── Block checks ───────────────────────────────────────────────────────────

/** Returns true if a block document exists in either direction between the two UIDs. */
export async function hasBlockBetween(uidA: string, uidB: string): Promise<boolean> {
  const [snapAB, snapBA] = await Promise.all([
    db().collection('blocks').doc(`${uidA}_${uidB}`).get(),
    db().collection('blocks').doc(`${uidB}_${uidA}`).get(),
  ]);
  return snapAB.exists || snapBA.exists;
}

/** Throws 'permission-denied' if any block exists between uidA and uidB. */
export async function requireNoBlock(uidA: string, uidB: string): Promise<void> {
  if (await hasBlockBetween(uidA, uidB)) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'This action is not available due to a block between these users.',
    );
  }
}

// ── Privacy checks ─────────────────────────────────────────────────────────

export type ProfileVisibility = 'Public' | 'Private';

/** Reads the target's privacy document and returns their profileVisibility. */
export async function getProfileVisibility(targetUid: string): Promise<ProfileVisibility> {
  const snap = await db().collection('privacy').doc(targetUid).get();
  if (!snap.exists) return 'Private'; // treat missing privacy doc as private
  return (snap.data()!['profileVisibility'] as ProfileVisibility) ?? 'Private';
}

/** Returns true if callerUid already follows targetUid. */
export async function isFollowing(callerUid: string, targetUid: string): Promise<boolean> {
  const snap = await db().collection('follows').doc(`${callerUid}_${targetUid}`).get();
  return snap.exists;
}

/**
 * For a private account, ensures callerUid is already following targetUid.
 * Throws 'permission-denied' otherwise.
 */
export async function requireCanReadTarget(callerUid: string, targetUid: string): Promise<void> {
  const visibility = await getProfileVisibility(targetUid);
  if (visibility === 'Private' && !(await isFollowing(callerUid, targetUid))) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'This profile is private.',
    );
  }
}

// ── Suspend check ──────────────────────────────────────────────────────────

/** Throws 'permission-denied' if the user's storage has been suspended. */
export async function requireNotStorageSuspended(uid: string): Promise<void> {
  const snap = await db().collection('storage_usage').doc(uid).get();
  if (snap.exists && snap.data()!['isSuspended'] === true) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Your account has been suspended due to storage abuse.',
    );
  }
}
