// V2d PHASE 4 — renderer over the DERIVED MODEL.
//
// V2c rendered straight from the legacy CSV rows, so a hand edit to `implementationStatus` moved the
// published number (F4-1) and the byte-exact gate compared the legacy view against itself (F4-5).
// This renderer accepts ONLY the derived model. The legacy columns are not reachable from here: they are
// not in the model, not in the active CSV, and not in this file's inputs.
//
// Rules enforced by construction:
//   * every number comes from the metric registry (metrics.mjs) — no local formula, no stored summary;
//   * EXTRA-121 is derived from its real row, never printed as a literal (F4-6);
//   * per-block uses the SAME compute functions as the globals;
//   * the words "live", "implemented" and "playable" are never applied to statically-observed components.
import { BY_ID, computeAll, perBlock } from './metrics.mjs';

export const BLOCKS = ['Enfoque', 'Velocidad', 'Logica', 'Lenguaje', 'Memoria', 'Decisiones', 'Emociones', 'Creatividad'];

// Words that would misdescribe a static observation. LEXICON_HONESTY_GATE asserts none of these is applied
// to the staticComponentsVerified figure.
export const FORBIDDEN_STATIC_WORDS = ['live-reachable', 'live reachable', 'playable', 'mechanic-implemented'];

const HEADLINE = [
  'targetSlots', 'assignedCandidates', 'codeAssigned', 'staticComponentsVerified', 'staticComponentsUnresolved',
  'designOnlyAssigned', 'targetUnassigned', 'exerciseRouteVerified', 'rendererVerified', 'runtimeVerified',
  'distinctnessApproved', 'trainingReady',
];

export function renderPortfolio(model, aux) {
  const slots = Object.values(model.perSlot ?? {});
  if (!slots.length) throw new Error('renderPortfolio: derived model carries no slots — refusing to render an empty view');
  if (!model.metrics) throw new Error('renderPortfolio: derived model carries no metric block');
  // Recompute from rows. The model's stored `metrics` summary is NOT trusted for rendering; the validator's
  // MODEL_METRIC_CONSISTENCY_GATE compares the two so a stale summary fails instead of being published.
  const m = computeAll(slots);
  const total = m.targetSlots;
  const base = slots.filter((s) => BLOCKS.includes(s.block));
  const extra = slots.filter((s) => !BLOCKS.includes(s.block));
  const extraBlocks = [...new Set(extra.map((s) => s.block))];

  const L = [];
  L.push('# MindBoost Exercise Portfolio — canonical view (GENERATED from the derived model)');
  L.push('');
  L.push('> **GENERATED — do NOT hand-edit.** Regenerate with `node scripts/portfolio/build-portfolio.mjs`,');
  L.push('> then `node scripts/portfolio/validate-portfolio.mjs`. A byte-exact gate guards this file.');
  L.push('>');
  L.push('> **What this view is.** Every number below is derived from independent authorities:');
  L.push('> editorial (`MINDBOOST_EXERCISE_PORTFOLIO.csv` — which slots exist and what they are assigned to),');
  L.push('> code-observation (`PORTFOLIO_CODE_OBSERVATIONS.json` — per-axis semantic bindings in hashed source),');
  L.push('> runtime/QA (`PORTFOLIO_RUNTIME_EVIDENCE.json` — executed runs only), and scientific review.');
  L.push('> No authority may attest another, and this view introduces no fact its inputs do not carry.');
  L.push('>');
  L.push('> **What "static components verified" does NOT mean.** It means the declaration, mechanic and generator');
  L.push('> were each bound to a real semantic construct in hashed source. It does **not** mean the exercise is');
  L.push('> reachable, playable, tested, measurable or ready to ship. Those are separate axes, each reported below.');
  L.push('>');
  L.push('> **UNRESOLVED is not absence.** It means the available strategy cannot resolve that architecture.');
  L.push('>');
  L.push('> **Retired.** The V2c status columns (`implementationStatus`, `mechanicImplemented`, `launchReadiness`, …)');
  L.push('> were moved to `PORTFOLIO_V2C_LEGACY_CLAIMS.csv` (`HISTORICAL_UNVERIFIED_CLAIMS`). They govern nothing:');
  L.push('> the numbers they produced are no longer expressible by this pipeline.');
  L.push('');
  L.push(`## Metrics (/${total} target slots)`);
  L.push('');
  L.push('| Metric | Value | Formula | Means |');
  L.push('|---|---|---|---|');
  for (const id of HEADLINE) {
    const def = BY_ID[id];
    const val = id === 'targetSlots' ? `${m[id]}` : `${m[id]}/${total}`;
    L.push(`| ${def.label} | ${val} | \`${def.formula}\` | ${def.semantic} |`);
  }
  L.push('');
  L.push('## Per-block');
  L.push('');
  L.push('| Block | Slots | Assigned | Code-assigned | Static verified | Static unresolved | Design-only | Unassigned | Route verified | Runtime verified | TRAINING_READY |');
  L.push('|---|---|---|---|---|---|---|---|---|---|---|');
  const row = (label, b) => `| ${label} | ${b.targetSlots} | ${b.assignedCandidates} | ${b.codeAssigned} | ${b.staticComponentsVerified} | ${b.staticComponentsUnresolved} | ${b.designOnlyAssigned} | ${b.targetUnassigned} | ${b.exerciseRouteVerified} | ${b.runtimeVerified} | ${b.trainingReady} |`;
  for (const b of perBlock(base, BLOCKS)) L.push(row(b.block, b));
  for (const b of perBlock(extra, extraBlocks)) L.push(row(`${b.block} (extra)`, b));
  L.push(`| **Total** | **${total}** | **${m.assignedCandidates}** | **${m.codeAssigned}** | **${m.staticComponentsVerified}** | **${m.staticComponentsUnresolved}** | **${m.designOnlyAssigned}** | **${m.targetUnassigned}** | **${m.exerciseRouteVerified}** | **${m.runtimeVerified}** | **${m.trainingReady}** |`);
  L.push('');
  L.push('## Runtime');
  L.push('');
  L.push(`Authority: \`${model.runtime.authority ?? 'ABSENT'}\` · state: **${model.runtime.state}** · runs recorded: ${model.runtime.validRuns} · slots runtime-verified: ${model.runtime.verifiedSlots}`);
  L.push('');
  L.push(model.runtime.provenance);
  L.push('');
  L.push('## TRAINING_READY');
  L.push('');
  L.push(`**${m.trainingReady}/${total}.** ${model.trainingReady.provenance}`);
  L.push('');
  L.push(model.trainingReady.rule);
  L.push('');
  L.push(`## Auxiliaries (${aux.length}, excluded from the ${total})`);
  L.push('');
  L.push('| auxId | kind | codeExerciseId | distinctnessReviewStatus | reasonExcluded |');
  L.push('|---|---|---|---|---|');
  for (const a of aux) L.push(`| ${a.auxId} | ${a.kind} | ${a.codeExerciseId} | ${a.distinctnessReviewStatus} | ${a.reasonExcluded} |`);
  L.push('');
  return L.join('\n') + '\n';
}
