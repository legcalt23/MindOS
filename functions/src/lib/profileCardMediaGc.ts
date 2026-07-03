/**
 * profileCardMediaGc.ts — pure reconciliation core for the profile-card orphan-media GC.
 *
 * WHY THIS EXISTS
 * The KMP client controls orphan media primarily by delete-on-mutation (replace/remove/reset/
 * discard delete the previous object best-effort). That misses *crash orphans*: an upload that
 * succeeds but whose Firestore commit/cleanup never runs (process killed mid-flow) leaves an
 * unreferenced object in Storage. Reclaiming those was deliberately NOT done client-side: a client
 * list-and-delete sweep risks deleting *referenced* media if a doc read is incomplete/transient
 * (data-loss). The safe owner is this server-side job.
 *
 * SAFETY CONTRACT (an object is deleted ONLY if ALL hold):
 *   1. It lives under profile_cards_{private|public}/{uid}/media/ (a card-media object).
 *   2. Its path is NOT referenced by the owner's private OR public card doc. Reference extraction is
 *      deliberately OVER-INCLUSIVE: every string anywhere in the parsed payload that looks like one
 *      of THIS uid's media paths is treated as referenced, so schema drift can never strand a
 *      referenced object.
 *   3. The object is older than a grace period (covers the upload→commit race and clock skew).
 *   4. Its age is known. Unknown age ⇒ never deleted.
 * If a card doc EXISTS but its payload is missing or unparseable, the whole user is SKIPPED (we
 * cannot enumerate references, so we delete nothing). Admin-SDK reads are strongly consistent, so a
 * present doc is the authoritative reference set.
 *
 * This file has NO firebase-admin / firebase-functions imports: all logic is pure and unit-tested
 * with plain fakes. The scheduled Cloud Function wrapper (profileCardGc.ts) injects real I/O.
 */

/** Firestore collections that hold profile-card documents (the reference source of truth). */
export type CardDocCollection = 'profile_cards_private' | 'profile_cards_public';

export const CARD_DOC_COLLECTIONS: readonly CardDocCollection[] = [
  'profile_cards_private',
  'profile_cards_public',
];

/** Top-level Storage prefixes under which a user's card media lives. */
export const CARD_MEDIA_STORAGE_PREFIXES: readonly string[] = [
  'profile_cards_private/',
  'profile_cards_public/',
];

/** Default age below which an unreferenced object is protected (upload→commit race + skew). */
export const DEFAULT_GRACE_MS = 48 * 60 * 60 * 1000; // 48h

/** Backstop so a single run can never fan out without bound; overflow is logged, never silent. */
export const DEFAULT_MAX_USERS = 5_000;

/** profile_cards_{private|public}/{uid}/media/{file} — {uid} is group 1, {file} is one segment. */
const CARD_MEDIA_PATH = /^profile_cards_(?:private|public)\/([^/]+)\/media\/([^/]+)$/;

/** A Storage object that belongs to a user's card-media folder. */
export interface CardMediaObject {
  /** Full object path, e.g. profile_cards_public/u1/media/m1.jpg */
  readonly path: string;
  /** Creation time in epoch ms; NaN/non-finite when unknown. */
  readonly createdAtMs: number;
}

/** The state of one card document for a user. */
export interface CardDocState {
  readonly exists: boolean;
  /** The serialized JSON payload string; null when absent or not a string. */
  readonly payload: string | null;
}

export interface UserOrphanPlan {
  readonly uid: string;
  readonly totalObjects: number;
  readonly referencedCount: number;
  readonly toDelete: readonly string[];
  /** Unreferenced but within the grace window. */
  readonly protectedRecent: number;
  /** Unreferenced but age could not be determined. */
  readonly protectedUnknownAge: number;
  /** True when references could not be enumerated; nothing is deleted for this user. */
  readonly skipped: boolean;
  readonly skipReason?: 'unparseable-doc';
}

export function isCardMediaPath(path: string): boolean {
  return CARD_MEDIA_PATH.test(path);
}

/** Returns the owning uid of a card-media path, or null when the path is not card media. */
export function ownerUidOfCardMedia(path: string): string | null {
  const match = CARD_MEDIA_PATH.exec(path);
  return match ? match[1] : null;
}

function collectStrings(value: unknown, sink: (s: string) => void): void {
  if (typeof value === 'string') {
    sink(value);
    return;
  }
  if (Array.isArray(value)) {
    for (const item of value) collectStrings(item, sink);
    return;
  }
  if (value && typeof value === 'object') {
    for (const item of Object.values(value as Record<string, unknown>)) {
      collectStrings(item, sink);
    }
  }
}

/**
 * Over-inclusive reference extraction. Parses [payload] and returns every string within it that is a
 * card-media path owned by [uid]. Returns null when the payload cannot be parsed — the caller MUST
 * then skip deletion for that user (references are unknown).
 */
export function referencedMediaPathsFromPayload(payload: string, uid: string): Set<string> | null {
  let parsed: unknown;
  try {
    parsed = JSON.parse(payload);
  } catch {
    return null;
  }
  const referenced = new Set<string>();
  collectStrings(parsed, (candidate) => {
    if (ownerUidOfCardMedia(candidate) === uid) {
      referenced.add(candidate);
    }
  });
  return referenced;
}

function skipPlan(uid: string, totalObjects: number): UserOrphanPlan {
  return {
    uid,
    totalObjects,
    referencedCount: 0,
    toDelete: [],
    protectedRecent: 0,
    protectedUnknownAge: 0,
    skipped: true,
    skipReason: 'unparseable-doc',
  };
}

/**
 * Pure planner: given a user's media objects and both card docs, decides which objects are safe to
 * delete. Never performs I/O. See the SAFETY CONTRACT at the top of the file.
 */
export function planUserOrphans(
  uid: string,
  objects: readonly CardMediaObject[],
  privateDoc: CardDocState,
  publicDoc: CardDocState,
  nowMs: number,
  graceMs: number,
): UserOrphanPlan {
  const referenced = new Set<string>();
  for (const doc of [privateDoc, publicDoc]) {
    if (!doc.exists) continue;
    if (typeof doc.payload !== 'string') {
      return skipPlan(uid, objects.length);
    }
    const refs = referencedMediaPathsFromPayload(doc.payload, uid);
    if (refs === null) {
      return skipPlan(uid, objects.length);
    }
    refs.forEach((path) => referenced.add(path));
  }

  const toDelete: string[] = [];
  let protectedRecent = 0;
  let protectedUnknownAge = 0;
  for (const obj of objects) {
    if (referenced.has(obj.path)) continue;
    if (!Number.isFinite(obj.createdAtMs)) {
      protectedUnknownAge += 1;
      continue;
    }
    if (nowMs - obj.createdAtMs <= graceMs) {
      protectedRecent += 1;
      continue;
    }
    toDelete.push(obj.path);
  }

  return {
    uid,
    totalObjects: objects.length,
    referencedCount: referenced.size,
    toDelete,
    protectedRecent,
    protectedUnknownAge,
    skipped: false,
  };
}

// ── Orchestration (I/O injected; no Firebase imports) ─────────────────────────

export interface CardMediaGcIo {
  /** Lists every card-media object across both Storage prefixes. */
  listAllObjects(): Promise<CardMediaObject[]>;
  /** Reads one card document for a user. */
  readDoc(collection: CardDocCollection, uid: string): Promise<CardDocState>;
  /** Deletes a single Storage object by path. */
  deleteObject(path: string): Promise<void>;
  /** Current epoch ms. */
  nowMs(): number;
}

export interface CardMediaGcOptions {
  graceMs?: number;
  maxUsers?: number;
  /** When true, plans and counts but performs NO deletions. */
  dryRun?: boolean;
}

export interface CardMediaGcSummary {
  usersScanned: number;
  objectsScanned: number;
  /** Objects whose path did not parse as card media (ignored). */
  malformedPaths: number;
  /** Total objects the plan would delete (informative in both modes). */
  plannedDeletions: number;
  /** Objects actually deleted (0 in dry-run). */
  deleted: number;
  deleteFailures: number;
  skippedUsers: number;
  protectedRecent: number;
  protectedUnknownAge: number;
  /** Users above maxUsers that were NOT processed this run (logged, never silent). */
  cappedUsers: number;
  dryRun: boolean;
}

function groupByOwner(objects: readonly CardMediaObject[]): {
  byUid: Map<string, CardMediaObject[]>;
  malformedPaths: number;
} {
  const byUid = new Map<string, CardMediaObject[]>();
  let malformedPaths = 0;
  for (const obj of objects) {
    const uid = ownerUidOfCardMedia(obj.path);
    if (!uid) {
      malformedPaths += 1;
      continue;
    }
    const list = byUid.get(uid) ?? [];
    list.push(obj);
    byUid.set(uid, list);
  }
  return { byUid, malformedPaths };
}

/**
 * Reconciles all card media against the card docs and deletes safe orphans. Enumerates users from
 * Storage (not from a Firestore collection) so media left behind by deleted accounts — whose docs no
 * longer exist — is also reclaimed. I/O is injected, so this is fully testable with plain fakes.
 */
export async function reconcileAllCardMedia(
  io: CardMediaGcIo,
  options: CardMediaGcOptions = {},
): Promise<CardMediaGcSummary> {
  const graceMs = options.graceMs ?? DEFAULT_GRACE_MS;
  const maxUsers = options.maxUsers ?? DEFAULT_MAX_USERS;
  const dryRun = options.dryRun ?? false;
  const nowMs = io.nowMs();

  const objects = await io.listAllObjects();
  const { byUid, malformedPaths } = groupByOwner(objects);

  const allUids = [...byUid.keys()];
  const uids = allUids.slice(0, maxUsers);

  const summary: CardMediaGcSummary = {
    usersScanned: uids.length,
    objectsScanned: objects.length,
    malformedPaths,
    plannedDeletions: 0,
    deleted: 0,
    deleteFailures: 0,
    skippedUsers: 0,
    protectedRecent: 0,
    protectedUnknownAge: 0,
    cappedUsers: Math.max(0, allUids.length - uids.length),
    dryRun,
  };

  for (const uid of uids) {
    const [privateDoc, publicDoc] = await Promise.all([
      io.readDoc('profile_cards_private', uid),
      io.readDoc('profile_cards_public', uid),
    ]);
    const plan = planUserOrphans(uid, byUid.get(uid) ?? [], privateDoc, publicDoc, nowMs, graceMs);
    if (plan.skipped) {
      summary.skippedUsers += 1;
      continue;
    }
    summary.protectedRecent += plan.protectedRecent;
    summary.protectedUnknownAge += plan.protectedUnknownAge;
    summary.plannedDeletions += plan.toDelete.length;
    if (dryRun) continue;
    for (const path of plan.toDelete) {
      try {
        await io.deleteObject(path);
        summary.deleted += 1;
      } catch {
        summary.deleteFailures += 1;
      }
    }
  }

  return summary;
}
