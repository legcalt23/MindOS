/**
 * counterShards.ts — distributed (sharded) social counters (audit A1 fix).
 *
 * WHY: counters/{uid} was a single hot document mutated with read-then-write inside every
 * follow/request/block transaction. An account with viral fan-in (> ~1 write/s to that doc)
 * hit Firestore's per-document sustained-write ceiling and the transactions ABORTED —
 * follows failed. See the scalability audit (A1) and CLAUDE.md.
 *
 * DESIGN:
 *  - Only the FAN-IN fields — followersCount, incomingPendingRequestsCount — receive
 *    concurrent writes from many third parties onto one victim doc, so ONLY these are
 *    sharded. followingCount / outgoingPendingRequestsCount / blockedUsersCount count the
 *    owner's OWN outgoing actions (dominant single-writer); they stay on counters/{uid} via
 *    an atomic FieldValue.increment (also no read-then-write). Their >= 0 floor is preserved
 *    by the callers' existing idempotency guards (a decrement only runs when the relation
 *    exists), not by a Math.max read.
 *  - Delta writes go to a random shard counters/{uid}/shards/{0..N-1} via FieldValue.increment
 *    — never read-then-write, so no optimistic-lock abort and N× the sustained-write ceiling.
 *  - The CLIENT read-model counters/{uid} is UNCHANGED (rules/read path untouched, M-01): it
 *    is kept eventually-consistent by the aggregateCounterShards trigger, which re-sums the
 *    shards and materializes the parent (coerced >= 0).
 *  - The first increment for a uid seeds shard 0 with the legacy parent values inside the same
 *    transaction and stamps a migratedToShards marker on shards/_meta, so two concurrent
 *    first-writers cannot double-seed (the loser retries and sees the marker).
 */
import * as admin from 'firebase-admin';

const FieldValue = admin.firestore.FieldValue;

/** Number of counting shards per uid (shards 0..COUNTER_SHARD_COUNT-1). */
export const COUNTER_SHARD_COUNT = 10;

/**
 * Marker document id. Never a counting shard: it records that the legacy parent value was
 * seeded into shard 0, and is read (never written after migration) on every mutation so
 * concurrent first-writers converge instead of double-seeding.
 */
const SHARD_META_ID = '_meta';

/** Fields with concurrent third-party fan-in — the only ones that need sharding. */
export const SHARDED_COUNTER_FIELDS = ['followersCount', 'incomingPendingRequestsCount'] as const;
export type ShardedCounterField = typeof SHARDED_COUNTER_FIELDS[number];

function counterRef(uid: string): admin.firestore.DocumentReference {
  return admin.firestore().collection('counters').doc(uid);
}

export function counterShardsCollection(uid: string): admin.firestore.CollectionReference {
  return counterRef(uid).collection('shards');
}

export interface CounterMigrationState {
  migrated: boolean;
  legacy: admin.firestore.DocumentData | null;
}

/**
 * Reads the sharded-counter migration state for [uid] INSIDE a transaction. MUST be called
 * during the read phase (before any write): it reads the marker doc, and — only when not yet
 * migrated — the legacy parent counters/{uid}. In steady state the marker read is read-only
 * (never written after migration) so it does not contend with the aggregation trigger.
 */
export async function readCounterMigration(
  tx: admin.firestore.Transaction,
  uid: string,
): Promise<CounterMigrationState> {
  const metaSnap = await tx.get(counterShardsCollection(uid).doc(SHARD_META_ID));
  if (metaSnap.exists && metaSnap.data()?.['migratedToShards'] === true) {
    return { migrated: true, legacy: null };
  }
  const legacySnap = await tx.get(counterRef(uid));
  return { migrated: false, legacy: legacySnap.data() ?? {} };
}

/**
 * Applies sharded-field deltas for [uid] during the transaction's WRITE phase. First write for
 * a uid seeds shard 0 with the legacy virals + these deltas and stamps the marker; afterwards
 * each delta is a FieldValue.increment on a random shard.
 */
export function applyShardedCounterDelta(
  tx: admin.firestore.Transaction,
  uid: string,
  deltas: Partial<Record<ShardedCounterField, number>>,
  migration: CounterMigrationState,
): void {
  const hasDelta = SHARDED_COUNTER_FIELDS.some((field) => (deltas[field] ?? 0) !== 0);
  if (!hasDelta) return;
  const shards = counterShardsCollection(uid);
  if (!migration.migrated) {
    const seed: Record<string, number> = {};
    for (const field of SHARDED_COUNTER_FIELDS) {
      const base = (migration.legacy?.[field] as number) ?? 0;
      seed[field] = base + (deltas[field] ?? 0);
    }
    tx.set(shards.doc('0'), seed, { merge: true });
    tx.set(shards.doc(SHARD_META_ID), { migratedToShards: true }, { merge: true });
    return;
  }
  const increments: Record<string, admin.firestore.FieldValue> = {};
  for (const field of SHARDED_COUNTER_FIELDS) {
    const delta = deltas[field];
    if (delta) increments[field] = FieldValue.increment(delta);
  }
  if (Object.keys(increments).length === 0) return;
  const shardId = Math.floor(Math.random() * COUNTER_SHARD_COUNT);
  tx.set(shards.doc(String(shardId)), increments, { merge: true });
}

/**
 * Increments non-sharded (single-writer) parent counter fields atomically.
 * followingCount / outgoingPendingRequestsCount / blockedUsersCount: dominant single-writer,
 * so a plain FieldValue.increment on counters/{uid} is contention-safe (no read-then-write).
 */
export function incrementParentCounterFields(
  tx: admin.firestore.Transaction,
  uid: string,
  deltas: Record<string, number>,
): void {
  const increments: Record<string, admin.firestore.FieldValue> = {};
  for (const [field, delta] of Object.entries(deltas)) {
    if (delta) increments[field] = FieldValue.increment(delta);
  }
  if (Object.keys(increments).length === 0) return;
  tx.set(counterRef(uid), increments, { merge: true });
}

/**
 * Aggregates the sharded fields for [uid], coerced to >= 0. Returns null when the shard set is
 * empty (never seeded, or dropped by account deletion) so the aggregation trigger does not
 * resurrect a deleted parent.
 */
export async function aggregateShardedCounterFields(
  uid: string,
): Promise<Record<ShardedCounterField, number> | null> {
  const snap = await counterShardsCollection(uid).get();
  if (snap.empty) return null;
  const totals: Record<ShardedCounterField, number> = {
    followersCount: 0,
    incomingPendingRequestsCount: 0,
  };
  snap.forEach((doc) => {
    const data = doc.data();
    for (const field of SHARDED_COUNTER_FIELDS) {
      const value = data[field];
      if (typeof value === 'number') totals[field] += value;
    }
  });
  for (const field of SHARDED_COUNTER_FIELDS) {
    totals[field] = Math.max(0, totals[field]);
  }
  return totals;
}

/** Sums a single sharded field for [uid], coerced to >= 0 (0 when the shard set is empty). */
export async function sumShards(uid: string, field: ShardedCounterField): Promise<number> {
  const totals = await aggregateShardedCounterFields(uid);
  return totals?.[field] ?? 0;
}
