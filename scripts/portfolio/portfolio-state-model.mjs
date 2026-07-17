#!/usr/bin/env node
// V2d PHASE 4 — ORTHOGONAL, NON-SHADOWING STATE MODEL.
//
// Replaces the V2c monolithic `implementationStatus` and its if/else-if validator chain.
//
// WHY ORTHOGONAL: one scalar cannot carry "is it assigned", "do its parts exist", "can you reach it",
// "was it run", "does it pass QA", "is it scientifically distinct" and "can it ship" at once. V2c tried,
// and the result was LIVE_REACHABLE — a word that silently meant all seven. Each question now has its own
// axis with its own authority, and a row may be VERIFIED on one and UNKNOWN on the next without either
// contaminating the other.
//
// WHY NON-SHADOWING: F4-2/F4-3/F4-7 proved the else-if chain evaluates ONE category per row. A row that was
// simultaneously UNASSIGNED, LIVE_REACHABLE, mechanic-less, route-less and generator-contradicting reported
// exactly 2 diagnostics — all from the assignment family, none about "live". Fix what you are told and the
// row is still invalid. Here every family runs against every row, unconditionally, and all diagnostics are
// returned. A row with five defects produces five diagnostics.

// ---------------------------------------------------------------- closed vocabularies
// An unrecognised value is a RED GATE, never a silently-tolerated unknown.
export const ASSIGNMENT_STATES = ['CODE_ASSIGNED', 'DESIGN_ONLY_ASSIGNED', 'TARGET_UNASSIGNED'];
export const STATIC_STATES = ['VERIFIED_PRESENT', 'VERIFIED_ABSENT', 'UNRESOLVED', 'NOT_APPLICABLE'];
export const RUNTIME_STATES = ['NOT_RUN', 'VERIFIED', 'FAILED', 'BLOCKED_PLATFORM'];
export const PLATFORM_STATES = ['UNKNOWN', 'PASS', 'FAILED', 'BLOCKED_PLATFORM', 'NOT_APPLICABLE'];
export const QA_STATES = ['UNKNOWN', 'PASS', 'FAILED', 'NOT_APPLICABLE'];
export const DISTINCTNESS_STATES = ['NOT_REVIEWED', 'PROVISIONAL', 'APPROVED', 'REJECTED', 'NOT_APPLICABLE'];
export const MEASUREMENT_STATES = ['UNKNOWN', 'NOT_ELIGIBLE', 'PROFILE_ELIGIBLE', 'OFFICIAL_MEASUREMENT'];
export const READINESS_STATES = ['NOT_READY', 'TRAINING_READY'];
export const DEFINITION_STATES = ['UNASSIGNED', 'IDEA', 'DESIGNED'];

export const COMPONENT_AXES = ['declaration', 'mechanic', 'generator'];
export const QA_AXES = ['offline', 'tutorial', 'practice', 'completion', 'accessibility', 'reducedMotion', 'permissionFallback', 'difficulty', 'scoring', 'persistence', 'dossier'];
export const PLATFORM_AXES = ['android', 'ios'];

// A state that positively attests the axis. Everything else blocks readiness.
const POSITIVE = { static: 'VERIFIED_PRESENT', runtime: 'VERIFIED', platform: 'PASS', qa: 'PASS', distinctness: 'APPROVED' };

// ---------------------------------------------------------------- runtime projection (C7)
// The ONLY source of runtime facts. Absent run ⇒ NOT_RUN by absence — never by inference from static code.
export function runtimeFor(slotId, runtime) {
  const runs = (runtime?.runs ?? []).filter((r) => r.slotId === slotId);
  if (!runs.length) return { state: 'NOT_RUN', runs: [], provenance: 'no run exists for this slot in the runtime authority (NOT_RUN by absence — never inferred from static presence)' };
  const worst = runs.some((r) => r.result === 'FAILED') ? 'FAILED'
    : runs.some((r) => r.result === 'BLOCKED_PLATFORM') ? 'BLOCKED_PLATFORM'
      : runs.every((r) => r.result === 'VERIFIED') ? 'VERIFIED' : 'NOT_RUN';
  return { state: worst, runs: runs.map((r) => ({ platform: r.platform, result: r.result, testOrDossier: r.testOrDossier, snapshotSha256: r.snapshotSha256, executedAtUtc: r.executedAtUtc })),
    provenance: `derived from ${runs.length} executed run(s) in the runtime authority` };
}

// ---------------------------------------------------------------- build one orthogonal row
export function buildSlot(row, obs, runtime) {
  const code = (row.codeExerciseId || '').trim();
  const design = (row.designExerciseId || '').trim();
  const assignmentState = code ? 'CODE_ASSIGNED' : design ? 'DESIGN_ONLY_ASSIGNED' : 'TARGET_UNASSIGNED';

  const o = code ? obs.perId[code] : null;
  const axisState = (a) => (assignmentState !== 'CODE_ASSIGNED' ? 'NOT_APPLICABLE' : (o?.axes?.[a]?.state ?? 'UNRESOLVED'));
  const componentAxes = Object.fromEntries(COMPONENT_AXES.map((a) => [a, axisState(a)]));
  const considered = COMPONENT_AXES.filter((a) => componentAxes[a] !== 'NOT_APPLICABLE');
  const staticState = assignmentState !== 'CODE_ASSIGNED' ? 'NOT_APPLICABLE'
    : considered.length && considered.every((a) => componentAxes[a] === 'VERIFIED_PRESENT') ? 'VERIFIED_PRESENT'
      : 'UNRESOLVED';

  return {
    slotId: row.slotId, block: row.block,
    codeExerciseId: code || null, designExerciseId: design || null,
    definitionStatus: row.definitionStatus,
    assignment: { state: assignmentState, authority: 'editorial' },
    staticComponents: {
      state: staticState, axes: componentAxes,
      blocking: considered.filter((a) => componentAxes[a] !== 'VERIFIED_PRESENT'),
      authority: 'code-observation',
      meaning: 'static components observed in source. NOT reachable, NOT playable, NOT tested, NOT measurable.',
    },
    renderer: { state: axisState('renderer'), authority: 'code-observation' },
    exerciseRoute: { state: axisState('exerciseRoute'), authority: 'code-observation',
      meaning: 'proves THIS exercise id is dispatched. Block reachability does NOT satisfy it.' },
    blockReachability: { state: axisState('blockReachability'), authority: 'code-observation',
      meaning: 'context only: the block has a root screen and the hub references it. Identical for every id in the block, therefore never evidence about a given exercise.' },
    runtime: { ...runtimeFor(row.slotId, runtime), authority: 'runtime-qa' },
    platform: Object.fromEntries(PLATFORM_AXES.map((p) => [p, { state: 'UNKNOWN', authority: 'runtime-qa',
      provenance: 'no QA authority artifact declares this axis; UNKNOWN is not a failure claim, it is the absence of evidence' }])),
    qa: Object.fromEntries(QA_AXES.map((q) => [q, { state: 'UNKNOWN', authority: 'runtime-qa',
      provenance: 'no QA authority artifact declares this axis' }])),
    scientificDistinctness: { state: row.distinctnessReviewStatus || 'NOT_REVIEWED', authority: 'scientific-design',
      evidence: row.distinctnessEvidence || null },
    measurement: { state: 'UNKNOWN', authority: 'scientific-design',
      provenance: 'measurement eligibility requires a scoring/measurement authority decision; none exists for any slot' },
  };
}

// TRAINING_READY is DERIVED and can never be read from a file. Any absence of positive evidence blocks it.
// MB-V2D-R09 — TRAINING readiness and MEASUREMENT eligibility are ORTHOGONAL, and conflating them is a
// defect in both directions:
//   • Letting training readiness imply measurement eligibility is the V2c false-green ("it runs, so it counts").
//   • Letting an unresolved measurement ruling block training readiness is a false-RED: it reports an exercise
//     as unfit to TRAIN because nobody has yet ruled whether its numbers may COUNT. Those are different
//     questions with different authorities.
//
// "Can a user safely and usefully train with this?" is answered by code + runtime + platform + QA +
// distinctness. "May its output feed the profile / an official score?" is answered by the measurement
// authority. An exercise with measurement=UNKNOWN is TRAINING_ONLY: trainable, not measurable. That is the
// conservative state — it withholds the profile claim (rigor charter: null ≠ 0, CALIBRATING) WITHOUT
// fabricating a training defect that does not exist.
//
// Safety is not weakened by the split: qa.* and platform.* still gate training, and they are what carry
// safety, accessibility and stability. The measurement ruling governs only whether the numbers count.
export function deriveReadiness(s) {
  const blocking = [];
  if (s.assignment.state !== 'CODE_ASSIGNED') blocking.push(`assignment=${s.assignment.state} (needs CODE_ASSIGNED)`);
  if (s.staticComponents.state !== POSITIVE.static) blocking.push(`staticComponents=${s.staticComponents.state} (needs ${POSITIVE.static})`);
  if (s.exerciseRoute.state !== POSITIVE.static) blocking.push(`exerciseRoute=${s.exerciseRoute.state} (needs ${POSITIVE.static}; blockReachability does not substitute)`);
  if (s.renderer.state !== POSITIVE.static) blocking.push(`renderer=${s.renderer.state} (needs ${POSITIVE.static})`);
  if (s.runtime.state !== POSITIVE.runtime) blocking.push(`runtime=${s.runtime.state} (needs ${POSITIVE.runtime})`);
  for (const p of PLATFORM_AXES) if (s.platform[p].state !== POSITIVE.platform) blocking.push(`platform.${p}=${s.platform[p].state} (needs ${POSITIVE.platform})`);
  for (const q of QA_AXES) if (s.qa[q].state !== POSITIVE.qa) blocking.push(`qa.${q}=${s.qa[q].state} (needs ${POSITIVE.qa})`);
  if (s.scientificDistinctness.state !== POSITIVE.distinctness) blocking.push(`scientificDistinctness=${s.scientificDistinctness.state} (needs ${POSITIVE.distinctness})`);
  // measurement is deliberately NOT consulted here — see deriveMeasurementEligibility.
  return { state: blocking.length ? 'NOT_READY' : 'TRAINING_READY', blocking,
    rule: 'TRAINING_READY requires POSITIVE evidence on every TRAINING axis (assignment · staticComponents · renderer · exerciseRoute · runtime · platform.* · qa.* · scientificDistinctness). UNRESOLVED, NOT_RUN, BLOCKED_PLATFORM or missing evidence each block it. measurement is NOT a training axis (MB-V2D-R09). Derived only — never read from an authority file.' };
}

/** Orthogonal axis: may this slot's output feed the profile or an official score? Never implied by training. */
export function deriveMeasurementEligibility(s) {
  const training = deriveReadiness(s);
  const m = s.measurement.state;
  // Fail-closed: UNKNOWN is not "probably fine". It is "no ruling exists", which permits training and
  // forbids counting.
  const state = m === 'OFFICIAL_MEASUREMENT' ? 'OFFICIAL_MEASUREMENT'
    : m === 'PROFILE_ELIGIBLE' ? 'PROVISIONAL_MEASUREMENT'
      : 'TRAINING_ONLY';
  const blocking = [];
  if (m === 'UNKNOWN') blocking.push('measurement=UNKNOWN — no ruling from the measurement authority exists; the slot may be trained with, but nothing it produces may count');
  if (m === 'NOT_ELIGIBLE') blocking.push('measurement=NOT_ELIGIBLE — ruled out by the measurement authority');
  // A slot that is not even trainable cannot be measured either: measurement rides on a real, executed run.
  if (training.state !== 'TRAINING_READY') blocking.push(`training readiness is ${training.state} — measurement needs an executed, qualifying run, so it cannot outrank training`);
  return { state, blocking,
    rule: 'Orthogonal to TRAINING_READY. OFFICIAL_MEASUREMENT needs an explicit ruling; PROFILE_ELIGIBLE yields PROVISIONAL_MEASUREMENT; UNKNOWN/NOT_ELIGIBLE yield TRAINING_ONLY (trainable, never counted). TRAINING_ONLY is NOT a defect and never means "ineligible to train" — it means the profile claim is withheld (null ≠ 0).' };
}

// ---------------------------------------------------------------- invariant families
// EVERY family runs against EVERY row. No else-if. Each returns [] or a list of diagnostics.
const D = (slotId, family, msg) => ({ slotId, family, message: msg });

export const FAMILIES = {
  schema: (s, row, ctx) => {
    const d = [];
    for (const c of ctx.retiredColumns) if (c in row) d.push(D(s.slotId, 'schema', `active row carries RETIRED column "${c}" — its authority is not editorial (see PORTFOLIO_V2C_LEGACY_CLAIMS.csv)`));
    if (!DEFINITION_STATES.includes(row.definitionStatus)) d.push(D(s.slotId, 'schema', `definitionStatus="${row.definitionStatus}" not in closed enum [${DEFINITION_STATES}]`));
    if (!DISTINCTNESS_STATES.includes(s.scientificDistinctness.state)) d.push(D(s.slotId, 'schema', `distinctnessReviewStatus="${s.scientificDistinctness.state}" not in closed enum`));
    // P4-R1: `entityType`/`countsTowardTarget` are GONE from the active schema. Membership is a property of
    // the container — this CSV is by definition the target-slot authority, so every physical row in it is a
    // target slot and is validated. A row can no longer switch off its own invariants by editing a cell.
    // Any attempt to reintroduce a self-asserted discriminator is caught here AND by PORTFOLIO_ACTIVE_SCHEMA_GATE.
    for (const disc of ['entityType', 'countsTowardTarget']) {
      if (disc in row) d.push(D(s.slotId, 'schema', `row carries the retired self-asserted membership discriminator "${disc}"="${row[disc]}" — target membership comes from the container, never from the row`));
    }
    if (!s.slotId || !/^[A-Z]+-\d{2,3}$/.test(s.slotId)) d.push(D(s.slotId || '(no slotId)', 'schema', `slotId "${s.slotId}" is absent or malformed — every physical row in the target authority must be an identifiable slot`));
    // closed-vocabulary checks on the derived axes themselves
    if (!ASSIGNMENT_STATES.includes(s.assignment.state)) d.push(D(s.slotId, 'schema', `assignment="${s.assignment.state}" not in closed enum`));
    if (!STATIC_STATES.includes(s.staticComponents.state)) d.push(D(s.slotId, 'schema', `staticComponents="${s.staticComponents.state}" not in closed enum`));
    if (!RUNTIME_STATES.includes(s.runtime.state)) d.push(D(s.slotId, 'schema', `runtime="${s.runtime.state}" not in closed enum`));
    if (!MEASUREMENT_STATES.includes(s.measurement.state)) d.push(D(s.slotId, 'schema', `measurement="${s.measurement.state}" not in closed enum`));
    return d;
  },

  assignment: (s, row) => {
    const d = [];
    const code = s.codeExerciseId, design = s.designExerciseId;
    if (s.assignment.state === 'TARGET_UNASSIGNED' && (code || design)) d.push(D(s.slotId, 'assignment', 'TARGET_UNASSIGNED requires neither a code nor a design assignment'));
    if (s.assignment.state === 'DESIGN_ONLY_ASSIGNED' && code) d.push(D(s.slotId, 'assignment', 'DESIGN_ONLY_ASSIGNED must not carry a codeExerciseId'));
    if (s.assignment.state === 'CODE_ASSIGNED' && !code) d.push(D(s.slotId, 'assignment', 'CODE_ASSIGNED requires a codeExerciseId'));
    if (row.definitionStatus === 'UNASSIGNED' && (code || design)) d.push(D(s.slotId, 'assignment', `definitionStatus=UNASSIGNED but the row carries ids (code="${code ?? ''}", design="${design ?? ''}")`));
    if (row.definitionStatus !== 'UNASSIGNED' && !code && !design) d.push(D(s.slotId, 'assignment', `definitionStatus=${row.definitionStatus} but no code or design assignment exists`));
    return d;
  },

  identity: (s, row, ctx) => {
    const d = [];
    for (const [k, v] of [['canonicalExerciseId', row.canonicalExerciseId], ['codeExerciseId', row.codeExerciseId], ['designExerciseId', row.designExerciseId]]) {
      if (v && v.includes(';')) d.push(D(s.slotId, 'identity', `${k} is multi-valued ("${v}") — one slot, one id`));
      if (v && /\b[A-Za-z]*\d+\s*[-–—]\s*[A-Za-z]*\d+\b|\.\./.test(v)) d.push(D(s.slotId, 'identity', `${k} looks like a range/list ("${v}")`));
    }
    if (s.codeExerciseId && !row.canonicalExerciseId) d.push(D(s.slotId, 'identity', 'codeExerciseId present but unmapped to a canonicalExerciseId'));
    if (s.codeExerciseId && ctx.auxCodeIds.has(s.codeExerciseId)) d.push(D(s.slotId, 'identity', `auxiliary id ${s.codeExerciseId} occupies a target slot`));
    return d;
  },

  codeObservationReferentiality: (s, row, ctx) => {
    const d = [];
    if (s.assignment.state !== 'CODE_ASSIGNED') return d;
    if (!ctx.observedIds.has(s.codeExerciseId)) d.push(D(s.slotId, 'codeObservationReferentiality', `editorial authority claims codeExerciseId="${s.codeExerciseId}" that the code-observation authority never observed`));
    return d;
  },

  staticComponents: (s) => {
    const d = [];
    if (s.assignment.state === 'CODE_ASSIGNED' && s.staticComponents.state === 'VERIFIED_ABSENT') d.push(D(s.slotId, 'staticComponents', 'VERIFIED_ABSENT requires an exhaustivenessEvidenceId; no strategy qualifies (VERIFIED_ABSENT_ALLOWED_STRATEGIES: 0)'));
    if (s.assignment.state !== 'CODE_ASSIGNED' && s.staticComponents.state !== 'NOT_APPLICABLE') d.push(D(s.slotId, 'staticComponents', `a slot with assignment=${s.assignment.state} cannot carry staticComponents=${s.staticComponents.state}`));
    return d;
  },

  route: (s) => {
    const d = [];
    if (s.exerciseRoute.state === 'VERIFIED_PRESENT' && s.blockReachability.state !== 'VERIFIED_PRESENT') d.push(D(s.slotId, 'route', 'exerciseRoute=VERIFIED_PRESENT while its block is not reachable — an exercise cannot be routed inside an unreachable block'));
    return d;
  },

  runtime: (s, row, ctx) => {
    const d = [];
    for (const r of (ctx.runtime?.runs ?? []).filter((x) => x.slotId === s.slotId)) {
      for (const f of ['codeExerciseId', 'platform', 'snapshotSha256', 'testOrDossier', 'result', 'provenance', 'executedAtUtc']) {
        if (!r[f]) d.push(D(s.slotId, 'runtime', `run is missing required field "${f}" — an unbound run is not evidence`));
      }
      if (!RUNTIME_STATES.includes(r.result)) d.push(D(s.slotId, 'runtime', `run result="${r.result}" not in closed enum [${RUNTIME_STATES}]`));
      if (r.result === 'VERIFIED' && !r.evidencePath) d.push(D(s.slotId, 'runtime', 'run claims VERIFIED without an evidencePath'));
      if (r.codeExerciseId && s.codeExerciseId && r.codeExerciseId !== s.codeExerciseId) d.push(D(s.slotId, 'runtime', `run codeExerciseId="${r.codeExerciseId}" does not match the slot's "${s.codeExerciseId}"`));
    }
    if (s.runtime.state !== 'NOT_RUN' && s.assignment.state !== 'CODE_ASSIGNED') d.push(D(s.slotId, 'runtime', `runtime=${s.runtime.state} on a slot with no code assignment`));
    return d;
  },

  qa: (s) => {
    const d = [];
    for (const q of QA_AXES) if (!QA_STATES.includes(s.qa[q].state)) d.push(D(s.slotId, 'qa', `qa.${q}="${s.qa[q].state}" not in closed enum`));
    for (const p of PLATFORM_AXES) if (!PLATFORM_STATES.includes(s.platform[p].state)) d.push(D(s.slotId, 'qa', `platform.${p}="${s.platform[p].state}" not in closed enum`));
    for (const q of QA_AXES) if (s.qa[q].state === 'PASS' && s.runtime.state !== 'VERIFIED') d.push(D(s.slotId, 'qa', `qa.${q}=PASS while runtime=${s.runtime.state} — a QA pass cannot exist without a run`));
    return d;
  },

  measurement: (s) => {
    const d = [];
    if (['PROFILE_ELIGIBLE', 'OFFICIAL_MEASUREMENT'].includes(s.measurement.state) && s.runtime.state !== 'VERIFIED') d.push(D(s.slotId, 'measurement', `measurement=${s.measurement.state} while runtime=${s.runtime.state} — measurement eligibility cannot precede a run`));
    return d;
  },

  readiness: (s) => {
    const d = [];
    const derived = deriveReadiness(s);
    if (derived.state === 'TRAINING_READY' && derived.blocking.length) d.push(D(s.slotId, 'readiness', 'internal: TRAINING_READY with blocking reasons'));
    if (s.readiness && s.readiness.state !== derived.state) d.push(D(s.slotId, 'readiness', `readiness=${s.readiness.state} contradicts the derivation (${derived.state}: ${derived.blocking.slice(0, 3).join('; ')})`));
    return d;
  },
};

// Run EVERY family against EVERY row and return EVERY diagnostic. This is the anti-shadowing contract.
export function validateSlot(s, row, ctx) {
  return Object.entries(FAMILIES).flatMap(([, fn]) => fn(s, row, ctx));
}
