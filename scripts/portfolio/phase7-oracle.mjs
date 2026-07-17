#!/usr/bin/env node
// V2d PHASE 7 — FULL ADVERSARIAL ORACLE over the integrated snapshot.
//
// Runs every suite against ONE frozen input snapshot and classifies each mutation into exactly one outcome:
//
//   KILLED                  the mutation changed its target AND the expected gate failed with the expected diagnostic
//   SURVIVED                the mutation changed its target and NO gate caught it            → oracle failure
//   DID_NOT_ALTER_TARGET    the mutation was a no-op — it proves nothing                     → oracle failure
//   WRONG_GATE              something failed, but not the gate that owns the defect          → oracle failure
//   WRONG_DIAGNOSTIC        the right gate failed with a message that misdescribes the defect → oracle failure
//   RESTORATION_FAILURE     the suite did not put the bytes back                             → oracle failure
//   NOT_TESTED              the host could not exercise the guard — honest, not a pass
//
// A no-op mutation is an ORACLE failure, not a pass: it means the test cannot discriminate the fix from the
// bug, which is exactly how V2c's M2/M4 passed vacuously.
//
// The lock races are deliberately NOT re-run: the lock is outside this diff and is verified by hash drift.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => (existsSync(rel(p)) ? createHash('sha256').update(readFileSync(rel(p))).digest('hex') : 'ABSENT');

// ---------------------------------------------------------------- frozen input snapshot
const FROZEN_INPUTS = [
  'MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md',
  'PORTFOLIO_CODE_OBSERVATIONS.json', 'PORTFOLIO_DERIVED_MODEL.json', 'PORTFOLIO_RUNTIME_EVIDENCE.json',
  'PORTFOLIO_SOURCE_MANIFEST_V2.json', 'PORTFOLIO_SOURCE_ARCHIVE.mbsrc', 'PORTFOLIO_SOURCE_INDEX.json',
  'PORTFOLIO_V2C_LEGACY_CLAIMS.csv', 'PORTFOLIO_AUTHORITY_MAP.md', 'MINDBOOST_AUDIT_LEDGER.md',
];
// The lock artifacts are verified by DRIFT ONLY — their races are expensive and out of this diff.
const LOCK_ARTIFACTS = {
  'scripts/ledger-lock.mjs': '880fe6460e944618e7a8b3f5d9ddc56dca86358ded965cf5e3b0cc327db1c370',
  'scripts/portfolio/lock-liveness-tests.mjs': '8e03b90959ab',
  'scripts/portfolio/lock-tests.mjs': '659066c45c1a',
  'scripts/portfolio/lock-race-worker.mjs': '49fbbd4d4f65',
  'LOCK_LIVENESS_RESULT.json': '13c566de0640',
};

const before = Object.fromEntries(FROZEN_INPUTS.map((p) => [p, sha(p)]));
console.log('PHASE 7 — FROZEN INPUT SNAPSHOT');
for (const [p, h] of Object.entries(before)) console.log(`  ${p.padEnd(38)} ${h.slice(0, 16)}`);
const snapshotId = createHash('sha256').update(JSON.stringify(before)).digest('hex');
console.log(`  SNAPSHOT ID ${snapshotId}\n`);

// ---------------------------------------------------------------- lock drift (no races)
const lockDrift = [];
for (const [p, want] of Object.entries(LOCK_ARTIFACTS)) {
  const got = sha(p);
  if (!got.startsWith(want.slice(0, 12))) lockDrift.push(`${p}: ${got.slice(0, 12)}… != pinned ${want.slice(0, 12)}…`);
}
console.log(`LOCK DRIFT (races NOT re-run — out of diff, verified by hash): ${lockDrift.length ? 'DRIFT' : 'clean 5/5'}`);
lockDrift.forEach((d) => console.log(`  ! ${d}`));

// ---------------------------------------------------------------- run the suites
const SUITES = [
  { id: 'phase2', name: 'Phase 2 — authority model (F1-F6 + M1-M10)', script: 'scripts/portfolio/authority-tests.mjs', result: 'PORTFOLIO_AUTHORITY_TESTS_RESULT.json' },
  { id: 'phase3', name: 'Phase 3 — evidence manifest (P0 + P3-01..16 + P3-R)', script: 'scripts/portfolio/evidence-mutation-tests.mjs', result: 'PORTFOLIO_EVIDENCE_MUTATIONS_RESULT.json' },
  { id: 'phase4', name: 'Phase 4 — state model + P4-R1 row membership', script: 'scripts/portfolio/phase4-mutation-tests.mjs', result: 'PHASE4_MUTATION_RESULT.json' },
  { id: 'phase5', name: 'Phase 5 — packaging', script: 'scripts/portfolio/phase5-mutation-tests.mjs', result: 'PHASE5_MUTATION_RESULT.json' },
  // MB-V2D-R03 — a suite that is not in this list is not in the denominator, and a mutation outside the
  // denominator protects nothing. The mode suite is registered here for exactly that reason.
  { id: 'r03', name: 'MB-V2D-R03 — repo/bundle mode may never downgrade', script: 'scripts/portfolio/r03-mode-mutation-tests.mjs', result: 'R03_MODE_MUTATION_RESULT.json' },
];

const suiteRuns = [];
for (const s of SUITES) {
  const t0 = Number(process.hrtime.bigint() / 1000000n);
  let code = 0, out = '';
  try { out = execFileSync(process.execPath, [rel(s.script)], { encoding: 'utf8', cwd: root, stdio: ['ignore', 'pipe', 'pipe'], timeout: 540000 }); }
  catch (e) { code = e.status ?? 1; out = (e.stdout || '') + (e.stderr || ''); }
  const ms = Number(process.hrtime.bigint() / 1000000n) - t0;
  const res = existsSync(rel(s.result)) ? JSON.parse(readFileSync(rel(s.result), 'utf8')) : null;
  suiteRuns.push({ ...s, exitCode: code, durationMs: ms, result: res, tail: out.trim().split('\n').slice(-2).join(' | ') });
  console.log(`\n${s.name}\n  exit=${code} · ${ms}ms · ${suiteRuns.at(-1).tail}`);
}

// ---------------------------------------------------------------- classify every mutation
function classify(m, suiteId) {
  // Phase 4/5 shape: {id,title,pass,expectGate,expectedStatus,actualStatus,targetChanged,diagnosticMatched,bytesRestored}
  // Phase 2/3 shape: {id,pass,detail} — these suites assert internally and record pass/fail only.
  const id = m.id ?? m.name ?? '(unnamed)';
  if (/NOT_TESTED/i.test(m.detail ?? '') || /NOT_TESTED/i.test(m.title ?? '') || /NOT_TESTED/i.test(m.detail ?? '')) return { id, outcome: 'NOT_TESTED', why: 'the host could not exercise this guard; recorded honestly rather than scored as a pass' };
  if (m.bytesRestored === false) return { id, outcome: 'RESTORATION_FAILURE', why: 'the suite did not restore the bytes it mutated' };
  if (m.targetChanged === false) return { id, outcome: 'DID_NOT_ALTER_TARGET', why: 'the mutation was a no-op — it cannot discriminate the fix from the bug' };
  if (m.pass === true) return { id, outcome: 'KILLED', why: m.expectGate ? `${m.expectGate} → ${m.actualStatus} with the expected diagnostic` : 'assertion held against the live artifacts' };
  // failed: work out WHY it failed
  if (m.expectGate && m.actualStatus && m.actualStatus !== m.expectedStatus) {
    if (m.actualStatus === 'PASS' || m.actualStatus === '(gate absent)') return { id, outcome: 'SURVIVED', why: `${m.expectGate} stayed ${m.actualStatus} — the mutation was not caught` };
    return { id, outcome: 'WRONG_GATE', why: `${m.expectGate} was ${m.actualStatus}, wanted ${m.expectedStatus}` };
  }
  if (m.diagnosticMatched === false) return { id, outcome: 'WRONG_DIAGNOSTIC', why: 'the right gate failed but the message misdescribes the defect' };
  return { id, outcome: 'SURVIVED', why: m.detail ? String(m.detail).slice(0, 120) : 'suite reported failure' };
}

const classified = [];
for (const s of suiteRuns) {
  const list = s.result?.mutations ?? s.result?.checks ?? s.result?.probes ?? [];
  for (const m of list) classified.push({ suite: s.id, ...classify(m, s.id) });
  if (!list.length && s.result) classified.push({ suite: s.id, id: '(suite)', outcome: s.exitCode === 0 ? 'KILLED' : 'SURVIVED', why: `suite-level result only; exit=${s.exitCode}` });
}

// ---------------------------------------------------------------- historical V2c false-greens
// These are the claims V2c published as green and that the current pipeline must make unexpressible.
const md = readFileSync(rel('MINDBOOST_EXERCISE_PORTFOLIO.md'), 'utf8');
const model = JSON.parse(readFileSync(rel('PORTFOLIO_DERIVED_MODEL.json'), 'utf8'));
const csvHdr = readFileSync(rel('MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8').split('\n')[0];
const falseGreens = [
  { id: 'V2C-FG-01', claim: '"Live-reachable 43/121" published as a current fact', dead: !/Live-reachable/.test(md), how: 'the metric does not exist in the registry and the token is absent from the published view' },
  { id: 'V2C-FG-02', claim: '"Mechanic-implemented 44/121" published as a current fact', dead: !/Mechanic-implemented/.test(md), how: 'same — retired with the legacy columns' },
  { id: 'V2C-FG-03', claim: 'LIVE_REACHABLE as a state in the model', dead: !JSON.stringify(model.perSlot).includes('LIVE_REACHABLE'), how: 'STATE_INVARIANTS_GATE fails if the token reappears in the model' },
  { id: 'V2C-FG-04', claim: 'implementationStatus as an editorial column', dead: !csvHdr.includes('implementationStatus'), how: 'PORTFOLIO_ACTIVE_SCHEMA_GATE rejects it (P4-01)' },
  { id: 'V2C-FG-05', claim: 'a row excluding itself from validation via entityType', dead: !csvHdr.includes('entityType'), how: 'ROW_MEMBERSHIP_GATE + removed from the schema (P4-08)' },
  // BEHAVIOURAL, not textual. An earlier revision of this probe grepped for the string
  // `setStatus('RUNTIME_PLAYABILITY_GATE', 'NOT_RUN'` and reported the defect ALIVE — but the Phase 4 code
  // reaches that call inside `if (n === 0)`, where n is the authority's run count, and answers PASS when a
  // real run exists. The grep flagged a DERIVED call as if it were the V2c hard-coded literal: a naive regex
  // used as semantic proof, which is the exact error class this audit exists to catch. The honest test is
  // whether the gate MOVES when the authority moves — which is what P4-09/P4-10 execute against live code.
  { id: 'V2C-FG-06', claim: 'RUNTIME gate hard-coded to NOT_RUN regardless of the authority',
    dead: (() => {
      const r = existsSync(rel('PHASE4_MUTATION_RESULT.json')) ? JSON.parse(readFileSync(rel('PHASE4_MUTATION_RESULT.json'), 'utf8')) : null;
      const p9 = r?.mutations?.find((m) => m.id === 'P4-09');   // a VALID run must move the gate off NOT_RUN
      const p10 = r?.mutations?.find((m) => m.id === 'P4-10');  // an UNBOUND run must be rejected
      return !!p9 && p9.pass === true && p9.targetChanged === true && p9.actualStatus === 'PASS'
        && !!p10 && p10.pass === true && p10.actualStatus === 'FAIL';
    })(),
    how: 'BEHAVIOURAL: P4-09 injects a valid run → RUNTIME_PLAYABILITY_GATE moves to PASS with runtimeVerified 0→1 (the V2c literal stayed NOT_RUN under the identical probe — F4-4); P4-10 injects a run missing platform/snapshot → FAIL. The gate demonstrably tracks the authority.' },
  { id: 'V2C-FG-07', claim: 'EXTRA-121 printed as a literal table row', dead: !/\|\s*EXTRA-121\s*\|\s*0\/1\s*\|/.test(md), how: 'derived from the real row (P4-11/P4-12)' },
  { id: 'V2C-FG-08', claim: 'a manifest hashing 490 paths whose bytes are absent', dead: JSON.parse(readFileSync(rel('PORTFOLIO_SOURCE_INDEX.json'), 'utf8')).archive.entryCount > 0, how: 'the source archive carries the bytes; clean room recomputes them without the repo' },
];
console.log('\nHISTORICAL V2c FALSE-GREENS (must be unexpressible now)');
for (const f of falseGreens) console.log(`  ${f.dead ? 'DEAD   ' : 'ALIVE !'} ${f.id} — ${f.claim}\n            ${f.how}`);

// ---------------------------------------------------------------- restoration proof
const after = Object.fromEntries(FROZEN_INPUTS.map((p) => [p, sha(p)]));
const moved = FROZEN_INPUTS.filter((p) => before[p] !== after[p]);

// ---------------------------------------------------------------- verdict
const tally = classified.reduce((a, c) => { a[c.outcome] = (a[c.outcome] ?? 0) + 1; return a; }, {});
const ORACLE_FAILURES = ['SURVIVED', 'DID_NOT_ALTER_TARGET', 'WRONG_GATE', 'WRONG_DIAGNOSTIC', 'RESTORATION_FAILURE'];
const failures = classified.filter((c) => ORACLE_FAILURES.includes(c.outcome));
const aliveGreens = falseGreens.filter((f) => !f.dead);

console.log('\nCLASSIFICATION');
for (const [k, v] of Object.entries(tally).sort()) console.log(`  ${k.padEnd(22)} ${v}`);
console.log(`\nRESTORATION: ${moved.length === 0 ? 'all frozen inputs byte-identical after the full oracle' : `${moved.length} MOVED: ${moved.join(', ')}`}`);

const pass = failures.length === 0 && aliveGreens.length === 0 && moved.length === 0 && lockDrift.length === 0;
writeFileSync(rel('PHASE7_ORACLE_RESULT.json'), JSON.stringify({
  artifact: 'V2d Phase 7 adversarial oracle', schemaVersion: '1.0.0',
  contract: 'Every mutation is classified into exactly one outcome. KILLED and NOT_TESTED are the only non-failures; a no-op mutation is an ORACLE failure because it cannot discriminate the fix from the bug.',
  lockPolicy: 'The lock races are NOT re-run: the lock is outside this diff and is verified by hash drift only.',
  snapshotId, frozenInputsBefore: before, frozenInputsAfter: after, restorationClean: moved.length === 0,
  lockDrift, suites: suiteRuns.map(({ id, name, exitCode, durationMs, tail }) => ({ id, name, exitCode, durationMs, tail })),
  classification: classified, tally, oracleFailures: failures,
  historicalFalseGreens: falseGreens, falseGreensStillAlive: aliveGreens,
  verdict: pass ? 'PASS' : 'FAIL',
}, null, 2) + '\n', 'utf8');

console.log(`\nPHASE 7 ORACLE: ${pass ? 'PASS' : 'FAIL'}`);
for (const f of failures) console.log(`  ! [${f.suite}] ${f.id} → ${f.outcome}: ${f.why}`);
for (const f of aliveGreens) console.log(`  ! ${f.id} STILL EXPRESSIBLE — ${f.claim}`);
process.exit(pass ? 0 : 1);
