#!/usr/bin/env node
// V2d — CLEAN-ROOM TEST on the THREE-ROOT contract (MB-V2D-R3-A).
//
// This is the BUNDLE ENTRYPOINT. It states mode='bundle' internally; no file in the tree can change that.
//
// What changed and why. The previous version used ONE directory: it extracted the container into `dest`, then
// extracted the source archive into `dest/sources`, then wrote `dest/_probe.mjs`. So the runbook polluted the
// very tree whose closed-set property it then verified, and Phase 8 could never reach 12/12. The fixes that
// were available at that point — allowlisting derived filenames, caching the closed-set claim before mutating,
// relaxing the contradiction — would each have deleted the invariant rather than satisfied it.
//
//   inputRoot   pristine container extraction ONLY. Validated as a closed set, sealed, never written again.
//   workRoot    sources, regenerated artifacts, the probe. Everything mutable lives here.
//   outputRoot  this transcript.
//
// inputRoot immutability is proven by fingerprint(before) === fingerprint(after), not by a read-only bit.
import { readFileSync, writeFileSync, rmSync, existsSync, readdirSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join, resolve, sep } from 'node:path';
import { createRoots, sealInput, disjointnessViolation, assertWithin, THREE_ROOT_CONTRACT } from './three-roots.mjs';
import { validateContext } from './validation-mode.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const ZIP = rel('MINDBOOST_V2D_CANDIDATE_BUNDLE.zip');
const steps = [];
const step = (name, ok, detail) => { steps.push({ name, ok, detail }); console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}\n      ${detail}`); };

// The number of steps this run MUST record. A run that throws half-way must not be able to report "6/6" —
// a denominator that shrinks to fit the steps that happened to survive is a false green, and an earlier
// version of this file did exactly that: process.exit() inside finally discards the in-flight exception, so a
// throw at step 7 printed "CLEAN ROOM: 6/6 steps passed" and exited 0 while silently skipping the inputRoot
// immutability proof — the one property the three-root design exists to establish.
// KEYED contract, not a bare count. A bare `steps.length < EXPECTED_STEPS` cannot tell "the wrong step ran
// twice while another was skipped" from a complete run, and it has a real boundary flaw: a crash at the LAST
// step makes the catch block's own synthetic entry bring the count back to exactly EXPECTED_STEPS, so
// `incomplete` reads false even though the final step never ran. Keying by name makes the missing step
// nameable instead of merely uncounted.
const EXPECTED_STEP_NAMES = Object.freeze([
  'three roots are physically disjoint',
  'container hash matches the out-of-band publication',
  'pristine extraction into an empty inputRoot',
  'inputRoot validates as a closed set (mode stated, not detected)',
  'inputRoot sealed',
  'bundle validator passes from inputRoot (cwd = workRoot)',
  'every source hash recomputed from the bundled bytes',
  'SHA256SUMS verified from inputRoot',
  'no bundled path escapes its extraction root',
  'inputRoot is byte-identical after the whole run',
]);
const EXPECTED_STEPS = EXPECTED_STEP_NAMES.length;
const R = createRoots('mindboost-cleanroom');
let seal = null;
let aborted = null;
let immutability = null;
console.log(`CLEAN ROOM (three roots)\n  inputRoot  ${R.inputRoot}\n  workRoot   ${R.workRoot}\n  outputRoot ${R.outputRoot}\n(the repository must never be read from inside them)\n`);

try {
  // 0. the roots themselves must be physically disjoint before anything is written
  const dv = disjointnessViolation({ inputRoot: R.inputRoot, workRoot: R.workRoot, outputRoot: R.outputRoot });
  step('three roots are physically disjoint', dv === null,
    dv ?? 'inputRoot / workRoot / outputRoot resolve to three distinct directories, none an ancestor or descendant of another after link resolution. Checked on REAL paths — a junction between roots defeats any string comparison.');

  // 1. container hash, published OUTSIDE the container
  const zipSha = createHash('sha256').update(readFileSync(ZIP)).digest('hex');
  const published = readFileSync(rel('BUNDLE_CONTAINER_HASH.txt'), 'utf8');
  step('container hash matches the out-of-band publication', published.includes(zipSha),
    `zip sha256 ${zipSha.slice(0, 24)}… · BUNDLE_CONTAINER_HASH.txt ${published.includes(zipSha) ? 'agrees' : 'DISAGREES'}. The hash file lives outside inputRoot by construction: a container cannot carry its own digest.`);

  // 2. pristine extraction into inputRoot — and NOTHING else ever goes here
  const tar = join(process.env.SYSTEMROOT || 'C:\\Windows', 'System32', 'tar.exe');
  execFileSync(tar, ['-xf', ZIP, '-C', R.inputRoot], { stdio: 'pipe', timeout: 240000 });
  const count = (function walk(d) { let n = 0; for (const e of readdirSync(d, { withFileTypes: true })) n += e.isDirectory() ? walk(join(d, e.name)) : 1; return n; })(R.inputRoot);
  step('pristine extraction into an empty inputRoot', count > 0,
    `${count} files extracted by a standard tool (store-only zip, no extra fields). inputRoot now holds the container and nothing else — no sources/, no probe, no reports.`);

  // 3. inputRoot is a CLOSED SET — checked BEFORE it is used, and before anything could pollute it
  const ctx = validateContext({ mode: 'bundle', inputRoot: R.inputRoot });
  step('inputRoot validates as a closed set (mode stated, not detected)', ctx.gate === 'PASS',
    ctx.diagnostics.map((d) => `${d.code}: ${d.message}`).join(' | ').slice(0, 400));

  // 4. seal: fingerprint + best-effort read-only. The fingerprint is the proof.
  seal = sealInput(R.inputRoot);
  step('inputRoot sealed', seal.before.fileCount === count,
    `fingerprint ${seal.before.digest.slice(0, 16)}… over ${seal.before.fileCount} files · read-only attribute applied to ${seal.attributeApplied} file(s), ${seal.attributeFailed} refused. ${seal.note}`);

  // 5. bundle validator, run FROM inputRoot, writing nothing into it
  let out = '', code = 0;
  try { out = execFileSync(process.execPath, [join(R.inputRoot, 'scripts/portfolio/validate-bundle.mjs'), R.inputRoot], { encoding: 'utf8', cwd: R.workRoot, stdio: ['ignore', 'pipe', 'pipe'], timeout: 240000 }); }
  catch (e) { code = e.status ?? 1; out = (e.stdout || '') + (e.stderr || ''); }
  step('bundle validator passes from inputRoot (cwd = workRoot)', code === 0 && /BUNDLE GATES: PASS/.test(out),
    out.trim().split('\n').filter((l) => /GATE|GATES/.test(l)).join(' · ').slice(0, 300) || out.slice(0, 200));

  // 6. the whole point: recompute every source hash. Sources extract to workRoot; the probe lives in workRoot.
  const probe = assertWithin(R.workRoot, '_probe.mjs', 'the probe script');
  const srcOut = assertWithin(R.workRoot, 'sources', 'the extracted sources');
  writeFileSync(probe, `
// pathToFileURL, not a raw path: ESM refuses a bare Windows absolute path ("C:\\…") with
// ERR_UNSUPPORTED_ESM_URL_SCHEME because it parses "c:" as a protocol.
import { extractArchive } from ${JSON.stringify(pathToFileURL(join(R.inputRoot, 'scripts/portfolio/build-source-archive.mjs')).href)};
import { readFileSync } from 'node:fs'; import { join } from 'node:path'; import { createHash } from 'node:crypto';
const IN = ${JSON.stringify(R.inputRoot.replace(/\\/g, '/'))};
const WORK = ${JSON.stringify(srcOut.replace(/\\/g, '/'))};
const idx = JSON.parse(readFileSync(join(IN, 'PORTFOLIO_SOURCE_INDEX.json'), 'utf8'));
// Read the archive FROM inputRoot, write the extraction INTO workRoot. inputRoot is never a write target.
extractArchive(readFileSync(join(IN, idx.archive.filename)), WORK);
let ok = 0; const bad = [];
for (const e of idx.entries) {
  const d = readFileSync(join(WORK, e.path));
  (createHash('sha256').update(d).digest('hex') === e.sha256 && d.length === e.byteLength) ? ok++ : bad.push(e.path);
}
console.log(JSON.stringify({ verified: ok, of: idx.entries.length, mismatched: bad.length, sample: bad.slice(0, 3) }));
`, 'utf8');
  let probeOut = '';
  try { probeOut = execFileSync(process.execPath, [probe], { encoding: 'utf8', cwd: R.workRoot, stdio: ['ignore', 'pipe', 'pipe'], timeout: 240000 }).trim(); }
  catch (e) { throw new Error(`probe failed: ${((e.stderr || '') + (e.stdout || '')).split('\n').filter(Boolean).slice(0, 3).join(' | ')}`); }
  const r = JSON.parse(probeOut);
  step('every source hash recomputed from the bundled bytes', r.verified === r.of && r.mismatched === 0,
    `verified ${r.verified}/${r.of} · mismatched ${r.mismatched} — recomputed WITHOUT the original repository. Archive read from inputRoot; extraction written to workRoot.`);

  // 7. SHA256SUMS re-verified from inputRoot
  const sums = readFileSync(join(R.inputRoot, 'BUNDLE_SHA256SUMS.txt'), 'utf8').split('\n').filter((l) => /^[0-9a-f]{64}\s{2}/.test(l));
  let sok = 0; const sbad = [];
  for (const l of sums) {
    const [h, ...r2] = l.split(/\s{2}/);
    const p = join(R.inputRoot, r2.join('  ').trim());
    if (!existsSync(p)) { sbad.push(r2.join('  ')); continue; }
    createHash('sha256').update(readFileSync(p)).digest('hex') === h ? sok++ : sbad.push(r2.join('  '));
  }
  step('SHA256SUMS verified from inputRoot', sbad.length === 0 && sok === sums.length,
    `${sok}/${sums.length} hashes re-verified${sbad.length ? ` · failed: ${sbad.slice(0, 3).join(', ')}` : ''}`);

  // 8. containment of every declared path
  const idx = JSON.parse(readFileSync(join(R.inputRoot, 'PORTFOLIO_SOURCE_INDEX.json'), 'utf8'));
  const escapes = (idx.pathIndex ?? []).filter((p) => { const a = resolve(R.workRoot, p); return a !== R.workRoot && !a.startsWith(R.workRoot + sep); });
  step('no bundled path escapes its extraction root', escapes.length === 0,
    `${(idx.pathIndex ?? []).length} paths checked · ${escapes.length} escapes`);

  // 9. THE contract: inputRoot never moved. Verified ONCE, here, and the result is captured for the
  // transcript — the `finally` must never re-run it (see the note there).
  const v = seal.verify();
  immutability = v;
  step('inputRoot is byte-identical after the whole run', v.immutable,
    v.immutable
      ? `fingerprint ${v.before.slice(0, 16)}… → ${v.after.slice(0, 16)}… over ${v.fileCountAfter} files. Sources, probe and reports all landed in workRoot/outputRoot. This is the property the single-root design could not have.`
      : `DRIFT: ${v.addedCount} added, ${v.removedCount} removed. added=${JSON.stringify(v.added)} removed=${JSON.stringify(v.removed)}`);
} catch (e) {
  // Record the abort as a FAILED step. Never let it vanish into finally's process.exit().
  aborted = { message: e?.message ?? String(e), stack: (e?.stack ?? '').split('\n').slice(0, 4).join(' | ') };
  step('run completed without aborting', false, `ABORTED at step ${steps.length + 1} of ${EXPECTED_STEPS}: ${aborted.message}`);
} finally {
  // Reconcile by NAME against the frozen contract: which contracted steps produced a result, and did anything
  // unexpected appear? The synthetic abort entry is deliberately not in EXPECTED_STEP_NAMES, so it can never
  // pad the count back up to "complete".
  const ran = new Set(steps.map((s) => s.name));
  const missingSteps = EXPECTED_STEP_NAMES.filter((n) => !ran.has(n));
  const unexpectedSteps = steps.map((s) => s.name).filter((n) => !EXPECTED_STEP_NAMES.includes(n) && n !== 'run completed without aborting');
  const failed = steps.filter((s) => !s.ok);
  const incomplete = missingSteps.length > 0 || unexpectedSteps.length > 0;
  if (incomplete && !aborted) {
    console.log(`FAIL  contract reconciliation\n      missing: ${JSON.stringify(missingSteps)} · unexpected: ${JSON.stringify(unexpectedSteps)}`);
  }
  const transcript = {
    artifact: 'V2d clean-room transcript (three-root)', schemaVersion: '2.0.0',
    contract: THREE_ROOT_CONTRACT,
    meaning: 'The bundle was extracted into a pristine inputRoot OUTSIDE the repository and verified there. All mutation went to workRoot; all reporting to outputRoot. Any step needing a file the bundle does not carry is a source-completeness failure, not a workaround.',
    mode: 'bundle — stated by this entrypoint, never detected from the tree',
    zip: 'MINDBOOST_V2D_CANDIDATE_BUNDLE.zip',
    // REUSE the value captured at step 9 — never re-invoke seal.verify() here. An earlier revision called it a
    // SECOND time, unguarded, inside this finally: a throw from it crashed the process before the transcript
    // was written, so the PREVIOUS run's report survived on disk and read as a clean 10/10 to anyone not
    // checking the exit code. A stale green is worse than a red.
    inputRootImmutable: immutability ?? { immutable: false, why: aborted ? 'run aborted before the immutability check' : 'immutability never verified' },
    // `of` is the CONTRACTED step count, not the number that happened to run. Reporting `steps.length` as the
    // denominator is what let a 6-step abort print "6/6".
    steps, passed: steps.length - failed.length, of: EXPECTED_STEPS, executed: steps.length,
    missingSteps, unexpectedSteps,
    aborted, complete: !incomplete && !aborted,
  };
  // outputRoot receives the report; the repo copy is the published one. Each write is guarded: a failure to
  // write the report must not crash the process, because that would leave the PREVIOUS report on disk looking
  // like a fresh pass.
  try { if (existsSync(R.outputRoot)) writeFileSync(join(R.outputRoot, 'CLEAN_ROOM_RESULT.json'), JSON.stringify(transcript, null, 2) + '\n', 'utf8'); } catch (e) { console.error(`could not write the outputRoot transcript: ${e.message}`); }
  try { writeFileSync(rel('PHASE5_CLEAN_ROOM_RESULT.json'), JSON.stringify(transcript, null, 2) + '\n', 'utf8'); } catch (e) { console.error(`could not write the published transcript: ${e.message}`); }
  try { R.dispose(); } catch { /* temp tree removal is best-effort */ }
  console.log(`\nCLEAN ROOM: ${steps.length - failed.length}/${EXPECTED_STEPS} steps passed${incomplete ? ` (INCOMPLETE — missing: ${missingSteps.join(', ') || 'none'}${unexpectedSteps.length ? `; unexpected: ${unexpectedSteps.join(', ')}` : ''})` : ''}`);
  if (aborted) console.log(`ABORTED: ${aborted.message}\n  ${aborted.stack}`);
  // exitCode, NOT process.exit(): calling exit() inside finally discards an in-flight exception, which is the
  // exact pattern this file's header forbids and which produced the original "6/6" false green.
  process.exitCode = failed.length || incomplete || aborted ? 1 : 0;
}
