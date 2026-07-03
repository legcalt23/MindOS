/**
 * profileCardGc.ts — scheduled Cloud Function that reclaims orphaned profile-card media.
 *
 * Thin adapter: builds the real Admin-SDK I/O and delegates ALL logic to the pure, unit-tested core
 * in lib/profileCardMediaGc.ts (see its SAFETY CONTRACT). Enumerates users from Storage so media left
 * by deleted accounts (whose docs are gone) is also reclaimed.
 *
 * Operational env switches (safe first-run controls):
 *   PROFILE_CARD_GC_DRY_RUN     — '1'|'true' ⇒ plan + log, delete nothing.
 *   PROFILE_CARD_GC_GRACE_HOURS — integer ⇒ override the unreferenced-object grace window (default 48h).
 *
 * Logs counts only (no UIDs, no Storage paths) — the summary is PII-free by construction.
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { logSafeError } from './lib/privacyLogging';
import {
  CardDocCollection,
  CardMediaGcIo,
  CardMediaObject,
  CARD_MEDIA_STORAGE_PREFIXES,
  DEFAULT_GRACE_MS,
  isCardMediaPath,
  reconcileAllCardMedia,
} from './lib/profileCardMediaGc';

const db = () => admin.firestore();

function isDryRun(): boolean {
  const value = process.env['PROFILE_CARD_GC_DRY_RUN'];
  return value === '1' || value === 'true';
}

function graceMsFromEnv(): number {
  const raw = process.env['PROFILE_CARD_GC_GRACE_HOURS'];
  if (!raw) return DEFAULT_GRACE_MS;
  const hours = Number.parseInt(raw, 10);
  return Number.isFinite(hours) && hours > 0 ? hours * 60 * 60 * 1000 : DEFAULT_GRACE_MS;
}

function objectCreatedAtMs(metadata: { timeCreated?: string; updated?: string } | undefined): number {
  const stamp = metadata?.timeCreated ?? metadata?.updated;
  return stamp ? Date.parse(stamp) : Number.NaN;
}

/** Builds the Admin-SDK-backed I/O used by the reconciliation core. */
export function buildAdminCardMediaGcIo(): CardMediaGcIo {
  const bucket = () => admin.storage().bucket();

  return {
    async listAllObjects(): Promise<CardMediaObject[]> {
      const objects: CardMediaObject[] = [];
      for (const prefix of CARD_MEDIA_STORAGE_PREFIXES) {
        const [files] = await bucket().getFiles({ prefix });
        for (const file of files) {
          if (!isCardMediaPath(file.name)) continue;
          objects.push({ path: file.name, createdAtMs: objectCreatedAtMs(file.metadata) });
        }
      }
      return objects;
    },
    async readDoc(collection: CardDocCollection, uid: string) {
      const snap = await db().collection(collection).doc(uid).get();
      if (!snap.exists) return { exists: false, payload: null };
      const payload = snap.data()?.['payload'];
      return { exists: true, payload: typeof payload === 'string' ? payload : null };
    },
    async deleteObject(path: string): Promise<void> {
      await bucket().file(path).delete();
    },
    nowMs(): number {
      return Date.now();
    },
  };
}

/**
 * Scheduled daily reconciliation. Throwing marks the run failed (retried on the next schedule);
 * per-object delete failures are tolerated and counted inside the core.
 */
export const reconcileProfileCardMedia = functions.pubsub
  .schedule('every 24 hours')
  .onRun(async () => {
    const dryRun = isDryRun();
    const graceMs = graceMsFromEnv();
    functions.logger.info('profileCardMediaGc: start', { dryRun, graceHours: graceMs / 3_600_000 });
    try {
      const summary = await reconcileAllCardMedia(buildAdminCardMediaGcIo(), { dryRun, graceMs });
      functions.logger.info('profileCardMediaGc: complete', { ...summary });
      return null;
    } catch (error) {
      functions.logger.error('profileCardMediaGc: failed', { error: logSafeError(error) });
      throw error;
    }
  });
