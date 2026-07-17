#!/usr/bin/env node
// V2d Phase 6-R2 lock LIVENESS / CORRUPT-RACE / TOCTOU suite.
// Complements lock-tests.mjs (mutual exclusion for ABSENT + EXPIRED generations).
//
// EPISTEMOLOGY (6-R2.3). A result is one of:
//   PASS             behaviour actually executed and asserted
//   FAIL             expectation not met
//   KNOWN_RESIDUAL   a violation REPRODUCED via a real interleaving (hook fires after the final
//                    validation, in the true residual window) — declared, not pretended fixed
//   NOT_TESTED       not executed in this environment
//   BLOCKED_PLATFORM needs another host/filesystem
// KNOWN_RESIDUAL is never fabricated by asserting `false`; it is only emitted when the violating
// interleaving was actually driven and its bad outcome observed.
//
// Everything runs in a private temp dir with injected LEDGER_PATH/LOCK_PATH. The real lock is never touched.
import { mkdtempSync, writeFileSync, readFileSync, existsSync, rmSync, openSync, closeSync, writeSync, readdirSync } from 'node:fs';
import os, { tmpdir } from 'node:os';
import { Worker } from 'node:worker_threads';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import * as LOCK from '../ledger-lock.mjs';

const { acquire, release, renew, readLock, writeLedgerUnderLock } = LOCK;
const __hooks = LOCK.__hooks ?? {};
const here = dirname(fileURLToPath(import.meta.url));
const workerUrl = new URL('./lock-race-worker.mjs', import.meta.url);
const REAL_LOCK = join(here, '..', '..', 'MINDBOOST_AUDIT_LEDGER.lock');
const realLockExistedBefore = existsSync(REAL_LOCK);

const results = [];
let fails = 0;
const S = { PASS: 'PASS', FAIL: 'FAIL', KNOWN_RESIDUAL: 'KNOWN_RESIDUAL', NOT_TESTED: 'NOT_TESTED', BLOCKED_PLATFORM: 'BLOCKED_PLATFORM' };
function record(id, state, msg, detail) {
  console.log(`  ${state.padEnd(16)} ${id}  ${msg}`);
  results.push({ id, state, msg, ...(detail ? { detail } : {}) });
  if (state === S.FAIL) fails++;
}
const check = (id, cond, msg, detail) => record(id, cond ? S.PASS : S.FAIL, msg, detail);
const tmp = () => mkdtempSync(join(tmpdir(), 'lockliv-'));
const reset = () => { for (const k of Object.keys(__hooks)) __hooks[k] = null; };
const HEX = 'deadbeef'.repeat(4);

function validLockRec(taskId, token, ageMs, leaseMs) {
  return { taskId, owner: taskId, fencingToken: token, acquiredUtc: new Date(Date.now() - ageMs).toISOString(), leaseMs, ledgerHashAtAcquire: null };
}
async function race(lockPath, ledgerPath, n, leaseMs = 1_800_000) {
  return Promise.all(Array.from({ length: n }, (_, i) => new Promise((res) => {
    const w = new Worker(workerUrl, { workerData: { lockPath, ledgerPath, taskId: `w${i}`, leaseMs } });
    w.on('message', (m) => { w.terminate(); res(m); });
    w.on('error', (e) => res({ won: false, reason: 'WORKER_ERR:' + e.message }));
  })));
}
// A round is SAFE iff never >=2 winners AND (exactly 1 winner) OR (0 winners and every contender reported
// an explicit fail-closed corrupt/orphan diagnostic). Silent zero-winners is NOT acceptable.
function judgeRound(res) {
  const wins = res.filter((r) => r.won).length;
  if (wins >= 2) return { safe: false, why: `${wins} winners` };
  if (wins === 1) return { safe: true, kind: 'single-winner' };
  const explicit = res.every((r) => /CORRUPT_LOCK|MANUAL_REPAIR|ORPHAN|CLAIM/.test(String(r.reason)));
  return explicit ? { safe: true, kind: 'fail-closed' } : { safe: false, why: `0 winners without an explicit diagnostic (reasons: ${[...new Set(res.map((r) => r.reason))].join(',')})` };
}
async function corruptRace(id, label, seed, rounds = 100, n = 24) {
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  let multi = 0, badZero = 0, single = 0, failClosed = 0, worst = null;
  for (let r = 0; r < rounds; r++) {
    const lp = join(d, `r${r}.lock`);
    writeFileSync(lp, seed);
    const res = await race(lp, lg, n);
    const j = judgeRound(res);
    if (!j.safe) { if (/winners/.test(j.why)) multi++; else badZero++; worst ??= j.why; }
    else if (j.kind === 'single-winner') single++; else failClosed++;
  }
  rmSync(d, { recursive: true, force: true });
  const safe = multi === 0 && badZero === 0;
  record(id, safe ? S.PASS : S.FAIL,
    `${label} ${rounds}×${n} → never ≥2 winners (multi=${multi}, unsafe-zero=${badZero}, single=${single}, fail-closed=${failClosed})${worst ? ' | first: ' + worst : ''}`,
    { multi, badZero, single, failClosed });
  return { multi, badZero };
}

// Runs the REAL CLI in a child process against an injected lock/ledger path. R1/R2 must exercise the
// operator-facing entry point, not the library function the operator never calls.
const lockScript = join(here, '..', 'ledger-lock.mjs');
function cli(lockPath, ledgerPath, args) {
  return spawnSync(process.execPath, [lockScript, ...args], {
    env: { ...process.env, LOCK_PATH: lockPath, LEDGER_PATH: ledgerPath }, encoding: 'utf8',
  });
}
// Clock that returns `values` in order and repeats the last one. Lets a test place a backward jump at an
// exact call site (e.g. between publication and the final classification).
function clockSeq(values) { let i = 0; return () => values[Math.min(i++, values.length - 1)]; }
const parseLock = (p) => { try { return JSON.parse(readFileSync(p, 'utf8')); } catch { return null; } };

console.log('LOCK LIVENESS / CORRUPT-RACE / TOCTOU (isolated temp; real lock untouched):');
console.log(`  [env] CORRUPT_GRACE=${process.env.LOCK_CORRUPT_GRACE_MS ?? '(default)'} CLAIM_GRACE=${process.env.LOCK_CLAIM_GRACE_MS ?? '(default)'}`);

// ---- C1: empty-corrupt race ---------------------------------------------------------------------------
await corruptRace('C1', 'empty-corrupt race', '');

// ---- C2: truncated-corrupt race -----------------------------------------------------------------------
await corruptRace('C2', 'truncated-corrupt race', '{"taskId":"half","fencing');

// ---- C3: slow LEGITIMATE creator (non-conforming/legacy open("wx")→delay→write) ------------------------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const fd = openSync(lp, 'wx');                       // creator won the create, content not published yet
  const contenders = await race(lp, lg, 12);           // they observe CORRUPT while the creator is slow
  const wins = contenders.filter((r) => r.won).length;
  const survived = existsSync(lp);
  // the legitimate creator finally publishes its content
  writeSync(fd, JSON.stringify(validLockRec('slowCreator', HEX, 0, 1_800_000))); closeSync(fd);
  const rec = readLock(lp);
  check('C3', wins === 0 && survived && !!rec && rec.taskId === 'slowCreator',
    `slow legitimate creator must not be deleted and must yield no second owner (contender wins=${wins}, path survived=${survived}, holder=${rec ? rec.taskId : 'GONE'})`);
  rmSync(d, { recursive: true, force: true });
}

// ---- C4: replaced-during-reap -------------------------------------------------------------------------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  writeFileSync(lp, '');                                // contender will observe CORRUPT
  // Install a VALID successor lock while the contender is inside its corrupt-handling path.
  const successor = validLockRec('successor', 'a'.repeat(32), 0, 1_800_000);
  const p = race(lp, lg, 1);
  setTimeout(() => { try { writeFileSync(lp, JSON.stringify(successor)); } catch {} }, 5);
  const res = await p;
  const cur = readLock(lp);
  check('C4', !res[0].won && !!cur && cur.taskId === 'successor',
    `a stale corrupt observation must never delete a valid successor (contender won=${res[0].won}/${res[0].reason}, holder=${cur ? cur.taskId : 'DELETED'})`);
  rmSync(d, { recursive: true, force: true });
}

// ---- C5: schema-invalid must never be HELD indefinitely ------------------------------------------------
{
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const cases = {
    'empty-object': {},
    'bad-acquiredUtc': { ...validLockRec('t', HEX, 0, 1000), acquiredUtc: 'not-a-date' },
    'leaseMs-NaN': { ...validLockRec('t', HEX, 0, 1000), leaseMs: NaN },
    'leaseMs-negative': { ...validLockRec('t', HEX, 0, 1000), leaseMs: -5 },
    'leaseMs-zero': { ...validLockRec('t', HEX, 0, 1000), leaseMs: 0 },
    'token-missing': (() => { const r = validLockRec('t', HEX, 0, 1000); delete r.fencingToken; return r; })(),
    'token-not-hex': { ...validLockRec('t', HEX, 0, 1000), fencingToken: 'ZZZZ' },
    'taskId-missing': (() => { const r = validLockRec('t', HEX, 0, 1000); delete r.taskId; return r; })(),
  };
  const bad = [];
  for (const [name, rec] of Object.entries(cases)) {
    const lp = join(d, `${name}.lock`);
    writeFileSync(lp, JSON.stringify(rec));
    const r = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'probe', owner: 'probe' });
    // must NOT be silently HELD and must NOT be granted; an explicit corrupt diagnostic is required
    const okShape = !r.ok && /CORRUPT/.test(String(r.reason));
    if (!okShape) bad.push(`${name}→${r.ok ? 'ACQUIRED' : r.reason}`);
  }
  check('C5', bad.length === 0, `schema-invalid locks must be explicit CORRUPT (never HELD/EXHAUSTED/granted)${bad.length ? ' | offenders: ' + bad.join(', ') : ''}`);
  rmSync(d, { recursive: true, force: true });
}

// ---- C6: renew crossing expiry between first read and commit -------------------------------------------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  let t = 1_000_000;
  const nowFn = () => t;
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't1', owner: 't1', leaseMs: 1000, now: nowFn });
  let temps = [];
  if (a.ok) {
    // advance past expiry AFTER the initial validation but BEFORE the commit-time re-validation
    __hooks.afterRenewPreValidate = () => { t += 5000; };
    const r = renew({ lockPath: lp, taskId: 't1', token: a.token, now: nowFn });
    reset();
    const fs2 = readdirSync(d).filter((f) => f.includes('.tmp.'));
    temps = fs2;
    check('C6', !r.ok && r.reason === 'EXPIRED' && fs2.length === 0,
      `renew crossing expiry → EXPIRED and no temp published (got ${r.ok ? 'ok' : r.reason}, leftover temps=${fs2.length})`);
  } else {
    record('C6', S.FAIL, `setup failed: acquire with injected clock returned ${a.reason} (clock injection unsupported?)`);
  }
  rmSync(d, { recursive: true, force: true });
}

// ---- C7: parseable orphan takeover-claim — recovery per DECLARED policy --------------------------------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  writeFileSync(lp, JSON.stringify(validLockRec('ghost', HEX, 3_600_000, 1000)));   // expired
  // exactly the fully-formed claim atomicCreateWithContent would produce
  writeFileSync(`${lp}.takeover.${HEX}`, JSON.stringify({ token: HEX, at: Date.now(), by: 'crashedReaper' }));
  const r = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'newOwner', owner: 'newOwner' });
  const declared = !r.ok && /CLAIM|ORPHAN/.test(String(r.reason)) && r.manualRepairRequired === true;
  check('C7', declared,
    `fresh parseable orphan claim → declared policy is fail-closed + explicit diagnostic + manual repair (got ${r.ok ? 'ACQUIRED (auto-reaped a possibly-live claimant)' : r.reason})`);
  rmSync(d, { recursive: true, force: true });
}

// ---- R1: the PUBLISHED repairHint for an orphaned claim must actually be executable --------------------
// acquire() tells the operator to run `repair --confirm-no-writer`. If that command refuses because the
// lock is EXPIRED (not CORRUPT), the diagnostic is a dead end: the lock is permanently unacquirable.
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  writeFileSync(lp, JSON.stringify(validLockRec('ghost', HEX, 3_600_000, 1000)));       // EXPIRED
  const claim = `${lp}.takeover.${HEX}`;
  writeFileSync(claim, JSON.stringify({ token: HEX, at: Date.now(), by: 'crashedReaper' }));
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'newOwner', owner: 'newOwner' });
  const diagnosed = !a.ok && a.reason === 'TAKEOVER_CLAIM_ORPHANED' && a.manualRepairRequired === true;
  const rep = cli(lp, lg, ['repair', '--confirm-no-writer']);                            // exactly the hint
  const claimGone = !existsSync(claim);
  const quarantined = readdirSync(d).some((f) => f.includes('.quarantine.'));
  const after = acquire({ lockPath: lp, ledgerPath: lg, taskId: 'newOwner2', owner: 'newOwner2' });
  check('R1', diagnosed && rep.status === 0 && claimGone && quarantined && after.ok,
    `orphaned claim: acquire diagnoses it AND the published repairHint executes (repair exit=${rep.status}, claimRemoved=${claimGone}, quarantined=${quarantined}, reacquire=${after.ok ? 'ok' : after.reason})`,
    { repairStderr: (rep.stderr || '').trim().split('\n').pop() });
  rmSync(d, { recursive: true, force: true });
}

// ---- R2: the CLI must not silently substitute a default for an INVALID lease ---------------------------
// `Number(arg) || DEFAULT` maps 0 and NaN to the 30-minute default. A lease is a safety parameter: an
// operator who asks for an invalid one must be refused, not quietly given a different one.
{
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const bad = [];
  for (const v of ['0', 'NaN', '-5', '1.5', String(25 * 60 * 60 * 1000), 'abc']) {
    const lp = join(d, `lease-${encodeURIComponent(v)}.lock`);
    const r = cli(lp, lg, ['acquire', 't', 'o', v]);
    const refused = r.status !== 0 && /BAD_LEASE_MS/.test(r.stderr || '') && !existsSync(lp);
    if (!refused) bad.push(`${v}→${r.status === 0 ? 'ACQUIRED' : (r.stderr || '').trim()}`);
  }
  // absent lease must still take the documented default
  const lpDef = join(d, 'default.lock');
  const def = cli(lpDef, lg, ['acquire', 't', 'o']);
  const defOk = def.status === 0 && parseLock(lpDef)?.leaseMs === 1_800_000;
  check('R2', bad.length === 0 && defOk,
    `CLI rejects invalid leases and publishes no lock; absent lease still defaults to 1800000 (defaultOk=${defOk})${bad.length ? ' | offenders: ' + bad.join(', ') : ''}`);
  rmSync(d, { recursive: true, force: true });
}

// ---- R3: acquire must not report ok=true for a lock that classifies as CORRUPT -------------------------
// The return path calls classifyLock() but ignores its state, so a backward clock jump between publication
// and the final read yields ok=true carrying a record the classifier rejects.
{
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const T = 1_700_000_000_000, BACK = 120_000;                    // > CLOCK_SKEW_TOLERANCE_MS (60s)
  // (a) ABSENT → create. nowFn calls: [classify-loop, mkRec, classify-return]
  const lpA = join(d, 'create.lock');
  const ra = acquire({ lockPath: lpA, ledgerPath: lg, taskId: 't', owner: 't', now: clockSeq([T, T, T - BACK]) });
  const clsA = LOCK.classifyLock(lpA, T - BACK);
  const aBad = ra.ok && clsA.state === 'CORRUPT';
  // (b) EXPIRED → takeover. nowFn calls: [classify-loop, claim.at, still-classify, mkRec, classify-return]
  const lpB = join(d, 'takeover.lock');
  writeFileSync(lpB, JSON.stringify({ ...validLockRec('old', HEX, 0, 1000), acquiredUtc: new Date(T - 3_600_000).toISOString() }));
  const rb = acquire({ lockPath: lpB, ledgerPath: lg, taskId: 't2', owner: 't2', now: clockSeq([T, T, T, T, T - BACK]) });
  const clsB = LOCK.classifyLock(lpB, T - BACK);
  const bBad = rb.ok && clsB.state === 'CORRUPT';
  check('R3', !aBad && !bBad,
    `acquire never returns ok=true while classifyLock() rejects the published lock (create: ok=${ra.ok}/state=${clsA.state}, takeover: ok=${rb.ok}/state=${clsB.state})`);
  rmSync(d, { recursive: true, force: true });
}

// ---- R5: repair policy matrix — must fix exactly two classes and refuse everything else ---------------
{
  const d = tmp(); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const bad = [];

  // (a) CORRUPT lock, end-to-end: quarantined byte-preserving → reacquire succeeds
  const lpC = join(d, 'corrupt.lock'); const corruptBytes = '{"taskId":"half","fencing';
  writeFileSync(lpC, corruptBytes);
  const repC = cli(lpC, lg, ['repair', '--confirm-no-writer']);
  const qC = readdirSync(d).find((f) => f.startsWith('corrupt.lock.quarantine.'));
  const bytesKept = qC && readFileSync(join(d, qC), 'utf8') === corruptBytes;
  const reC = acquire({ lockPath: lpC, ledgerPath: lg, taskId: 'after', owner: 'after' });
  if (!(repC.status === 0 && bytesKept && reC.ok)) bad.push(`corrupt-e2e(exit=${repC.status},bytesPreserved=${!!bytesKept},reacquire=${reC.ok ? 'ok' : reC.reason})`);

  // (b) orphaned claim, end-to-end: claim quarantined byte-preserving → takeover succeeds, lock untouched
  const lpO = join(d, 'orphan.lock');
  writeFileSync(lpO, JSON.stringify(validLockRec('ghost', HEX, 3_600_000, 1000)));
  const lockBytes = readFileSync(lpO, 'utf8');
  const claimBytes = JSON.stringify({ token: HEX, at: 1, by: 'crashedReaper' });
  writeFileSync(`${lpO}.takeover.${HEX}`, claimBytes);
  const repO = cli(lpO, lg, ['repair', '--confirm-no-writer']);
  const qO = readdirSync(d).find((f) => f.startsWith(`orphan.lock.takeover.${HEX}.quarantine.`));
  const claimKept = qO && readFileSync(join(d, qO), 'utf8') === claimBytes;
  const lockUntouched = existsSync(lpO) && readFileSync(lpO, 'utf8') === lockBytes;
  const reO = acquire({ lockPath: lpO, ledgerPath: lg, taskId: 'taker', owner: 'taker' });
  if (!(repO.status === 0 && claimKept && lockUntouched && reO.ok)) bad.push(`orphan-e2e(exit=${repO.status},bytesPreserved=${!!claimKept},lockUntouched=${lockUntouched},takeover=${reO.ok ? 'ok' : reO.reason})`);

  // (c) no --confirm-no-writer → refused, artifact intact
  const lpN = join(d, 'noconfirm.lock'); writeFileSync(lpN, '');
  const repN = cli(lpN, lg, ['repair']);
  if (!(repN.status !== 0 && existsSync(lpN))) bad.push(`no-confirm(exit=${repN.status},survived=${existsSync(lpN)})`);

  // (d) VALID lock → refused, never touched
  const lpV = join(d, 'valid.lock');
  writeFileSync(lpV, JSON.stringify(validLockRec('liveOwner', HEX, 0, 1_800_000)));
  const repV = cli(lpV, lg, ['repair', '--confirm-no-writer']);
  if (!(repV.status !== 0 && existsSync(lpV))) bad.push(`valid(exit=${repV.status},survived=${existsSync(lpV)})`);

  // (e) EXPIRED with NO claim → refused (a normal acquire takes it over; repair must not touch it)
  const lpE = join(d, 'expired.lock');
  writeFileSync(lpE, JSON.stringify(validLockRec('ghost2', HEX, 3_600_000, 1000)));
  const repE = cli(lpE, lg, ['repair', '--confirm-no-writer']);
  if (!(repE.status !== 0 && existsSync(lpE))) bad.push(`expired-no-claim(exit=${repE.status},survived=${existsSync(lpE)})`);

  check('R5', bad.length === 0,
    `repair fixes exactly {corrupt lock, orphaned claim} byte-preserving and refuses {no-confirm, VALID, EXPIRED-without-claim}${bad.length ? ' | offenders: ' + bad.join(', ') : ''}`);
  rmSync(d, { recursive: true, force: true });
}

// ---- L3: expired owner must not revive its lease -------------------------------------------------------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'x');
  const tok = 'cafebabe'.repeat(4);
  writeFileSync(lp, JSON.stringify(validLockRec('oldOwner', tok, 3_600_000, 1000)));
  const before = readFileSync(lp, 'utf8');
  const r = renew({ lockPath: lp, taskId: 'oldOwner', token: tok });
  check('L3', !r.ok && r.reason === 'EXPIRED' && readFileSync(lp, 'utf8') === before,
    `expired owner renew() refused and lease not revived (got ${r.ok ? 'ok' : r.reason})`);
  rmSync(d, { recursive: true, force: true });
}

// ---- L4/L5/L6: the validate→mutate residual window, driven by a REAL protocol takeover -----------------
// 6-R3.4 option A. The barrier no longer swaps the file behind the protocol's back: it advances an injected
// clock past the holder's lease and calls the real acquire(), so a genuine, protocol-conforming successor
// exists inside the window. That makes these residuals evidence about COOPERATIVE writers, not about a
// hypothetical non-conforming one.
const T0 = 1_700_000_000_000;
function residualCase(hookName, leaseMs = 1000) {
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'ORIGINAL');
  let t = T0; const nowFn = () => t;
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't1', owner: 't1', leaseMs, now: nowFn });
  let takeover = null;
  __hooks[hookName] = () => {
    t = T0 + leaseMs + 5000;                              // t1's lease is now genuinely expired
    takeover = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't2', owner: 't2', now: nowFn });
  };
  return { d, lp, lg, a, nowFn, took: () => !!takeover?.ok, takeoverReason: () => takeover?.reason };
}

{
  const c = residualCase('afterReleaseValidate');
  const r = release({ lockPath: c.lp, taskId: 't1', token: c.a.token, now: c.nowFn });
  reset();
  const real = c.took();
  const clobbered = real && !existsSync(c.lp);            // real successor unlinked by the departing owner
  record('L4', clobbered ? S.KNOWN_RESIDUAL : (real ? S.PASS : S.FAIL),
    clobbered
      ? 'RESIDUAL REPRODUCED (real cooperative takeover): release unlinks a successor that legitimately acquired after release\'s final validation — the filesystem offers no compare-and-unlink'
      : (real ? 'release did not clobber a real successor acquired in the residual window' : `no real takeover occurred in the barrier (${c.takeoverReason()}) — case did not exercise the window`),
    { realTakeover: real, releaseResult: r.reason ?? 'ok', successorSurvived: existsSync(c.lp) });
  rmSync(c.d, { recursive: true, force: true });
}

{
  const c = residualCase('afterLedgerValidate');
  const r = writeLedgerUnderLock({ ledgerPath: c.lg, lockPath: c.lp, taskId: 't1', token: c.a.token, content: 'HIJACK', now: c.nowFn });
  reset();
  const real = c.took();
  const published = readFileSync(c.lg, 'utf8') === 'HIJACK';
  record('L5', real && published ? S.KNOWN_RESIDUAL : (real ? S.PASS : S.FAIL),
    real && published
      ? 'RESIDUAL REPRODUCED (real cooperative takeover): a fenced writer still publishes the ledger after a legitimate successor acquired the lock — the filesystem offers no compare-and-rename'
      : (real ? 'ledger write did not publish after a real takeover in the residual window' : `no real takeover occurred in the barrier (${c.takeoverReason()}) — case did not exercise the window`),
    { realTakeover: real, writeResult: r.reason ?? 'ok', ledgerPublished: published });
  rmSync(c.d, { recursive: true, force: true });
}

{
  const c = residualCase('afterRenewValidate');
  const r = renew({ lockPath: c.lp, taskId: 't1', token: c.a.token, now: c.nowFn });
  reset();
  const real = c.took();
  const cur = parseLock(c.lp);
  const clobbered = real && cur && cur.taskId === 't1';   // renew overwrote the real successor
  record('L6', clobbered ? S.KNOWN_RESIDUAL : (real ? S.PASS : S.FAIL),
    clobbered
      ? 'RESIDUAL REPRODUCED (real cooperative takeover): renew renames its temp over a successor that legitimately acquired after renew\'s commit re-validation'
      : (real ? 'renew did not clobber a real successor acquired in the residual window' : `no real takeover occurred in the barrier (${c.takeoverReason()}) — case did not exercise the window`),
    { realTakeover: real, renewResult: r.reason ?? 'ok', holder: cur && cur.taskId });
  rmSync(c.d, { recursive: true, force: true });
}

// ---- L7: simulated pre-rename interruption; real process death and power loss are NOT tested -----------
{
  const d = tmp(); const lp = join(d, 'x.lock'); const lg = join(d, 'ledger.md'); writeFileSync(lg, 'ORIGINAL_LEDGER');
  const a = acquire({ lockPath: lp, ledgerPath: lg, taskId: 't', owner: 't' });
  const r = writeLedgerUnderLock({ ledgerPath: lg, lockPath: lp, taskId: 't', token: a.token, content: 'NEW', stopBeforeRename: true });
  // This returns early via a flag; it does not kill a process. It shows the write ORDER is crash-safe by
  // construction (temp+fsync, rename last) — it does not prove behaviour under real process death.
  check('L7-SIMULATED_PRE_RENAME_INTERRUPTION', !r.ok && readFileSync(lg, 'utf8') === 'ORIGINAL_LEDGER',
    'returning before the final rename leaves the ledger intact (in-process simulation via stopBeforeRename, NOT a killed process)');
  record('L7-PROCESS_DEATH', S.NOT_TESTED,
    'real process death NOT executed: would require a separate worker/process, a confirmed barrier, an external kill, and post-mortem verification');
  record('L7-POWER_LOSS', S.NOT_TESTED,
    'power-loss / fs-durability (rename durability without parent-dir fsync, write barriers) NOT executed on this host; requires fault-injection / VM / filesystem harness');
  rmSync(d, { recursive: true, force: true });
}

record('L0', existsSync(REAL_LOCK) === realLockExistedBefore ? S.PASS : S.FAIL, `real ledger lock untouched (existed before=${realLockExistedBefore})`);

// ---- result artifact ----------------------------------------------------------------------------------
const by = (s) => results.filter((r) => r.state === s);
const residuals = by(S.KNOWN_RESIDUAL);
const notTested = by(S.NOT_TESTED);
const out = {
  suite: 'ledger-lock liveness / corrupt-race / TOCTOU / repair (Phase 6-R3)',
  host: { platform: process.platform, release: os.release(), node: process.version, arch: process.arch },
  config: { corruptGraceMs: process.env.LOCK_CORRUPT_GRACE_MS ?? null, claimGraceMs: process.env.LOCK_CLAIM_GRACE_MS ?? null },
  claimScope: 'This artifact reports what the tested design currently demonstrates on this host. It is not a general proof about the filesystem or about writers that bypass this protocol.',
  fixedChecks: fails === 0 ? 'PASS' : 'FAIL',
  fixedCheckFailures: fails,
  mutualExclusionScope:
    'Demonstrated ONLY for the generations actually raced: (a) ABSENT generation (T1, 8 and 24 contenders), (b) live/held lock (T2, 24), (c) EXPIRED generation takeover via the claim CAS (T3, 100x24), (d) corrupt generations under the fail-closed policy (C1/C2, 100x24: never >=2 winners). NOT demonstrated: atomicity of the validate->mutate windows (L4/L5/L6 reproduce violations there with a REAL cooperative takeover), real process death (L7-PROCESS_DEATH), power-loss / fs durability (L7-POWER_LOSS), non-conforming writers that bypass this protocol, clock-jump behaviour beyond C6/R3. Race FREQUENCIES are environment- and timing-dependent and are not invariants.',
  knownResiduals: residuals.map((r) => ({ id: r.id, msg: r.msg, detail: r.detail })),
  notTested: notTested.map((r) => ({ id: r.id, msg: r.msg })),
  overallSafetyGate: 'PARTIAL',
  results,
};
writeFileSync(join(here, '..', '..', 'LOCK_LIVENESS_RESULT.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');
console.log(`\nfixedChecks=${out.fixedChecks} (failures=${fails}) knownResiduals=${residuals.length} notTested=${notTested.length} overallSafetyGate=PARTIAL`);
process.exit(fails === 0 ? 0 : 1);
