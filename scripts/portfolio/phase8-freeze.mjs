#!/usr/bin/env node
// V2d PHASE 8 — FREEZE + CLEAN-ROOM REPRODUCTION on the THREE-ROOT contract (MB-V2D-R3-A2).
//
// What changed, and why each change was forced:
//
//  1. THREE ROOTS, shared with the clean room (three-roots.mjs — one implementation, never a second copy).
//     The old runner extracted into `dest`, then wrote `_verify.mjs` into it, extracted the sources into it,
//     and regenerated the model and view into it. The directory whose integrity it was verifying was the same
//     directory it was scribbling in.
//  2. EXPLICIT MODE. The old runner invoked validate-portfolio with no mode; the validator defaulted to repo;
//     repo mode requires a delivery manifest a bundle deliberately does not carry. That was the 11/12 failure:
//     nobody stated a wrong mode — a default answered a question nobody asked.
//  3. FIXED DENOMINATOR. The old verdict was `steps.filter(s => !s.ok).length === 0` over emitted steps, so an
//     exception at check 7 left a shorter list that could still read "all passing". The denominator is now the
//     frozen registry: twelve specifications, twelve results, always.
//  4. NO process.exit() IN finally — it discards an in-flight exception. main() + top-level catch + exitCode.
import { readFileSync, writeFileSync, existsSync, readdirSync, mkdirSync, rmSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';
import { PHASE8_CHECKS, EXPECTED_CHECK_COUNT, contractViolations, reconcile } from './phase8-checks.mjs';
import { createRoots, sealInput, disjointnessViolation, assertWithin } from './three-roots.mjs';
import { beginRun, currentSnapshotId } from './atomic-report.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const REPO = root;
const sha = (p) => (existsSync(rel(p)) ? createHash('sha256').update(readFileSync(rel(p))).digest('hex') : 'ABSENT');
const shaAbs = (p) => (existsSync(p) ? createHash('sha256').update(readFileSync(p)).digest('hex') : 'ABSENT');
// A gate with no timeout is a gate that can hang forever, which in CI is indistinguishable from one that is
// merely slow — and a hung freeze reports nothing at all rather than reporting a failure.
const CHILD_TIMEOUT_MS = 240000;
const run = (exe, args, opts = {}) => { try { return { code: 0, out: execFileSync(exe, args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], timeout: CHILD_TIMEOUT_MS, ...opts }) }; } catch (e) { return { code: e.status ?? 1, out: (e.stdout || '') + (e.stderr || '') + (e.killed ? `\n[child killed after ${CHILD_TIMEOUT_MS}ms timeout]` : '') }; } };
const TAR = join(process.env.SYSTEMROOT || 'C:\\Windows', 'System32', 'tar.exe');
const ZIP = 'MINDBOOST_V2D_CANDIDATE_BUNDLE.zip';

// Every check starts NOT_STARTED. A check that is never reached KEEPS that state and still counts.
const results = {};
for (const c of PHASE8_CHECKS) results[c.checkId] = { state: 'NOT_STARTED', detail: null, failureCode: null };

const record = (checkId, ok, detail, failureCode = null) => {
  const spec = PHASE8_CHECKS.find((c) => c.checkId === checkId);
  results[checkId] = { state: ok ? 'PASS' : 'FAIL', detail, failureCode: ok ? null : (failureCode ?? spec.expectedFailureCode) };
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${checkId} ${spec.name}\n      ${detail}`);
};

async function main() {
  const cv = contractViolations();
  if (cv.length) { for (const v of cv) console.error(`CONTRACT: ${v}`); return { infra: true, reason: 'check registry is invalid' }; }

  const manifest = JSON.parse(readFileSync(rel('BUNDLE_MANIFEST.json'), 'utf8'));
  const frozen = {};
  for (const a of manifest.artifacts) frozen[a.path] = sha(a.path);
  frozen[ZIP] = sha(ZIP);
  const freezeId = createHash('sha256').update(JSON.stringify(frozen)).digest('hex');
  console.log(`PHASE 8 FREEZE (three-root)\n  ${manifest.artifacts.length} artifacts · FREEZE ID ${freezeId}\n  contract: ${EXPECTED_CHECK_COUNT} checks, denominator fixed before execution\n`);

  // P8-01
  const drift = manifest.artifacts.filter((a) => sha(a.path) !== a.sha256);
  record('P8-01', drift.length === 0, drift.length
    ? `${drift.length} artifact(s) drifted since the manifest was written: ${drift.slice(0, 3).map((d) => d.path).join(', ')}`
    : `all ${manifest.artifacts.length} artifacts match the hashes the bundle manifest recorded`);

  // P8-02
  const V2C = 'MINDBOOST_PORTFOLIO_V2C_HANDOFF_8795736d0b1afbb6da1b319d16b862536850ee39f711d6236f4900238dc548fd.zip';
  const v2cSha = sha(V2C);
  record('P8-02', v2cSha === '0f8e2f928bd50f9df6381a1e17a26f6e228febfd56a817c67a5158c2f1c990b8',
    `${v2cSha.slice(0, 24)}… — the historical snapshot that recovered the poisoned CSV in Phase 4 (MB-PORTFOLIO-0018)`);

  // P8-03
  const led = run(process.execPath, [rel('scripts/validate-audit-ledger.mjs')], { cwd: root });
  record('P8-03', led.code === 0 && /LEDGER OK/.test(led.out), led.out.trim().split('\n').pop());

  // P8-04
  const zipSha = sha(ZIP);
  const pub = readFileSync(rel('BUNDLE_CONTAINER_HASH.txt'), 'utf8');
  const zipEntries = run(TAR, ['-tf', rel(ZIP)]);
  const containsOwnHash = zipEntries.out.includes('BUNDLE_CONTAINER_HASH.txt');
  record('P8-04', pub.includes(zipSha) && !containsOwnHash,
    `${zipSha.slice(0, 24)}… in BUNDLE_CONTAINER_HASH.txt; the file is NOT inside the zip (${containsOwnHash ? 'IT IS — a container cannot contain its own hash' : 'correct'})`);

  const R = createRoots('mindboost-freeze');
  let seal = null;
  try {
    const dv = disjointnessViolation({ inputRoot: R.inputRoot, workRoot: R.workRoot, outputRoot: R.outputRoot });
    if (dv) throw new Error(`compromised root layout: ${dv}`);
    console.log(`  inputRoot  ${R.inputRoot}\n  workRoot   ${R.workRoot}\n  outputRoot ${R.outputRoot}\n`);

    // P8-05 — pristine extraction, closed-set validation under an EXPLICIT mode, then seal.
    run(TAR, ['-xf', rel(ZIP), '-C', R.inputRoot]);
    const n = (function w(d) { let c = 0; for (const e of readdirSync(d, { withFileTypes: true })) c += e.isDirectory() ? w(join(d, e.name)) : 1; return c; })(R.inputRoot);
    const { validateContext } = await import(pathToFileURL(join(R.inputRoot, 'scripts/portfolio/validation-mode.mjs')).href);
    const ctx = validateContext({ mode: 'bundle', inputRoot: R.inputRoot });
    seal = sealInput(R.inputRoot);
    record('P8-05', ctx.gate === 'PASS' && n > 0 && seal.before.fileCount === n,
      `${n} files extracted; closed-set gate ${ctx.gate} (${ctx.diagnostics.map((d) => d.code).join(',')}); sealed at fingerprint ${seal.before.digest.slice(0, 16)}… over ${seal.before.fileCount} files. Mode 'bundle' is STATED by this entrypoint — never detected from the tree.`,
      ctx.gate === 'PASS' ? null : 'INPUT_ROOT_NOT_CLOSED');

    // P8-06
    const bv = run(process.execPath, [join(R.inputRoot, 'scripts/portfolio/validate-bundle.mjs'), R.inputRoot], { cwd: R.workRoot });
    record('P8-06', bv.code === 0 && /BUNDLE GATES: PASS/.test(bv.out),
      bv.out.trim().split('\n').filter((l) => /GATES:/.test(l)).join(' · ').slice(0, 240));

    // P8-07 — archive READ from inputRoot, sources extracted INTO workRoot.
    const probe = assertWithin(R.workRoot, '_verify.mjs', 'the verification probe');
    const srcOut = assertWithin(R.workRoot, 'sources', 'the extracted sources');
    writeFileSync(probe, `
import { extractArchive } from ${JSON.stringify(pathToFileURL(join(R.inputRoot, 'scripts/portfolio/build-source-archive.mjs')).href)};
import { readFileSync } from 'node:fs'; import { join } from 'node:path'; import { createHash } from 'node:crypto';
const IN = ${JSON.stringify(R.inputRoot.replace(/\\/g, '/'))};
const WORK = ${JSON.stringify(srcOut.replace(/\\/g, '/'))};
const idx = JSON.parse(readFileSync(join(IN, 'PORTFOLIO_SOURCE_INDEX.json'), 'utf8'));
extractArchive(readFileSync(join(IN, idx.archive.filename)), WORK);
let ok = 0; const bad = [];
for (const e of idx.entries) { const d = readFileSync(join(WORK, e.path)); (createHash('sha256').update(d).digest('hex') === e.sha256 && d.length === e.byteLength) ? ok++ : bad.push(e.path); }
console.log(JSON.stringify({ verified: ok, of: idx.entries.length, mismatched: bad.length }));
`, 'utf8');
    const vr = run(process.execPath, [probe], { cwd: R.workRoot });
    const parsed = (() => { try { return JSON.parse(vr.out.trim()); } catch { return null; } })();
    record('P8-07', !!parsed && parsed.verified === parsed.of && parsed.mismatched === 0,
      parsed ? `verified ${parsed.verified}/${parsed.of} · mismatched ${parsed.mismatched} — WITHOUT the original repository. Archive read from inputRoot, extraction written to workRoot.` : `probe failed: ${vr.out.slice(0, 200)}`);

    // P8-08 — regenerate INSIDE workRoot.
    //
    // The tools resolve every path from their own location (`import.meta.url` → repo root), so running them out
    // of inputRoot made them try to WRITE their outputs into inputRoot. The seal refused with EPERM — the
    // read-only attribute blocking a real write attempt, which is the §7 property demonstrating itself rather
    // than being asserted. The lesson is architectural, not a permission to unseal: workRoot must BE the working
    // copy, so the tools resolve inputs and outputs entirely within it and inputRoot is never a write target.
    //
    // What gets copied is inputs only — tooling and authorities. The shipped model and view are then DELETED
    // from workRoot before regeneration, so a regeneration that silently produced nothing leaves them ABSENT and
    // P8-09 fails honestly. Without that delete, the copied shipped bytes would still be sitting there and the
    // comparison would degrade into shipped-vs-shipped: guaranteed to pass, proving nothing.
    const copyTree = (from, to) => {
      for (const e of readdirSync(from, { withFileTypes: true })) {
        const s = join(from, e.name), d = join(to, e.name);
        if (e.isDirectory()) { mkdirSync(d, { recursive: true }); copyTree(s, d); }
        else if (e.isFile()) { mkdirSync(dirname(d), { recursive: true }); writeFileSync(d, readFileSync(s)); }
      }
    };
    copyTree(R.inputRoot, R.workRoot);
    // Every artifact the regeneration is supposed to PRODUCE is purged first — including the intermediate
    // PORTFOLIO_CODE_OBSERVATIONS.json. observe-code.mjs happens to overwrite it unconditionally today, so
    // leaving it would be harmless *by accident*; purging it pins the invariant instead of relying on a
    // property of another file that a future change could quietly remove.
    const REGENERATED = ['PORTFOLIO_CODE_OBSERVATIONS.json', 'PORTFOLIO_DERIVED_MODEL.json', 'MINDBOOST_EXERCISE_PORTFOLIO.md'];
    const purged = [];
    for (const f of REGENERATED) {
      const p = assertWithin(R.workRoot, f, 'a regeneration target');
      if (existsSync(p)) { rmSync(p); purged.push(f); }
    }
    // Sources: extracted from the bundled archive into workRoot, at the repo-relative layout the tools expect.
    const { extractArchive } = await import(pathToFileURL(join(R.inputRoot, 'scripts/portfolio/build-source-archive.mjs')).href);
    const srcIdx = JSON.parse(readFileSync(join(R.inputRoot, 'PORTFOLIO_SOURCE_INDEX.json'), 'utf8'));
    extractArchive(readFileSync(join(R.inputRoot, srcIdx.archive.filename)), R.workRoot);

    const toolWork = join(R.workRoot, 'scripts/portfolio');
    const obs = run(process.execPath, [join(toolWork, 'observe-code.mjs')], { cwd: R.workRoot });
    const der = run(process.execPath, [join(toolWork, 'derive-portfolio-model.mjs')], { cwd: R.workRoot });
    const bld = run(process.execPath, [join(toolWork, 'build-portfolio.mjs')], { cwd: R.workRoot });
    const regenOk = obs.code === 0 && der.code === 0 && bld.code === 0;
    record('P8-08', regenOk, regenOk
      ? `observe-code → derive-portfolio-model → build-portfolio all exited 0 inside workRoot. Shipped outputs purged first (${purged.join(', ')}), so nothing stale could masquerade as regenerated; sources came from the bundled archive, never the repository.`
      : `observe=${obs.code} derive=${der.code} build=${bld.code} · ${(obs.out + der.out + bld.out).split('\n').filter((l) => /Error|error|Cannot|EPERM/.test(l)).slice(0, 2).join(' | ')}`);

    // P8-09 — generated(workRoot) vs shipped(inputRoot). ACROSS roots, never shipped-vs-shipped.
    const genModel = shaAbs(join(R.workRoot, 'PORTFOLIO_DERIVED_MODEL.json'));
    const genView = shaAbs(join(R.workRoot, 'MINDBOOST_EXERCISE_PORTFOLIO.md'));
    const shipModel = shaAbs(join(R.inputRoot, 'PORTFOLIO_DERIVED_MODEL.json'));
    const shipView = shaAbs(join(R.inputRoot, 'MINDBOOST_EXERCISE_PORTFOLIO.md'));
    record('P8-09', regenOk && genModel !== 'ABSENT' && genModel === shipModel && genView === shipView,
      `model generated(work) ${genModel.slice(0, 12)}… vs shipped(input) ${shipModel.slice(0, 12)}… · view generated(work) ${genView.slice(0, 12)}… vs shipped(input) ${shipView.slice(0, 12)}…. Compared across roots: the shipped copy is never copied into workRoot to stand in for a regenerated one.`);

    // P8-10 — THE fix: explicit bundle mode against the pristine inputRoot.
    const pv = run(process.execPath, [join(R.inputRoot, 'scripts/portfolio/validate-portfolio.mjs'), '--mode=bundle'], { cwd: R.workRoot });
    record('P8-10', /BLOCKING GATES: PASS/.test(pv.out),
      `${pv.out.trim().split('\n').filter((l) => /BLOCKING/.test(l)).join(' · ') || pv.out.slice(0, 200)} — invoked with --mode=bundle. With no mode the validator now exits 2 (configuration error), and previously it silently defaulted to repo, which is what produced the 11/12.`);

    // P8-11
    const lv = run(process.execPath, [join(R.inputRoot, 'scripts/validate-audit-ledger.mjs')], { cwd: R.inputRoot });
    record('P8-11', lv.code === 0 && /LEDGER OK/.test(lv.out), lv.out.trim().split('\n').pop());

    // P8-12 — repo drift AND inputRoot immutability. Both, or neither is proven.
    const post = {};
    for (const a of manifest.artifacts) post[a.path] = sha(a.path);
    post[ZIP] = sha(ZIP);
    const moved = Object.keys(frozen).filter((p) => frozen[p] !== post[p]);
    const iv = seal.verify();
    record('P8-12', moved.length === 0 && iv.immutable,
      `repo: ${moved.length ? `${moved.length} moved: ${moved.slice(0, 3).join(', ')}` : `all ${Object.keys(frozen).length} frozen artifacts byte-identical`} · inputRoot: ${iv.immutable ? `fingerprint ${iv.before.slice(0, 16)}… unchanged over ${iv.fileCountAfter} files (0 added, 0 removed)` : `DRIFT — ${iv.addedCount} added / ${iv.removedCount} removed: ${JSON.stringify(iv.added.slice(0, 3))}`}`);

    return { R, seal, freezeId, frozen, post, moved, zipSha, v2cSha, manifest, inputImmutable: iv };
  } finally {
    // Cleanup only. No exit, no verdict — an exception must survive this block.
    try { R.dispose(); } catch { /* the temp tree is best-effort */ }
  }
}

// §5 — the run is opened BEFORE the first check so an interrupted execution is detectable on disk. Without this,
// a hard kill leaves the previous green report intact and unattributable to any run.
const run8 = beginRun(rel('PHASE8_FREEZE_RESULT.json'), { snapshotId: currentSnapshotId(REPO), tool: 'phase8-freeze' });

let outcome = null, fatal = null;
try { outcome = await main(); }
catch (e) { fatal = { message: e?.message ?? String(e), stack: (e?.stack ?? '').split('\n').slice(0, 5).join(' | ') }; }

// Any check still NOT_STARTED after a fatal is ABORTED — it is reported, never dropped.
if (fatal) for (const [id, r] of Object.entries(results)) if (r.state === 'NOT_STARTED') results[id] = { state: 'ABORTED', detail: `not reached: ${fatal.message}`, failureCode: 'ABORTED' };

const rec = reconcile(results);
const verified = rec.reconciled && rec.passed === EXPECTED_CHECK_COUNT && !fatal && !outcome?.infra;

run8.publish({
  artifact: 'V2d Phase 8 freeze + clean-room reproduction (three-root)',
  contract: { expectedCheckCount: EXPECTED_CHECK_COUNT, denominatorRule: 'the denominator is the frozen registry, never the emitted results; a check that never ran is ABORTED/NOT_STARTED and still counts against 12' },
  checks: PHASE8_CHECKS, results, reconciliation: rec, fatal,
  freezeId: outcome?.freezeId ?? null, frozenArtifacts: outcome?.frozen ?? null,
  postFreeze: outcome?.post ?? null, postFreezeDrift: outcome?.moved ?? null,
  inputRootImmutable: outcome?.inputImmutable ?? null,
  containerSha256: outcome?.zipSha ?? null, v2cSha256: outcome?.v2cSha ?? null,
  passed: rec.passed, of: EXPECTED_CHECK_COUNT,
  verdict: verified ? 'FREEZE_VERIFIED' : 'FREEZE_FAILED',
}, { status: verified ? 'completed' : 'failed' });

console.log(`\nPHASE 8: ${verified ? 'FREEZE_VERIFIED' : 'FREEZE_FAILED'} (${rec.passed}/${EXPECTED_CHECK_COUNT})`);
console.log(`  states: ${JSON.stringify(rec.counts)}${rec.reconciled ? '' : ' · RECONCILIATION FAILED: ' + rec.violations.join('; ')}`);
for (const [id, r] of Object.entries(results)) if (r.state !== 'PASS') console.log(`  ! ${id} ${r.state}${r.failureCode ? ` [${r.failureCode}]` : ''}: ${String(r.detail).slice(0, 160)}`);
if (fatal) console.log(`  FATAL: ${fatal.message}\n    ${fatal.stack}`);
process.exitCode = verified ? 0 : 1;
