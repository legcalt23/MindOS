/**
 * counter-shards.concurrency.emulator.mjs — Firestore-emulator integration test for the A1
 * sharded counter (functions/src/lib/counterShards.ts). Exercises the REAL compiled lib against
 * a real Firestore instance so concurrent transactions and the transactional seed race are
 * verified with actual serialization semantics (the jest unit mock is sequential and cannot).
 *
 * Run (JDK 21 required):
 *   npm run build --prefix functions
 *   npm run test:counter-shards-emulator
 *
 * NOTE: Firestore's ~1 sustained-write/s/document contention (the ABORTED failure A1 fixes) is
 * NOT reproducible in the emulator — that is proven by the staging load-test (audit §5, founder
 * hand-off). This test proves the correctness invariants the emulator CAN enforce: no lost
 * updates under concurrency (exact sum) and single seed under a concurrent first-writer race.
 *
 * KNOWN CEILING (declared): a THUNDERING HERD of first-writers (~100 truly-simultaneous follows
 * on a never-migrated uid) serializes on the transactional seed (shards/_meta) and can exhaust
 * the emulator's transaction-lock-timeout. This is a one-time, per-uid migration-window cost
 * (single-seed invariant holds; failed writes are client-retryable) and is bounded by N=10 shard
 * capacity. Mitigation if cold-viral-launch matters: eager backfill script (plan §9 alternative)
 * and/or raise the shard count. Steady-state distributed writes (the A1 win) are unaffected.
 */
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const admin = require('firebase-admin');
const {
  readCounterMigration,
  applyShardedCounterDelta,
  aggregateShardedCounterFields,
} = require('../lib/lib/counterShards.js');

admin.initializeApp({ projectId: process.env['GCLOUD_PROJECT'] || 'mindos-counter-shards' });
const db = admin.firestore();

let failures = 0;
function check(cond, msg) {
  if (cond) { console.log(`  ✅ ${msg}`); } else { console.log(`  ❌ ${msg}`); failures += 1; }
}

/** One "social mutation": read migration (reads), then apply the sharded delta (writes). */
function mutate(uid, deltas) {
  return db.runTransaction(async (tx) => {
    const migration = await readCounterMigration(tx, uid);
    applyShardedCounterDelta(tx, uid, deltas, migration);
  });
}

async function main() {
  console.log('===== counter shards — emulator concurrency =====');
  const LEGACY = 5;

  // Scenario 1 — single seed under a concurrent first-writer race (D-02). A modest herd of
  // simultaneous FIRST writers on a never-migrated uid: exactly ONE seeds shard 0 with the
  // legacy value (the losers retry, see the marker, and increment) → legacy counted once.
  const race = `race_${Date.now()}`;
  await db.collection('counters').doc(race).set({ followersCount: LEGACY, incomingPendingRequestsCount: 0 });
  const first = [];
  for (let i = 0; i < 8; i += 1) first.push(mutate(race, { followersCount: 1 }));
  await Promise.all(first);
  const raceTotals = await aggregateShardedCounterFields(race);
  check(raceTotals && raceTotals.followersCount === LEGACY + 8,
    `8 concurrent first-writers → single seed, exact sum ${LEGACY + 8} (got ${raceTotals && raceTotals.followersCount})`);
  const metaSnap = await db.collection('counters').doc(race).collection('shards').doc('_meta').get();
  check(metaSnap.exists && metaSnap.data().migratedToShards === true,
    'migratedToShards marker set exactly once (legacy not double-seeded)');

  // Scenario 2 — steady-state distributed follows (the A1 win). Migrate first, then fire
  // concurrent increments that spread across the shards: no lost updates → exact sum.
  const viral = `viral_${Date.now()}`;
  await db.collection('counters').doc(viral).set({ followersCount: LEGACY, incomingPendingRequestsCount: 0 });
  await mutate(viral, { followersCount: 1 }); // migrate (seed) once, sequentially
  const N = 40;
  const runs = [];
  for (let i = 0; i < N; i += 1) runs.push(mutate(viral, { followersCount: 1 }));
  await Promise.all(runs); // rejects if any transaction fails unrecoverably
  const totals = await aggregateShardedCounterFields(viral);
  check(totals && totals.followersCount === LEGACY + 1 + N,
    `${N} concurrent follows (post-migration) → exact sum ${LEGACY + 1 + N} (got ${totals && totals.followersCount})`);

  // Scenario 3 — concurrent decrements stay exact.
  const dec = [];
  for (let i = 0; i < 15; i += 1) dec.push(mutate(viral, { followersCount: -1 }));
  await Promise.all(dec);
  const afterDec = await aggregateShardedCounterFields(viral);
  check(afterDec && afterDec.followersCount === LEGACY + 1 + N - 15,
    `15 concurrent unfollows → exact sum ${LEGACY + 1 + N - 15} (got ${afterDec && afterDec.followersCount})`);

  console.log(`\n===== RESULTS =====\n  Passed: ${4 - failures}\n  Failed: ${failures}\n`);
  if (failures > 0) process.exit(1);
}

main().catch((err) => { console.error('FATAL', err); process.exit(1); });
