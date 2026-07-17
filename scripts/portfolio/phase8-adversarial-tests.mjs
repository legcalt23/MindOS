#!/usr/bin/env node
// MB-V2D-R3-A2 §4/§12 — adversarial tests against the Phase 8 runner itself.
//
// "12/12" is worth nothing unless an exception cannot shrink the denominator. The clean room already printed
// "6/6" while skipping four checks, so this is not hypothetical: it is the exact failure mode, reproduced once
// already in this codebase. These tests attack the runner's accounting rather than the artifacts.
import { writeFileSync, readFileSync, existsSync, mkdtempSync, mkdirSync, rmSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { createHash } from 'node:crypto';
import { PHASE8_CHECKS, EXPECTED_CHECK_COUNT, CHECK_STATES, contractViolations, reconcile } from './phase8-checks.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const sha = (s) => createHash('sha256').update(s).digest('hex');

// R3-A3 — DECLARED LOCALLY, NOT IMPORTED. Every assertion below used to import EXPECTED_CHECK_COUNT from the very
// module it is policing, so an attacker who decremented the constant moved this suite's goalpost with it: delete
// P8-12, set the constant to 11, and the runner reported "FREEZE_VERIFIED (11/11)" while this suite reported
// 12/12 KILLED. A test that imports its expectation from the artifact under test cannot fail on that artifact.
const LOCAL_EXPECTED_COUNT = 12;
const LOCAL_EXPECTED_IDS = ['P8-01', 'P8-02', 'P8-03', 'P8-04', 'P8-05', 'P8-06', 'P8-07', 'P8-08', 'P8-09', 'P8-10', 'P8-11', 'P8-12'];
// §6 — pinned BEFORE any mutation runs, so the isolation claim is measured against the tree we started from.
const canonicalBefore = sha(readFileSync(join(root, 'scripts/portfolio/phase8-freeze.mjs'), 'utf8'));
const results = [];
const rec = (id, requirement, pass, evidence) => {
  results.push({ id, requirement, pass, classification: pass ? 'KILLED' : 'SURVIVED', evidence });
  console.log(`${pass ? 'KILLED  ' : 'SURVIVED'} ${id} — ${requirement}\n         ${evidence}`);
};

// Simulate a run that aborts at position `k`: checks 1..k-1 PASS, the rest are never reached.
// This is exactly what an exception at check k produces in the real runner, whose post-catch pass marks every
// still-NOT_STARTED check ABORTED.
const abortAt = (k) => {
  const r = {};
  PHASE8_CHECKS.forEach((c, i) => { r[c.checkId] = { state: i < k - 1 ? 'PASS' : 'NOT_STARTED' }; });
  for (const [id, v] of Object.entries(r)) if (v.state === 'NOT_STARTED') r[id] = { state: 'ABORTED' };
  return r;
};

export function run() {
  results.length = 0;

  // ---- contract itself
  rec('P8A-00', 'the frozen registry is internally consistent', contractViolations().length === 0,
    `${PHASE8_CHECKS.length} checks, unique ids, unique order, every dependsOn resolvable, every check carries an expectedFailureCode. Violations: ${JSON.stringify(contractViolations())}`);

  // R3-A3 — the registry must match a goalpost this file declares, NOT one it imports. P8A-00 above can be
  // satisfied by a registry that is merely self-consistent; a review shrank the contract to eleven checks and
  // every test here stayed green because the expectation travelled with the artifact.
  { const actualIds = PHASE8_CHECKS.map((c) => c.checkId);
    const idsMatch = actualIds.length === LOCAL_EXPECTED_IDS.length && actualIds.every((id, i) => id === LOCAL_EXPECTED_IDS[i]);
    rec('P8A-00b', 'the registry matches the LOCALLY declared twelve — the goalpost cannot travel with the attack',
      idsMatch && PHASE8_CHECKS.length === LOCAL_EXPECTED_COUNT,
      idsMatch ? `registry is exactly the ${LOCAL_EXPECTED_COUNT} ids declared in this file: [${LOCAL_EXPECTED_IDS.join(', ')}]. Deleting a check and decrementing EXPECTED_CHECK_COUNT now fails here, where the expectation is a literal rather than an import.`
        : `REGISTRY DIVERGED: [${actualIds.join(', ')}] != locally declared [${LOCAL_EXPECTED_IDS.join(', ')}]`);
  }

  // ---- an exception at EVERY position 1..12 must never yield N/N PASS
  {
    const bad = [];
    for (let k = 1; k <= LOCAL_EXPECTED_COUNT; k++) {
      const rc = reconcile(abortAt(k));
      // The three properties the contract demands of an aborted run.
      const denominatorHeld = rc.of === LOCAL_EXPECTED_COUNT && rc.total === LOCAL_EXPECTED_COUNT;
      const notAllPass = rc.passed < LOCAL_EXPECTED_COUNT;
      const laterReported = rc.counts.ABORTED === LOCAL_EXPECTED_COUNT - (k - 1);
      if (!denominatorHeld || !notAllPass || !laterReported) bad.push(`k=${k}: of=${rc.of} total=${rc.total} passed=${rc.passed} aborted=${rc.counts.ABORTED}`);
    }
    rec('P8A-01', 'an exception at any position 1–12 never reports N/N PASS', bad.length === 0,
      bad.length ? `FAILURES: ${bad.join(' · ')}`
        : `all 12 abort positions checked. In each, the denominator stays 12, passed < 12, and the unreached checks are reported ABORTED rather than dropped. An abort at k=1 yields 0/12 with 12 ABORTED; at k=12 yields 11/12 with 1 ABORTED. This is the property the clean room lacked when it printed "6/6" for a 10-check contract.`);
  }

  // ---- a missing result must not be silently tolerated
  { const r = abortAt(13); delete r['P8-07'];
    const rc = reconcile(r);
    rec('P8A-02', 'a missing check result → reconciliation FAILS', !rc.reconciled && rc.violations.some((v) => /missing result for P8-07/.test(v)),
      `reconciled=${rc.reconciled} · ${rc.violations.join('; ')}. A result that never arrives cannot be read as "fine".`);
  }

  // ---- a duplicate/unknown check id must be rejected
  { const r = abortAt(13); r['P8-99'] = { state: 'PASS' };
    const rc = reconcile(r);
    rec('P8A-03', 'an unknown check id → reconciliation FAILS', !rc.reconciled && rc.violations.some((v) => /unknown check result "P8-99"/.test(v)),
      `reconciled=${rc.reconciled} · ${rc.violations.filter((v) => /P8-99/.test(v)).join('; ')}. Passing a check the registry never specified cannot raise the score — the denominator is the registry, not the results.`);
  }

  // ---- an unknown state must be rejected
  { const r = abortAt(13); r['P8-03'] = { state: 'PROBABLY_FINE' };
    const rc = reconcile(r);
    rec('P8A-04', 'an unknown state → reconciliation FAILS', !rc.reconciled && rc.violations.some((v) => /unknown state/.test(v)),
      `reconciled=${rc.reconciled} · ${rc.violations.filter((v) => /unknown state/.test(v)).join('; ')}. States come from one closed enum [${CHECK_STATES.join(', ')}]; an invented state cannot be counted as passing.`);
  }

  // ---- a dynamic denominator (the actual historical defect) must be impossible
  { const r = abortAt(7);                      // 6 PASS, 6 ABORTED
    const rc = reconcile(r);
    const emittedOnly = Object.values(r).filter((x) => x.state === 'PASS').length;
    rec('P8A-05', 'the denominator cannot shrink to the emitted results', rc.of === LOCAL_EXPECTED_COUNT && rc.passed === emittedOnly && rc.passed !== rc.of,
      `an abort at check 7 emits ${emittedOnly} PASS. The OLD accounting (passed/emitted) would read ${emittedOnly}/${emittedOnly} = "all green"; the contract reads ${rc.passed}/${rc.of}. This is MB-V2D-R3-A2 §4's core requirement and the exact bug that produced "CLEAN ROOM: 6/6" for a 10-step run.`);
  }

  // ---- ABORTED / INFRA_ERROR are not passes
  { const r = abortAt(13); r['P8-05'] = { state: 'ABORTED' }; r['P8-06'] = { state: 'INFRA_ERROR' };
    const rc = reconcile(r);
    rec('P8A-06', 'ABORTED and INFRA_ERROR never count as PASS', rc.reconciled && rc.passed === LOCAL_EXPECTED_COUNT - 2,
      `passed=${rc.passed}/${rc.of} with one ABORTED and one INFRA_ERROR. Only PASS passes — a crash is not a proof, and neither is an infrastructure failure.`);
  }

  // ------------------------------------------------------------------ RUNNER mutations (not helper-only)
  // Everything above tests reconcile() in isolation. That is NOT enough, and a review proved it: with the
  // suite unchanged at 7/7, a compound mutation of phase8-freeze.mjs alone produced
  //   "PHASE 8: FREEZE_VERIFIED (6/12)" with exitCode 0
  // — a real false green, six checks executed, invisible to every test here. The load-bearing mechanism is the
  // runner's own wiring (the NOT_STARTED pre-seed, the NOT_STARTED→ABORTED conversion, and the `verified`
  // conjunction), and none of it was exercised. These mutations edit the RUNNER in a disposable copy and
  // execute it end-to-end, so a revert of any guard is caught by a test rather than by a human reading a diff.
  // R3-A3 §6 — a mutation that changes no bytes tests nothing. P8A-07 once SURVIVED for exactly this reason: the
  // patch was a no-op on the happy path, so both arms ran identical code and the "divergence" was noise. A no-op
  // is an ORACLE FAILURE, not a pass, so `targetChanged` is asserted here rather than trusted per-test.
  // `expectNoOp` is the one deliberate exception: the unmutated baseline arm.
  // `corrupt` damages a real artifact INSIDE the disposable copy, so an oracle can be stated in terms of ground
  // truth ("this pinned file is provably wrong, therefore the run must fail") instead of in terms of whatever the
  // runner's own bookkeeping reports. Every oracle that reads only the runner's output is relative to the artifact
  // under test — that is how record() could launder FAIL into PASS with all twelve tests green.
  const mutateAndRun = (label, patch, { expectNoOp = false, corrupt = [] } = {}) => {
    const work = mkdtempSync(join(tmpdir(), 'p8a-runner-'));
    try {
      // A disposable copy of the repo tree the runner needs. The canonical repo is never mutated (MB-V2D-R06).
      const copy = (rp) => { const s = join(root, rp), d = join(work, rp); if (!existsSync(s)) return; mkdirSync(dirname(d), { recursive: true }); writeFileSync(d, readFileSync(s)); };
      const manifest = JSON.parse(readFileSync(join(root, 'BUNDLE_MANIFEST.json'), 'utf8'));
      for (const a of manifest.artifacts) copy(a.path);
      for (const f of ['BUNDLE_MANIFEST.json', 'BUNDLE_CONTAINER_HASH.txt', 'MINDBOOST_V2D_CANDIDATE_BUNDLE.zip',
        'MINDBOOST_PORTFOLIO_V2C_HANDOFF_8795736d0b1afbb6da1b319d16b862536850ee39f711d6236f4900238dc548fd.zip',
        'scripts/validate-audit-ledger.mjs', 'scripts/portfolio/phase8-checks.mjs', 'scripts/portfolio/three-roots.mjs',
        'scripts/portfolio/validation-mode.mjs', 'scripts/portfolio/build-source-archive.mjs',
        // Every module the runner imports must be copied, or it dies on an unresolved import BEFORE any check and
        // every arm degenerates to NO_VERDICT — which looks like a mutation "working" while proving nothing. The
        // baseline arm (P8A-10) is what surfaces an omission here; it caught this file being missed.
        'scripts/portfolio/atomic-report.mjs', 'R3A_FINAL_SNAPSHOT.json']) copy(f);
      const runnerPath = join(work, 'scripts/portfolio/phase8-freeze.mjs');
      mkdirSync(dirname(runnerPath), { recursive: true });
      const before = readFileSync(join(root, 'scripts/portfolio/phase8-freeze.mjs'), 'utf8');
      const after = patch(before);
      const targetChanged = before !== after;
      if (!expectNoOp && !targetChanged) {
        throw new Error(`ORACLE FAILURE: mutation "${label}" changed no bytes of the runner. A no-op cannot discriminate — the pattern it targets has moved or was never there.`);
      }
      writeFileSync(runnerPath, after, 'utf8');
      const targetHashes = { beforeSha: sha(before), afterSha: sha(after), targetChanged, bytesDelta: after.length - before.length };
      // Ground truth for the oracle: damage the copy AFTER it is staged, and assert the damage actually landed.
      // A corruption that silently missed its target would make the run pass for the right reason by accident.
      const corrupted = [];
      for (const rp of corrupt) {
        const f = join(work, rp);
        if (!existsSync(f)) throw new Error(`ORACLE FAILURE: cannot corrupt "${rp}" — not present in the disposable copy`);
        const orig = readFileSync(f);
        writeFileSync(f, Buffer.concat([orig, Buffer.from('CORRUPTED_BY_P8A')]));
        corrupted.push({ path: rp, wasSha: sha(orig.toString('binary')), nowSha: sha(readFileSync(f).toString('binary')) });
      }
      let out = '', code = 0;
      try { out = execFileSync(process.execPath, [runnerPath], { encoding: 'utf8', cwd: work, stdio: ['ignore', 'pipe', 'pipe'], timeout: 240000 }); }
      catch (e) { code = e.status ?? 1; out = (e.stdout || '') + (e.stderr || ''); }
      const verdict = /PHASE 8: (FREEZE_VERIFIED|FREEZE_FAILED) \((\d+)\/(\d+)\)/.exec(out);
      return { label, code, verdict: verdict?.[1] ?? 'NO_VERDICT', passed: Number(verdict?.[2] ?? -1), of: Number(verdict?.[3] ?? -1), out: out.slice(-400), corrupted, ...targetHashes };
    } finally { rmSync(work, { recursive: true, force: true }); }
  };

  // R1 — drop the NOT_STARTED pre-seed AND skip checks. Both halves are required: on a happy path every check
  // records its own result, so removing the pre-seed alone changes nothing observable — a NO-OP mutation, which
  // the contract counts as an oracle failure, not a pass. The pre-seed is load-bearing ONLY when a check is
  // skipped, so the mutation must produce a skip to reach the guard it targets. (First draft of this test did
  // not, reported SURVIVED, and was right to: it was testing nothing.)
  { const r = mutateAndRun('no pre-seed + skipped checks', (s) => s
      .replace("for (const c of PHASE8_CHECKS) results[c.checkId] = { state: 'NOT_STARTED', detail: null, failureCode: null };", '')
      .replace('    // P8-07 — archive READ from inputRoot, sources extracted INTO workRoot.',
        '    return { R, seal, freezeId, frozen, post: {}, moved: [], zipSha, v2cSha, manifest, inputImmutable: null };\n    // P8-07'));
    const falseGreen = r.verdict === 'FREEZE_VERIFIED' && r.passed < r.of;
    rec('P8A-07', 'REVERT the NOT_STARTED pre-seed WITH skipped checks → still no false green', !falseGreen,
      `real runner executed with the pre-seed removed and an early return after check 6 → verdict=${r.verdict} (${r.passed}/${r.of}) exit=${r.code}. ` +
      `Without the pre-seed the six unreached checks are absent from \`results\` entirely; reconcile()'s "missing result for X" and the state-sum invariant catch it, so the verdict cannot be VERIFIED.`);
  }

  // R2 — drop the reconciliation term from the verdict conjunction.
  //
  // The first version of this test was a TAUTOLOGY and could never fail. Its oracle was
  //   `r.verdict !== 'FREEZE_VERIFIED' || r.passed === r.of`
  // but verdict==='FREEZE_VERIFIED' already IMPLIES rec.passed === EXPECTED_CHECK_COUNT, which IS
  // r.passed === r.of — so the second disjunct is true exactly whenever the first is false. It asserted
  // nothing about the code. Empirically confirmed: dropping the term alone produced output byte-identical to
  // the baseline, and the test still reported KILLED. The one guard written to catch "someone drops
  // rec.reconciled" could not catch it.
  //
  // To discriminate, the mutation must reach a state where `passed === 12` AND `reconciled === false` at the
  // same time — otherwise the surviving `rec.passed === EXPECTED_CHECK_COUNT` term masks the missing one. So
  // it injects a spurious check-id result (unknown to the frozen registry → reconciled=false) while every real
  // check still passes. With rec.reconciled in the formula the verdict must be FREEZE_FAILED; without it, the
  // runner would call a registry-violating run VERIFIED.
  // State ABORTED, deliberately NOT 'PASS'. A spurious PASS would raise `passed` to 13, which the surviving
  // `rec.passed === EXPECTED_CHECK_COUNT` term catches by itself — so both arms would fail and the mutation
  // would again isolate nothing (first attempt did exactly that: FREEZE_FAILED 13/12 in both arms). ABORTED
  // leaves passed at exactly 12 while the unknown id and the 13-way state sum make reconciled false, so
  // `rec.reconciled` is the ONLY term that can refuse the run. That is what makes the two arms diverge.
  const injectSpurious = "\nresults['P8-SPURIOUS'] = { state: 'ABORTED', detail: 'injected: not in the frozen registry', failureCode: 'ABORTED' };\n";
  {
    const withGuard = mutateAndRun('spurious check id, rec.reconciled INTACT', (s) => s.replace(
      'const rec = reconcile(results);', injectSpurious + 'const rec = reconcile(results);'));
    const withoutGuard = mutateAndRun('spurious check id, rec.reconciled DROPPED', (s) => s
      .replace('const rec = reconcile(results);', injectSpurious + 'const rec = reconcile(results);')
      .replace('const verified = rec.reconciled && rec.passed === EXPECTED_CHECK_COUNT && !fatal && !outcome?.infra;',
        'const verified = rec.passed === EXPECTED_CHECK_COUNT && !fatal && !outcome?.infra;'));
    // The test is meaningful ONLY if the two arms differ — that difference IS the guard.
    const guardHolds = withGuard.verdict === 'FREEZE_FAILED' && withGuard.code !== 0;
    const guardIsLoadBearing = withoutGuard.verdict === 'FREEZE_VERIFIED';
    rec('P8A-08', 'rec.reconciled is load-bearing: a registry-violating run verifies WITHOUT it and fails WITH it',
      guardHolds && guardIsLoadBearing,
      `guard INTACT → ${withGuard.verdict} (${withGuard.passed}/${withGuard.of}) exit=${withGuard.code} · guard DROPPED → ${withoutGuard.verdict} (${withoutGuard.passed}/${withoutGuard.of}) exit=${withoutGuard.code}. ` +
      (guardHolds && guardIsLoadBearing
        ? 'The two arms diverge, which is what proves the term does work rather than decorate. All 12 real checks PASS in both arms; only the spurious id makes reconciliation false, so `rec.passed === 12` cannot mask its absence.'
        : `NOT DISCRIMINATING — guardHolds=${guardHolds} guardIsLoadBearing=${guardIsLoadBearing}. If both arms agree, this test asserts nothing about rec.reconciled.`));
  }

  // R3 — the compound the reviewer used to force a false green: no pre-seed + no reconciliation term + an
  // early silent return after check 6. This is the exact mutation that produced "FREEZE_VERIFIED (6/12)".
  { const r = mutateAndRun('compound: no pre-seed + no reconcile term + early return', (s) => s
      .replace("for (const c of PHASE8_CHECKS) results[c.checkId] = { state: 'NOT_STARTED', detail: null, failureCode: null };", '')
      .replace('const verified = rec.reconciled && rec.passed === EXPECTED_CHECK_COUNT && !fatal && !outcome?.infra;',
        'const verified = rec.passed === EXPECTED_CHECK_COUNT && !fatal && !outcome?.infra;')
      .replace("    // P8-07 — archive READ from inputRoot, sources extracted INTO workRoot.",
        "    return { R, seal, freezeId, frozen, post: {}, moved: [], zipSha, v2cSha, manifest, inputImmutable: null };\n    // P8-07"));
    const falseGreen = r.verdict === 'FREEZE_VERIFIED' && r.passed < r.of;
    rec('P8A-09', 'the reviewer\'s compound mutation can no longer produce FREEZE_VERIFIED with checks unrun',
      !falseGreen,
      `real runner executed with all three guards reverted → verdict=${r.verdict} (${r.passed}/${r.of}) exit=${r.code}. ` +
      (falseGreen ? 'STILL A FALSE GREEN — the accounting is not load-bearing.'
        : `A false green requires passed < of AND verdict FREEZE_VERIFIED; that combination did not occur. Previously this exact mutation yielded "FREEZE_VERIFIED (6/12)" with exit 0 while this suite reported 7/7.`));
  }

  // R4 — baseline: the UNMUTATED runner must still verify, or every red above is meaningless.
  // `expectNoOp` is REQUIRED here and only here: the baseline is the one arm whose patch must change nothing.
  { const r = mutateAndRun('unmutated baseline', (s) => s, { expectNoOp: true });
    rec('P8A-10', 'BASELINE: the unmutated runner still reaches 12/12 in a disposable copy',
      r.verdict === 'FREEZE_VERIFIED' && r.passed === LOCAL_EXPECTED_COUNT && r.code === 0 && r.targetChanged === false,
      `verdict=${r.verdict} (${r.passed}/${r.of}) exit=${r.code}, targetChanged=${r.targetChanged}. Without a passing baseline the mutations prove only that the runner can fail.`);
  }

  // R5 — THE NUMERATOR. Everything above polices the DENOMINATOR (12 results, valid states, sum = 12). Nothing
  // policed whether a result is TRUE. A review flipped one line in record() —
  //     results[checkId] = { state: ok ? 'PASS' : 'FAIL', ... }   ->   { state: 'PASS', ... }
  // — corrupted the V2c handoff (the very artifact that recovered the poisoned CSV in MB-PORTFOLIO-0018), and the
  // runner published `FREEZE_VERIFIED 12/12 reconciled=true status=completed` while this suite reported 12/12
  // KILLED. Shape was fully defended; fidelity was not defended at all.
  //
  // The fix is to state the oracle in GROUND TRUTH: a pinned artifact is provably corrupt, therefore the run must
  // fail — a claim record() cannot influence. Both arms carry the same corruption; only record() differs.
  {
    const V2C = 'MINDBOOST_PORTFOLIO_V2C_HANDOFF_8795736d0b1afbb6da1b319d16b862536850ee39f711d6236f4900238dc548fd.zip';
    const LAUNDER = [
      "results[checkId] = { state: ok ? 'PASS' : 'FAIL', detail, failureCode: ok ? null : (failureCode ?? spec.expectedFailureCode) };",
      "results[checkId] = { state: 'PASS', detail, failureCode: null };",
    ];
    const honest = mutateAndRun('corrupted V2c, record() INTACT', (s) => s, { expectNoOp: true, corrupt: [V2C] });
    const laundered = mutateAndRun('corrupted V2c, record() LAUNDERS FAIL→PASS', (s) => {
      if (!s.includes(LAUNDER[0])) throw new Error('ORACLE FAILURE: record() no longer matches the laundering target — this test has drifted and is no longer attacking the numerator');
      return s.replace(LAUNDER[0], LAUNDER[1]);
    }, { corrupt: [V2C] });
    const honestFails = honest.verdict === 'FREEZE_FAILED' && honest.code === 1;
    const launderLies = laundered.verdict === 'FREEZE_VERIFIED';
    rec('P8A-12', 'NUMERATOR: a provably corrupt pinned artifact must fail the run — record() cannot launder FAIL into PASS',
      honestFails && launderLies,
      honestFails && launderLies
        ? `Ground-truth oracle, independent of the runner's own bookkeeping. Same corruption in both arms: record() INTACT → ${honest.verdict} (${honest.passed}/${honest.of}) exit=${honest.code} · record() LAUNDERED → ${laundered.verdict} (${laundered.passed}/${laundered.of}) exit=${laundered.code}. The arms diverge, so record()'s state mapping is load-bearing and this test goes red if anyone ships the laundering. Previously that one-line edit published FREEZE_VERIFIED 12/12 with the suite reporting 12/12 KILLED.`
        : `NOT DISCRIMINATING — honestFails=${honestFails} (${honest.verdict} exit=${honest.code}), launderLies=${launderLies} (${laundered.verdict}). If the honest arm does not fail on a corrupt artifact, the oracle is broken; if the laundered arm does not lie, the mutation missed.`);
  }

  // §6 — the canonical runner must be byte-identical after eleven mutations. Isolation is a claim until measured;
  // a suite that silently edited the tree it audits would invalidate the very snapshot the reviewers cite.
  const canonicalAfter = sha(readFileSync(join(root, 'scripts/portfolio/phase8-freeze.mjs'), 'utf8'));
  rec('P8A-11', 'ISOLATION: the canonical runner is byte-identical after all mutations',
    canonicalAfter === canonicalBefore,
    canonicalAfter === canonicalBefore
      ? `scripts/portfolio/phase8-freeze.mjs sha256 ${canonicalBefore.slice(0, 16)}… unchanged across ${results.length} mutations — every patch landed in an mkdtemp copy`
      : `CANONICAL MUTATED: ${canonicalBefore.slice(0, 16)}… → ${canonicalAfter.slice(0, 16)}…`);

  const killed = results.filter((r) => r.classification === 'KILLED').length;
  console.log(`\nPHASE 8 ADVERSARIAL: ${killed}/${results.length} KILLED`);
  return { suite: 'MB-V2D-R3-A3-adversarial', total: results.length, killed, canonicalRunnerSha256: canonicalAfter, mutations: results };
}

if (process.argv[1]?.endsWith('phase8-adversarial-tests.mjs')) {
  const r = run();
  writeFileSync(join(root, 'PHASE8_ADVERSARIAL_RESULT.json'), JSON.stringify(r, null, 2) + '\n', 'utf8');
  process.exitCode = r.killed === r.total ? 0 : 1;
}
