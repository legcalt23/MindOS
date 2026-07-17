#!/usr/bin/env node
// R3-A5 §1B — AUTHORING tool for docs/portfolio/R3A5_BRANCH_REGISTRY.json.
//
// PROVENANCE, NOT AUTHORITY. The committed JSON is the authority; this file records how it was authored and is
// never read by the validator or the generator. Run once; if it and the JSON ever disagree, the JSON wins and
// this file is stale. It exists so the 40 canonical rows are not hand-typed 23 fields at a time — hand-typing is
// exactly how the R3-A4 counts drifted from the table they described.
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { join } from 'node:path';

const REPO = 'C:/Users/curro/AndroidStudioProjects/MindOS';
const sha = (b) => createHash('sha256').update(b).digest('hex');
const FREEZE = 'scripts/portfolio/phase8-freeze.mjs';
const FREEZE_SHA = sha(readFileSync(join(REPO, FREEZE)));
const VMODE = 'scripts/portfolio/validation-mode.mjs';
const VMODE_SHA = sha(readFileSync(join(REPO, VMODE)));

// The 50 enumerated legacy entries, recovered verbatim from the R3-A4 workflow journal.
const legacySrc = JSON.parse(readFileSync('C:/Users/curro/AppData/Local/Temp/claude/C--Users-curro-AndroidStudioProjects-MindOS/4d83c382-ff5b-4aba-a855-7aeaf73e8062/scratchpad/branches.json', 'utf8'));
const LEGACY = [];
for (const r of legacySrc) for (const b of r.branches) LEGACY.push({ id: b.branchId, expr: (b.conjunct || '').replace(/\s+/g, ' ').trim() });
LEGACY.sort((a, b) => a.id.localeCompare(b.id));

// ---------------------------------------------------------------- classification decisions (the judgement)
// D1 class · structuralType · disposition · testStatus · rationale · proof
// Every legacy id appears exactly once here or in OMITTED below. Nothing may vanish.
const D = {
  'P8-01.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'Disk-side operand: a declared artifact must not be edited after the manifest recorded it.', null],
  'P8-01.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_NOT_DESIGNED', "Absence arm. Real property (an artifact must not vanish) but implemented via the 'ABSENT' string sentinel, which shares a namespace with a digest.", null],
  'P8-01.b3': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'Manifest-side operand: the recorded digest must not be rewritten to bless whatever is on disk.', null],
  'P8-01.b4': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_NOT_DESIGNED', 'The declared set must be complete. Today artifacts[] is simultaneously the subject under test and the loop bound, so shrinking it yields PASS over the survivors. The property is real; the implementation cannot express it. Expected set must come from the external contract (§2).', null],
  'P8-01.b5': ['DEAD_OR_UNREACHABLE', 'FAULT_ABORT_SCENARIO', 'MOVE_TO_FAULT_MATRIX', 'TEST_NOT_DESIGNED', 'Manifest JSON.parse throws before any record(); all twelve go ABORTED. Not a branch of the predicate — an abort path. Belongs to D-D and remains part of the end-to-end guarantee.', null],
  'P8-02.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The V2c handoff must be present.', null],
  'P8-02.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'REPRODUCED_UNBOUND', 'The V2c handoff must be byte-identical to the pinned digest.', null],
  'P8-02.b3': ['DEAD_OR_UNREACHABLE', 'FAULT_ABORT_SCENARIO', 'MOVE_TO_FAULT_MATRIX', 'TEST_NOT_DESIGNED', 'sha() has no regular-file/readability guard; an unreadable zip throws → ABORTED. Abort path, not a predicate branch.', null],
  'P8-03.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_PRESENT_NOT_DISCRIMINATING', 'The ledger validator must succeed (exit code arm).', null],
  'P8-03.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_PRESENT_NOT_DISCRIMINATING', 'The validator must report OK (text arm). Regex unanchored; needs a structured result rather than prose matching.', null],
  'P8-04.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The published container hash must name the candidate zip.', null],
  'P8-04.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The container hash file must not be inside the container it describes.', null],
  'P8-04.b3': ['DEFECT_DISCARDED_SIGNAL', 'DISCARDED_SIGNAL', 'INCORPORATE', 'TEST_NOT_DESIGNED', "tar -tf's exit code is obtained and thrown away: a failed listing yields containsOwnHash=false and therefore a VACUOUS PASS. The signal exists; nothing consumes it.", null],
  'P8-04.b4': ['DEAD_OR_UNREACHABLE', 'FAULT_ABORT_SCENARIO', 'MOVE_TO_FAULT_MATRIX', 'TEST_NOT_DESIGNED', 'Unguarded readFileSync of BUNDLE_CONTAINER_HASH.txt outside any try; ENOENT escapes main(). Abort path.', null],
  'P8-04.b5': ['DEFECT_DISCARDED_SIGNAL', 'DISCARDED_SIGNAL', 'REDESIGN', 'TEST_NOT_DESIGNED', "sha(ZIP) returns the 'ABSENT' sentinel rather than a typed read error; the check then fails for the wrong stated reason and cascades.", null],
  'P8-05.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'inputRoot must validate as a closed set under an explicitly stated mode.', null],
  'P8-05.b2': ['REDUNDANT_OR_IMPLIED', 'LOCAL_PREDICATE', 'DELETE', 'PROOF_VERIFIED', 'n > 0 is implied by b1 and can never be the sole falsified branch.',
    'PROOF (from code, not narrative). (1) phase8-freeze.mjs:94 calls validateContext({mode:"bundle", inputRoot: R.inputRoot}) with NO manifest argument. (2) validation-mode.mjs:164-166 — bundle mode with manifest undefined reads p = join(inputRoot, "BUNDLE_MANIFEST.json"); if (!existsSync(p)) { diagnostics.push(D("MANIFEST_ABSENT", ...)); return fail(); } and fail() returns gate:"FAIL". Therefore gate==="PASS" ⟹ BUNDLE_MANIFEST.json EXISTS inside inputRoot. (3) phase8-freeze.mjs:92 computes n by the recursive walker (function w(d){ let c=0; for (const e of readdirSync(d,{withFileTypes:true})) c += e.isDirectory() ? w(join(d,e.name)) : 1; return c; })(R.inputRoot) — it walks the SAME R.inputRoot, counts every non-directory entry, and excludes nothing, so it counts BUNDLE_MANIFEST.json. (4) ∴ gate==="PASS" ⟹ n ≥ 1 ⟹ n > 0. b1 ⟹ b2. ∎ PRECONDITION: holds for mode "bundle" only; in mode "repo" validateContext returns PASS at validation-mode.mjs:159 without reading any manifest, but phase8-freeze states "bundle" as a literal at the call site.'],
  'P8-05.b3': ['DEAD_OR_UNREACHABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'PROOF_PENDING', 'seal.before.fileCount === n compares two semantically identical walkers over the same static tree; true by construction. Needs a reachability proof or removal.', null],
  'P8-05.b4': ['DEFECT_DISCARDED_SIGNAL', 'DISCARDED_SIGNAL', 'INCORPORATE', 'TEST_NOT_DESIGNED', 'The extraction exit code is discarded at phase8-freeze.mjs:91; a failed extraction is not distinguished from an empty one.', null],
  'P8-05.b5': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The bundled validateContext (from inputRoot) must be the one used, not the repo copy.', null],
  'P8-06.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_PRESENT_NOT_DISCRIMINATING', 'The bundle validator child must succeed (exit code arm).', null],
  'P8-06.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_PRESENT_NOT_DISCRIMINATING', 'The child must report PASS (text arm). Unanchored regex; needs structured output.', null],
  'P8-07.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'Every declared source entry must re-hash from the bundled archive alone.', null],
  'P8-07.b3': ['REDUNDANT_OR_IMPLIED', 'LOCAL_PREDICATE', 'DELETE', 'PROOF_VERIFIED', 'mismatched === 0 is algebraically implied by verified === of.',
    'PROOF (from the probe source at phase8-freeze.mjs:108-118). DEFINITIONS: the probe declares `let ok = 0; const bad = [];` then iterates `for (const e of idx.entries) { const d = readFileSync(join(WORK, e.path)); (createHash("sha256").update(d).digest("hex") === e.sha256 && d.length === e.byteLength) ? ok++ : bad.push(e.path); }` and emits `{ verified: ok, of: idx.entries.length, mismatched: bad.length }`. LOOP INVARIANT: the ternary is total over each iteration — exactly one of ok++ / bad.push executes per entry, and neither is executed elsewhere. Hence after k completed iterations ok + bad.length = k. TERMINATION: if the loop runs to completion k = idx.entries.length = of, so verified + mismatched = of. DERIVATION: verified === of ⟹ mismatched = of − verified = 0; and mismatched === 0 ⟹ verified = of. ∴ b2 ⟺ b3; b3 adds no protection. ∎ PRECONDITION, EXPLICIT: this holds only if the loop COMPLETES. readFileSync(join(WORK, e.path)) throws on a missing extracted file, aborting the probe; the emitted JSON is then absent, JSON.parse fails, and parsed === null — caught by legacy P8-07.b1, which is NOT redundant and is retained as a fault/abort scenario in D-D (row R-023). Removing b3 therefore does not remove the abort guarantee.'],
  'P8-07.b4': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'Declared byteLength must agree with the recovered bytes. NOT a cryptographic assumption: falsifiable by editing byteLength in PORTFOLIO_SOURCE_INDEX.json alone — metadata, no second preimage required. The R3-A4 CRYPTOGRAPHIC_ASSUMPTION claim is withdrawn.', null],
  'P8-08.b1': ['REQUIRED_FALSIFIABLE', 'SHARED_BINDING', 'KEEP', 'TEST_NOT_DESIGNED', 'observe-code must run to completion. Definition site of the regenOk conjunction (phase8-freeze.mjs:163).', null],
  'P8-08.b2': ['REQUIRED_FALSIFIABLE', 'SHARED_BINDING', 'KEEP', 'TEST_NOT_DESIGNED', 'derive-portfolio-model must run to completion. Definition site of regenOk.', null],
  'P8-08.b3': ['REQUIRED_FALSIFIABLE', 'SHARED_BINDING', 'KEEP', 'TEST_NOT_DESIGNED', 'build-portfolio must run to completion. Definition site of regenOk.', null],
  'P8-09.b4': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_NOT_DESIGNED', "The model must actually be produced. Reclassified DEAD_OR_UNREACHABLE → REQUIRED_FALSIFIABLE: it was only unreachable because 'ABSENT' conflates MISSING/READ_ERROR/INVALID_TYPE with a digest. Once the read result is a discriminated type, 'the model was produced' is a real, falsifiable property.", null],
  'P8-09.b5': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The regenerated model must equal the shipped model across roots.', null],
  'P8-09.b6': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The regenerated view must equal the shipped view across roots.', null],
  'P8-10.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_NOT_DESIGNED', 'The portfolio validator must report blocking gates passing. Needs structured output, not prose.', null],
  'P8-10.b2': ['DEFECT_DISCARDED_SIGNAL', 'DISCARDED_SIGNAL', 'INCORPORATE', 'TEST_NOT_DESIGNED', "pv.code === 0 is ABSENT from the predicate. P8-06 and P8-11 both gate on their child's exit code; P8-10 does not. The signal exists and is discarded.", null],
  'P8-10.b3': ['DEFECT_DISCARDED_SIGNAL', 'DISCARDED_SIGNAL', 'REDESIGN', 'TEST_NOT_DESIGNED', 'The regex /BLOCKING GATES: PASS/ is unanchored — any diagnostic line carrying the literal matches. LIVE FALSE-PASS.', null],
  'P8-10.b4': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_NOT_DESIGNED', "The child must be invoked with an explicit --mode=bundle. Reclassified DIFF_OR_AUTHORITY_ONLY → REQUIRED_FALSIFIABLE: 'unobservable at runtime' was false. A harness inside the TCB can capture executable/argv/cwd/env; it is unobservable only because the collector does not record them. That is a defect, not a property of the world.", null],
  'P8-10.b5': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The invoked validator binary must be the bundled one from inputRoot.', null],
  'P8-11.b1': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_PRESENT_NOT_DISCRIMINATING', 'The ledger validator must succeed against the extracted ledger (exit code arm).', null],
  'P8-11.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'REDESIGN', 'TEST_PRESENT_NOT_DISCRIMINATING', 'It must report OK (text arm). Unanchored regex.', null],
  'P8-12.b2': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'The frozen repo artifacts and the candidate zip must be byte-identical after the run. Merge target for P8-12.b1: one expression (moved.length === 0), two key subsets.', null],
  'P8-12.b3': ['REQUIRED_FALSIFIABLE', 'LOCAL_PREDICATE', 'KEEP', 'TEST_NOT_DESIGNED', 'inputRoot must be byte-identical after the run (fingerprint before === after).', null],
};

// Legacy entries that do NOT become their own canonical row.
// mergeTargetLegacy: for REMOVED_AS_DUPLICATE, the legacy id whose row this one collapses into.
const OMITTED = {
  'P8-06.b3': ['DELEGATED', null, "BUNDLE_ROLE_GATE is a gate of the child validator, reached only via P8-06.b1/b2. It is a requirement of validate-bundle's contract, not a conjunct of P8-06's two-operand predicate."],
  'P8-06.b4': ['DELEGATED', null, "DELIVERY_INTEGRITY_GATE — a child gate, not a branch of the freeze predicate."],
  'P8-06.b5': ['DELEGATED', null, "SOURCE_COMPLETENESS_GATE — a child gate, not a branch of the freeze predicate."],
  'P8-06.b6': ['DELEGATED', null, "CLEAN_ROOM_CONTAINMENT_GATE — a child gate, not a branch of the freeze predicate."],
  'P8-06.b7': ['DELEGATED', null, "RUNBOOK_GATE — a child gate, not a branch of the freeze predicate."],
  'P8-07.b1': ['MOVED_TO_FAULT_MATRIX', null, "!!parsed is the probe's abort guard, not a property of the sources. It is the precondition under which P8-07.b3's algebraic proof holds, so it is retained in D-D rather than deleted."],
  'P8-09.b1': ['REMOVED_AS_DUPLICATE', 'P8-08.b1', 'regenOk sub-conjunct obs.code === 0. regenOk is ONE const bound at phase8-freeze.mjs:163 and read by both record("P8-08") and record("P8-09"); counting it again at the P8-09 call site double-counts one property across two call sites.'],
  'P8-09.b2': ['REMOVED_AS_DUPLICATE', 'P8-08.b2', 'regenOk sub-conjunct der.code === 0 — same shared binding, same double-count.'],
  'P8-09.b3': ['REMOVED_AS_DUPLICATE', 'P8-08.b3', 'regenOk sub-conjunct bld.code === 0 — same shared binding, same double-count.'],
};

// P8-12.b1 and P8-12.b2 are one expression (moved.length === 0) partitioned by key subset; they merge.
const MERGES = { 'P8-12.b1': 'P8-12.b2' };

// ---------------------------------------------------------------- trust boundaries (NOT from the 50)
const BOUNDARIES = [
  ['record()', 'phase8-freeze.mjs:42', 'record(checkId, ok, detail, failureCode = null)', 'The state of a check must reflect its evidence.', 'A one-line force makes every one of the twelve PASS. This is the F-01 root and the reason a per-check matrix cannot see the risk.', 'TEST_PRESENT_NOT_DISCRIMINATING', ['P8A-12']],
  ['reconcile()/PASSING', 'phase8-checks.mjs:91', 'reconcile(results) / PASSING = [PASS]', 'The denominator is the registry; only PASS passes.', 'Widening PASSING or dropping rec.reconciled launders all twelve.', 'TEST_PRESENT_NOT_DISCRIMINATING', ['P8A-05', 'P8A-06', 'P8A-08']],
  ['run()', 'phase8-freeze.mjs:31', 'run(exe, args, opts)', 'A child must be the authorised executable, with the authorised argv/cwd/env.', 'Spreads ...opts into execFileSync and never pins env; subject redirect and env injection are invisible because nothing records what actually ran. Blast radius P8-03/06/08/10/11.', 'TEST_NOT_DESIGNED', []],
  ['sha()', 'phase8-freeze.mjs:29', "existsSync(rel(p)) ? createHash(...) : 'ABSENT'", 'A digest read must distinguish PRESENT / MISSING / READ_ERROR / INVALID_TYPE.', "A string sentinel in the digest namespace. Four sites (P8-01/02/04/12); at P8-12 'ABSENT'==='ABSENT' reads as byte-identical. Violates this repo's own CLAUDE.md rules of 2026-07-04 and 2026-07-05.", 'TEST_NOT_DESIGNED', []],
  ['publisher', 'phase8-freeze.mjs:206', 'run8.publish({... verdict ...}, {status})', 'The durable report must not contradict its own checks.', 'REPRODUCED: editing only the publish payload leaves record/reconcile/console/exitCode honest while the durable report says FREEZE_VERIFIED status=completed with P8-02 FAIL inside it. Every existing falsifier asserts state+exit; none asserts the published verdict.', 'REPRODUCED_UNBOUND', []],
  ['consumer', 'atomic-report.mjs:77', 'readReport(canonicalPath, {expectedSnapshotId})', 'A report must be rejected if stale, partial, wrong-run, wrong-contract, wrong-snapshot or self-contradictory.', 'ZERO production call sites (grep: only its own export). The enforcement half of snapshot binding never executes, and it would certify the laundered report above as valid.', 'TEST_NOT_DESIGNED', []],
];

// ---------------------------------------------------------------- build
const rows = [];
let n = 0;
const rid = () => `R-${String(++n).padStart(3, '0')}`;
const byLegacy = new Map();

for (const l of LEGACY) {
  if (OMITTED[l.id] || MERGES[l.id]) continue;
  const d = D[l.id];
  if (!d) throw new Error(`ORACLE FAILURE: legacy ${l.id} has no classification decision and is not omitted/merged`);
  const [d1, st, disp, ts, rationale, proof] = d;
  const id = rid();
  byLegacy.set(l.id, id);
  const legacyIds = [l.id];
  for (const [from, to] of Object.entries(MERGES)) if (to === l.id) legacyIds.unshift(from);
  const history = [];
  if (l.id === 'P8-10.b4') history.push({ from: 'DIFF_OR_AUTHORITY_ONLY', to: 'REQUIRED_FALSIFIABLE', reason: 'A harness inside the TCB can capture executable/argv/cwd/env. "Unobservable at runtime" was false: it is unobserved because the collector does not record it. CEO R3-A5 §6.' });
  if (l.id === 'P8-09.b4') history.push({ from: 'DEAD_OR_UNREACHABLE', to: 'REQUIRED_FALSIFIABLE', reason: "Unreachable only because the 'ABSENT' sentinel conflates MISSING/READ_ERROR/INVALID_TYPE with a digest. Once typed, 'the model was produced' is falsifiable. CEO R3-A5 §6." });
  if (l.id === 'P8-01.b4') history.push({ from: 'DEFECT_DISCARDED_SIGNAL', to: 'REQUIRED_FALSIFIABLE', reason: 'The hybrid "DS→RF" published in R3-A4 was not a valid class. Current class is REQUIRED_FALSIFIABLE (the property is real); the defect is recorded as disposition REDESIGN — the expected set must come from the external contract, not from artifacts[] auditing itself.' });
  rows.push({
    rowId: id, legacyIds, originStage: 'R3A4_WORKFLOW', structuralType: st,
    sourceFile: FREEZE, sourceFileSha256: FREEZE_SHA, sourceLocation: l.id.startsWith('P8-05.b2') ? 'phase8-freeze.mjs:92-96' : `record('${l.id.split('.')[0]}')`,
    exactExpression: l.expr, candidateProperty: rationale.split('.')[0] + '.',
    authorityStatus: 'CONTRACT_PENDING', d1Classification: d1, classificationHistory: history,
    disposition: disp, mergedInto: null, delegatedContract: null, owner: 'R3-A5',
    testStatus: ts, testIds: l.id === 'P8-02.b2' ? ['R3A4-PROBE-V2C'] : [],
    evidenceIds: l.id === 'P8-02.b2' ? ['EV-R3A4-PUBLISHER-PROBE'] : [],
    dependencies: [], rationale, proof, notes: '',
  });
}

// merged rows point at their target
for (const [from, to] of Object.entries(MERGES)) {
  const target = byLegacy.get(to);
  const id = rid();
  rows.push({
    rowId: id, legacyIds: [from], originStage: 'R3A4_WORKFLOW', structuralType: 'LOCAL_PREDICATE',
    sourceFile: FREEZE, sourceFileSha256: FREEZE_SHA, sourceLocation: "record('P8-12')",
    exactExpression: LEGACY.find((x) => x.id === from).expr,
    candidateProperty: 'Repo artifacts must be byte-identical after the run.',
    authorityStatus: 'CONTRACT_PENDING', d1Classification: 'REQUIRED_FALSIFIABLE', classificationHistory: [],
    disposition: 'MERGED_INTO', mergedInto: target, delegatedContract: null, owner: 'R3-A5',
    testStatus: 'TEST_NOT_DESIGNED', testIds: [], evidenceIds: [], dependencies: [],
    rationale: `Same expression as ${to} (moved.length === 0), partitioned only by which keys of frozen{} are inspected. One property, two key subsets — merged rather than double-counted.`,
    proof: null, notes: '',
  });
}

// Rows for the legacy entries that do not become their own property. Every rowId comes from the SAME rid()
// sequence — hardcoding ids here collided with generated ones (R-023/026/027/028) and produced duplicate rowIds,
// which is exactly the class of defect the validator must refuse.
const omittedRowId = {};
for (const [lid, [disp, mergeTargetLegacy, reason]] of Object.entries(OMITTED)) {
  const l = LEGACY.find((x) => x.id === lid);
  const isDelegated = disp === 'DELEGATED';
  const id = rid();
  omittedRowId[lid] = id;
  rows.push({
    rowId: id, legacyIds: [lid], originStage: 'R3A4_WORKFLOW',
    structuralType: isDelegated ? 'DELEGATED_CHILD_REQUIREMENT' : (disp === 'MOVED_TO_FAULT_MATRIX' ? 'FAULT_ABORT_SCENARIO' : 'SHARED_BINDING'),
    sourceFile: FREEZE, sourceFileSha256: FREEZE_SHA, sourceLocation: lid,
    exactExpression: l.expr, candidateProperty: reason.split('.')[0] + '.',
    authorityStatus: 'CONTRACT_PENDING',
    d1Classification: isDelegated ? 'DIFF_OR_AUTHORITY_ONLY' : (disp === 'MOVED_TO_FAULT_MATRIX' ? 'DEAD_OR_UNREACHABLE' : 'REDUNDANT_OR_IMPLIED'),
    classificationHistory: [],
    disposition: isDelegated ? 'DELEGATE_TO_CHILD_CONTRACT' : (disp === 'MOVED_TO_FAULT_MATRIX' ? 'MOVE_TO_FAULT_MATRIX' : 'MERGED_INTO'),
    mergedInto: mergeTargetLegacy ? byLegacy.get(mergeTargetLegacy) : null,
    delegatedContract: isDelegated ? 'validate-bundle.mjs child contract — PENDING: no contract document exists; §2 owns it' : null,
    owner: 'R3-A5', testStatus: isDelegated ? 'DELEGATED_GATE_PENDING' : 'TEST_NOT_DESIGNED',
    testIds: [], evidenceIds: [], dependencies: [], rationale: reason, proof: null, notes: '',
  });
}

for (const [name, loc, expr, prop, why, ts, tids] of BOUNDARIES) {
  rows.push({
    rowId: rid(), legacyIds: [], originStage: 'R3A5_TRUST_BOUNDARY_ANALYSIS', structuralType: 'TRUST_BOUNDARY',
    sourceFile: loc.split(':')[0].startsWith('phase8-checks') ? 'scripts/portfolio/phase8-checks.mjs' : (loc.startsWith('atomic-report') ? 'scripts/portfolio/atomic-report.mjs' : FREEZE),
    sourceFileSha256: loc.startsWith('phase8-checks') ? sha(readFileSync(join(REPO, 'scripts/portfolio/phase8-checks.mjs'))) : (loc.startsWith('atomic-report') ? sha(readFileSync(join(REPO, 'scripts/portfolio/atomic-report.mjs'))) : FREEZE_SHA),
    sourceLocation: loc, exactExpression: expr, candidateProperty: prop,
    authorityStatus: 'CONTRACT_PENDING', d1Classification: 'REQUIRED_FALSIFIABLE', classificationHistory: [],
    disposition: 'REDESIGN', mergedInto: null, delegatedContract: null, owner: 'R3-A5',
    testStatus: ts, testIds: tids, evidenceIds: name === 'publisher' ? ['EV-R3A4-PUBLISHER-PROBE'] : [],
    dependencies: [], rationale: why, proof: null, notes: `Trust boundary "${name}" — NOT a branch of any check. Blast radius spans multiple checks.`,
  });
}

const legacyInventory = LEGACY.map((l) => {
  const om = OMITTED[l.id];
  const merged = MERGES[l.id];
  const row = rows.find((r) => r.legacyIds.includes(l.id));
  let disposition, canonicalRow, reason;
  if (om) { disposition = om[0]; canonicalRow = omittedRowId[l.id]; reason = om[2]; }
  else if (merged) { disposition = 'MERGED'; canonicalRow = row.rowId; reason = row.rationale; }
  else { disposition = 'KEPT'; canonicalRow = row.rowId; reason = row.rationale; }
  return { legacyId: l.id, origin: 'R3-A4 twelve-agent per-check analysis (workflow wf_5c528cca-034)', exactExpression: l.expr, sourceArtifact: `${FREEZE}@${FREEZE_SHA.slice(0, 16)}`, disposition, canonicalRow, reason };
});

const out = {
  registrySchemaVersion: '1.0.0',
  generatedFrom: { workflowRunId: 'wf_5c528cca-034', sourceSnapshotId: '5cae6b907d067464b98b571c3f60df06260a9eee422b677ff768ecf346e414ca', note: 'Legacy rows recovered verbatim from the R3-A4 workflow journal; classifications are R3-A5 judgements under CEO D1.' },
  authorityNote: 'THIS JSON IS THE AUTHORITY. Counts are never stored here — validate-branch-registry.mjs computes every denominator from these rows and R3A5_BRANCH_MATRIX.md is generated, never hand-written. authorityStatus is CONTRACT_PENDING everywhere because PHASE8_FREEZE_CONTRACT_V1 does not exist yet: code under audit cannot be its own normative source.',
  rows, legacyInventory,
};
writeFileSync(join(REPO, 'docs/portfolio/R3A5_BRANCH_REGISTRY.json'), JSON.stringify(out, null, 2) + '\n');
console.log(`rows=${rows.length}  legacy=${legacyInventory.length}`);
