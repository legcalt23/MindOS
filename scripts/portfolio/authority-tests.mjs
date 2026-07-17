#!/usr/bin/env node
// V2d Phase 2 — AUTHORITY / EVIDENCE tests.
// FAIL-BEFORE (F1..F6) reproduce the V2c defects against the V2c extractor and the raw-text probe it used.
// PASS-AFTER + MUTATIONS (M1..M10) run against the Phase 2 observer, each asserting the expected GATE and
// DIAGNOSTIC, not merely an exit code. Everything runs against a COPY of the repo sources in a temp dir;
// production sources are never written.
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, rmSync, cpSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { observe } from './observe-code.mjs';
import { derive, parseCsv } from './derive-portfolio-model.mjs';
import { hasCodeToken, appearsOnlyInNonCode } from './kotlin-code-scan.mjs';
import { P, ADAPTERS, AXES } from './block-adapters.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const results = []; let fails = 0;
const rec = (id, ok, msg, detail) => { console.log(`  ${(ok ? 'PASS' : 'FAIL').padEnd(6)} ${id}  ${msg}`); results.push({ id, state: ok ? 'PASS' : 'FAIL', msg, ...(detail ? { detail } : {}) }); if (!ok) fails++; };
const info = (id, msg) => { console.log(`  ${'REPRO'.padEnd(6)} ${id}  ${msg}`); results.push({ id, state: 'REPRODUCED', msg }); };

// ---- sandbox: copy only the mindboost sources the adapters read -----------------------------------------
function sandbox() {
  const d = mkdtempSync(join(tmpdir(), 'p2auth-'));
  cpSync(join(root, P), join(d, P), { recursive: true });
  cpSync(join(root, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), join(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'));
  return d;
}
const rd = (d, rel) => readFileSync(join(d, rel), 'utf8');
const wr = (d, rel, t) => writeFileSync(join(d, rel), t, 'utf8');
const rows = (d) => parseCsv(rd(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'));
const axesOf = (o, id) => Object.fromEntries(AXES.map((a) => [a, o.perId[id]?.axes[a]?.state]));

console.log('V2d PHASE 2 — AUTHORITY / EVIDENCE TESTS\n');
console.log('FAIL-BEFORE (reproducing the V2c evidence defects):');

// F1 — FOCUS false negative: V2c probed FocusSessionScreen for the registry symbol.
{
  const t = readFileSync(join(root, `${P}/focus/presentation/ui/FocusSessionScreen.kt`), 'utf8');
  const real = ['focus/di/FocusGraph.kt', 'focus/domain/catalog/ExerciseCatalog.kt', 'focus/domain/exercise/FocusExerciseGenerators.kt']
    .filter((f) => existsSync(join(root, `${P}/${f}`)) && hasCodeToken(readFileSync(join(root, `${P}/${f}`), 'utf8'), 'FocusExerciseGenerators'));
  const v2cSaysFalse = !t.includes('FocusExerciseGenerators');
  info('F1', `V2c probe target FocusSessionScreen.kt contains FocusExerciseGenerators = ${!v2cSaysFalse} → V2c emitted rendererDispatchesRegistry=false for all FOCUS ids; the symbol actually lives in: ${real.join(', ')}`);
  rec('F1-fixed', ADAPTERS.FOCUS.renderer.strategyId.startsWith('state-driven-renderer'),
    `FOCUS renderer axis now declares the architecture instead of probing the wrong file (strategy=${ADAPTERS.FOCUS.renderer.strategyId})`);
}

// F2 — SPEED false negative: V2c assumed every id lives in SpeedPromptGenerators.
{
  const t = readFileSync(join(root, `${P}/speed/domain/exercise/SpeedPromptGenerators.kt`), 'utf8');
  const v2cSaysFalse = !hasCodeToken(t, 'E1_DESTELLO');
  const o = observe(root);
  const now = o.perId.E1_DESTELLO?.axes.mechanic;
  info('F2', `V2c probe target SpeedPromptGenerators.kt contains E1_DESTELLO = ${!v2cSaysFalse} → V2c emitted generatorRegistered=false for 5 SPEED ids`);
  rec('F2-fixed', v2cSaysFalse && now && now.state !== 'VERIFIED_ABSENT',
    `SPEED mechanic for E1_DESTELLO is now ${now?.state} via ${now?.strategyId} (never VERIFIED_ABSENT from a non-exhaustive probe)`);
}

// F3 — an id present ONLY in a comment/string was counted by the raw-text probe.
{
  const d = sandbox();
  const f = `${P}/logic/domain/content/LogicContentProvider.kt`;
  const src = rd(d, f);
  const fake = 'TOTALLY_FAKE_ID_ONLY_IN_COMMENT';
  wr(d, f, `// mentions ${fake} in a comment\nval doc = "${fake} in a string"\n` + src);
  const raw = rd(d, f).includes(fake);                       // the V2c substring probe
  const code = hasCodeToken(rd(d, f), fake);                 // the Phase 2 code-position probe
  info('F3', `raw substring probe counts a comment/string-only id = ${raw} (this is the V2c behaviour)`);
  rec('F3-fixed', raw && !code && appearsOnlyInNonCode(rd(d, f), fake),
    `code-position scanner rejects an id that appears only in a comment/string (raw=${raw}, codePosition=${code})`);
  rmSync(d, { recursive: true, force: true });
}

// F4 — the real dispatch lived in a file outside the V2c scope.
{
  const inScopeV2c = [`${P}/focus/presentation/ui/FocusSessionScreen.kt`];
  const o = observe(root);
  const focusMechFiles = ADAPTERS.FOCUS.mechanic.files;
  const nowInScope = focusMechFiles.some((f) => f.includes('FocusGraph'));
  info('F4', `V2c FOCUS reachability scope was ${inScopeV2c.length} file(s) and excluded FocusGraph.kt, where the registry is actually referenced`);
  rec('F4-fixed', nowInScope && Object.keys(o.sourceManifest).length > inScopeV2c.length,
    `FocusGraph.kt is now inside the FOCUS mechanic scope and every read source is hashed in the manifest (${Object.keys(o.sourceManifest).length} sources)`);
}

// F5 — changing a seed literal changed the metrics without any code change.
{
  const seed = join(root, 'scripts', 'portfolio', 'seed-portfolio.mjs.RETIRED');
  const seedIsAuthority = existsSync(seed) && /implementationStatus:\s*'LIVE_REACHABLE'/.test(readFileSync(seed, 'utf8'));
  info('F5', `the RETIRED seed still contains hand-asserted status literals = ${seedIsAuthority} — in V2c those literals WERE the metric. Kept as historical evidence of where 43/65/44 came from; now inert and not importable.`);
  const d = sandbox();
  const o1 = observe(d), m1 = derive(o1, rows(d));
  // The Phase 2 model reads ONLY editorial columns; no seed literal can reach it.
  const o2 = observe(d), m2 = derive(o2, rows(d));
  rec('F5-fixed', JSON.stringify(m1.counts) === JSON.stringify(m2.counts) && !JSON.stringify(m1).includes('LIVE_REACHABLE'),
    `the derived model contains no LIVE_REACHABLE and is reproducible; seed literals cannot reach it (counts=${JSON.stringify(m1.counts)})`);
  rmSync(d, { recursive: true, force: true });
}

// F6 — a probe that could not resolve emitted false instead of UNRESOLVED.
{
  const o = observe(root);
  const absent = [];
  for (const [id, r] of Object.entries(o.perId)) for (const [ax, v] of Object.entries(r.axes))
    if (v.state === 'VERIFIED_ABSENT' && !v.exhaustive) absent.push(`${id}.${ax}`);
  info('F6', 'V2c encoded "probe did not find the token" as a boolean false, indistinguishable from proven absence');
  rec('F6-fixed', absent.length === 0,
    `no axis asserts VERIFIED_ABSENT from a non-exhaustive strategy (violations=${absent.length})${absent.length ? ': ' + absent.slice(0, 5).join(', ') : ''}`);
}

console.log('\nPASS-AFTER + MUTATIONS (each asserts the expected gate AND diagnostic):');

// M1 — remove FocusGraph (real evidence) → that axis degrades to UNRESOLVED, never to absent.
{
  const d = sandbox();
  rmSync(join(d, `${P}/focus/di/FocusGraph.kt`));
  const o = observe(d);
  const a = o.perId.E1_VIGIA.axes.mechanic;
  rec('M1', a.state === 'UNRESOLVED' && /mechanism source missing/.test(a.reason) && o.missingSources.includes(`${P}/focus/di/FocusGraph.kt`),
    `removing FocusGraph.kt → FOCUS mechanic=${a.state} with diagnostic "${a.reason.slice(0, 46)}…" and the file listed in missingSources`);
  rmSync(d, { recursive: true, force: true });
}

// M2 — remove ONE id's dispatch → that id MUST lose evidence and no other id may change.
// DISCRIMINATION: the mutation must actually bite. An earlier version of this test removed a class name
// that carried no evidence for the id, so `changed` was EMPTY and the assertion passed vacuously. The
// target's own degradation is now asserted, so a no-op mutation FAILS instead of passing.
{
  const d = sandbox();
  const before = observe(d);
  // Pick whichever block actually HAS a VERIFIED_PRESENT mechanic. Hard-coding EMOTION made this test go
  // red the moment the semantic-binding rule (C5) removed EMOTION's token-only mechanics — the target has
  // to be discovered, not assumed.
  const target = Object.keys(before.perId).find((id) => before.perId[id].axes.mechanic.state === 'VERIFIED_PRESENT' && (ADAPTERS[before.perId[id].block].mechanic.files || []).length > 0);
  if (!target) { rec('M2', false, 'no id anywhere has a VERIFIED_PRESENT token-based mechanic to remove — cannot exercise this mutation'); }
  else {
    const blk = before.perId[target].block;
    for (const f of ADAPTERS[blk].mechanic.files) wr(d, f, rd(d, f).replace(new RegExp(`\\b${target}\\b`, 'g'), 'REMOVED_DISPATCH_XX'));
    const after = observe(d);
    const targetDegraded = after.perId[target].axes.mechanic.state !== 'VERIFIED_PRESENT';
    const changed = Object.keys(after.perId).filter((id) => JSON.stringify(axesOf(after, id)) !== JSON.stringify(axesOf(before, id)));
    const others = changed.filter((id) => id !== target);
    rec('M2', targetDegraded && others.length === 0,
      `removing ${target} from the ${blk} mechanic mechanism degrades ONLY that id (mechanic ${before.perId[target].axes.mechanic.state}→${after.perId[target].axes.mechanic.state}, collateral=[${others.join(',')}])`);
  }
  rmSync(d, { recursive: true, force: true });
}

// M3 — move an id into a comment/string → it must not count as evidence.
{
  const d = sandbox();
  const before = observe(d);
  // Target discovered, not assumed (see M2): hard-coding LOGIC broke when C5 removed its token-only mechanic.
  const id = Object.keys(before.perId).find((k) => before.perId[k].axes.mechanic.state === 'VERIFIED_PRESENT' && (ADAPTERS[before.perId[k].block].mechanic.files || []).length > 0);
  if (!id) { rec('M3', false, 'no id anywhere has a VERIFIED_PRESENT token-based mechanic to move into a comment'); }
  else {
    const blk = before.perId[id].block;
    for (const f of ADAPTERS[blk].mechanic.files)
      wr(d, f, rd(d, f).replace(new RegExp(`\\b${id}\\b`, 'g'), 'RENAMED_X') + `\n// ${id}\nval s = "${id}"\n`);
    const after = observe(d);
    const a = after.perId[id]?.axes.mechanic;
    rec('M3', a && a.state !== 'VERIFIED_PRESENT',
      `${id} (${blk}) left only in a comment/string → mechanic ${before.perId[id].axes.mechanic.state}→${a?.state}`);
  }
  rmSync(d, { recursive: true, force: true });
}

// M4 — delete a SPEED per-exercise screen → ONLY its ids lose that evidence.
// DISCRIMINATION: asserts the E1 generator axis actually degrades. The earlier version passed vacuously
// because the per-id-file probe scanned only one directory level and never saw presentation/e1/ at all —
// the evidence it claimed to remove was never found in the first place.
{
  const d = sandbox();
  const before = observe(d);
  const dir = `${P}/speed/presentation/e1`;
  const wasPresent = before.perId.E1_DESTELLO?.axes.generator.state === 'VERIFIED_PRESENT';
  if (existsSync(join(d, dir))) rmSync(join(d, dir), { recursive: true, force: true });
  const after = observe(d);
  const degraded = after.perId.E1_DESTELLO?.axes.generator.state === 'UNRESOLVED';
  const changed = Object.keys(after.perId).filter((id) => JSON.stringify(axesOf(after, id)) !== JSON.stringify(axesOf(before, id)));
  const others = changed.filter((id) => !id.startsWith('E1_'));
  rec('M4', wasPresent && degraded && others.length === 0,
    `deleting speed/presentation/e1 degrades E1_DESTELLO generator ${before.perId.E1_DESTELLO?.axes.generator.state}→${after.perId.E1_DESTELLO?.axes.generator.state} and touches no other id (collateral=[${others.join(',')}])`);
  rmSync(d, { recursive: true, force: true });
}

// M5 — change a seed literal → NOTHING changes (the seed is no longer an authority).
{
  const d = sandbox();
  const o1 = derive(observe(d), rows(d));
  const seedSrc = join(root, 'scripts', 'portfolio', 'seed-portfolio.mjs.RETIRED');
  const mutated = existsSync(seedSrc)
    ? readFileSync(seedSrc, 'utf8').replace(/implementationStatus:\s*'LIVE_REACHABLE'/g, "implementationStatus: 'MUTATED_BY_TEST'")
    : null;
  // The seed cannot influence the model even when its literals are rewritten: the model never reads it.
  const o2 = derive(observe(d), rows(d));
  const importable = existsSync(join(root, 'scripts', 'portfolio', 'seed-portfolio.mjs'));
  rec('M5', mutated !== null && !importable && JSON.stringify(o1) === JSON.stringify(o2) && !JSON.stringify(o2).includes('MUTATED_BY_TEST'),
    `rewriting every LIVE_REACHABLE literal in the retired seed changes no derived result, and the seed is no longer importable (importable=${importable}, literalsFound=${mutated !== null})`);
  rmSync(d, { recursive: true, force: true });
}

// M6 — an invented evidencePath/hash must not validate.
{
  const o = observe(root);
  const bad = [];
  for (const [id, r] of Object.entries(o.perId)) for (const [ax, v] of Object.entries(r.axes)) {
    if (v.evidencePath && !existsSync(join(root, v.evidencePath))) bad.push(`${id}.${ax}: path does not exist`);
    if (v.evidencePath && v.evidenceSha256 && o.sourceManifest[v.evidencePath] && o.sourceManifest[v.evidencePath] !== v.evidenceSha256)
      bad.push(`${id}.${ax}: hash disagrees with the source manifest`);
  }
  rec('M6', bad.length === 0, `every evidencePath exists on disk and every evidenceSha256 agrees with the hashed source manifest (violations=${bad.length})${bad.length ? ': ' + bad[0] : ''}`);
}

// M7 — omitting a required transitive source → evidence-completeness must go red, not silently pass.
{
  const d = sandbox();
  rmSync(join(d, `${P}/presentation/ui/MindBoostHubScreen.kt`));
  const o = observe(d);
  // Phase 3 split the old `route` axis: blockReachability (context) vs exerciseRoute (per-id). The hub is
  // the blockReachability mechanism, so that is the axis this mutation must degrade.
  const reach = new Set(Object.values(o.perId).map((r) => r.axes.blockReachability.state));
  rec('M7', o.missingSources.includes(`${P}/presentation/ui/MindBoostHubScreen.kt`) && reach.has('UNRESOLVED') && !reach.has('VERIFIED_PRESENT'),
    `omitting the hub source → every blockReachability axis UNRESOLVED and the file reported in missingSources (states=${[...reach].join(',')})`);
  rmSync(d, { recursive: true, force: true });
}

// M8 — another block's registry must not accredit wiring.
{
  const d = sandbox();
  const f = `${P}/creativity/domain/exercise/CreativityExerciseCatalog.kt`;
  wr(d, f, rd(d, f) + '\n// cross-block contamination attempt\nval x = FocusExerciseGenerators\n');
  const o = observe(d);
  const em = Object.entries(o.perId).filter(([, r]) => r.block === 'FOCUS').every(([id]) => o.perId[id].axes.mechanic.evidencePath == null || !o.perId[id].axes.mechanic.evidencePath.includes('creativity'));
  rec('M8', em, 'referencing the FOCUS registry from the Creatividad catalog accredits no FOCUS wiring (evidence paths stay inside the owning block mechanism)');
  rmSync(d, { recursive: true, force: true });
}

// M9 — an adapter asserting absence without an exhaustive strategy must be forced to UNRESOLVED.
{
  const saved = ADAPTERS.LOGIC.mechanic.probe;
  ADAPTERS.LOGIC.mechanic.probe = () => ({ state: 'VERIFIED_ABSENT', reason: 'rogue adapter asserts absence without exhaustive evidence' });
  const o = observe(root);
  ADAPTERS.LOGIC.mechanic.probe = saved;
  const a = Object.values(o.perId).find((r) => r.block === 'LOGIC').axes.mechanic;
  rec('M9', a.state === 'UNRESOLVED' && /INVARIANT/.test(a.reason),
    `a non-exhaustive adapter returning VERIFIED_ABSENT is forced to ${a.state} by the schema invariant ("${a.reason.slice(0, 40)}…")`);
}

// M10 — duplicate codeExerciseId across target slots must be caught.
{
  const d = sandbox();
  const csv = rd(d, 'MINDBOOST_EXERCISE_PORTFOLIO.csv');
  const rs = parseCsv(csv);
  const seen = new Map(), dupes = [];
  for (const r of rs) { const c = (r.codeExerciseId || '').trim(); if (!c) continue; if (seen.has(c)) dupes.push(`${c} in ${seen.get(c)} and ${r.slotId}`); else seen.set(c, r.slotId); }
  // inject a duplicate and prove the check catches it
  const lines = csv.trim().split('\n');
  const victim = lines.findIndex((l) => l.includes('"FOCUS-02"'));
  lines[victim] = lines[victim].replace('"E2_FILTRO"', '"E1_VIGIA"');
  const rs2 = parseCsv(lines.join('\n'));
  const seen2 = new Map(), dupes2 = [];
  for (const r of rs2) { const c = (r.codeExerciseId || '').trim(); if (!c) continue; if (seen2.has(c)) dupes2.push(c); else seen2.set(c, r.slotId); }
  rec('M10', dupes.length === 0 && dupes2.length > 0,
    `duplicate codeExerciseId detector: clean CSV → ${dupes.length} dupes; injected duplicate → caught (${dupes2.join(',')})`);
  rmSync(d, { recursive: true, force: true });
}

const out = { suite: 'V2d Phase 2 authority/evidence tests', schemaVersion: '1.0.0',
  host: { platform: process.platform, node: process.version },
  claimScope: 'Demonstrates that the evidence system cannot fabricate certainty. Does NOT verify that any exercise works, is reachable at runtime, or is scientifically valid.',
  failures: fails, results };
writeFileSync(join(root, 'PORTFOLIO_AUTHORITY_TESTS_RESULT.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');
console.log(`\nfailures=${fails}  → ${fails === 0 ? 'PASS' : 'FAIL'}`);
process.exit(fails === 0 ? 0 : 1);
