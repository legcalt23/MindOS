#!/usr/bin/env node
// Isolated lock safety tests (A). Everything runs in a private temp dir with injected LEDGER_PATH /
// LOCK_PATH; NOTHING touches the real ledger lock. Concurrency uses worker_threads (real fs-level
// O_EXCL/rename contention). Each test fails BEFORE the fencing-token/atomic-reap fix and passes after.
import { mkdtempSync, writeFileSync, readFileSync, existsSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { Worker } from 'node:worker_threads';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { acquire, release, renew, validateToken, readLock, writeLedgerUnderLock } from '../ledger-lock.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const workerUrl = new URL('./lock-race-worker.mjs', import.meta.url);
const REAL_LOCK = join(here, '..', '..', 'MINDBOOST_AUDIT_LEDGER.lock');
const realLockExistedBefore = existsSync(REAL_LOCK);

let fails = 0;
const ok = (cond, msg) => { console.log(`  ${cond ? 'PASS' : 'FAIL'}  ${msg}`); if (!cond) fails++; };

function tmp() { return mkdtempSync(join(tmpdir(), 'locktest-')); }
function expiredLock(lockPath, taskId = 'ghost') {
  writeFileSync(lockPath, JSON.stringify({ taskId, owner: taskId, fencingToken: 'deadbeef'.repeat(4), acquiredUtc: new Date(Date.now() - 3_600_000).toISOString(), leaseMs: 1000, ledgerHashAtAcquire: null }));
}
async function race(lockPath, ledgerPath, n, leaseMs = 1_800_000) {
  const workers = Array.from({ length: n }, (_, i) => new Promise((res) => {
    const w = new Worker(workerUrl, { workerData: { lockPath, ledgerPath, taskId: `w${i}`, leaseMs } });
    w.on('message', (m) => { w.terminate(); res(m); });
    w.on('error', () => res({ won: false, reason: 'WORKER_ERR' }));
  }));
  return Promise.all(workers);
}

console.log('LOCK SAFETY TESTS (isolated temp; real lock untouched):');

// T1: N competitors on an empty lock → exactly one winner (8 and 24).
for (const n of [8, 24]) {
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const res = await race(lp, lg, n);
  const winners = res.filter((r) => r.won).length;
  ok(winners === 1, `T1 empty lock, ${n} competitors → exactly 1 winner (got ${winners})`);
  rmSync(d, { recursive: true, force: true });
}

// T2: a LIVE preexisting lock → 0 winners and the lock survives intact.
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const live = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'holder', owner: 'holder', leaseMs: 3_600_000 });
  const before = readFileSync(lp, 'utf8');
  const res = await race(lp, lg, 24);
  const winners = res.filter((r) => r.won).length;
  const after = readFileSync(lp, 'utf8');
  ok(winners === 0, `T2 live lock, 24 competitors → 0 winners (got ${winners})`);
  ok(before === after, 'T2 live lock survives byte-intact');
  ok(validateToken({ lockPath: lp, taskId: 'holder', token: live.token }), 'T2 holder token still valid');
  rmSync(d, { recursive: true, force: true });
}

// T3: expired lock, 24 competitors, 100 rounds → exactly one winner EVERY round.
{
  let bad = 0; let multi = 0; let zero = 0;
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  for (let round = 0; round < 100; round++) {
    const lp = join(d, `r${round}.lock`);
    expiredLock(lp);
    const res = await race(lp, lg, 24);
    const w = res.filter((r) => r.won).length;
    if (w !== 1) { bad++; if (w > 1) multi++; if (w === 0) zero++; }
  }
  ok(bad === 0, `T3 expired takeover ×100 rounds/24 competitors → exactly 1 winner each (bad=${bad}, multi=${multi}, zero=${zero})`);
  rmSync(d, { recursive: true, force: true });
}

// T4: release with correct taskId but WRONG token → rejected; correct token → accepted.
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't', owner: 't' });
  const wrong = release({ lockPath: lp, taskId: 't', token: 'wrong'.repeat(6) });
  ok(!wrong.ok && wrong.reason === 'TOKEN_MISMATCH', 'T4 release with wrong token → rejected');
  ok(existsSync(lp), 'T4 lock survives a rejected release');
  const right = release({ lockPath: lp, taskId: 't', token: a.token });
  ok(right.ok && !existsSync(lp), 'T4 release with correct token → accepted');
  rmSync(d, { recursive: true, force: true });
}

// T5: old owner after an expired-takeover cannot write or release (fencing).
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'ORIGINAL');
  expiredLock(lp, 'oldOwner'); const oldToken = 'deadbeef'.repeat(4);
  const takeover = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'newOwner', owner: 'newOwner' });
  ok(takeover.ok, 'T5 new owner takes over expired lock');
  const oldWrite = writeLedgerUnderLock({ ledgerPath: lg, lockPath: lp, taskId: 'oldOwner', token: oldToken, content: 'HIJACK' });
  ok(!oldWrite.ok, 'T5 old owner cannot write ledger after takeover');
  ok(readFileSync(lg, 'utf8') === 'ORIGINAL', 'T5 ledger unchanged by rejected old-owner write');
  const oldRel = release({ lockPath: lp, taskId: 'oldOwner', token: oldToken });
  ok(!oldRel.ok && oldRel.reason === 'TOKEN_MISMATCH', 'T5 old owner cannot release the new lock');
  rmSync(d, { recursive: true, force: true });
}

// T6: crash before rename → original ledger intact.
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'ORIGINAL_LEDGER');
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't', owner: 't' });
  const r = writeLedgerUnderLock({ ledgerPath: lg, lockPath: lp, taskId: 't', token: a.token, content: 'NEW_CONTENT', stopBeforeRename: true });
  ok(!r.ok && r.reason === 'STOPPED_BEFORE_RENAME', 'T6 write stopped before rename');
  ok(readFileSync(lg, 'utf8') === 'ORIGINAL_LEDGER', 'T6 original ledger intact after simulated crash');
  const good = writeLedgerUnderLock({ ledgerPath: lg, lockPath: lp, taskId: 't', token: a.token, content: 'NEW_CONTENT' });
  ok(good.ok && readFileSync(lg, 'utf8') === 'NEW_CONTENT', 'T6 completed write atomically replaces ledger');
  rmSync(d, { recursive: true, force: true });
}

// T7: the tests never created/touched the real ledger lock.
ok(existsSync(REAL_LOCK) === realLockExistedBefore, `T7 real lock state unchanged (existed before=${realLockExistedBefore})`);

// Result artifact (the validator's LEDGER_LOCK_SAFETY_GATE is tied to this — no static-inspection bluff).
writeFileSync(join(here, '..', '..', 'LOCK_SAFETY_RESULT.json'),
  JSON.stringify({ suite: 'ledger-lock isolated safety', pass: fails === 0, fails, tests: ['empty-1winner-8/24', 'live-lock-survives', 'expired-takeover-100x24-1winner', 'wrong-token-rejected', 'old-owner-fenced-out', 'crash-before-rename-intact', 'real-lock-untouched'] }, null, 2) + '\n', 'utf8');
console.log(fails === 0 ? 'LOCK SAFETY TESTS: PASS' : `LOCK SAFETY TESTS: FAIL (${fails})`);
process.exit(fails === 0 ? 0 : 1);
