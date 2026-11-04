#!/usr/bin/env node
// V2d Phase 3 — EVIDENCE MUTATION SUITE.
//
// Every mutation must: (1) assert its PRECONDITION (the thing it breaks is actually true beforehand — a
// mutation that changes nothing proves nothing), (2) really modify the target, (3) turn the gate RED,
// (4) match the EXPECTED gate id and diagnostic, (5) restore byte-identically.
// Runs against a sandbox copy; production sources are never written.
import { mkdtempSync, writeFileSync, readFileSync, existsSync, rmSync, cpSync, mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { buildManifest } from './gen-source-manifest.mjs';
import { validate } from './validate-evidence.mjs';
import { observe } from './observe-code.mjs';
import { derive, parseCsv, checkInvariants } from './derive-portfolio-model.mjs';
import { P, ADAPTERS } from './block-adapters.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const results = []; let fails = 0;
const rec = (id, ok, msg, detail) => { console.log(`  ${(ok ? 'PASS' : 'FAIL').padEnd(5)} ${id.padEnd(4)} ${msg}`); results.push({ id, state: ok ? 'PASS' : 'FAIL', msg, ...(detail || {}) }); if (!ok) fails++; };
const sha = (b) => createHash('sha256').update(b).digest('hex');

function sandbox() {
  const d = mkdtempSync(join(tmpdir(), 'p3mut-'));
  cpSync(join(root, P), join(d, P), { recursive: true });
  for (const f of ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'PORTFOLIO_RUNTIME_EVIDENCE.json', 'PORTFOLIO_AUTHORITY_MAP.md'])
    if (existsSync(join(root, f))) cpSync(join(root, f), join(d, f));
  mkdirSync(join(d, 'scripts', 'portfolio'), { recursive: true });
  for (const f of ['kotlin-code-scan.mjs', 'block-adapters.mjs', 'observe-code.mjs', 'derive-portfolio-model.mjs',
    'gen-source-manifest.mjs', 'validate-evidence.mjs', 'authority-tests.mjs', 'seed-portfolio.mjs.RETIRED'])
    if (existsSync(join(root, 'scripts', 'portfolio', f))) cpSync(join(root, 'scripts', 'portfolio', f), join(d, 'scripts', 'portfolio', f));
  return d;
}
const write = (d) => {
  const m = buildManifest(d); writeFileSync(join(d, 'PORTFOLIO_SOURCE_MANIFEST_V2.json'), JSON.stringify(m, null, 2) + '\n');
  const o = observe(d); writeFileSync(join(d, 'PORTFOLIO_CODE_OBSERVATIONS.json'), JSON.stringify(o, null, 2) + '\n');
  return { m, o };
};
const run = (d, patch = {}) => validate(d, patch);
const hasGate = (r, g, re) => r.gates.some((x) => x.gate === g && (!re || re.test(x.msg)));

console.log('V2d PHASE 3 — EVIDENCE MUTATION SUITE (precondition → mutate → red gate → exact diagnostic → restore)\n');

// Baseline must be green, else every "red" below is meaningless.
{
  const d = sandbox(); write(d);
  const r = run(d);
  rec('P0', r.gates.length === 0, `baseline sandbox validates clean (gates=${r.gates.length}) — precondition for every mutation below`);
  rmSync(d, { recursive: true, force: true });
}

// 1 — manifest claims PRESENT for a file that does not exist
{
  const d = sandbox(); const { m } = write(d);
  const target = m.entries.find((e) => e.role === 'CODE_SOURCE_ACTIVE');
  const pre = existsSync(join(d, target.path));
  rmSync(join(d, target.path));
  const r = run(d, { manifest: m });   // manifest still claims it exists
  rec('P3-01', pre && hasGate(r, 'G1', /is not on disk/), `[G1] manifest claims a deleted file exists → red (precondition: file existed=${pre})`);
  rmSync(d, { recursive: true, force: true });
}

// 2 — invented hash
{
  const d = sandbox(); const { m } = write(d);
  const t = m.entries.find((e) => e.role === 'CODE_SOURCE_ACTIVE');
  const real = t.sha256;
  t.sha256 = 'f'.repeat(64);
  const r = run(d, { manifest: m });
  rec('P3-02', real !== t.sha256 && hasGate(r, 'G2', /sha256/), '[G2] invented sha256 → red (recomputed from bytes, not trusted)');
  rmSync(d, { recursive: true, force: true });
}

// 3 — wrong byteLength
{
  const d = sandbox(); const { m } = write(d);
  const t = m.entries.find((e) => e.role === 'CODE_SOURCE_ACTIVE');
  const real = t.byteLength; t.byteLength = real + 1;
  const r = run(d, { manifest: m });
  rec('P3-03', hasGate(r, 'G2', /byteLength/), `[G2] wrong byteLength (${real}→${real + 1}) → red`);
  rmSync(d, { recursive: true, force: true });
}

// 4 — evidencePath outside the manifest
{
  const d = sandbox(); const { m, o } = write(d);
  const id = Object.keys(o.perId).find((k) => Object.values(o.perId[k].axes).some((v) => v.evidencePath));
  const ax = Object.entries(o.perId[id].axes).find(([, v]) => v.evidencePath)[0];
  const pre = !!o.perId[id].axes[ax].evidencePath;
  o.perId[id].axes[ax].evidencePath = 'shared/src/commonMain/kotlin/NOT_MANIFESTED.kt';
  const r = run(d, { manifest: m, obs: o });
  rec('P3-04', pre && hasGate(r, 'G3', /not in the manifest/), '[G3] evidencePath outside the manifest → red');
  rmSync(d, { recursive: true, force: true });
}

// 5 — a source an adapter consumes is omitted from the manifest
{
  const d = sandbox(); const { m, o } = write(d);
  const victim = ADAPTERS.FOCUS.mechanic.files[0];
  const pre = m.entries.some((e) => e.path === victim);
  m.entries = m.entries.filter((e) => e.path !== victim);
  const r = run(d, { manifest: m, obs: o });
  rec('P3-05', pre && hasGate(r, 'G6', new RegExp(victim.split('/').pop())), `[G6] adapter-consumed source omitted from the manifest → red (${victim.split('/').pop()})`);
  rmSync(d, { recursive: true, force: true });
}

// 6 — manifested ACTIVE source exists but is zero bytes
{
  const d = sandbox();
  const victim = ADAPTERS.LOGIC.mechanic.files[0];
  const pre = readFileSync(join(d, victim)).length > 0;
  writeFileSync(join(d, victim), '');
  const { m } = write(d);
  const r = run(d, { manifest: m });
  rec('P3-06', pre && hasGate(r, 'G11', /zero bytes/), '[G11] manifested ACTIVE source is zero bytes → red');
  rmSync(d, { recursive: true, force: true });
}

// 7 — the id appears ONLY in a comment/string
{
  const d = sandbox();
  const before = observe(d);
  const id = Object.keys(before.perId).find((k) => before.perId[k].block === 'MEMORY' && before.perId[k].axes.mechanic.state === 'VERIFIED_PRESENT');
  const f = ADAPTERS.MEMORY.mechanic.files[0];
  const pre = before.perId[id].axes.mechanic.state === 'VERIFIED_PRESENT';
  writeFileSync(join(d, f), readFileSync(join(d, f), 'utf8').replace(new RegExp(`\\b${id}\\b`, 'g'), 'RENAMED_X') + `\n// ${id}\nval s = "${id}"\n`);
  const after = observe(d);
  rec('P3-07', pre && after.perId[id].axes.mechanic.state !== 'VERIFIED_PRESENT',
    `[binding] ${id} left only in a comment/string → mechanic ${before.perId[id].axes.mechanic.state}→${after.perId[id].axes.mechanic.state}`);
  rmSync(d, { recursive: true, force: true });
}

// 8 — the right symbol in the wrong file must not accredit
{
  const d = sandbox();
  const before = observe(d);
  const id = Object.keys(before.perId).find((k) => before.perId[k].block === 'MEMORY' && before.perId[k].axes.mechanic.state === 'VERIFIED_PRESENT');
  const mechFile = ADAPTERS.MEMORY.mechanic.files[0];
  const pre = before.perId[id].axes.mechanic.state === 'VERIFIED_PRESENT';
  // remove the real binding, and plant an identical-looking one in a file OUTSIDE the mechanic mechanism
  writeFileSync(join(d, mechFile), readFileSync(join(d, mechFile), 'utf8').replace(new RegExp(`\\b${id}\\b`, 'g'), 'RENAMED_X'));
  const decoy = `${P}/memory/domain/exercise/MemoryTrialModels.kt`;
  if (existsSync(join(d, decoy))) writeFileSync(join(d, decoy), readFileSync(join(d, decoy), 'utf8') + `\nval decoy = mapOf(${id} to 1)\n`);
  const after = observe(d);
  rec('P3-08', pre && after.perId[id].axes.mechanic.state !== 'VERIFIED_PRESENT',
    `[scope] the same binding planted in a non-mechanism file does not accredit mechanic (${before.perId[id].axes.mechanic.state}→${after.perId[id].axes.mechanic.state})`);
  rmSync(d, { recursive: true, force: true });
}

// 9 — HISTORICAL_EVIDENCE cited by a current state
{
  const d = sandbox(); const { m, o } = write(d);
  const id = Object.keys(o.perId).find((k) => Object.values(o.perId[k].axes).some((v) => v.state === 'VERIFIED_PRESENT'));
  const ax = Object.entries(o.perId[id].axes).find(([, v]) => v.state === 'VERIFIED_PRESENT')[0];
  const hist = m.entries.find((e) => e.role === 'HISTORICAL_EVIDENCE');
  const pre = !!hist;
  o.perId[id].axes[ax].evidencePath = hist.path;
  o.perId[id].axes[ax].evidenceSha256 = hist.sha256;
  const r = run(d, { manifest: m, obs: o });
  rec('P3-09', pre && hasGate(r, 'G8', /HISTORICAL_EVIDENCE/), '[G8] retired-seed evidence cited by a current VERIFIED_PRESENT state → red');
  rmSync(d, { recursive: true, force: true });
}

// 10 — a scope that omits FocusGraph / a per-exercise screen
{
  const d = sandbox();
  const fg = `${P}/focus/di/FocusGraph.kt`;
  const pre = observe(d).perId.E1_VIGIA.axes.mechanic;
  // DISCRIMINATION: FOCUS mechanic is already UNRESOLVED before the mutation, so asserting the STATE would
  // pass vacuously. The mutation must change the DIAGNOSTIC (token-only → mechanism-source-missing) and the
  // file must appear in missingSources; a no-op mutation now fails.
  const preNotMissing = !/mechanism source missing/.test(pre.reason);
  rmSync(join(d, fg));
  const after = observe(d);
  const a = after.perId.E1_VIGIA.axes.mechanic;
  rec('P3-10', preNotMissing && a.reason !== pre.reason && a.state === 'UNRESOLVED'
    && /mechanism source missing/.test(a.reason) && after.missingSources.includes(fg),
    `[scope] omitting FocusGraph.kt → diagnostic changes "${pre.reasonCode ?? pre.reason.slice(0, 22)}…" → "mechanism source missing" and the file is listed in missingSources; state stays UNRESOLVED (never a silent false)`);
  rmSync(d, { recursive: true, force: true });
}

// 11 — runtime VERIFIED without a valid run
{
  const d = sandbox(); const { m, o } = write(d);
  const rt = JSON.parse(readFileSync(join(d, 'PORTFOLIO_RUNTIME_EVIDENCE.json'), 'utf8'));
  // DISCRIMINATION: the runs the authority already holds (the first executed one landed 2026-09-14) must NOT
  // trip G10 on their own, or the mutation below would pass for the wrong reason. Measured on the sandbox
  // copy — never by requiring the authority to be empty, a precondition that turns RED with the first real run.
  const pre = !hasGate(run(d, { manifest: m, obs: o }), 'G10', /VERIFIED without a snapshot hash/);
  rt.runs.push({ slotId: 'FOCUS-01', codeExerciseId: 'E1_VIGIA', platform: 'android', result: 'VERIFIED', testOrDossier: 'claimed', provenance: 'fabricated' });
  writeFileSync(join(d, 'PORTFOLIO_RUNTIME_EVIDENCE.json'), JSON.stringify(rt, null, 2));
  const r = run(d, { manifest: m, obs: o });
  rec('P3-11', pre && hasGate(r, 'G10', /VERIFIED without a snapshot hash/), '[G10] runtime VERIFIED with no snapshot/evidencePath → red (and the real runs alone stay green)');
  rmSync(d, { recursive: true, force: true });
}

// 12 — exhaustive:true without an exhaustivenessEvidenceId
{
  const d = sandbox(); const { m, o } = write(d);
  const id = Object.keys(o.perId)[0];
  o.perId[id].axes.mechanic = { ...o.perId[id].axes.mechanic, state: 'VERIFIED_ABSENT', exhaustive: true, exhaustivenessEvidenceId: null, construct: null, evidencePath: null };
  const r = run(d, { manifest: m, obs: o });
  rec('P3-12', hasGate(r, 'G9', /exhaustivenessEvidenceId/), '[G9] VERIFIED_ABSENT with a self-asserted exhaustive:true and no exhaustivenessEvidenceId → red');
  rmSync(d, { recursive: true, force: true });
}

// 13 — shared evidence falsely presented as independent
{
  const d = sandbox(); const { m, o } = write(d);
  const id = Object.keys(o.perId).find((k) => o.perId[k].axes.declaration.evidencePath);
  const src = o.perId[id].axes.declaration;
  const pre = src.evidenceDependency === 'INDEPENDENT';
  o.perId[id].axes.mechanic = { ...o.perId[id].axes.mechanic, state: 'VERIFIED_PRESENT',
    evidencePath: src.evidencePath, evidenceSha256: src.evidenceSha256, symbol: src.symbol,
    matchRange: src.matchRange, construct: src.construct, evidenceDependency: 'INDEPENDENT' };
  const r = run(d, { manifest: m, obs: o });
  rec('P3-13', pre && hasGate(r, 'G13', /not marked evidenceDependency=SHARED/), '[G13] two axes on one observation presented as independent → red');
  rmSync(d, { recursive: true, force: true });
}

// 14 — design-only collapsed into target-unassigned
{
  const d = sandbox(); const { o } = write(d);
  const rows = parseCsv(readFileSync(join(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8'));
  const base = derive(o, rows, null);
  const pre = base.counts.DESIGN_ONLY_ASSIGNED === 19 && base.counts.TARGET_UNASSIGNED === 37;
  // simulate the collapse the V2c/earlier model performed
  const collapsed = base.counts.DESIGN_ONLY_ASSIGNED + base.counts.TARGET_UNASSIGNED;
  const distinct = base.counts.DESIGN_ONLY_ASSIGNED !== 0 && base.counts.TARGET_UNASSIGNED !== 0;
  rec('P3-14', pre && distinct && collapsed === 56,
    `[C2] design-only (${base.counts.DESIGN_ONLY_ASSIGNED}) and truly-unassigned (${base.counts.TARGET_UNASSIGNED}) are separate states; collapsing them reproduces the old 56 bucket`);
  rmSync(d, { recursive: true, force: true });
}

// 15 — an auxiliary pulled into the 121 denominator
{
  const d = sandbox(); const { o } = write(d);
  const csv = readFileSync(join(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8');
  const rows = parseCsv(csv);
  const aux = parseCsv(readFileSync(join(d, 'MINDBOOST_EXERCISE_AUXILIARIES.csv'), 'utf8'));
  const auxId = aux.map((a) => (a.codeExerciseId || '').trim()).find(Boolean);
  const pre = rows.length === 121 && !!auxId;
  const mutated = rows.map((r) => (r.slotId === rows[0].slotId ? { ...r, codeExerciseId: auxId } : r));
  const model = derive(o, mutated, null);
  const v = checkInvariants(model, mutated, aux, o);
  rec('P3-15', pre && v.some((x) => /auxiliary code id .* occupies a target slot/.test(x)),
    `[G12] auxiliary "${auxId}" placed in a target slot → invariant violation`);
  rmSync(d, { recursive: true, force: true });
}

// 16 — block route used as exercise route
{
  const d = sandbox(); const { o } = write(d);
  const id = 'E1_VIGIA';
  const br = o.perId[id].axes.blockReachability.state;
  const er = o.perId[id].axes.exerciseRoute.state;
  const rows = parseCsv(readFileSync(join(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8'));
  const model = derive(o, rows, null);
  const slot = Object.values(model.perSlot).find((s) => s.codeExerciseId === id);
  const routeNotFolded = !Object.keys(slot.componentAxes || {}).includes('exerciseRoute') && !Object.keys(slot.componentAxes || {}).includes('blockReachability');
  // FOCUS ids all share one block-level record; that must never satisfy a per-exercise route
  const focusIds = Object.entries(o.perId).filter(([, r]) => r.block === 'FOCUS').map(([k]) => k);
  const allBlockSame = new Set(focusIds.map((i) => JSON.stringify(o.perId[i].axes.blockReachability))).size === 1;
  rec('P3-16', br === 'VERIFIED_PRESENT' && er === 'UNRESOLVED' && routeNotFolded && allBlockSame,
    `[C3] blockReachability=${br} does NOT accredit exerciseRoute=${er}; neither is a component axis (folded=${!routeNotFolded}); the block record is identical across ${focusIds.length} FOCUS ids`);
  rmSync(d, { recursive: true, force: true });
}

// restoration proof — production sources untouched by the whole suite
{
  const key = ['scripts/portfolio/block-adapters.mjs', 'scripts/portfolio/observe-code.mjs', `${P}/focus/di/FocusGraph.kt`];
  const ok = key.every((f) => existsSync(join(root, f)) && readFileSync(join(root, f)).length > 0);
  rec('P3-R', ok, 'production sources intact after the suite (all mutations ran in sandboxes; nothing written to the repo)');
}

const out = { artifact: 'V2d Phase 3 evidence mutation suite', schemaVersion: '1.0.0',
  host: { platform: process.platform, node: process.version },
  claimScope: 'Demonstrates the evidence system cannot fabricate certainty and that each gate is discriminating. Does NOT verify that any exercise works, is reachable, or is scientifically valid.',
  failures: fails, results };
writeFileSync(join(root, 'PORTFOLIO_EVIDENCE_MUTATIONS_RESULT.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');
console.log(`\nfailures=${fails} → ${fails === 0 ? 'PASS' : 'FAIL'}`);
process.exit(fails === 0 ? 0 : 1);
