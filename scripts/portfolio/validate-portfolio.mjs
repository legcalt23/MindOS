#!/usr/bin/env node
// V2d PHASE 4 VALIDATOR — consumes AUTHORITIES + the DERIVED MODEL. The legacy CSV columns are gone from
// the active schema and unreachable from here.
//
// What changed vs V2c, and why:
//   * no if/else-if chain — every invariant FAMILY runs against every row, so a row with five defects gets
//     five diagnostics (F4-2/F4-3/F4-7 proved the chain reported one category and stayed silent on the rest);
//   * RUNTIME_PLAYABILITY_GATE is DERIVED from the runtime authority, not a hard-coded literal (F4-4);
//   * TRAINING_READY is DERIVED, never read from a file;
//   * the byte-exact view gate is backed by MD_PUBLISHED_NUMBERS_GATE, which re-parses the published bytes
//     and compares them to authority-derived values WITHOUT the renderer — so builder and validator sharing
//     renderPortfolio can no longer produce a mutually-consistent lie (F4-5);
//   * PORTFOLIO_ACTIVE_SCHEMA_GATE rejects any retired column reappearing in the active CSV (F4-1).
import { readFileSync, existsSync, statSync, readdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsv, headerOf } from './csv.mjs';
import { validateContext, MODES } from './validation-mode.mjs';
import { renderPortfolio, BLOCKS, FORBIDDEN_STATIC_WORDS } from './render.mjs';
import { buildModel, parseCsv as parseModelCsv } from './derive-portfolio-model.mjs';
import { ACTIVE_COLUMNS, RETIRED_COLUMNS } from './migrate-legacy-columns.mjs';
import { computeAll, perBlock, formulaTruthViolations, BY_ID } from './metrics.mjs';
import { deriveReadiness } from './portfolio-state-model.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const E = process.env;
const paths = {
  csv: E.PORTFOLIO_CSV || rel('MINDBOOST_EXERCISE_PORTFOLIO.csv'),
  aux: E.PORTFOLIO_AUX || rel('MINDBOOST_EXERCISE_AUXILIARIES.csv'),
  md: E.PORTFOLIO_MD || rel('MINDBOOST_EXERCISE_PORTFOLIO.md'),
  obs: E.PORTFOLIO_OBSERVATIONS || rel('PORTFOLIO_CODE_OBSERVATIONS.json'),
  model: E.PORTFOLIO_MODEL || rel('PORTFOLIO_DERIVED_MODEL.json'),
  runtime: E.PORTFOLIO_RUNTIME || rel('PORTFOLIO_RUNTIME_EVIDENCE.json'),
  legacy: E.PORTFOLIO_LEGACY || rel('PORTFOLIO_V2C_LEGACY_CLAIMS.csv'),
  legacyMeta: E.PORTFOLIO_LEGACY_META || rel('PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json'),
  manifest: E.PORTFOLIO_MANIFEST || rel('PORTFOLIO_SOURCE_MANIFEST_V2.json'),
  delivery: E.PORTFOLIO_DELIVERY || rel('DELIVERY_MANIFEST.json'),
  sums: E.PORTFOLIO_SHA256SUMS || rel('SHA256SUMS.txt'),
  lockres: E.PORTFOLIO_LOCKRESULT || rel('LOCK_SAFETY_RESULT.json'),
};
const readJson = (p) => { try { return JSON.parse(readFileSync(p, 'utf8')); } catch { return null; } };

const gates = {};
const gate = (name, cond, diag) => { gates[name] ??= { status: 'PASS', diagnostics: [] }; if (!cond) { gates[name].status = 'FAIL'; gates[name].diagnostics.push(diag); } };
const setStatus = (name, status, diag) => { gates[name] ??= { status: 'PASS', diagnostics: [] }; gates[name].status = status; if (diag) gates[name].diagnostics.push(diag); };

const csvText = readFileSync(paths.csv, 'utf8');
const allRows = parseCsv(csvText);
const rows = allRows; // P4-R1: no row may exclude itself from validation via its own field
const aux = existsSync(paths.aux) ? parseCsv(readFileSync(paths.aux, 'utf8')) : [];
const obs = readJson(paths.obs);
const onDiskModel = readJson(paths.model);
const runtime = readJson(paths.runtime);
const legacyMeta = readJson(paths.legacyMeta);
const md = existsSync(paths.md) ? readFileSync(paths.md, 'utf8') : '';

// ---------- PORTFOLIO_ACTIVE_SCHEMA_GATE — the retired columns must never return as active input ----------
{
  const hdr = headerOf(csvText);
  for (const c of RETIRED_COLUMNS) gate('PORTFOLIO_ACTIVE_SCHEMA_GATE', !hdr.includes(c),
    `active CSV carries RETIRED column "${c}". Its authority is code-observation/runtime/scientific — not editorial. Historical values live in PORTFOLIO_V2C_LEGACY_CLAIMS.csv.`);
  for (const c of ACTIVE_COLUMNS) gate('PORTFOLIO_ACTIVE_SCHEMA_GATE', hdr.includes(c), `active CSV is missing editorial column "${c}"`);
  for (const c of hdr) gate('PORTFOLIO_ACTIVE_SCHEMA_GATE', ACTIVE_COLUMNS.includes(c), `active CSV carries undeclared column "${c}" — every column needs an explicit owner`);
}

// ---------- LEGACY_ISOLATION_GATE — the historical artifact exists, is labelled, and feeds nothing ----------
{
  gate('LEGACY_ISOLATION_GATE', existsSync(paths.legacy), 'PORTFOLIO_V2C_LEGACY_CLAIMS.csv missing — retired claims must be preserved, not deleted');
  gate('LEGACY_ISOLATION_GATE', !!legacyMeta, 'PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json missing');
  if (legacyMeta) {
    gate('LEGACY_ISOLATION_GATE', legacyMeta.role === 'HISTORICAL_EVIDENCE', `legacy meta role="${legacyMeta.role}" (want HISTORICAL_EVIDENCE)`);
    gate('LEGACY_ISOLATION_GATE', legacyMeta.disclaimer === 'HISTORICAL_UNVERIFIED_CLAIMS', 'legacy meta lacks the HISTORICAL_UNVERIFIED_CLAIMS disclaimer');
    gate('LEGACY_ISOLATION_GATE', !!legacyMeta.sourceCsv?.sha256AtMigration, 'legacy meta lacks the source CSV hash');
    gate('LEGACY_ISOLATION_GATE', !!legacyMeta.v2cSnapshot?.csvSha256, 'legacy meta lacks the V2c snapshot reference');
    if (existsSync(paths.legacy)) {
      const disk = createHash('sha256').update(readFileSync(paths.legacy)).digest('hex');
      gate('LEGACY_ISOLATION_GATE', disk === legacyMeta.legacyCsvSha256, `legacy claims CSV hash ${disk.slice(0, 12)}… != meta ${String(legacyMeta.legacyCsvSha256).slice(0, 12)}… (the historical record was altered)`);
    }
  }
  // The rule is precise: no active tool may PARSE the historical claims into rows or let a retired column
  // influence a published number. Computing a checksum over its bytes is integrity verification, not
  // consumption, and is explicitly permitted — that is exactly what this gate itself does above.
  const ACTIVE_TOOLS = ['render.mjs', 'build-portfolio.mjs', 'validate-portfolio.mjs', 'derive-portfolio-model.mjs',
    'observe-code.mjs', 'metrics.mjs', 'portfolio-state-model.mjs', 'block-adapters.mjs'];
  for (const t of ACTIVE_TOOLS) {
    const p = rel(`scripts/portfolio/${t}`);
    if (!existsSync(p)) continue;
    const src = readFileSync(p, 'utf8');
    gate('LEGACY_ISOLATION_GATE', !/parseCsv\w*\([^)]*(LEGACY|legacy)/.test(src), `active tool ${t} PARSES the historical legacy claims into rows`);
    gate('LEGACY_ISOLATION_GATE', !/legacyMeta\?\.retiredColumns|legacy\w*\.retiredColumns/.test(src), `active tool ${t} takes its retired-column list from the HISTORICAL artifact instead of from code`);
  }
  // Belt and braces: no retired column name may appear as a data key in the model or as a metric label.
  if (onDiskModel) {
    const modelKeys = new Set(Object.values(onDiskModel.perSlot ?? {}).flatMap((s) => Object.keys(s)));
    for (const c of RETIRED_COLUMNS) gate('LEGACY_ISOLATION_GATE', !modelKeys.has(c), `derived model carries a retired column "${c}" as a per-slot field`);
  }
}

// ---------- derive the model from the authorities, independently of what is on disk ----------
let fresh = null;
if (!obs) setStatus('DERIVED_MODEL_FRESHNESS_GATE', 'FAIL', 'code-observation authority missing');
else fresh = buildModel({ obs, rows, aux, runtime });

// ---------- ROW_MEMBERSHIP_GATE (P4-R1) — membership belongs to the container, never to the row ----------
// Reproduced defect: while `entityType` was an active column it was filtered BEFORE any invariant family
// ran, so one cell switched off every invariant on a row (4 diagnostics → 0) and the validator then called
// the row "missing" while it sat in the file carrying its defects. Fix: this CSV IS the target authority.
{
  const hdr = headerOf(csvText);
  for (const disc of ['entityType', 'countsTowardTarget']) {
    gate('ROW_MEMBERSHIP_GATE', !hdr.includes(disc),
      `active CSV carries the self-asserted membership discriminator "${disc}". A row must not be able to decide whether it is validated. Target membership comes from the container: every physical row of this file is a target slot.`);
  }
  gate('ROW_MEMBERSHIP_GATE', allRows.length === 121, `the target authority has ${allRows.length} PHYSICAL rows, expected exactly 121 — membership is counted from the file, not from a field`);
  gate('ROW_MEMBERSHIP_GATE', rows.length === allRows.length, `${allRows.length - rows.length} physical row(s) were filtered out before validation — no row may be excluded from its invariants`);
  const sids = allRows.map((r) => r.slotId);
  gate('ROW_MEMBERSHIP_GATE', new Set(sids).size === 121, `${new Set(sids).size} unique slotIds across 121 physical rows`);
  for (const r of allRows) gate('ROW_MEMBERSHIP_GATE', !!r.slotId, 'a physical row carries no slotId — it could never be diagnosed by slot');
  // An auxiliary smuggled into the target file is an ERROR, not an ignored row.
  const auxCodes = new Set(aux.map((a) => (a.codeExerciseId || '').trim()).filter(Boolean));
  const auxIds = new Set(aux.map((a) => (a.auxId || '').trim()).filter(Boolean));
  for (const r of allRows) {
    const c = (r.codeExerciseId || '').trim();
    gate('ROW_MEMBERSHIP_GATE', !(c && auxCodes.has(c)), `${r.slotId}: auxiliary code id "${c}" occupies a target slot — auxiliaries come only from MINDBOOST_EXERCISE_AUXILIARIES.csv`);
    gate('ROW_MEMBERSHIP_GATE', !auxIds.has(r.slotId), `${r.slotId}: an auxiliary id appears as a target slotId`);
  }
  // Every physical row must reach the model: the count of validated slots equals the count of rows.
  if (fresh) gate('ROW_MEMBERSHIP_GATE', fresh.slots.length === allRows.length,
    `${fresh.slots.length} slots were validated but the file holds ${allRows.length} physical rows — a row escaped validation`);
}

// ---------- TARGET_SLOT_SEMANTICS_GATE ----------
{
  gate('TARGET_SLOT_SEMANTICS_GATE', rows.length === 121, `expected 121 target rows, got ${rows.length}`);
  const KEYS = { Enfoque: 'FOCUS', Velocidad: 'SPEED', Logica: 'LOGIC', Lenguaje: 'LANG', Memoria: 'MEMORY', Decisiones: 'DECIDE', Emociones: 'EMOTION', Creatividad: 'CREATE' };
  for (const b of Object.values(KEYS)) for (let i = 1; i <= 15; i++) { const id = `${b}-${String(i).padStart(2, '0')}`; gate('TARGET_SLOT_SEMANTICS_GATE', rows.some((r) => r.slotId === id), `missing slot ${id}`); }
  gate('TARGET_SLOT_SEMANTICS_GATE', rows.some((r) => r.slotId === 'EXTRA-121'), 'missing EXTRA-121');
  const ids = rows.map((r) => r.slotId);
  gate('TARGET_SLOT_SEMANTICS_GATE', new Set(ids).size === ids.length, 'duplicate slotId');
  const cs = rows.map((r) => r.canonicalExerciseId).filter(Boolean);
  const dup = [...new Set(cs.filter((v, i) => cs.indexOf(v) !== i))];
  gate('TARGET_SLOT_SEMANTICS_GATE', dup.length === 0, `duplicate canonicalExerciseId: ${dup}`);
  gate('TARGET_SLOT_SEMANTICS_GATE', aux.some((a) => a.codeExerciseId === 'EX16_CALIBRACION_CONFIANZA'), 'EX16 overflow not registered in auxiliaries');
}

// ---------- STATE_INVARIANTS_GATE — every family, every row, every diagnostic (non-shadowing) ----------
if (fresh) {
  for (const d of fresh.diagnostics) gate('STATE_INVARIANTS_GATE', false, `[${d.family}] ${d.slotId}: ${d.message}`);
  // the model must never carry the retired vocabulary
  const json = JSON.stringify(fresh.slots);
  for (const w of ['LIVE_REACHABLE', 'implementationStatus', 'mechanicImplemented']) {
    gate('STATE_INVARIANTS_GATE', !json.includes(w), `derived model reintroduced the retired token "${w}"`);
  }
}

// ---------- DERIVED_MODEL_FRESHNESS_GATE — on-disk model must equal a fresh derivation ----------
if (fresh && onDiskModel) {
  const freshMetrics = computeAll(fresh.slots);
  for (const [k, v] of Object.entries(freshMetrics)) {
    gate('DERIVED_MODEL_FRESHNESS_GATE', onDiskModel.metrics?.[k] === v,
      `stale derived model: metric ${k} on disk=${onDiskModel.metrics?.[k]} but a fresh derivation from the authorities yields ${v}`);
  }
  gate('DERIVED_MODEL_FRESHNESS_GATE', Object.keys(onDiskModel.perSlot ?? {}).length === fresh.slots.length,
    `stale derived model: ${Object.keys(onDiskModel.perSlot ?? {}).length} slots on disk vs ${fresh.slots.length} derived`);
  gate('DERIVED_MODEL_FRESHNESS_GATE', onDiskModel.schemaVersion === '4.0.0', `derived model schemaVersion=${onDiskModel.schemaVersion} (want 4.0.0)`);
} else if (!onDiskModel) setStatus('DERIVED_MODEL_FRESHNESS_GATE', 'FAIL', 'PORTFOLIO_DERIVED_MODEL.json missing');

// ---------- MODEL_METRIC_CONSISTENCY_GATE — a stored summary may never disagree with the rows ----------
if (onDiskModel?.perSlot) {
  const recomputed = computeAll(Object.values(onDiskModel.perSlot));
  for (const [k, v] of Object.entries(recomputed)) {
    gate('MODEL_METRIC_CONSISTENCY_GATE', onDiskModel.metrics?.[k] === v,
      `stored summary metric ${k}=${onDiskModel.metrics?.[k]} but recomputing from the model's own rows yields ${v}`);
  }
}

// ---------- COUNT_RECONCILIATION_GATE — arithmetic, recomputed from rows ----------
if (fresh) {
  const m = computeAll(fresh.slots);
  const sum = m.staticComponentsVerified + m.staticComponentsUnresolved + m.designOnlyAssigned + m.targetUnassigned;
  gate('COUNT_RECONCILIATION_GATE', sum === rows.length, `orthogonal states sum to ${sum}, expected ${rows.length}`);
  gate('COUNT_RECONCILIATION_GATE', m.codeAssigned === m.staticComponentsVerified + m.staticComponentsUnresolved,
    `codeAssigned=${m.codeAssigned} != staticVerified+staticUnresolved=${m.staticComponentsVerified + m.staticComponentsUnresolved}`);
  gate('COUNT_RECONCILIATION_GATE', m.assignedCandidates === m.codeAssigned + m.designOnlyAssigned,
    `assignedCandidates=${m.assignedCandidates} != codeAssigned+designOnly=${m.codeAssigned + m.designOnlyAssigned}`);
  const editorialAssigned = rows.filter((r) => r.definitionStatus !== 'UNASSIGNED').length;
  gate('COUNT_RECONCILIATION_GATE', m.assignedCandidates === editorialAssigned,
    `assignedCandidates=${m.assignedCandidates} does not reconcile with the editorial definitionStatus!=UNASSIGNED count ${editorialAssigned}`);
  // per-block + extra must reconcile with the total, using the same compute functions
  const extraBlocks = [...new Set(fresh.slots.filter((s) => !BLOCKS.includes(s.block)).map((s) => s.block))];
  const blockSum = perBlock(fresh.slots, [...BLOCKS, ...extraBlocks]).reduce((a, b) => a + b.targetSlots, 0);
  gate('COUNT_RECONCILIATION_GATE', blockSum === rows.length, `per-block slots sum to ${blockSum}, expected ${rows.length}`);
  const auxIds = new Set(aux.map((a) => (a.codeExerciseId || '').trim()).filter(Boolean));
  for (const s of fresh.slots) gate('COUNT_RECONCILIATION_GATE', !(s.codeExerciseId && auxIds.has(s.codeExerciseId)), `${s.slotId}: auxiliary ${s.codeExerciseId} occupies a target slot`);
}

// ---------- FORMULA_TRUTH_GATE — the printed formula must describe the executed function ----------
for (const v of formulaTruthViolations()) gate('FORMULA_TRUTH_GATE', false, v);

// ---------- RUNTIME_PLAYABILITY_GATE — DERIVED from the runtime authority (was hard-coded in V2c) ----------
if (!runtime) setStatus('RUNTIME_PLAYABILITY_GATE', 'FAIL', 'runtime authority artifact missing — runtime state is not assertable');
else {
  const REQUIRED = ['slotId', 'codeExerciseId', 'platform', 'snapshotSha256', 'testOrDossier', 'result', 'provenance', 'executedAtUtc'];
  let rejected = 0;
  for (const r of runtime.runs ?? []) {
    for (const f of REQUIRED) if (!r[f]) { gate('RUNTIME_PLAYABILITY_GATE', false, `run for ${r.slotId ?? '(no slot)'} is missing required field "${f}" — an unbound run is not evidence`); rejected++; }
    if (r.result === 'VERIFIED' && !r.evidencePath) { gate('RUNTIME_PLAYABILITY_GATE', false, `run for ${r.slotId} claims VERIFIED without an evidencePath`); rejected++; }
    if (r.slotId && !rows.some((x) => x.slotId === r.slotId)) { gate('RUNTIME_PLAYABILITY_GATE', false, `run references unknown slotId "${r.slotId}"`); rejected++; }
  }
  if (gates.RUNTIME_PLAYABILITY_GATE?.status !== 'FAIL') {
    const verified = fresh ? computeAll(fresh.slots).runtimeVerified : 0;
    const n = (runtime.runs ?? []).length;
    if (n === 0) setStatus('RUNTIME_PLAYABILITY_GATE', 'NOT_RUN', 'no run exists in the runtime authority — NOT_RUN by absence, derived (not hard-coded) and never inferred from static presence');
    else setStatus('RUNTIME_PLAYABILITY_GATE', 'PASS', `${n} run(s) recorded; ${verified} slot(s) runtime-verified`);
  }
}

// ---------- TRAINING_READY_DERIVATION_GATE — derived only, never read ----------
if (fresh) {
  const m = computeAll(fresh.slots);
  gate('TRAINING_READY_DERIVATION_GATE', !RETIRED_COLUMNS.includes('launchReadiness') || !headerOf(csvText).includes('launchReadiness'),
    'launchReadiness is readable from the active CSV — TRAINING_READY must be derived, never read');
  // Check BOTH the fresh derivation and the PUBLISHED model. A forged readiness in the published artifact
  // must be caught by the gate that owns readiness — and it must name EVERY absent axis, not just the first.
  const toCheck = [...fresh.slots.map((s) => ['derived', s]), ...Object.values(onDiskModel?.perSlot ?? {}).map((s) => ['published', s])];
  for (const [origin, s] of toCheck) {
    const d = deriveReadiness(s);
    gate('TRAINING_READY_DERIVATION_GATE', d.state === (s.readiness?.state ?? d.state),
      `${s.slotId} (${origin} model): stored readiness ${s.readiness?.state} != derivation ${d.state}. Every axis that blocks it: ${d.blocking.join('; ')}`);
  }
  setStatus('TRAINING_READY_DERIVATION_GATE', gates.TRAINING_READY_DERIVATION_GATE?.status === 'FAIL' ? 'FAIL' : 'PASS',
    gates.TRAINING_READY_DERIVATION_GATE?.status === 'FAIL' ? undefined : `TRAINING_READY ${m.trainingReady}/${rows.length}, derived from every axis`);
}

// ---------- GENERATED_VIEW_GATE — byte-exact, rendered from a FRESH derivation ----------
if (fresh && onDiskModel) {
  const expected = renderPortfolio({ ...onDiskModel, perSlot: Object.fromEntries(fresh.slots.map((s) => [s.slotId, s])) }, aux);
  gate('GENERATED_VIEW_GATE', expected === md, `published Markdown differs from a fresh render of the derived model (bytes ${md.length} vs ${expected.length})`);
}

// ---------- MD_PUBLISHED_NUMBERS_GATE — independent re-parse of the published bytes ----------
// This is the non-circular half of the view check: it never calls the renderer. It reads the numbers OUT of
// the published Markdown and compares them to authority-derived values.
if (fresh) {
  const m = computeAll(fresh.slots);
  const published = (label) => { const r = new RegExp(`\\|\\s*${label}\\s*\\|\\s*(\\d+)(?:/(\\d+))?\\s*\\|`).exec(md); return r ? Number(r[1]) : null; };
  for (const id of ['assignedCandidates', 'codeAssigned', 'staticComponentsVerified', 'staticComponentsUnresolved', 'designOnlyAssigned', 'targetUnassigned', 'exerciseRouteVerified', 'runtimeVerified', 'trainingReady']) {
    const p = published(BY_ID[id].label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    gate('MD_PUBLISHED_NUMBERS_GATE', p === m[id], `published "${BY_ID[id].label}" = ${p} but the authorities derive ${m[id]}`);
  }
  // EXTRA-121 must be derived from its real row, not a literal (F4-6).
  const extraSlot = fresh.slots.find((s) => s.slotId === 'EXTRA-121');
  if (extraSlot) {
    const line = md.split('\n').find((l) => /^\|\s*EXTRA.*\(extra\)\s*\|/.test(l));
    gate('MD_PUBLISHED_NUMBERS_GATE', !!line, 'no derived EXTRA row found in the published table');
    if (line) {
      const cells = line.split('|').map((c) => c.trim());
      const assigned = extraSlot.assignment.state !== 'TARGET_UNASSIGNED' ? 1 : 0;
      gate('MD_PUBLISHED_NUMBERS_GATE', Number(cells[3]) === assigned, `published EXTRA "Assigned"=${cells[3]} but the row derives ${assigned} — is it a literal?`);
    }
  }
  gate('MD_PUBLISHED_NUMBERS_GATE', !/\|\s*EXTRA-121\s*\|\s*0\/1\s*\|/.test(md), 'the published view contains the V2c hard-coded EXTRA-121 literal row');
}

// ---------- LEXICON_HONESTY_GATE — never call a static observation live/playable/implemented ----------
{
  for (const w of FORBIDDEN_STATIC_WORDS) {
    gate('LEXICON_HONESTY_GATE', !new RegExp(`\\|\\s*${w}`, 'i').test(md), `published view uses "${w}" as a metric label — static components are not ${w}`);
  }
  gate('LEXICON_HONESTY_GATE', !/Live-reachable \| \d+/i.test(md), 'published view still carries the V2c "Live-reachable" metric');
  gate('LEXICON_HONESTY_GATE', !/Mechanic-implemented \| \d+/i.test(md), 'published view still carries the V2c "Mechanic-implemented" metric');
}

// ---------- IN_REPO_PACKAGE_GATE ----------
const delivery = readJson(paths.delivery);
const manifest = readJson(paths.manifest);
// ---------- MODE SELECTION (MB-V2D-R03) ----------
// An earlier version inferred the mode from the presence of gen-delivery.mjs / BUNDLE_MANIFEST.json — i.e.
// from DELETABLE BYTES INSIDE THE VERY TREE BEING EVALUATED. Deleting the manifest AND the generator while
// planting a BUNDLE_MANIFEST.json downgraded a repo to NOT_APPLICABLE: the attacker chose the mode.
//
// Now the mode comes from OUTSIDE the evaluated bytes (argv), and when it is not stated it is DERIVED and
// then REFUTED against a cross-invariant that cannot be forged downward:
//
//   A bundle is a CLOSED set — BUNDLE_MANIFEST.json enumerates every file and the tree contains only those.
//   A repo is an OPEN set — it necessarily holds sources no bundle manifest lists (shared/, androidApp/, …).
//
// So "this is a bundle" is a CHECKABLE claim, not a self-assertion. To make a repo pass as a bundle you must
// delete the repo — at which point it really is one. Ambiguity fails closed to the STRICTER mode (repo).
// The mode is an ARGUMENT, required and closed-enum. There is no autodetection and no default: every version
// that let the tree choose its own policy was broken within a day (see validation-mode.mjs for the history).
// This entrypoint is the repo entrypoint, so it states repo unless the caller says otherwise; the clean-room
// entrypoint states bundle. `--mode=` with an empty value is invalid, not absent — VM-03 turned on exactly
// that distinction, so the raw argv value is passed through verbatim rather than ||-defaulted to null.
// NO DEFAULT. An earlier revision read `modeFlag === undefined ? 'repo' : …`, which is exactly the defaulting
// §3 forbids: it silently answered a question the caller never asked, and it is why Phase 8 validated a BUNDLE
// under REPO rules and failed 11/12 without anyone stating a wrong mode. An unstated mode is a configuration
// error — distinct from a content failure, and reported with its own exit code (2 vs 1).
const modeFlag = process.argv.find((a) => a.startsWith('--mode='));
const modeArg = modeFlag === undefined ? undefined : modeFlag.slice('--mode='.length);
const decided = validateContext({ mode: modeArg, inputRoot: root });
const mode = decided.mode;
setStatus('VALIDATION_MODE_GATE', decided.gate,
  decided.diagnostics.map((d) => `${d.code}: ${d.message}`).join(' | '));
if (decided.gate === 'FAIL' && decided.mode === null) {
  // An invalid mode is a caller error, not a finding about the portfolio. Refuse to render a verdict at all:
  // continuing would publish gate results computed under no policy.
  console.error(`VALIDATION_MODE_GATE: FAIL — ${decided.diagnostics.map((d) => d.message).join(' ')}`);
  console.error(`usage: validate-portfolio.mjs [--mode=${MODES.join('|')}]`);
  process.exit(2);
}

if (mode === 'bundle') setStatus('IN_REPO_PACKAGE_GATE', 'NOT_APPLICABLE',
  `running in bundle mode: delivery packaging is a repo-side concern and DELIVERY_MANIFEST.json is deliberately not bundled. The equivalent check here is validate-bundle.mjs (BUNDLE_ROLE / DELIVERY_INTEGRITY / SOURCE_COMPLETENESS / CLEAN_ROOM_CONTAINMENT / RUNBOOK), which recomputes every artifact from disk. Mode: stated explicitly by the entrypoint, never detected from the tree.`);
else if (!delivery) setStatus('IN_REPO_PACKAGE_GATE', 'FAIL',
  `DELIVERY_MANIFEST.json missing in repo mode — packaging is unverifiable. Mode: stated explicitly by the entrypoint, never detected from the tree. Deleting the manifest (with or without the generator, with or without a planted BUNDLE_MANIFEST.json) cannot downgrade this gate: the mode is not chosen by the bytes under evaluation.`);
else {
  const sumsText = existsSync(paths.sums) ? readFileSync(paths.sums, 'utf8') : '';
  const sumLines = sumsText.split('\n').filter((l) => /^[0-9a-f]{64}\s{2}/.test(l));
  const sumMap = new Map(sumLines.map((l) => { const [h, ...rest] = l.split(/\s{2}/); return [rest.join('  ').trim(), h]; }));
  gate('IN_REPO_PACKAGE_GATE', sumLines.length > 0, 'SHA256SUMS.txt is empty / has no valid hash lines');
  for (const f of (delivery.artifacts || [])) {
    // HISTORICAL_EVIDENCE is hash-checked too: it must not be a current fact, but it must not be tampered with.
    if (f.role !== 'hashed' && f.role !== 'HISTORICAL_EVIDENCE') continue;
    const abs = rel(f.path);
    gate('IN_REPO_PACKAGE_GATE', existsSync(abs), `delivery: required artifact missing: ${f.path}`);
    if (!existsSync(abs)) continue;
    gate('IN_REPO_PACKAGE_GATE', statSync(abs).size > 0, `delivery: zero-byte artifact: ${f.path}`);
    const disk = createHash('sha256').update(readFileSync(abs)).digest('hex');
    gate('IN_REPO_PACKAGE_GATE', sumMap.get(f.path) === disk, `delivery: SHA256SUMS hash missing/mismatch for ${f.path}`);
  }
  for (const [p] of sumMap) gate('IN_REPO_PACKAGE_GATE', existsSync(rel(p)), `delivery: SHA256SUMS lists a hash for a missing file: ${p}`);
  gate('IN_REPO_PACKAGE_GATE', delivery.ledgerHashes?.fullFile && delivery.ledgerHashes?.selfExcluding && delivery.ledgerHashes.fullFile !== delivery.ledgerHashes.selfExcluding,
    'delivery: ledger full-file and self-excluding hashes must be present and distinct');
  if (manifest) for (const s of (manifest.entries || manifest.sources || [])) {
    if (s.role === 'HISTORICAL_EVIDENCE') continue;
    gate('IN_REPO_PACKAGE_GATE', s.exists !== false && s.status !== 'MISSING', `manifest source not present: ${s.path}`);
  }
}
if (gates.GENERATED_VIEW_GATE?.status === 'FAIL') gate('IN_REPO_PACKAGE_GATE', false, 'generated MD not byte-exact');

// ---------- unchanged external / lock gates ----------
setStatus('EXTERNAL_HANDOFF_AVAILABILITY_GATE', 'PENDING', 'external artifact availability unverifiable from this session (MB-PORTFOLIO-0004 OPEN)');
const lockRes = readJson(paths.lockres);
if (!lockRes) setStatus('LEDGER_LOCK_SAFETY_GATE', 'FAIL', 'LOCK_SAFETY_RESULT.json missing (run scripts/portfolio/lock-tests.mjs)');
else gate('LEDGER_LOCK_SAFETY_GATE', lockRes.pass === true, `lock safety tests not passing (${lockRes.fails} fails)`);
setStatus('LEDGER_LOCK_ENFORCEMENT_GATE', 'PARTIAL', 'lock available + race-proven, but not enforced on the Edit-tool / other-session write path (MB-LEDGER-0001)');

// ---------- honest completeness gates (these SHOULD be red; they measure the real gap) ----------
if (fresh) {
  const m = computeAll(fresh.slots);
  gate('SLOT_ASSIGNMENT_COMPLETENESS_GATE', m.targetUnassigned === 0, `${m.targetUnassigned} slots have no assignment`);
  gate('STATIC_COMPONENTS_COMPLETENESS_GATE', m.staticComponentsUnresolved === 0, `${m.staticComponentsUnresolved} code-assigned slots have at least one unresolved component axis`);
  gate('EXERCISE_ROUTE_COMPLETENESS_GATE', m.exerciseRouteVerified === rows.length, `${rows.length - m.exerciseRouteVerified}/${rows.length} slots have no proven per-exercise route`);
  gate('TARGET_LAUNCH_READINESS_GATE', m.trainingReady === rows.length, `TRAINING_READY ${m.trainingReady}/${rows.length} (CAT-005 requires ${rows.length}/${rows.length})`);
}

// ---------- report ----------
const ORDER = ['VALIDATION_MODE_GATE', 'PORTFOLIO_ACTIVE_SCHEMA_GATE', 'ROW_MEMBERSHIP_GATE', 'LEGACY_ISOLATION_GATE', 'TARGET_SLOT_SEMANTICS_GATE', 'STATE_INVARIANTS_GATE',
  'DERIVED_MODEL_FRESHNESS_GATE', 'MODEL_METRIC_CONSISTENCY_GATE', 'COUNT_RECONCILIATION_GATE', 'FORMULA_TRUTH_GATE',
  'RUNTIME_PLAYABILITY_GATE', 'TRAINING_READY_DERIVATION_GATE', 'GENERATED_VIEW_GATE', 'MD_PUBLISHED_NUMBERS_GATE',
  'LEXICON_HONESTY_GATE', 'IN_REPO_PACKAGE_GATE', 'EXTERNAL_HANDOFF_AVAILABILITY_GATE', 'LEDGER_LOCK_SAFETY_GATE',
  'LEDGER_LOCK_ENFORCEMENT_GATE', 'SLOT_ASSIGNMENT_COMPLETENESS_GATE', 'STATIC_COMPONENTS_COMPLETENESS_GATE',
  'EXERCISE_ROUTE_COMPLETENESS_GATE', 'TARGET_LAUNCH_READINESS_GATE'];
// VALIDATION_MODE_GATE is BLOCKING: a mode contradiction (claiming bundle over an open tree) must fail the
// build, not merely annotate it. A gate absent from BLOCKING can never fail anything — that was the
// ROW_MEMBERSHIP_GATE defect caught in Phase 4-R1, and it is not repeated here.
const BLOCKING = ['VALIDATION_MODE_GATE', 'PORTFOLIO_ACTIVE_SCHEMA_GATE', 'ROW_MEMBERSHIP_GATE', 'LEGACY_ISOLATION_GATE', 'TARGET_SLOT_SEMANTICS_GATE', 'STATE_INVARIANTS_GATE',
  'DERIVED_MODEL_FRESHNESS_GATE', 'MODEL_METRIC_CONSISTENCY_GATE', 'COUNT_RECONCILIATION_GATE', 'FORMULA_TRUTH_GATE',
  'TRAINING_READY_DERIVATION_GATE', 'GENERATED_VIEW_GATE', 'MD_PUBLISHED_NUMBERS_GATE', 'LEXICON_HONESTY_GATE',
  'IN_REPO_PACKAGE_GATE', 'LEDGER_LOCK_SAFETY_GATE'];
for (const g of ORDER) gates[g] ??= { status: 'PASS', diagnostics: [] };
const blockingFail = BLOCKING.some((g) => gates[g].status === 'FAIL');

if (process.argv.includes('--json')) { console.log(JSON.stringify({ gates, blockingFail }, null, 2)); process.exit(blockingFail ? 1 : 0); }
for (const g of ORDER) { console.log(`${g}: ${gates[g].status}`); for (const d of gates[g].diagnostics.slice(0, 6)) console.log(`    - ${d}`); if (gates[g].diagnostics.length > 6) console.log(`    … ${gates[g].diagnostics.length - 6} more`); }
console.log(blockingFail ? 'BLOCKING GATES: FAIL' : 'BLOCKING GATES: PASS');
process.exit(blockingFail ? 1 : 0);
