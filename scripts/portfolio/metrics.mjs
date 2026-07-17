#!/usr/bin/env node
// V2d PHASE 4 — SINGLE METRIC REGISTRY.
//
// V2c duplicated its formulas across computeCounts, the renderer, the validator and the docs. The published
// label "Mechanic implemented = live + hidden" did not describe what the code computed. Here there is exactly
// ONE definition per metric: `compute` IS the formula, `formula` is its human rendering, and `semantic` states
// what the number means and — as importantly — what it does not.
//
// FORMULA_TRUTH_GATE checks `formula` against `compute.toString()` so a function can never drift from its label.
// Every consumer (render, validator, docs) reads this registry; nothing recomputes a metric locally.

const count = (slots, p) => slots.filter(p).length;

export const METRICS = [
  { metricId: 'targetSlots', label: 'Target slots', formula: 'slots.length',
    semantic: 'The denominator: 8 blocks × 15 + EXTRA-121. Auxiliaries are never counted here.',
    compute: (slots) => slots.length },

  { metricId: 'assignedCandidates', label: 'Assigned candidates', formula: 'assignment != TARGET_UNASSIGNED',
    semantic: 'A human editor assigned this slot to a code id or a design id. Says NOTHING about whether the code exists.',
    compute: (slots) => count(slots, (s) => s.assignment.state !== 'TARGET_UNASSIGNED') },

  { metricId: 'codeAssigned', label: 'Code-assigned', formula: 'assignment == CODE_ASSIGNED',
    semantic: 'The editorial authority names a codeExerciseId for this slot. Not a claim that the code was found.',
    compute: (slots) => count(slots, (s) => s.assignment.state === 'CODE_ASSIGNED') },

  { metricId: 'staticComponentsVerified', label: 'Static components verified', formula: 'staticComponents == VERIFIED_PRESENT',
    semantic: 'Every component axis (declaration, mechanic, generator) was bound to a real semantic construct in hashed source. ' +
      'This is NOT live, NOT reachable, NOT playable, NOT implemented-and-working, NOT tested.',
    compute: (slots) => count(slots, (s) => s.staticComponents.state === 'VERIFIED_PRESENT') },

  { metricId: 'staticComponentsUnresolved', label: 'Static components unresolved', formula: 'staticComponents == UNRESOLVED',
    semantic: 'The slot names code, but at least one component axis could not be resolved by the available strategy. ' +
      'UNRESOLVED means "our strategy cannot resolve this architecture" — it is NOT evidence of absence.',
    compute: (slots) => count(slots, (s) => s.staticComponents.state === 'UNRESOLVED') },

  { metricId: 'designOnlyAssigned', label: 'Design-only assigned', formula: 'assignment == DESIGN_ONLY_ASSIGNED',
    semantic: 'A design exists and no code is claimed. No code observation is possible or expected.',
    compute: (slots) => count(slots, (s) => s.assignment.state === 'DESIGN_ONLY_ASSIGNED') },

  { metricId: 'targetUnassigned', label: 'Target unassigned', formula: 'assignment == TARGET_UNASSIGNED',
    semantic: 'The slot exists in the target of 121 and has neither a design nor code. Genuinely empty.',
    compute: (slots) => count(slots, (s) => s.assignment.state === 'TARGET_UNASSIGNED') },

  { metricId: 'exerciseRouteVerified', label: 'Exercise route verified', formula: 'exerciseRoute == VERIFIED_PRESENT',
    semantic: 'This specific exercise id was proven to be dispatched. Block reachability does NOT satisfy this axis.',
    compute: (slots) => count(slots, (s) => s.exerciseRoute.state === 'VERIFIED_PRESENT') },

  { metricId: 'rendererVerified', label: 'Renderer verified', formula: 'renderer == VERIFIED_PRESENT',
    semantic: 'A renderer construct was bound for this id. Several blocks dispatch on state, not on id, so this axis is architecturally unresolvable there.',
    compute: (slots) => count(slots, (s) => s.renderer.state === 'VERIFIED_PRESENT') },

  { metricId: 'runtimeVerified', label: 'Runtime verified', formula: 'runtime == VERIFIED',
    semantic: 'An executed run in the runtime authority verified this slot. Never inferred from static presence.',
    compute: (slots) => count(slots, (s) => s.runtime.state === 'VERIFIED') },

  { metricId: 'runtimeNotRun', label: 'Runtime not run', formula: 'runtime == NOT_RUN',
    semantic: 'No run exists. This is the absence of evidence, NOT a failure claim.',
    compute: (slots) => count(slots, (s) => s.runtime.state === 'NOT_RUN') },

  { metricId: 'distinctnessApproved', label: 'Distinctness approved', formula: 'scientificDistinctness == APPROVED',
    semantic: 'A human scientific review approved this exercise as distinct from its siblings.',
    compute: (slots) => count(slots, (s) => s.scientificDistinctness.state === 'APPROVED') },

  { metricId: 'measurementUnknown', label: 'Measurement unknown', formula: 'measurement == UNKNOWN',
    semantic: 'No measurement authority has ruled on this slot.',
    compute: (slots) => count(slots, (s) => s.measurement.state === 'UNKNOWN') },

  { metricId: 'trainingReady', label: 'TRAINING_READY', formula: 'readiness == TRAINING_READY',
    semantic: 'Derived only. Requires positive evidence on every axis; any UNRESOLVED, NOT_RUN, UNKNOWN or BLOCKED_PLATFORM blocks it.',
    compute: (slots) => count(slots, (s) => s.readiness.state === 'TRAINING_READY') },
];

export const BY_ID = Object.fromEntries(METRICS.map((m) => [m.metricId, m]));
export const computeAll = (slots) => Object.fromEntries(METRICS.map((m) => [m.metricId, m.compute(slots)]));

// Per-block counts come from the SAME compute functions as the globals — no parallel formulas.
export function perBlock(slots, blocks) {
  return blocks.map((b) => {
    const rows = slots.filter((s) => s.block === b);
    return { block: b, slots: rows.length, ...computeAll(rows) };
  });
}

// FORMULA_TRUTH_GATE: the printed formula must describe the executed function. A mutation to either side
// without the other is a red gate. Checks that each identifier the label names is actually referenced.
export function formulaTruthViolations() {
  const v = [];
  for (const m of METRICS) {
    const src = m.compute.toString();
    const tokens = (m.formula.match(/[A-Za-z_][A-Za-z0-9_.]*/g) || []).filter((t) => !['length', 'slots'].includes(t));
    for (const t of tokens) {
      const leaf = t.split('.').pop();
      if (!src.includes(leaf)) v.push(`${m.metricId}: formula "${m.formula}" names "${t}" but compute() never references "${leaf}" — label and function have drifted`);
    }
    if (!m.semantic || m.semantic.length < 20) v.push(`${m.metricId}: missing a semantic description`);
  }
  return v;
}
