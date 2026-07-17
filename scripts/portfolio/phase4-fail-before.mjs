#!/usr/bin/env node
// V2d PHASE 4 — MANDATORY FAIL-BEFORE (F4-1..F4-7).
// Reproduces, against the PRE-Phase-4 snapshot, that the legacy CSV columns are still ACTIVE AUTHORITY
// and that the old validator shadows invariants. Each probe mutates a copy, observes, and restores bytes.
// This file is evidence: it must keep failing the same way if Phase 4 is ever reverted.
import { readFileSync, writeFileSync, copyFileSync, existsSync, unlinkSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => (existsSync(p) ? createHash('sha256').update(readFileSync(p)).digest('hex') : 'ABSENT');
const CSV = rel('MINDBOOST_EXERCISE_PORTFOLIO.csv');
const MD = rel('MINDBOOST_EXERCISE_PORTFOLIO.md');
const RT = rel('PORTFOLIO_RUNTIME_EVIDENCE.json');
const BAK = (p) => `${p}.p4bak`;

const results = [];
const rec = (id, title, proven, detail) => { results.push({ id, title, proven, detail }); console.log(`${proven ? 'REPRODUCED' : 'NOT REPRODUCED'}  ${id} — ${title}\n    ${detail}\n`); };

const backup = (ps) => ps.forEach((p) => existsSync(p) && copyFileSync(p, BAK(p)));
const restore = (ps) => ps.forEach((p) => { if (existsSync(BAK(p))) { copyFileSync(BAK(p), p); unlinkSync(BAK(p)); } });

// A mutation harness MUST be fail-closed about its own baseline. An earlier revision of this file threw
// mid-probe, left the CSV mutated AND the Markdown rebuilt from it, and the next run silently adopted the
// corrupted bytes as "the pre-snapshot" — reporting a green restore against a poisoned baseline.
// Two guards now make that impossible:
//   1. PRE-FLIGHT: every mutable input must hash-match its SHA256SUMS.txt pin before any probe runs.
//   2. probe(): backup/restore in try/finally, so a throw can never leave a mutation on disk.
function preflight() {
  const sums = readFileSync(rel('SHA256SUMS.txt'), 'utf8');
  const pin = (p) => (new RegExp(`^([0-9a-f]{64})\\s{2}${p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm').exec(sums) || [])[1];
  const bad = [];
  for (const p of ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md']) {
    const want = pin(p), got = sha(rel(p));
    if (!want) bad.push(`${p}: no pin in SHA256SUMS.txt`);
    else if (want !== got) bad.push(`${p}: on-disk ${got.slice(0, 12)}… != pinned ${want.slice(0, 12)}…`);
  }
  const strays = ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md', 'PORTFOLIO_RUNTIME_EVIDENCE.json']
    .filter((p) => existsSync(BAK(rel(p))));
  if (strays.length) bad.push(`stray .p4bak from a crashed run: ${strays.join(', ')} — restore them before re-running`);
  if (bad.length) { console.error('PRE-FLIGHT FAILED — refusing to run on a dirty baseline:'); bad.forEach((b) => console.error(`  ! ${b}`)); process.exit(2); }
  console.log('PRE-FLIGHT: mutable inputs match their SHA256SUMS.txt pins; no stray backups.\n');
}
function probe(files, fn) {
  backup(files);
  try { return fn(); } finally { restore(files); }
}

const node = (script, args = []) => {
  try { return { code: 0, out: execFileSync(process.execPath, [rel(script), ...args], { encoding: 'utf8', cwd: root }) }; }
  catch (e) { return { code: e.status ?? 1, out: (e.stdout || '') + (e.stderr || '') }; }
};
const validatorJson = () => { const r = node('scripts/portfolio/validate-portfolio.mjs', ['--mode=repo', '--json']); try { return JSON.parse(r.out); } catch { return null; } };
const counts = () => { const { parseCsv } = csvMod; const { computeCounts } = renderMod; return computeCounts(parseCsv(readFileSync(CSV, 'utf8'))); };

const csvMod = await import('./csv.mjs');
const renderMod = await import('./render.mjs');

// Row helpers operating on raw CSV text (preserve byte shape; only swap one quoted cell).
const header = () => readFileSync(CSV, 'utf8').split('\n')[0].split(',');
function setCell(slotId, column, value) {
  const text = readFileSync(CSV, 'utf8');
  const hdr = header();
  const idx = hdr.indexOf(column);
  if (idx < 0) throw new Error(`no column ${column}`);
  const lines = text.split('\n');
  for (let i = 1; i < lines.length; i++) {
    const cells = csvMod.parseCsvRaw(lines[i])[0];
    if (!cells || cells[hdr.indexOf('slotId')] !== slotId) continue;
    cells[idx] = value;
    lines[i] = cells.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',');
    writeFileSync(CSV, lines.join('\n'), 'utf8');
    return true;
  }
  throw new Error(`no row ${slotId}`);
}
const cellOf = (slotId, column) => {
  const rows = csvMod.parseCsv(readFileSync(CSV, 'utf8'));
  return rows.find((r) => r.slotId === slotId)?.[column];
};

preflight();
const SNAP = { csv: sha(CSV), md: sha(MD), obs: sha(rel('PORTFOLIO_CODE_OBSERVATIONS.json')), model: sha(rel('PORTFOLIO_DERIVED_MODEL.json')) };
console.log('PRE-PHASE-4 SNAPSHOT');
for (const [k, v] of Object.entries(SNAP)) console.log(`  ${k.padEnd(6)} ${v}`);
console.log('');

// ---------------------------------------------------------------- F4-1
// A legacy status edit changes computeCounts + the published Markdown, and touches NO code observation.
probe([CSV, MD], () => {
  const slot = 'FOCUS-01';
  const before = { impl: cellOf(slot, 'implementationStatus'), mech: cellOf(slot, 'mechanicImplemented'), counts: counts() };
  setCell(slot, 'implementationStatus', 'HIDDEN');
  setCell(slot, 'mechanicImplemented', 'false');
  const after = counts();
  const mdBefore = readFileSync(MD, 'utf8');
  node('scripts/portfolio/build-portfolio.mjs');
  const mdAfter = readFileSync(MD, 'utf8');
  const obsSame = sha(rel('PORTFOLIO_CODE_OBSERVATIONS.json')) === SNAP.obs;
  const countsChanged = before.counts.liveReachable !== after.liveReachable || before.counts.mechanicImplemented !== after.mechanicImplemented;
  const mdChanged = mdBefore !== mdAfter;
  rec('F4-1', 'legacy mutation controls published truth', countsChanged && mdChanged && obsSame,
    `${slot} implementationStatus ${before.impl}→HIDDEN, mechanicImplemented ${before.mech}→false ⇒ ` +
    `computeCounts.liveReachable ${before.counts.liveReachable}→${after.liveReachable}, ` +
    `mechanicImplemented ${before.counts.mechanicImplemented}→${after.mechanicImplemented}; Markdown changed=${mdChanged}; ` +
    `code observations unchanged=${obsSame} (sha still ${SNAP.obs.slice(0, 12)}…). A hand edit to a status column moved the published number and no code was consulted.`);
});

// ---------------------------------------------------------------- F4-2
// definitionStatus=UNASSIGNED + implementationStatus=LIVE_REACHABLE + a real codeExerciseId.
// The if/else-if chain enters the UNASSIGNED branch ONLY; every LIVE invariant is skipped.
probe([CSV], () => {
  const slot = 'CREATE-15';
  const wasDef = cellOf(slot, 'definitionStatus');
  setCell(slot, 'definitionStatus', 'UNASSIGNED');
  setCell(slot, 'implementationStatus', 'LIVE_REACHABLE');
  setCell(slot, 'codeExerciseId', 'E1_VIGIA');
  const j = validatorJson();
  const diags = j?.gates?.STATUS_CROSS_INVARIANTS_GATE?.diagnostics ?? [];
  const mine = diags.filter((d) => d.startsWith(slot));
  const sawUnassignedBranch = mine.some((d) => /UNASSIGNED with ids/.test(d));
  const sawLiveInvariant = mine.some((d) => /LIVE (without codeExerciseId|with mechanicImplemented|routeStatus|androidStatus|generator|measurement)/.test(d));
  rec('F4-2', 'shadowing: UNASSIGNED branch swallows LIVE invariants', sawUnassignedBranch && !sawLiveInvariant,
    `${slot}: definitionStatus ${wasDef}→UNASSIGNED, implementationStatus→LIVE_REACHABLE, codeExerciseId→E1_VIGIA. ` +
    `Diagnostics for this row: [${mine.map((d) => d.replace(`${slot}: `, '')).join(' | ') || 'none'}]. ` +
    `UNASSIGNED branch fired=${sawUnassignedBranch}; ANY LIVE_REACHABLE invariant fired=${sawLiveInvariant}. ` +
    `The row simultaneously claims "no assignment" and "live", and only the first branch is ever evaluated.`);
});

// ---------------------------------------------------------------- F4-3
// implementationStatus=IMPLEMENTED is a legal enum value with NO branch at all.
probe([CSV], () => {
  const slot = 'FOCUS-02';
  const was = cellOf(slot, 'implementationStatus');
  setCell(slot, 'implementationStatus', 'IMPLEMENTED');
  const j = validatorJson();
  const diags = (j?.gates?.STATUS_CROSS_INVARIANTS_GATE?.diagnostics ?? []).filter((d) => d.startsWith(slot));
  const enumRejected = diags.some((d) => /not in closed enum/.test(d));
  rec('F4-3', 'IMPLEMENTED escapes every implementation invariant', diags.length === 0 && !enumRejected,
    `${slot}: implementationStatus ${was}→IMPLEMENTED. It is in the closed enum (so not rejected) but matches ` +
    `no branch of the if/else-if chain: diagnostics for this row = ${diags.length}. ` +
    `Result: an "implemented" claim with zero implementation invariants applied.`);
});

// ---------------------------------------------------------------- F4-4
// RUNTIME_PLAYABILITY_GATE is a hard-coded literal: the runtime authority cannot move it.
probe([RT], () => {
  const rt = JSON.parse(readFileSync(RT, 'utf8'));
  const gBefore = validatorJson()?.gates?.RUNTIME_PLAYABILITY_GATE;
  rt.runs.push({ slotId: 'FOCUS-01', codeExerciseId: 'E1_VIGIA', platform: 'android',
    snapshotSha256: SNAP.csv, testOrDossier: 'F4-4-probe', result: 'VERIFIED',
    evidencePath: 'scripts/portfolio/phase4-fail-before.mjs', provenance: 'F4-4 probe', executedAtUtc: '2026-07-15T00:00:00Z' });
  writeFileSync(RT, JSON.stringify(rt, null, 2) + '\n', 'utf8');
  const gAfter = validatorJson()?.gates?.RUNTIME_PLAYABILITY_GATE;
  rec('F4-4', 'runtime gate is hard-coded, not derived', gBefore?.status === 'NOT_RUN' && gAfter?.status === 'NOT_RUN',
    `Added a fully-formed VERIFIED run for FOCUS-01 to the runtime authority. ` +
    `RUNTIME_PLAYABILITY_GATE before="${gBefore?.status}" after="${gAfter?.status}" — identical. ` +
    `validate-portfolio.mjs:setStatus('RUNTIME_PLAYABILITY_GATE','NOT_RUN',...) is a literal; the runtime authority is not read at all.`);
});

// ---------------------------------------------------------------- F4-5
// GENERATED_VIEW_GATE is circular: build and validate both call renderPortfolio(legacy rows), so the gate
// is green while the published view contradicts the derived model.
{
  const j = validatorJson();
  const gv = j?.gates?.GENERATED_VIEW_GATE?.status;
  const md = readFileSync(MD, 'utf8');
  const model = JSON.parse(readFileSync(rel('PORTFOLIO_DERIVED_MODEL.json'), 'utf8'));
  const mdLive = /\| Live-reachable \| (\d+)\/121/.exec(md)?.[1];
  const mdMech = /\| Mechanic-implemented \| (\d+)\/121/.exec(md)?.[1];
  const contradiction = Number(mdLive) !== model.counts.CODE_ASSIGNED_STATIC_VERIFIED;
  rec('F4-5', 'generated-view circularity', gv === 'PASS' && contradiction,
    `GENERATED_VIEW_GATE=${gv} (byte-exact) while the SAME snapshot's derived model says ` +
    `CODE_ASSIGNED_STATIC_VERIFIED=${model.counts.CODE_ASSIGNED_STATIC_VERIFIED}. The Markdown publishes ` +
    `"Live-reachable ${mdLive}/121" and "Mechanic-implemented ${mdMech}/121". Both sides of the gate are the same legacy view ` +
    `(build-portfolio.mjs and validate-portfolio.mjs both import renderPortfolio from render.mjs), so it can only prove ` +
    `self-consistency — never agreement with the authorities.`);
}

// ---------------------------------------------------------------- F4-6
// The EXTRA-121 per-block row is a hard-coded literal in render.mjs.
probe([CSV, MD], () => {
  const wasDef = cellOf('EXTRA-121', 'definitionStatus');
  setCell('EXTRA-121', 'definitionStatus', 'DESIGNED');
  setCell('EXTRA-121', 'designExerciseId', 'F4_6_PROBE');
  setCell('EXTRA-121', 'canonicalExerciseId', 'F4_6_PROBE');
  node('scripts/portfolio/build-portfolio.mjs');
  const line = readFileSync(MD, 'utf8').split('\n').find((l) => l.startsWith('| EXTRA-121 |'));
  rec('F4-6', 'EXTRA-121 table row is a literal', line === '| EXTRA-121 | 0/1 | 0 | 0 | 0 | 0 | 1 |',
    `EXTRA-121 definitionStatus ${wasDef}→DESIGNED + designExerciseId→F4_6_PROBE, then rebuilt. ` +
    `Published row is still: ${line} — render.mjs:L.push('| EXTRA-121 | 0/1 | 0 | 0 | 0 | 0 | 1 |') never reads the row.`);
});

// ---------------------------------------------------------------- F4-7
// Defects in THREE different categories at once. The else-if chain evaluates exactly ONE category per row,
// so the other categories' defects are never reported — fixing what you are told still leaves the row invalid.
probe([CSV], () => {
  const slot = 'LOGIC-15';
  setCell(slot, 'definitionStatus', 'UNASSIGNED');           // cat A (assignment): UNASSIGNED carrying a real id
  setCell(slot, 'implementationStatus', 'LIVE_REACHABLE');   // cat B (LIVE): claims live...
  setCell(slot, 'codeExerciseId', 'E1_VIGIA');
  setCell(slot, 'mechanicImplemented', 'false');             // cat B: ...with no mechanic
  setCell(slot, 'routeStatus', 'NONE');                      // cat B: ...and no route
  setCell(slot, 'generatorOrBankStatus', 'CATALOG_ONLY');    // cat C (generator): contradicts LIVE
  setCell(slot, 'androidStatus', 'NOT_WIRED');               // cat B: ...and not wired on Android
  const j = validatorJson();
  const mine = (j?.gates?.STATUS_CROSS_INVARIANTS_GATE?.diagnostics ?? []).filter((d) => d.startsWith(slot));
  const families = {
    assignment: mine.some((d) => /UNASSIGNED with ids/.test(d)),
    live: mine.some((d) => /LIVE (with mechanicImplemented|routeStatus|androidStatus|generator|measurement)/.test(d)),
  };
  const fired = Object.entries(families).filter(([, v]) => v).map(([k]) => k);
  rec('F4-7', 'multiple violations shadowed across categories', families.assignment && !families.live,
    `${slot} given defects in 3 categories at once (UNASSIGNED while carrying codeExerciseId=E1_VIGIA; ` +
    `LIVE_REACHABLE with mechanicImplemented=false, routeStatus=NONE, androidStatus=NOT_WIRED; ` +
    `generatorOrBankStatus=CATALOG_ONLY contradicting LIVE). Categories that fired: [${fired.join(', ')}]. ` +
    `LIVE_REACHABLE family fired=${families.live} — SILENT, though the row literally says implementationStatus=LIVE_REACHABLE. ` +
    `${mine.length} diagnostics reported, NONE about the live/mechanic/route/generator contradictions. ` +
    `The else-if chain evaluates ONE category per row: fix everything you are told and the row is still invalid.`);
});

// ---------------------------------------------------------------- integrity
const POST = { csv: sha(CSV), md: sha(MD), obs: sha(rel('PORTFOLIO_CODE_OBSERVATIONS.json')), model: sha(rel('PORTFOLIO_DERIVED_MODEL.json')), rt: sha(RT) };
const restored = POST.csv === SNAP.csv && POST.md === SNAP.md && POST.obs === SNAP.obs && POST.model === SNAP.model;
console.log(`RESTORATION: every mutated file restored byte-identical = ${restored}`);
for (const [k, v] of Object.entries(POST)) console.log(`  ${k.padEnd(6)} ${v}`);

const failed = results.filter((r) => !r.proven);
writeFileSync(rel('PHASE4_FAIL_BEFORE_RESULT.json'), JSON.stringify({
  artifact: 'V2d Phase 4 MANDATORY FAIL-BEFORE evidence', schemaVersion: '1.0.0',
  meaning: 'Each probe REPRODUCED means the pre-Phase-4 defect is real and was observed, not asserted.',
  preSnapshot: SNAP, postSnapshot: POST, restoredByteIdentical: restored,
  probes: results, reproduced: results.length - failed.length, of: results.length,
}, null, 2) + '\n', 'utf8');

console.log(`\nFAIL-BEFORE: ${results.length - failed.length}/${results.length} defects reproduced`);
process.exit(failed.length || !restored ? 1 : 0);
