#!/usr/bin/env node
// CONSERVATIVE DERIVATION (V2d Phase 2 + Phase 3 corrections C2/C3/C7). Emits PORTFOLIO_DERIVED_MODEL.json.
//
// Joins the authorities WITHOUT letting any of them self-attest:
//   editorial (CSV)                 → which 121 target slots exist; code/design assignments
//   code-observation (JSON)         → per-axis semantic-binding evidence for each declared code id
//   runtime/QA (JSON)               → executed runs ONLY; never derived from static presence
//
// C2 — target assignment states are ontologically distinct and are never collapsed:
//   CODE_ASSIGNED_STATIC_VERIFIED    slot has a codeExerciseId AND its static components are all present
//   CODE_ASSIGNED_STATIC_UNRESOLVED  slot has a codeExerciseId but some component axis is UNRESOLVED
//   DESIGN_ONLY_ASSIGNED             slot has a designExerciseId and NO codeExerciseId
//   TARGET_UNASSIGNED                slot has neither
// C3 — exerciseRoute is reported SEPARATELY and is never folded into the aggregate. blockReachability is
//      context only. There is no LIVE_REACHABLE and no per-exercise reachability claim anywhere.
// C7 — runtime comes from PORTFOLIO_RUNTIME_EVIDENCE.json. Absent/empty ⇒ NOT_RUN by absence of a valid run.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { ADAPTERS, CONTEXT_AXES } from './block-adapters.mjs';
import { buildSlot, deriveReadiness, validateSlot, FAMILIES } from './portfolio-state-model.mjs';
import { METRICS, computeAll, perBlock, formulaTruthViolations } from './metrics.mjs';
import { RETIRED_COLUMNS } from './migrate-legacy-columns.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const S = { PRESENT: 'VERIFIED_PRESENT', ABSENT: 'VERIFIED_ABSENT', UNRESOLVED: 'UNRESOLVED', NA: 'NOT_APPLICABLE' };

// Aggregate = option A: only COMPONENTS actually observed. exerciseRoute is deliberately excluded.
export const COMPONENT_AXES = ['declaration', 'mechanic', 'generator'];
export const REPORTED_SEPARATELY = ['exerciseRoute', 'renderer', ...CONTEXT_AXES];

export function parseCsv(text) {
  const lines = text.trim().split('\n');
  const hdr = lines[0].split(',');
  return lines.slice(1).map((l) => {
    const cells = []; let cur = '', q = false;
    for (let i = 0; i < l.length; i++) {
      const c = l[i];
      if (c === '"') { q = !q; continue; }
      if (c === ',' && !q) { cells.push(cur); cur = ''; continue; }
      cur += c;
    }
    cells.push(cur);
    const r = {}; hdr.forEach((h, i) => (r[h] = cells[i] ?? '')); return r;
  });
}

const code = (r) => (r.codeExerciseId || '').trim();
const design = (r) => (r.designExerciseId || '').trim();

export function derive(obs, rows, runtime) {
  const perSlot = {};
  const counts = { CODE_ASSIGNED_STATIC_VERIFIED: 0, CODE_ASSIGNED_STATIC_UNRESOLVED: 0, DESIGN_ONLY_ASSIGNED: 0, TARGET_UNASSIGNED: 0 };
  for (const r of rows) {
    const slot = r.slotId, c = code(r), d = design(r);
    const base = { slotId: slot, block: r.block, codeExerciseId: c || null, designExerciseId: d || null };
    if (!c && !d) { perSlot[slot] = { ...base, state: 'TARGET_UNASSIGNED', provenance: 'editorial authority declares neither a code nor a design assignment' }; counts.TARGET_UNASSIGNED++; continue; }
    if (!c) { perSlot[slot] = { ...base, state: 'DESIGN_ONLY_ASSIGNED', provenance: 'editorial authority declares a design assignment and no code assignment; no code observation is possible or expected' }; counts.DESIGN_ONLY_ASSIGNED++; continue; }
    const o = obs.perId[c];
    if (!o) {
      perSlot[slot] = { ...base, state: 'CODE_ASSIGNED_STATIC_UNRESOLVED', componentAxes: {}, separateAxes: {},
        provenance: 'the editorial authority claims a codeExerciseId the code-observation authority cannot see' };
      counts.CODE_ASSIGNED_STATIC_UNRESOLVED++; continue;
    }
    const componentAxes = Object.fromEntries(COMPONENT_AXES.map((a) => [a, o.axes[a]?.state ?? S.UNRESOLVED]));
    const separateAxes = Object.fromEntries(REPORTED_SEPARATELY.map((a) => [a, o.axes[a]?.state ?? S.UNRESOLVED]));
    const considered = COMPONENT_AXES.filter((a) => componentAxes[a] !== S.NA);
    const allPresent = considered.length > 0 && considered.every((a) => componentAxes[a] === S.PRESENT);
    const state = allPresent ? 'CODE_ASSIGNED_STATIC_VERIFIED' : 'CODE_ASSIGNED_STATIC_UNRESOLVED';
    counts[state]++;
    perSlot[slot] = { ...base, state, componentAxes, separateAxes,
      blockingAxes: considered.filter((a) => componentAxes[a] !== S.PRESENT),
      provenance: `component axes [${considered.join(', ')}] of ${c}; exerciseRoute/renderer/blockReachability reported separately and NOT folded in`,
      meaning: 'static components observed. This is NOT reachability, NOT playability and NOT a runtime claim.' };
  }
  return { perSlot, counts };
}

// Invariants: the model must be arithmetically honest or it is not published.
export function checkInvariants(model, rows, aux, obs) {
  const v = [];
  const total = Object.values(model.counts).reduce((a, b) => a + b, 0);
  if (total !== 121) v.push(`target states sum to ${total}, expected exactly 121`);
  if (rows.length !== 121) v.push(`editorial authority yields ${rows.length} target slots, expected 121`);
  for (const [slot, s] of Object.entries(model.perSlot)) {
    if (s.state === 'DESIGN_ONLY_ASSIGNED' && !(s.designExerciseId && !s.codeExerciseId)) v.push(`${slot}: DESIGN_ONLY_ASSIGNED requires a designExerciseId and no codeExerciseId`);
    if (s.state === 'TARGET_UNASSIGNED' && (s.designExerciseId || s.codeExerciseId)) v.push(`${slot}: TARGET_UNASSIGNED requires neither assignment`);
    if (s.state.startsWith('CODE_ASSIGNED') && !s.codeExerciseId) v.push(`${slot}: CODE_ASSIGNED_* requires a codeExerciseId`);
  }
  const codeAssigned = model.counts.CODE_ASSIGNED_STATIC_VERIFIED + model.counts.CODE_ASSIGNED_STATIC_UNRESOLVED;
  const assignedCandidates = codeAssigned + model.counts.DESIGN_ONLY_ASSIGNED;
  const editorialAssigned = rows.filter((r) => r.definitionStatus !== 'UNASSIGNED').length;
  if (assignedCandidates !== editorialAssigned) v.push(`assigned candidates ${assignedCandidates} (code ${codeAssigned} + design-only ${model.counts.DESIGN_ONLY_ASSIGNED}) does not reconcile with the editorial definitionStatus!=UNASSIGNED count ${editorialAssigned}`);
  // auxiliaries must never enter the denominator
  const auxIds = new Set(aux.map((a) => (a.codeExerciseId || '').trim()).filter(Boolean));
  for (const [slot, s] of Object.entries(model.perSlot)) if (s.codeExerciseId && auxIds.has(s.codeExerciseId) ) v.push(`${slot}: auxiliary code id ${s.codeExerciseId} occupies a target slot`);
  // no static axis may claim runtime
  if (JSON.stringify(obs).includes('"runtime"')) v.push('code-observation authority must not carry a runtime field');
  return v;
}

export function blockTable(obs) {
  return Object.entries(ADAPTERS).map(([k, a]) => {
    const ids = obs.blocks[k].declaredIds;
    const ok = (id, ax) => [S.PRESENT, S.NA].includes(obs.perId[id]?.axes[ax]?.state);
    const verified = ids.filter((id) => COMPONENT_AXES.every((ax) => ok(id, ax)));
    return { block: a.label, key: k, strategies: obs.blocks[k].strategies,
      declaredIds: ids.length, componentVerified: verified.length, componentUnresolved: ids.length - verified.length,
      exerciseRouteVerified: ids.filter((id) => obs.perId[id]?.axes.exerciseRoute?.state === S.PRESENT).length,
      blockReachability: ids.length ? (obs.perId[ids[0]]?.axes.blockReachability?.state ?? 'UNRESOLVED') : 'n/a',
      missingSourceFiles: obs.blocks[k].missingSourceFiles, limitations: a.limitations };
  });
}

// ---------------------------------------------------------------- PHASE 4 assembly
// The CLI now emits the ORTHOGONAL model. `derive`/`checkInvariants` above are unchanged and remain the
// Phase 2/3 contract their suites import. The v4 `counts` are COMPUTED FROM the orthogonal slots via the
// metric registry — one computation, not a parallel formula that could drift.
export function buildModel({ obs, rows, aux, runtime }) {
  const slots = rows.map((row) => {
    const s = buildSlot(row, obs, runtime);
    s.readiness = deriveReadiness(s);
    return s;
  });
  const ctx = {
    // The retired-column list comes from CODE, never from the historical artifact. Reading it out of
    // PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json would make an active decision depend on a HISTORICAL file.
    retiredColumns: RETIRED_COLUMNS,
    auxCodeIds: new Set(aux.map((a) => (a.codeExerciseId || '').trim()).filter(Boolean)),
    observedIds: new Set(Object.keys(obs.perId)),
    runtime,
  };
  const diagnostics = slots.flatMap((s) => validateSlot(s, rows.find((r) => r.slotId === s.slotId), ctx));
  const metrics = computeAll(slots);
  return { slots, diagnostics, metrics, ctx };
}

if (process.argv[1]?.endsWith('derive-portfolio-model.mjs')) {
  const obs = JSON.parse(readFileSync(join(root, 'PORTFOLIO_CODE_OBSERVATIONS.json'), 'utf8'));
  const all = parseCsv(readFileSync(join(root, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8'));
  const rows = all; // P4-R1: the CSV IS the target authority — every physical row is a target slot and is validated
  const aux = existsSync(join(root, 'MINDBOOST_EXERCISE_AUXILIARIES.csv')) ? parseCsv(readFileSync(join(root, 'MINDBOOST_EXERCISE_AUXILIARIES.csv'), 'utf8')) : [];
  const rtPath = join(root, 'PORTFOLIO_RUNTIME_EVIDENCE.json');
  const runtime = existsSync(rtPath) ? JSON.parse(readFileSync(rtPath, 'utf8')) : null;
  const { slots, diagnostics, metrics } = buildModel({ obs, rows, aux, runtime });
  const legacyModel = { counts: derive(obs, rows, runtime).counts, perSlot: {} };
  const violations = [
    ...checkInvariants({ ...legacyModel, perSlot: derive(obs, rows, runtime).perSlot }, rows, aux, obs),
    ...formulaTruthViolations(),
  ];
  const BLOCKS = ['Enfoque', 'Velocidad', 'Logica', 'Lenguaje', 'Memoria', 'Decisiones', 'Emociones', 'Creatividad'];

  // Reconciliation is recomputed from the registry, then arithmetically checked. It is never asserted.
  const counts = {
    CODE_ASSIGNED_STATIC_VERIFIED: metrics.staticComponentsVerified,
    CODE_ASSIGNED_STATIC_UNRESOLVED: metrics.staticComponentsUnresolved,
    DESIGN_ONLY_ASSIGNED: metrics.designOnlyAssigned,
    TARGET_UNASSIGNED: metrics.targetUnassigned,
  };
  const sum = Object.values(counts).reduce((a, b) => a + b, 0);
  if (sum !== rows.length) violations.push(`orthogonal states sum to ${sum}, expected ${rows.length}`);
  if (metrics.codeAssigned !== counts.CODE_ASSIGNED_STATIC_VERIFIED + counts.CODE_ASSIGNED_STATIC_UNRESOLVED) violations.push('codeAssigned does not reconcile with staticComponents verified+unresolved');

  const out = {
    artifact: 'MindBoost portfolio DERIVED MODEL (V2d Phase 4 — orthogonal, non-shadowing)', schemaVersion: '4.0.0',
    authorityNote: 'A VIEW over editorial + code-observation + runtime + scientific authorities. Introduces no fact its inputs do not carry, is never hand-edited, and is not itself an authority.',
    legacyAuthorityRetired: {
      statement: 'The V2c status columns no longer exist in the active CSV and no active tool reads them.',
      historicalArtifact: 'PORTFOLIO_V2C_LEGACY_CLAIMS.csv (role=HISTORICAL_EVIDENCE, disclaimer=HISTORICAL_UNVERIFIED_CLAIMS)',
      retiredColumns: RETIRED_COLUMNS,
      note: 'There is no LIVE_REACHABLE, no implementationStatus and no mechanicImplemented anywhere in this model. The published "43 live / 44 mechanic" numbers are not expressible.',
    },
    orthogonality: {
      statement: 'No monolithic implementationStatus. Each question has its own axis and its own authority.',
      axes: ['assignment', 'staticComponents{declaration,mechanic,generator}', 'renderer', 'exerciseRoute', 'blockReachability', 'runtime', 'platform{android,ios}', 'qa{11 axes}', 'scientificDistinctness', 'measurement', 'readiness'],
      staticIsNot: 'STATIC_COMPONENTS_PRESENT does not mean reachable, playable, tested, measurable or TRAINING_READY.',
      routeRule: 'blockReachability NEVER satisfies exerciseRoute.',
    },
    nonShadowing: {
      statement: 'Every invariant family is evaluated against every row. A row with five defects yields five diagnostics.',
      families: Object.keys(FAMILIES),
      replaces: 'the V2c if/else-if chain, which evaluated exactly one category per row (reproduced as F4-2, F4-3, F4-7)',
    },
    adaptersVersion: obs.adaptersVersion, scannerVersion: obs.scannerVersion,
    codeObservationSourceCount: Object.keys(obs.sourceManifest).length,
    targetSlots: rows.length,
    counts,
    metrics,
    metricRegistry: METRICS.map((m) => ({ metricId: m.metricId, label: m.label, formula: m.formula, semantic: m.semantic })),
    reconciliation: {
      targetSlots: rows.length,
      codeAssigned: metrics.codeAssigned,
      designOnlyAssigned: metrics.designOnlyAssigned,
      assignedCandidates: metrics.assignedCandidates,
      trulyUnassigned: metrics.targetUnassigned,
      observedCodeIds: Object.keys(obs.perId).length,
      auxiliariesExcludedFromDenominator: aux.length,
      identity: `${counts.CODE_ASSIGNED_STATIC_VERIFIED} + ${counts.CODE_ASSIGNED_STATIC_UNRESOLVED} + ${counts.DESIGN_ONLY_ASSIGNED} + ${counts.TARGET_UNASSIGNED} = ${sum}`,
    },
    invariantViolations: violations,
    diagnostics,
    runtime: {
      state: metrics.runtimeVerified ? 'SEE_RUNS' : 'NOT_RUN',
      authority: runtime ? 'PORTFOLIO_RUNTIME_EVIDENCE.json' : null,
      schemaVersion: runtime?.schemaVersion ?? null,
      validRuns: (runtime?.runs ?? []).length,
      verifiedSlots: metrics.runtimeVerified,
      provenance: metrics.runtimeVerified
        ? 'derived per-slot from executed runs in the runtime authority'
        : 'NOT_RUN by ABSENCE OF A VALID RUN — derived from the runtime authority, never hard-coded and never inferred from static presence',
    },
    trainingReady: { count: metrics.trainingReady, of: rows.length,
      provenance: 'DERIVED from the readiness rule over every axis. Never read from any authority file.',
      rule: deriveReadiness(slots[0]).rule },
    blockTable: blockTable(obs),
    perBlock: perBlock(slots, BLOCKS),
    extra: slots.filter((s) => !BLOCKS.includes(s.block)).map((s) => ({ slotId: s.slotId, assignment: s.assignment.state, staticComponents: s.staticComponents.state, readiness: s.readiness.state })),
    perSlot: Object.fromEntries(slots.map((s) => [s.slotId, s])),
  };
  writeFileSync(join(root, 'PORTFOLIO_DERIVED_MODEL.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');

  console.log(`DERIVED MODEL v${out.schemaVersion}: ${rows.length} target slots (orthogonal, non-shadowing)`);
  for (const [k, n] of Object.entries(counts)) console.log(`  ${k.padEnd(32)} ${n}`);
  console.log(`  ${'—'.repeat(36)}\n  identity: ${out.reconciliation.identity}`);
  console.log(`  exerciseRoute verified ${metrics.exerciseRouteVerified} · renderer verified ${metrics.rendererVerified} · runtime verified ${metrics.runtimeVerified} · TRAINING_READY ${metrics.trainingReady}/${rows.length}`);
  console.log(`  row diagnostics: ${diagnostics.length}`);
  for (const d of diagnostics.slice(0, 8)) console.log(`    - [${d.family}] ${d.slotId}: ${d.message}`);
  console.log(`  invariant violations: ${violations.length}`); violations.forEach((x) => console.log(`    ! ${x}`));
  process.exit(violations.length || diagnostics.length ? 1 : 0);
}
