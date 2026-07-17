#!/usr/bin/env node
// V2d PHASE 8 — THE TWELVE-CHECK CONTRACT (MB-V2D-R3-A2 §4).
//
// Frozen BEFORE execution and never derived from what happened to run. The previous runner computed its
// verdict as `steps.filter(s => !s.ok).length === 0` over the steps it had emitted — a denominator built from
// results. An exception at check 7 therefore produced a shorter list that could still be "all passing", which
// is precisely how the clean room once printed "6/6" while skipping its own immutability proof.
//
// Here the denominator is a constant: twelve specifications, twelve results, always. A check that never ran is
// NOT_STARTED and counts against the total; it does not vanish.
export const CHECK_STATES = Object.freeze(['NOT_STARTED', 'RUNNING', 'PASS', 'FAIL', 'ABORTED', 'INFRA_ERROR']);
export const PASSING = Object.freeze(['PASS']);

export const PHASE8_CHECKS = Object.freeze([
  { checkId: 'P8-01', order: 1, blocking: true, name: 'candidate drift check',
    property: 'every artifact on disk still hashes to what the bundle manifest recorded',
    inputs: ['BUNDLE_MANIFEST.json', 'repo artifacts'], expectedSuccess: 'zero drifted artifacts',
    expectedFailureCode: 'CANDIDATE_DRIFT', dependsOn: [] },
  { checkId: 'P8-02', order: 2, blocking: true, name: 'V2c handoff still immutable',
    property: 'the historical V2c snapshot is byte-identical; it is what recovered the poisoned CSV (MB-PORTFOLIO-0018)',
    inputs: ['V2c handoff zip'], expectedSuccess: 'sha256 matches the pinned constant',
    expectedFailureCode: 'V2C_MUTATED', dependsOn: [] },
  { checkId: 'P8-03', order: 3, blocking: true, name: 'ledger validates',
    property: 'the append-only audit ledger is internally consistent in the repo',
    inputs: ['MINDBOOST_AUDIT_LEDGER.md'], expectedSuccess: 'LEDGER OK',
    expectedFailureCode: 'LEDGER_INVALID', dependsOn: [] },
  { checkId: 'P8-04', order: 4, blocking: true, name: 'container hash published outside the container',
    property: 'the container digest is published out-of-band and is NOT inside the container it describes',
    inputs: ['candidate zip', 'BUNDLE_CONTAINER_HASH.txt'], expectedSuccess: 'hash agrees and the file is absent from the zip',
    expectedFailureCode: 'CONTAINER_HASH_SELF_REFERENTIAL', dependsOn: [] },
  // MIGRATED (R3-A2): was 'extracted into a NEW empty directory'. Intent preserved and strengthened — the
  // extraction target is now a dedicated inputRoot that is validated as a closed set under an EXPLICIT bundle
  // mode and then sealed. The old check only counted files; it could not notice that the runbook went on to
  // pollute the very directory it had extracted into.
  { checkId: 'P8-05', order: 5, blocking: true, name: 'pristine extraction into a sealed inputRoot',
    property: 'inputRoot receives the container and nothing else; it validates as a closed set (mode stated, never detected) and is fingerprinted before any use',
    inputs: ['candidate zip'], expectedSuccess: 'closed-set gate PASS and a fingerprint over every extracted file',
    expectedFailureCode: 'INPUT_ROOT_NOT_CLOSED', dependsOn: ['P8-04'] },
  { checkId: 'P8-06', order: 6, blocking: true, name: 'bundle validator passes from inputRoot',
    property: 'the bundle validator recomputes every artifact from the extracted bytes',
    inputs: ['inputRoot'], expectedSuccess: 'BUNDLE GATES: PASS',
    expectedFailureCode: 'BUNDLE_GATES_FAIL', dependsOn: ['P8-05'] },
  { checkId: 'P8-07', order: 7, blocking: true, name: 'every source hash recomputed inside the clean room',
    property: 'all declared sources are recomputed from the bundled archive with no access to the repository; the archive is READ from inputRoot and extracted INTO workRoot',
    inputs: ['inputRoot/PORTFOLIO_SOURCE_ARCHIVE.mbsrc', 'inputRoot/PORTFOLIO_SOURCE_INDEX.json'],
    expectedSuccess: 'verified === declared, mismatched === 0', expectedFailureCode: 'SOURCE_HASH_MISMATCH', dependsOn: ['P8-05'] },
  { checkId: 'P8-08', order: 8, blocking: true, name: 'model and published view regenerated from the extracted sources',
    property: 'the toolchain regenerates the model and the view inside workRoot from bundled inputs only',
    inputs: ['workRoot/sources', 'inputRoot tooling'], expectedSuccess: 'observe → derive → build all exit 0',
    expectedFailureCode: 'REGENERATION_FAILED', dependsOn: ['P8-07'] },
  { checkId: 'P8-09', order: 9, blocking: true, name: 'regenerated artifacts reproduce the shipped bytes',
    property: 'generated(workRoot) is byte-identical to shipped(inputRoot). Compared ACROSS roots — never shipped-vs-shipped, and the shipped copy is never copied into workRoot to stand in for a regenerated one',
    inputs: ['workRoot outputs', 'inputRoot outputs'], expectedSuccess: 'model and view hashes match across roots',
    expectedFailureCode: 'REGENERATION_MISMATCH', dependsOn: ['P8-08'] },
  { checkId: 'P8-10', order: 10, blocking: true, name: 'portfolio validator runs from inputRoot in bundle mode',
    property: 'the portfolio validator passes against the pristine inputRoot under an EXPLICIT bundle mode. It used to be invoked with no mode at all and silently defaulted to repo, so it demanded a repo-side delivery manifest that a bundle deliberately does not carry — the 11/12 failure',
    inputs: ['inputRoot'], expectedSuccess: 'BLOCKING GATES: PASS under --mode=bundle',
    expectedFailureCode: 'PORTFOLIO_GATES_FAIL', dependsOn: ['P8-05'] },
  { checkId: 'P8-11', order: 11, blocking: true, name: 'ledger validates from inputRoot',
    property: 'the ledger is verifiable from the bundled bytes alone',
    inputs: ['inputRoot/MINDBOOST_AUDIT_LEDGER.md'], expectedSuccess: 'LEDGER OK',
    expectedFailureCode: 'LEDGER_INVALID_IN_EXTRACT', dependsOn: ['P8-05'] },
  // MIGRATED (R3-A2): was 'no artifact modified after the freeze' (repo only). Intent preserved and extended to
  // inputRoot: the freeze must prove BOTH that the repo artifacts did not move AND that the pristine extraction
  // was never written to. Observing that the happy path did not change it is not proof; the fingerprint is.
  { checkId: 'P8-12', order: 12, blocking: true, name: 'nothing modified after the freeze (repo artifacts AND inputRoot)',
    property: 'every frozen repo artifact is byte-identical after the whole run, and inputRoot fingerprint(before) === fingerprint(after)',
    inputs: ['repo artifacts', 'inputRoot'], expectedSuccess: 'zero repo drift and an identical inputRoot fingerprint',
    expectedFailureCode: 'POST_FREEZE_DRIFT', dependsOn: ['P8-09', 'P8-10', 'P8-11'] },
]);

export const EXPECTED_CHECK_COUNT = 12;

// R3-A3 — THE CONTRACT, pinned as literals.
//
// `PHASE8_CHECKS.length !== EXPECTED_CHECK_COUNT` compares two values that live in this file and move TOGETHER.
// Deleting a check and decrementing the constant satisfied it perfectly: 11/11 FREEZE_VERIFIED, every adversarial
// test still green. The denominator was defended against RUNTIME shrinkage and not at all against EDIT shrinkage.
//
// A contract that is only self-consistent is not a contract. These literals are the goalpost: to shrink the
// registry you must now also edit a hard-coded 12 and a hard-coded id list, which is a visible, reviewable diff
// rather than a decrement that hides in plain sight.
export const CONTRACT_CHECK_COUNT = 12;
export const CONTRACT_CHECK_IDS = Object.freeze([
  'P8-01', 'P8-02', 'P8-03', 'P8-04', 'P8-05', 'P8-06', 'P8-07', 'P8-08', 'P8-09', 'P8-10', 'P8-11', 'P8-12',
]);

/** Contract invariants, asserted before a single check runs. A broken registry is an INFRA error, not a FAIL. */
export function contractViolations() {
  const v = [];
  // Pinned literals FIRST — these cannot be satisfied by editing the registry alone.
  if (EXPECTED_CHECK_COUNT !== CONTRACT_CHECK_COUNT) v.push(`EXPECTED_CHECK_COUNT is ${EXPECTED_CHECK_COUNT}; the contract pins ${CONTRACT_CHECK_COUNT}. The denominator is not editable.`);
  if (PHASE8_CHECKS.length !== CONTRACT_CHECK_COUNT) v.push(`registry declares ${PHASE8_CHECKS.length} checks; the contract pins ${CONTRACT_CHECK_COUNT}`);
  const actualIds = PHASE8_CHECKS.map((c) => c.checkId).join(',');
  if (actualIds !== CONTRACT_CHECK_IDS.join(',')) v.push(`registry ids [${actualIds}] != contract ids [${CONTRACT_CHECK_IDS.join(',')}] — a check was renamed, removed, reordered or added`);
  if (PHASE8_CHECKS.length !== EXPECTED_CHECK_COUNT) v.push(`registry declares ${PHASE8_CHECKS.length} checks, contract says ${EXPECTED_CHECK_COUNT}`);
  const ids = PHASE8_CHECKS.map((c) => c.checkId);
  const dupes = ids.filter((id, i) => ids.indexOf(id) !== i);
  if (dupes.length) v.push(`duplicate checkId(s): ${[...new Set(dupes)].join(', ')}`);
  const orders = PHASE8_CHECKS.map((c) => c.order);
  if (new Set(orders).size !== orders.length) v.push('duplicate order values');
  for (const c of PHASE8_CHECKS) {
    for (const d of c.dependsOn) if (!ids.includes(d)) v.push(`${c.checkId} depends on unknown check ${d}`);
    if (!c.expectedFailureCode) v.push(`${c.checkId} has no expectedFailureCode — a check that cannot say WHY it failed cannot be verified`);
  }
  return v;
}

/** Reconcile results against the frozen registry. The denominator is the registry, never the results. */
export function reconcile(results) {
  const v = [];
  const ids = PHASE8_CHECKS.map((c) => c.checkId);
  const seen = Object.keys(results);
  for (const id of ids) if (!(id in results)) v.push(`missing result for ${id}`);
  for (const id of seen) if (!ids.includes(id)) v.push(`unknown check result "${id}" — not in the frozen registry`);
  for (const [id, r] of Object.entries(results)) if (!CHECK_STATES.includes(r.state)) v.push(`${id} has unknown state "${r.state}"`);
  const counts = {};
  for (const s of CHECK_STATES) counts[s] = Object.values(results).filter((r) => r.state === s).length;
  const total = Object.values(counts).reduce((a, b) => a + b, 0);
  if (total !== EXPECTED_CHECK_COUNT) v.push(`state counts sum to ${total}, expected exactly ${EXPECTED_CHECK_COUNT}`);
  const passed = Object.values(results).filter((r) => PASSING.includes(r.state)).length;
  return { counts, total, passed, of: EXPECTED_CHECK_COUNT, reconciled: v.length === 0, violations: v };
}
