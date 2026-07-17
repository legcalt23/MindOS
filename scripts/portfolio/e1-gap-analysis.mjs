#!/usr/bin/env node
// ENFOQUE E1 → TRAINING_READY — GAP ANALYSIS, derived from the authorities.
//
// This is the first material artifact of the E1 vertical. It asserts nothing: every line is read from the
// V2d model that Phase 7/8 just froze and verified. The point is to establish what E1 actually needs before
// touching a line of production code — the V2c failure mode was declaring readiness from intention.
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { deriveReadiness, deriveMeasurementEligibility, QA_AXES, PLATFORM_AXES } from './portfolio-state-model.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const model = JSON.parse(readFileSync(rel('PORTFOLIO_DERIVED_MODEL.json'), 'utf8'));
const obs = JSON.parse(readFileSync(rel('PORTFOLIO_CODE_OBSERVATIONS.json'), 'utf8'));

const SLOT = 'FOCUS-01';
const s = model.perSlot[SLOT];
if (!s) { console.error(`${SLOT} not in the derived model`); process.exit(1); }
const readiness = deriveReadiness(s);
const measurement = deriveMeasurementEligibility(s);
const o = s.codeExerciseId ? obs.perId[s.codeExerciseId] : null;

// Each blocking axis, with WHO owns it and what would actually close it. No axis is closed by writing code
// alone: runtime needs an executed run, distinctness needs a human review, measurement needs a decision.
const OWNERS = {
  assignment: { authority: 'editorial', closes: 'already CODE_ASSIGNED' },
  staticComponents: { authority: 'code-observation', closes: 'every component axis (declaration/mechanic/generator) bound to a real semantic construct in hashed source' },
  renderer: { authority: 'code-observation', closes: 'a renderer construct bound to THIS id — FOCUS dispatches on FocusFieldState, not on id, so this is architecturally unresolvable by token probing (MB-PORTFOLIO-0013); needs call-graph resolution through FocusSessionStateHolder' },
  exerciseRoute: { authority: 'code-observation', closes: 'proof that THIS id is dispatched; blockReachability does not substitute (C3)' },
  runtime: { authority: 'runtime-qa', closes: 'an executed run recorded in PORTFOLIO_RUNTIME_EVIDENCE.json with slotId, codeExerciseId, platform, snapshotSha256, testOrDossier, result, provenance, executedAtUtc and an evidencePath — a run cannot be written by code, only executed' },
  platform: { authority: 'runtime-qa', closes: 'Android AND iOS both PASS. android=NOT_OBSERVED (no run recorded — schedulable here). ios=BLOCKED_EXTERNAL, not FAIL: the check is well-defined and the artifact is specified, but it needs a macOS host this environment lacks. BLOCKED_EXTERNAL is a statement about this environment, never about the exercise.' },
  qa: { authority: 'runtime-qa', closes: 'each of the 11 QA axes PASS, each requiring an executed run. All are currently NOT_OBSERVED — no evidence either way. NOT_OBSERVED is never rendered as FAIL.' },
  scientificDistinctness: { authority: 'scientific-design', closes: 'a review approving E1 as distinct from its siblings on mechanism/subskill/strategy — resolvable by evidence + the efficacy-guardian and training-gameplay-lab agents, NOT a founder decision (MB-V2D-R11). Cannot be self-certified by the implementer.' },
  measurement: { authority: 'scientific-design', closes: 'a measurement-authority ruling. NOTE (MB-V2D-R09): this axis does NOT block TRAINING_READY — it governs only whether output may COUNT. UNKNOWN fixes E1 at TRAINING_ONLY: trainable, never counted.' },
};

const axes = [
  { axis: 'assignment', state: s.assignment.state, want: 'CODE_ASSIGNED' },
  { axis: 'staticComponents', state: s.staticComponents.state, want: 'VERIFIED_PRESENT', detail: `component axes: ${JSON.stringify(s.staticComponents.axes)}; blocking: [${s.staticComponents.blocking.join(', ')}]` },
  { axis: 'renderer', state: s.renderer.state, want: 'VERIFIED_PRESENT' },
  { axis: 'exerciseRoute', state: s.exerciseRoute.state, want: 'VERIFIED_PRESENT' },
  { axis: 'blockReachability', state: s.blockReachability.state, want: 'context only — never satisfies exerciseRoute' },
  { axis: 'runtime', state: s.runtime.state, want: 'VERIFIED' },
  ...PLATFORM_AXES.map((p) => ({ axis: `platform.${p}`, state: s.platform[p].state, want: 'PASS' })),
  ...QA_AXES.map((q) => ({ axis: `qa.${q}`, state: s.qa[q].state, want: 'PASS' })),
  { axis: 'scientificDistinctness', state: s.scientificDistinctness.state, want: 'APPROVED' },
  { axis: 'measurement', state: s.measurement.state, want: 'not UNKNOWN' },
];

const open = axes.filter((a) => a.state !== a.want && !/context only|not UNKNOWN/.test(a.want) || (a.axis === 'measurement' && a.state === 'UNKNOWN'));
const byAuthority = {};
for (const a of axes) {
  const key = a.axis.split('.')[0];
  const own = OWNERS[key];
  if (!own) continue;
  const blocked = readiness.blocking.some((b) => b.startsWith(a.axis));
  if (!blocked) continue;
  (byAuthority[own.authority] ??= []).push({ axis: a.axis, state: a.state, closes: own.closes });
}

const out = {
  artifact: 'ENFOQUE E1 → TRAINING_READY gap analysis', schemaVersion: '1.0.0',
  derivedFrom: { model: 'PORTFOLIO_DERIVED_MODEL.json', schemaVersion: model.schemaVersion, observations: 'PORTFOLIO_CODE_OBSERVATIONS.json' },
  note: 'Derived, not asserted. Every state below is read from the authorities Phase 7/8 froze and verified.',
  slot: SLOT, codeExerciseId: s.codeExerciseId, canonical: s.designExerciseId, block: s.block,
  currentState: { assignment: s.assignment.state, staticComponents: s.staticComponents.state, readiness: s.readiness.state },
  axes,
  readinessRule: readiness.rule,
  blockingAxes: readiness.blocking,
  blockingCount: readiness.blocking.length,
  byAuthority,
  codeObservation: o ? Object.fromEntries(Object.entries(o.axes).map(([k, v]) => [k, { state: v.state, construct: v.construct ?? null, evidencePath: v.evidencePath ?? null, reason: v.reasonCode ?? v.reason ?? null }])) : null,
  measurementEligibility: measurement,
  orthogonality: {
    rule: 'TRAINING_READY (may a user train with this?) and MEASUREMENT eligibility (may its output count?) are orthogonal — MB-V2D-R09.',
    correction: 'A previous revision made measurement=UNKNOWN a BLOCKER of TRAINING_READY. That was a false-RED: it reported E1 as unfit to TRAIN because nobody had ruled whether its numbers may COUNT. The measurement ruling now fixes E1 conservatively at TRAINING_ONLY and no longer blocks training readiness. This REMOVED one axis from the training blocker set; it did not add a claim.',
    trainingOnlyMeans: 'trainable, never counted — the profile claim is withheld (null ≠ 0, CALIBRATING). TRAINING_ONLY is NOT a defect and never means "ineligible to train".',
  },
  // MB-V2D-R09 — states are distinguished, never collapsed. "I did not observe it" is not "it is absent",
  // and "this host cannot check it" is not "it fails".
  evidenceStates: {
    FAIL: 'direct evidence of non-compliance exists',
    NOT_OBSERVED: 'no evidence either way — the observation was never made. NEVER rendered as FAIL.',
    BLOCKED_EXTERNAL: 'checkable in principle, but not from this environment (needs macOS, a device, or a human ruling)',
    PASS: 'positive evidence observed, with provenance',
  },
  honestSummary:
    `E1 is ${readiness.blocking.length} axes away from TRAINING_READY. ${(byAuthority['code-observation'] ?? []).length} are closable by writing code here. ` +
    `${(byAuthority['runtime-qa'] ?? []).length} are NOT_OBSERVED — no run exists for any of the 121 slots, so no evidence exists either way; they are not failures. ` +
    `${(byAuthority['scientific-design'] ?? []).length} need a review that cannot be self-certified. ` +
    `platform.ios is BLOCKED_EXTERNAL, not FAIL and not impossible: the check is well-defined and the artifact to run is specified, but it needs a macOS host this environment does not have. ` +
    `A previous revision called E1 "structurally impossible" — that overstated a host limitation into a property of the exercise. The accurate statement is narrower: E1 cannot reach TRAINING_READY *from this Windows host*, because one of its axes is externally blocked here. Everything else is NOT_OBSERVED pending a run, which is a schedulable act, not a wall.`,
  firstHonestStep:
    'The only E1 work that is both in-scope and closable tonight is the code-observation side: make the renderer and exerciseRoute axes resolvable for FOCUS ids. That is a TOOLING change (call-graph resolution through FocusSessionStateHolder), not a production-code change, and it is tracked as MB-PORTFOLIO-0013. Everything else is gated on a run, a device, or a human.',
};
writeFileSync(rel('E1_GAP_ANALYSIS.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');

console.log(`ENFOQUE E1 (${SLOT} · ${s.codeExerciseId}) → TRAINING_READY gap\n`);
console.log(`  assignment       ${s.assignment.state}`);
console.log(`  staticComponents ${s.staticComponents.state}  axes=${JSON.stringify(s.staticComponents.axes)}`);
console.log(`  readiness        ${s.readiness.state}  (${readiness.blocking.length} blocking axes)\n`);
for (const [auth, list] of Object.entries(byAuthority)) {
  console.log(`  ${auth} — ${list.length} axis/axes`);
  for (const a of list) console.log(`      ${a.axis.padEnd(24)} ${a.state}`);
}
console.log(`\n${out.honestSummary}\n`);
console.log(out.firstHonestStep);
