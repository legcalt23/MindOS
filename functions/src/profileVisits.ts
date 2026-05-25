/**
 * profileVisits.ts
 * Callable Cloud Function: recordProfileVisit
 *
 * SECURITY CONTRACT:
 *  1. requireAppCheck — reject unverified clients
 *  2. requireAuth     — reject unauthenticated
 *  3. validate input  — strict schema
 *  4. self-visit      — silently ignored (no-op)
 *  5. block check     — no visit recorded if block exists in either direction
 *  6. rate limit      — 60 visits/min per UID (prevents enumeration spam)
 *  7. deduplication   — dedupe by (visitor, target, 10-min window)
 *  8. Admin SDK write — upsert profile_visits/{viewerUid}_{ownerUid}
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { requireAppCheck, requireAuth, hasBlockBetween } from './lib/guards';
import { checkRateLimit } from './lib/rateLimiter';
import { validateProfileVisitInput } from './lib/validators';

const db = () => admin.firestore();
// admin.firestore.FieldValue not needed here (writes use plain numbers)

/** Deduplication window: only record one visit per (visitor, target) pair per 10 min */
const DEDUP_WINDOW_MS = 10 * 60 * 1000;

export const recordProfileVisit = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const callerUid = requireAuth(context);
  const { profileOwnerId } = validateProfileVisitInput(data);

  // Self-visits: silent no-op (not an error, just meaningless)
  if (callerUid === profileOwnerId) return { success: true, recorded: false };

  // No visit if there's a block in either direction
  if (await hasBlockBetween(callerUid, profileOwnerId)) {
    return { success: true, recorded: false };
  }

  await checkRateLimit(callerUid, 'profileVisit');

  const visitId  = `${callerUid}_${profileOwnerId}`;
  const visitRef = db().collection('profile_visits').doc(visitId);
  const now      = Date.now();

  await db().runTransaction(async (tx) => {
    const snap = await tx.get(visitRef);

    if (!snap.exists) {
      // First ever visit
      tx.set(visitRef, {
        viewerUserId:    callerUid,
        profileOwnerId,
        hasVisitedProfile: true,
        firstVisitedAt:  now,
        lastVisitedAt:   now,
      });
      return;
    }

    const existingData = snap.data()!;
    const lastVisited  = existingData['lastVisitedAt'] as number;

    // Deduplication: skip if visited within the dedup window
    if (now - lastVisited < DEDUP_WINDOW_MS) return;

    tx.update(visitRef, {
      lastVisitedAt:     now,
      hasVisitedProfile: true,
    });
  });

  return { success: true, recorded: true };
});
