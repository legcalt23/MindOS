#!/usr/bin/env node
// Ledger mutual-exclusion lock with FENCING TOKENS (MB-LEDGER-0001 / MB-LEDGER-0004 / MB-LEDGER-0005).
// Injectable paths (LEDGER_PATH / LOCK_PATH) and injectable clock/config so tests run in an isolated temp
// dir and NEVER touch the real lock.
//
// WHAT THIS DESIGN ACTUALLY GUARANTEES (claims deliberately narrow — see LOCK_LIVENESS_RESULT.json):
//  - Creation is ATOMIC-WITH-CONTENT via link(): a temp file is written+fsynced, then linked into place.
//    link() fails EEXIST if the target exists, so it is the single-winner arbiter AND the lock is
//    fully-formed the instant it appears. open('wx')→write is NOT used as the normal protocol: it leaves
//    an empty-file window that racers misread as corruption (that window produced a real multi-winner).
//  - An EXPIRED generation is taken over via a takeover-claim keyed to the OBSERVED fencingToken
//    (atomic create-with-content, EEXIST = someone else claims it). The claim holder re-reads and only
//    replaces the lock if it is STILL the same expired generation. This is a CAS on the generation.
//  - CORRUPT is FAIL-CLOSED. There is no automatic read→sleep→unlink recovery: there is no identity/CAS
//    between such a read and the unlink, so it can delete a valid successor another writer just created.
//    A visible liveness loss (CORRUPT_LOCK + manualRepairRequired) is preferable to two silent ledger
//    writers. Repair is an explicit, separate, evidence-producing command (`repair`).
//  - An ORPHANED takeover-claim is likewise fail-closed (TAKEOVER_CLAIM_ORPHANED + manualRepairRequired).
//    It is NOT time-heuristically reaped: a "stale" judgement cannot distinguish an orphan from a live
//    claimant without an identity check.
//  - release/renew/writeLedgerUnderLock validate taskId+token+live-lease, and RE-validate immediately
//    before the mutating syscall.
//  - acquire() never reports success for a lock the classifier rejects: publication is re-classified and
//    must come back VALID *and* ours, else PUBLISH_UNVERIFIED (6-R3.3).
//  - Every fail-closed diagnostic acquire() can emit has an executable `repair` path (6-R3.1). A hint that
//    cannot run is a dead end, not a diagnostic.
//
// This comment describes what the TESTED DESIGN CURRENTLY DEMONSTRATES (lock-tests.mjs T1–T7,
// lock-liveness-tests.mjs C1–C7/R1–R5/L3), not what the filesystem or this protocol could guarantee in
// general. The safety gate is PARTIAL.
//
// KNOWN RESIDUALS (declared, NOT fixed — reproduced by lock-liveness-tests.mjs L4/L5/L6 against a REAL
// protocol-conforming takeover, i.e. these bite cooperative writers, not only non-conforming ones):
//  - The re-validate→mutate window is not atomic. The filesystem offers no compare-and-unlink or
//    compare-and-rename, so a takeover landing inside that window can still be clobbered.
// NOT TESTED (never claim these as proven):
//  - Real PROCESS DEATH. L7 simulates an early return before the rename (stopBeforeRename); it shows the
//    write ORDER is crash-safe by construction, it does not execute a killed process.
//  - POWER-LOSS / fs durability (rename durability without a parent-directory fsync, write barriers).
//  - A non-conforming writer that bypasses this protocol is not constrained by it.
//  - Backward clock jumps are surfaced as CORRUPT (acquiredUtc in the future beyond tolerance) rather
//    than silently producing an immortal lock; finer clock-jump semantics are untested.
import { openSync, writeSync, closeSync, existsSync, readFileSync, unlinkSync, renameSync, fsyncSync, linkSync } from 'node:fs';
import { randomBytes, createHash } from 'node:crypto';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
export const defaultLedger = () => process.env.LEDGER_PATH || join(ROOT, 'MINDBOOST_AUDIT_LEDGER.md');
export const defaultLock = () => process.env.LOCK_PATH || join(ROOT, 'MINDBOOST_AUDIT_LEDGER.lock');

// Config is resolved PER CALL (never captured at import time, so tests may inject/alter it), validated,
// and rejects NaN / negative / out-of-range values instead of silently coercing them.
const MAX_LEASE_MS = 24 * 60 * 60 * 1000;
const CLOCK_SKEW_TOLERANCE_MS = 60_000;
const HEX32 = /^[0-9a-f]{32}$/;
function envInt(name, dflt) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return dflt;
  const n = Number(raw);
  return Number.isInteger(n) && n >= 0 ? n : dflt;
}
function resolveNow(now) {
  if (now === undefined) return () => (process.env.LOCK_NOW ? Number(process.env.LOCK_NOW) : Date.now());
  if (typeof now !== 'function') throw new TypeError('now must be a function');
  return now;
}
function validLeaseMs(v) { return Number.isInteger(v) && v > 0 && v <= MAX_LEASE_MS; }
const token16 = () => randomBytes(16).toString('hex');

// Deterministic barrier hooks for the TOCTOU tests. Each fires AFTER the final validation and IMMEDIATELY
// BEFORE the mutating syscall — i.e. inside the true residual window, so a test can drive a real takeover
// there and observe the actual outcome. Production leaves them null.
// `afterRenewPreValidate` is different in kind: it fires between renew's INITIAL validation and its
// commit-time re-validation, so a test can make the lease cross expiry mid-call (C6) — which the
// re-validation must catch. The `after*Validate` hooks fire in the residual window (L4/L5/L6).
export const __hooks = { afterReleaseValidate: null, afterLedgerValidate: null, afterRenewValidate: null, afterRenewPreValidate: null };

export function ledgerSelfExcludingHash(ledgerPath = defaultLedger()) {
  const t = readFileSync(ledgerPath, 'utf8');
  const body = t.split('\n').filter((l) => !l.startsWith('Canonical SHA-256')).join('\n');
  return createHash('sha256').update(body, 'utf8').digest('hex');
}

// ATOMIC create-with-content. Returns {ok} or {ok:false, code}. EEXIST => someone else holds the name.
function atomicCreateWithContent(path, payload, nonce) {
  const tmp = `${path}.mk.${nonce}`;
  try {
    const fd = openSync(tmp, 'wx');
    writeSync(fd, JSON.stringify(payload, null, 2)); fsyncSync(fd); closeSync(fd);
  } catch (e) { try { unlinkSync(tmp); } catch {} return { ok: false, code: e.code }; }
  try { linkSync(tmp, path); return { ok: true }; }
  catch (e) { return { ok: false, code: e.code }; }
  finally { try { unlinkSync(tmp); } catch { /* best-effort */ } }
}

// Strict schema. Anything not exactly conforming is CORRUPT — never "held forever", never coerced.
function schemaError(r) {
  if (!r || typeof r !== 'object' || Array.isArray(r)) return 'not-an-object';
  if (typeof r.taskId !== 'string' || !r.taskId.trim()) return 'taskId-missing-or-empty';
  if (typeof r.owner !== 'string' || !r.owner.trim()) return 'owner-missing-or-empty';
  if (typeof r.fencingToken !== 'string' || !HEX32.test(r.fencingToken)) return 'fencingToken-not-32-hex';
  if (typeof r.acquiredUtc !== 'string') return 'acquiredUtc-not-string';
  const t = Date.parse(r.acquiredUtc);
  if (!Number.isFinite(t)) return 'acquiredUtc-unparseable';
  if (!validLeaseMs(r.leaseMs)) return 'leaseMs-not-a-bounded-positive-integer';
  return null;
}

export const LockState = { ABSENT: 'ABSENT', VALID: 'VALID', EXPIRED: 'EXPIRED', CORRUPT: 'CORRUPT', IO_ERROR: 'IO_ERROR' };

// Classifies a lock path into exactly one state. ABSENT/CORRUPT/IO_ERROR are distinct — collapsing them
// (as readLock()'s null does) is what previously stranded acquire() and hid corruption.
export function classifyLock(lockPath = defaultLock(), nowMs = Date.now()) {
  let raw;
  try { raw = readFileSync(lockPath, 'utf8'); }
  catch (e) { return e.code === 'ENOENT' ? { state: LockState.ABSENT } : { state: LockState.IO_ERROR, code: e.code }; }
  let rec;
  try { rec = JSON.parse(raw); } catch { return { state: LockState.CORRUPT, why: 'unparseable-json' }; }
  const bad = schemaError(rec);
  if (bad) return { state: LockState.CORRUPT, why: bad, rec };
  const acq = Date.parse(rec.acquiredUtc);
  if (acq > nowMs + CLOCK_SKEW_TOLERANCE_MS) return { state: LockState.CORRUPT, why: 'acquiredUtc-in-future (clock jump?)', rec };
  return { state: nowMs > acq + rec.leaseMs ? LockState.EXPIRED : LockState.VALID, rec };
}

// Back-compat convenience: parsed record or null. Callers needing to distinguish states use classifyLock.
export function readLock(lockPath = defaultLock()) {
  const c = classifyLock(lockPath, Date.now());
  return c.rec && (c.state === LockState.VALID || c.state === LockState.EXPIRED) ? c.rec : (c.rec ?? null);
}

export function acquire({ lockPath = defaultLock(), taskId, owner, leaseMs = 1_800_000, ledgerPath = defaultLedger(), now, maxAttempts = 64 } = {}) {
  const nowFn = resolveNow(now);
  if (typeof taskId !== 'string' || !taskId.trim()) return { ok: false, reason: 'BAD_TASK_ID' };
  if (typeof owner !== 'string' || !owner.trim()) return { ok: false, reason: 'BAD_OWNER' };
  if (!validLeaseMs(leaseMs)) return { ok: false, reason: 'BAD_LEASE_MS' };
  const fencingToken = token16();
  const safeHash = () => { try { return ledgerSelfExcludingHash(ledgerPath); } catch { return null; } };
  const mkRec = () => ({ taskId, owner, fencingToken, acquiredUtc: new Date(nowFn()).toISOString(), leaseMs, ledgerHashAtAcquire: safeHash() });

  // 6-R3.3: never report success for a lock the classifier rejects. Publishing is not owning — re-classify
  // and only claim ok=true if the published artifact is VALID and is OURS. A backward clock jump, a foreign
  // overwrite, or an IO fault between publication and this read must surface, not be papered over by
  // returning a `rec` that classifyLock() would call CORRUPT. The artifact is NOT auto-deleted: removing it
  // has the same no-CAS problem as the corrupt auto-reap this design already rejects.
  const verifyPublished = () => {
    const c = classifyLock(lockPath, nowFn());
    if (c.state === LockState.VALID && c.rec.taskId === taskId && c.rec.fencingToken === fencingToken) {
      return { ok: true, token: fencingToken, rec: c.rec };
    }
    return { ok: false, reason: 'PUBLISH_UNVERIFIED', publishedState: c.state, why: c.why, manualRepairRequired: true,
      repairHint: `node scripts/ledger-lock.mjs repair --confirm-no-writer  (lock: ${lockPath}; published as ${c.state})` };
  };

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const cls = classifyLock(lockPath, nowFn());

    if (cls.state === LockState.IO_ERROR) return { ok: false, reason: 'IO_ERROR:' + cls.code };

    // FAIL-CLOSED. No automatic unlink: there is no CAS between observing corruption and removing it, so
    // an automatic reap can delete a valid successor. Surface it and require explicit repair.
    if (cls.state === LockState.CORRUPT) return { ok: false, reason: 'CORRUPT_LOCK', why: cls.why, manualRepairRequired: true, repairHint: `node scripts/ledger-lock.mjs repair --confirm-no-writer  (lock: ${lockPath})` };

    if (cls.state === LockState.VALID) return { ok: false, reason: 'HELD', by: cls.rec.taskId };

    if (cls.state === LockState.ABSENT) {
      const made = atomicCreateWithContent(lockPath, mkRec(), fencingToken);
      if (made.ok) return verifyPublished();
      if (made.code === 'EEXIST') continue;                       // lost the create race → re-classify
      return { ok: false, reason: 'CREATE_ERR:' + made.code };
    }

    // EXPIRED → CAS takeover keyed to the OBSERVED generation.
    const obs = cls.rec;
    const claim = `${lockPath}.takeover.${obs.fencingToken}`;
    const made = atomicCreateWithContent(claim, { token: obs.fencingToken, at: nowFn(), by: taskId }, fencingToken);
    if (!made.ok) {
      if (made.code !== 'EEXIST') return { ok: false, reason: 'CLAIM_ERR:' + made.code };
      continue;                                                   // another claimant owns this generation
    }
    try {
      const still = classifyLock(lockPath, nowFn());
      if (still.state === LockState.EXPIRED && still.rec.fencingToken === obs.fencingToken) {
        // Safe: we hold the only claim for this generation, and it is still that generation.
        const tmp = `${lockPath}.new.${fencingToken}`;
        const fd = openSync(tmp, 'wx'); writeSync(fd, JSON.stringify(mkRec(), null, 2)); fsyncSync(fd); closeSync(fd);
        renameSync(tmp, lockPath);
        return verifyPublished();
      }
      continue;                                                   // generation changed → someone took over
    } finally { try { unlinkSync(claim); } catch { /* raced */ } }
  }
  // Exhausted while a claim for the observed generation persisted: either heavy contention or an ORPHANED
  // claim (its reaper crashed before the finally-unlink). Fail-closed with a diagnostic — never a
  // time-heuristic reap, which cannot distinguish an orphan from a live claimant.
  const fin = classifyLock(lockPath, nowFn());
  if (fin.state === LockState.EXPIRED && existsSync(`${lockPath}.takeover.${fin.rec.fencingToken}`)) {
    return { ok: false, reason: 'TAKEOVER_CLAIM_ORPHANED', manualRepairRequired: true, repairHint: `node scripts/ledger-lock.mjs repair --confirm-no-writer  (claim: ${lockPath}.takeover.${fin.rec.fencingToken})` };
  }
  return { ok: false, reason: 'EXHAUSTED' };
}

export function validateToken({ lockPath = defaultLock(), taskId, token, now } = {}) {
  const cls = classifyLock(lockPath, resolveNow(now)());
  return cls.state === LockState.VALID && cls.rec.taskId === taskId && cls.rec.fencingToken === token;
}

// An EXPIRED owner must NOT revive its lease (it may already have been fenced out by a takeover); it must
// re-acquire and get a NEW token. The lease is re-checked immediately before the commit, so a renew that
// CROSSES expiry mid-call is refused and its temp is removed.
export function renew({ lockPath = defaultLock(), taskId, token, leaseMs, now } = {}) {
  const nowFn = resolveNow(now);
  const pre = classifyLock(lockPath, nowFn());
  if (pre.state !== LockState.VALID || pre.rec.taskId !== taskId || pre.rec.fencingToken !== token) {
    if (pre.state === LockState.EXPIRED && pre.rec && pre.rec.fencingToken === token) return { ok: false, reason: 'EXPIRED' };
    if (pre.state === LockState.CORRUPT) return { ok: false, reason: 'CORRUPT_LOCK', why: pre.why, manualRepairRequired: true };
    return { ok: false, reason: 'TOKEN_MISMATCH' };
  }
  const newLease = leaseMs === undefined ? pre.rec.leaseMs : leaseMs;
  if (!validLeaseMs(newLease)) return { ok: false, reason: 'BAD_LEASE_MS' };
  if (__hooks.afterRenewPreValidate) __hooks.afterRenewPreValidate();  // C6: lease may cross expiry here
  const tmp = `${lockPath}.tmp.${token}`;
  const fd = openSync(tmp, 'w'); writeSync(fd, JSON.stringify({ ...pre.rec, acquiredUtc: new Date(nowFn()).toISOString(), leaseMs: newLease }, null, 2)); fsyncSync(fd); closeSync(fd);
  const post = classifyLock(lockPath, nowFn());
  if (post.state !== LockState.VALID || post.rec.fencingToken !== token || post.rec.taskId !== taskId) {
    try { unlinkSync(tmp); } catch {}
    return { ok: false, reason: post.state === LockState.EXPIRED ? 'EXPIRED' : 'TAKEN_OVER' };
  }
  if (__hooks.afterRenewValidate) __hooks.afterRenewValidate();   // residual window (L6)
  renameSync(tmp, lockPath);
  return { ok: true };
}

export function release({ lockPath = defaultLock(), taskId, token, now } = {}) {
  const nowFn = resolveNow(now);
  const cls = classifyLock(lockPath, nowFn());
  if (cls.state === LockState.ABSENT) return { ok: false, reason: 'NO_LOCK' };
  if (cls.state === LockState.CORRUPT) return { ok: false, reason: 'CORRUPT_LOCK', why: cls.why, manualRepairRequired: true };
  if (!cls.rec || cls.rec.taskId !== taskId || cls.rec.fencingToken !== token) return { ok: false, reason: 'TOKEN_MISMATCH', currentTask: cls.rec && cls.rec.taskId };
  if (__hooks.afterReleaseValidate) __hooks.afterReleaseValidate(); // residual window (L4)
  unlinkSync(lockPath);
  return { ok: true };
}

// Ledger write under lock: validate → temp+fsync → re-validate → atomic rename.
// stopBeforeRename=true simulates a process crash before the final rename.
export function writeLedgerUnderLock({ ledgerPath = defaultLedger(), lockPath = defaultLock(), taskId, token, content, stopBeforeRename = false, now } = {}) {
  if (!validateToken({ lockPath, taskId, token, now })) return { ok: false, reason: 'TOKEN_INVALID_PRE' };
  const tmp = `${ledgerPath}.tmp.${token}`;
  const fd = openSync(tmp, 'w'); writeSync(fd, content); fsyncSync(fd); closeSync(fd);
  if (stopBeforeRename) return { ok: false, reason: 'STOPPED_BEFORE_RENAME', tmp };
  if (!validateToken({ lockPath, taskId, token, now })) { try { unlinkSync(tmp); } catch {} return { ok: false, reason: 'TOKEN_INVALID_PRE_RENAME' }; }
  if (__hooks.afterLedgerValidate) __hooks.afterLedgerValidate();  // residual window (L5)
  renameSync(tmp, ledgerPath);
  return { ok: true };
}

// ---------------- CLI ----------------
if (import.meta.url === pathToFileURL(process.argv[1] || '').href) {
  const [cmd, taskId, owner, leaseArg] = process.argv.slice(2);
  if (cmd === 'acquire') {
    // 6-R3.2: the default applies ONLY when the argument is absent. `Number(x) || DEFAULT` silently mapped
    // 0/NaN/"abc" to 30 minutes — an operator who asks for an invalid lease must be refused, never quietly
    // handed a different safety parameter. Present ⇒ convert literally and let validLeaseMs reject it.
    const r = acquire({ taskId, owner, ...(leaseArg === undefined ? {} : { leaseMs: Number(leaseArg) }) });
    if (r.ok) { console.log(`ACQUIRED token=${r.token} hash@acquire=${r.rec.ledgerHashAtAcquire}`); process.exit(0); }
    console.error(`NOT ACQUIRED reason=${r.reason}${r.by ? ' by=' + r.by : ''}${r.why ? ' why=' + r.why : ''}${r.manualRepairRequired ? '\n  ' + r.repairHint : ''}`); process.exit(1);
  } else if (cmd === 'release') {
    const token = process.env.LOCK_TOKEN || leaseArg;
    const r = release({ taskId, token });
    if (r.ok) { console.log('RELEASED'); process.exit(0); }
    console.error(`NOT RELEASED reason=${r.reason}`); process.exit(1);
  } else if (cmd === 'status') {
    const c = classifyLock(); console.log(JSON.stringify(c, null, 2)); process.exit(0);
  } else if (cmd === 'hash') { console.log(ledgerSelfExcludingHash()); process.exit(0); }
  else if (cmd === 'repair') {
    // EXPLICIT, separate, evidence-producing repair. Never automatic. Requires an operator assertion that no
    // writer is live, quarantines (never deletes) the offending artifact, and prints the evidence.
    // 6-R3.1: it repairs BOTH classes acquire() can fail-closed on, because a diagnostic whose published
    // repairHint cannot execute is a dead end — the lock stays permanently unacquirable:
    //   CORRUPT lock                    → quarantine the lock
    //   EXPIRED lock + the claim for ITS OWN fencingToken → quarantine that claim (the lock is untouched;
    //                                     a normal acquire then completes the takeover through the CAS)
    // Anything else (VALID, ABSENT, EXPIRED with no matching claim) is refused fail-closed. The path is
    // always the configured lock — never an operator-supplied argument.
    const lockPath = defaultLock();
    const confirm = process.argv.includes('--confirm-no-writer');
    const c = classifyLock(lockPath, Date.now());
    console.log(JSON.stringify({ lockPath, classification: c }, null, 2));

    const claimFor = (rec) => `${lockPath}.takeover.${rec.fencingToken}`;
    let target, kind;
    if (c.state === LockState.CORRUPT) { target = lockPath; kind = 'corrupt-lock'; }
    else if (c.state === LockState.EXPIRED && existsSync(claimFor(c.rec))) { target = claimFor(c.rec); kind = 'orphaned-takeover-claim'; }
    else {
      console.error(`repair refused: nothing repairable here (lock state=${c.state}${c.state === LockState.EXPIRED ? ', and no takeover claim exists for its generation' : ''}). VALID/ABSENT locks are never touched; an EXPIRED lock without an orphaned claim is taken over by a normal acquire.`);
      process.exit(1);
    }
    if (!confirm) { console.error(`repair refused: would quarantine ${kind}; re-run with --confirm-no-writer after verifying no ledger writer is live`); process.exit(2); }

    // Re-read and re-verify the generation IMMEDIATELY before quarantining, so a lock that changed while the
    // operator was deciding is never repaired on a stale observation.
    const again = classifyLock(lockPath, Date.now());
    const sameGeneration = again.state === c.state && (!c.rec || (again.rec && again.rec.fencingToken === c.rec.fencingToken));
    if (!sameGeneration || (kind === 'orphaned-takeover-claim' && !existsSync(target))) {
      console.error(`repair aborted: the lock changed under us (was ${c.state}/${c.rec ? c.rec.fencingToken : '-'}, now ${again.state}/${again.rec ? again.rec.fencingToken : '-'}). Nothing was touched.`);
      process.exit(3);
    }
    const q = `${target}.quarantine.${Date.now()}`;
    renameSync(target, q);                                         // quarantine, do not delete — bytes preserved
    console.log(JSON.stringify({ quarantined: kind, from: target, to: q, classification: again.state, fencingToken: again.rec ? again.rec.fencingToken : null }, null, 2));
    console.log('Evidence retained (bytes preserved). Re-run acquire.');
    process.exit(0);
  }
  else { console.error('usage: acquire|release|status|hash|repair <taskId> <owner> [leaseMs]  (LOCK_TOKEN env for release)'); process.exit(2); }
}
