#!/usr/bin/env node
// R3-A5 §1B §7 — strict validator for docs/portfolio/R3A5_BRANCH_REGISTRY.json.
//
// EVERY COUNT HERE IS COMPUTED. The registry stores no totals, and this validator accepts none as input: the
// R3-A4 matrix drifted precisely because a total was written by hand next to a table nobody counted. If a
// presentation total ever appears, rule 7 compares it against the computed value and fails on divergence.
//
// The validator does NOT import the registry as code. The JSON is the authority; this file only reads it.
import { readFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (b) => createHash('sha256').update(b).digest('hex');
const REGISTRY = 'docs/portfolio/R3A5_BRANCH_REGISTRY.json';
const SCHEMA = 'docs/portfolio/R3A5_BRANCH_REGISTRY.schema.json';
const MATRIX = 'docs/portfolio/R3A5_BRANCH_MATRIX.md';

const ENUMS = {
  structuralType: ['LOCAL_PREDICATE', 'SHARED_BINDING', 'TRUST_BOUNDARY', 'DELEGATED_CHILD_REQUIREMENT', 'FAULT_ABORT_SCENARIO', 'DISCARDED_SIGNAL', 'AUTHORITY_DIFF_INVARIANT', 'CRYPTOGRAPHIC_TRUST_ASSUMPTION'],
  d1Classification: ['REQUIRED_FALSIFIABLE', 'REDUNDANT_OR_IMPLIED', 'DEAD_OR_UNREACHABLE', 'CRYPTOGRAPHIC_ASSUMPTION', 'DIFF_OR_AUTHORITY_ONLY', 'DEFECT_DISCARDED_SIGNAL'],
  disposition: ['KEEP', 'DELETE', 'REDESIGN', 'INCORPORATE', 'DELEGATE_TO_CHILD_CONTRACT', 'MOVE_TO_FAULT_MATRIX', 'MERGED_INTO', 'SUPERSEDED'],
  testStatus: ['TEST_NOT_DESIGNED', 'TEST_DESIGNED_NOT_RUN', 'TEST_PRESENT_NOT_DISCRIMINATING', 'REPRODUCED_UNBOUND', 'DISCRIMINATING_VERIFIED', 'PROOF_PENDING', 'PROOF_VERIFIED', 'DIFF_GATE_PENDING', 'DELEGATED_GATE_PENDING'],
  authorityStatus: ['CONTRACT_PENDING', 'CONTRACT_APPROVED'],
  originStage: ['R3A4_WORKFLOW', 'R3A5_TRUST_BOUNDARY_ANALYSIS'],
  legacyDisposition: ['KEPT', 'MERGED', 'DELEGATED', 'MOVED_TO_FAULT_MATRIX', 'CONVERTED_TO_DEFECT', 'REMOVED_AS_DUPLICATE', 'REMOVED_WITH_PROOF', 'UNRECOVERABLE'],
};
const ROW_FIELDS = ['rowId', 'legacyIds', 'originStage', 'structuralType', 'sourceFile', 'sourceFileSha256', 'sourceLocation', 'exactExpression', 'candidateProperty', 'authorityStatus', 'd1Classification', 'classificationHistory', 'disposition', 'mergedInto', 'delegatedContract', 'owner', 'testStatus', 'testIds', 'evidenceIds', 'dependencies', 'rationale', 'proof', 'notes'];
const LEGACY_FIELDS = ['legacyId', 'origin', 'exactExpression', 'sourceArtifact', 'disposition', 'canonicalRow', 'reason'];

// Dispositions that mean "this row is no longer an active obligation in its own right".
const RETIRED = new Set(['MERGED_INTO', 'DELETE', 'SUPERSEDED', 'DELEGATE_TO_CHILD_CONTRACT', 'MOVE_TO_FAULT_MATRIX']);
const PROOF_REQUIRED = new Set(['REDUNDANT_OR_IMPLIED', 'DEAD_OR_UNREACHABLE']);
const VERIFIED_STATES = new Set(['DISCRIMINATING_VERIFIED', 'PROOF_VERIFIED']);

const errs = [];
const E = (code, msg) => errs.push({ code, msg });

if (!existsSync(rel(REGISTRY))) { console.error('E01_SCHEMA_INVALID: registry missing'); process.exit(1); }
let reg;
try { reg = JSON.parse(readFileSync(rel(REGISTRY), 'utf8')); }
catch (e) { console.error(`E01_SCHEMA_INVALID: ${e.message}`); process.exit(1); }

// 1 schema shape
if (reg.registrySchemaVersion !== '1.0.0') E('E01_SCHEMA_INVALID', `registrySchemaVersion ${reg.registrySchemaVersion}`);
if (!Array.isArray(reg.rows) || !reg.rows.length) E('E01_SCHEMA_INVALID', 'rows must be a non-empty array');
if (!Array.isArray(reg.legacyInventory) || !reg.legacyInventory.length) E('E01_SCHEMA_INVALID', 'legacyInventory must be a non-empty array');
if (!existsSync(rel(SCHEMA))) E('E01_SCHEMA_INVALID', 'schema file missing');
const rows = reg.rows ?? [], legacy = reg.legacyInventory ?? [];

const ids = new Set();
for (const r of rows) {
  // 2 unknown / missing field
  for (const k of Object.keys(r)) if (!ROW_FIELDS.includes(k)) E('E02_UNKNOWN_FIELD', `${r.rowId}: unknown field "${k}"`);
  for (const k of ROW_FIELDS) if (!(k in r)) E('E02_UNKNOWN_FIELD', `${r.rowId}: missing field "${k}"`);
  // 3 duplicate rowId
  if (ids.has(r.rowId)) E('E03_DUPLICATE_ROW_ID', `duplicate rowId ${r.rowId}`); else ids.add(r.rowId);
  if (!/^R-[0-9]{3}$/.test(r.rowId ?? '')) E('E01_SCHEMA_INVALID', `rowId "${r.rowId}" malformed`);
  // 6 invalid category
  for (const [f, vals] of Object.entries(ENUMS)) {
    if (f === 'legacyDisposition') continue;
    if (f in r && !vals.includes(r[f])) E('E06_INVALID_CATEGORY', `${r.rowId}: ${f}="${r[f]}" not in enum`);
  }
  // 15 source expression present
  if (!r.exactExpression || !String(r.exactExpression).trim()) E('E15_SOURCE_EXPRESSION_MISSING', `${r.rowId}: exactExpression empty`);
  // 16 source hash correct
  if (r.sourceFile && existsSync(rel(r.sourceFile))) {
    const actual = sha(readFileSync(rel(r.sourceFile)));
    if (actual !== r.sourceFileSha256) E('E16_SOURCE_HASH_DRIFT', `${r.rowId}: ${r.sourceFile} is ${actual.slice(0, 12)}…, registry says ${String(r.sourceFileSha256).slice(0, 12)}…`);
  } else if (r.sourceFile) E('E16_SOURCE_HASH_DRIFT', `${r.rowId}: sourceFile ${r.sourceFile} not found`);
  // 9 disposition vs active state
  if (r.disposition === 'MERGED_INTO' && !r.mergedInto) E('E09_DISPOSITION_CONFLICT', `${r.rowId}: MERGED_INTO with null mergedInto`);
  if (r.disposition !== 'MERGED_INTO' && r.mergedInto) E('E09_DISPOSITION_CONFLICT', `${r.rowId}: mergedInto set but disposition=${r.disposition}`);
  // 12 delegated row needs contract + owner
  if (r.disposition === 'DELEGATE_TO_CHILD_CONTRACT' && !r.delegatedContract) E('E12_DELEGATION_WITHOUT_CONTRACT', `${r.rowId}: delegated with no child contract`);
  if (r.structuralType === 'DELEGATED_CHILD_REQUIREMENT' && !r.delegatedContract) E('E12_DELEGATION_WITHOUT_CONTRACT', `${r.rowId}: delegated child requirement with no contract`);
  if (!r.owner) E('E12_DELEGATION_WITHOUT_CONTRACT', `${r.rowId}: no owner`);
  // 13 proof-required without proof
  if (PROOF_REQUIRED.has(r.d1Classification) && r.testStatus === 'PROOF_VERIFIED' && !r.proof) E('E13_PROOF_MISSING', `${r.rowId}: ${r.d1Classification} PROOF_VERIFIED without proof`);
  // 14 verified without test/evidence
  if (VERIFIED_STATES.has(r.testStatus)) {
    if (r.testStatus === 'DISCRIMINATING_VERIFIED' && (!r.testIds.length || !r.evidenceIds.length)) E('E14_VERIFIED_WITHOUT_EVIDENCE', `${r.rowId}: DISCRIMINATING_VERIFIED without testIds+evidenceIds`);
    if (r.testStatus === 'PROOF_VERIFIED' && !r.proof) E('E14_VERIFIED_WITHOUT_EVIDENCE', `${r.rowId}: PROOF_VERIFIED without proof`);
  }
  // 17 classification history coherence
  for (const h of r.classificationHistory ?? []) {
    if (!ENUMS.d1Classification.includes(h.from) || !ENUMS.d1Classification.includes(h.to)) E('E17_HISTORY_INCOHERENT', `${r.rowId}: history ${h.from}→${h.to} uses a non-enum class`);
    if (!h.reason?.trim()) E('E17_HISTORY_INCOHERENT', `${r.rowId}: history entry without reason`);
  }
  const last = (r.classificationHistory ?? []).at(-1);
  if (last && last.to !== r.d1Classification) E('E17_HISTORY_INCOHERENT', `${r.rowId}: history ends at ${last.to} but d1Classification is ${r.d1Classification}`);
  // 18 authority approved before §2
  if (r.authorityStatus === 'CONTRACT_APPROVED' && !existsSync(rel('docs/portfolio/PHASE8_FREEZE_CONTRACT_V1.md'))) {
    E('E18_AUTHORITY_PREMATURE', `${r.rowId}: CONTRACT_APPROVED but PHASE8_FREEZE_CONTRACT_V1.md does not exist — code under audit cannot be its own normative source`);
  }
  // 20 crypto assumption inside the local denominator
  if (r.d1Classification === 'CRYPTOGRAPHIC_ASSUMPTION' && r.structuralType === 'LOCAL_PREDICATE') {
    E('E20_CRYPTO_IN_LOCAL_DENOMINATOR', `${r.rowId}: a cryptographic assumption may not be counted as a local predicate; it belongs in R3A5_CRYPTO_ASSUMPTIONS.json`);
  }
}
// 10 mergedInto resolves · 11 no merge cycles
for (const r of rows) {
  if (!r.mergedInto) continue;
  if (!ids.has(r.mergedInto)) { E('E10_MERGE_TARGET_MISSING', `${r.rowId}: mergedInto ${r.mergedInto} does not exist`); continue; }
  const seen = new Set([r.rowId]);
  let cur = rows.find((x) => x.rowId === r.mergedInto);
  while (cur?.mergedInto) {
    if (seen.has(cur.rowId)) { E('E11_MERGE_CYCLE', `${r.rowId}: merge cycle through ${cur.rowId}`); break; }
    seen.add(cur.rowId);
    cur = rows.find((x) => x.rowId === cur.mergedInto);
  }
}
// 4 duplicate legacy id · 5 legacy vanished · legacy shape
const lids = new Set();
const claimed = new Set(rows.flatMap((r) => r.legacyIds ?? []));
for (const l of legacy) {
  for (const k of Object.keys(l)) if (!LEGACY_FIELDS.includes(k)) E('E02_UNKNOWN_FIELD', `${l.legacyId}: unknown field "${k}"`);
  if (lids.has(l.legacyId)) E('E04_DUPLICATE_LEGACY_ID', `duplicate legacy id ${l.legacyId}`); else lids.add(l.legacyId);
  if (!ENUMS.legacyDisposition.includes(l.disposition)) E('E06_INVALID_CATEGORY', `${l.legacyId}: disposition "${l.disposition}" not in enum`);
  if (l.disposition !== 'UNRECOVERABLE' && !l.canonicalRow) E('E05_LEGACY_VANISHED', `${l.legacyId}: no canonicalRow and not UNRECOVERABLE`);
  if (l.canonicalRow && !ids.has(l.canonicalRow)) E('E10_MERGE_TARGET_MISSING', `${l.legacyId}: canonicalRow ${l.canonicalRow} does not exist`);
  if (!l.reason?.trim()) E('E05_LEGACY_VANISHED', `${l.legacyId}: no reason`);
}
for (const c of claimed) if (!lids.has(c)) E('E05_LEGACY_VANISHED', `row claims legacy "${c}" which the inventory does not list`);
for (const l of lids) if (!claimed.has(l) && legacy.find((x) => x.legacyId === l)?.disposition !== 'UNRECOVERABLE') {
  E('E05_LEGACY_VANISHED', `legacy "${l}" is in the inventory but no row claims it`);
}

// 8 double counting: a rowId may appear in exactly one denominator
const DEN = {
  D_A_LOCAL_ACTIVE_PROPERTIES: (r) => r.structuralType === 'LOCAL_PREDICATE' && !RETIRED.has(r.disposition),
  D_B_TRUST_BOUNDARIES: (r) => r.structuralType === 'TRUST_BOUNDARY',
  D_C_DELEGATED_REQUIREMENTS: (r) => r.structuralType === 'DELEGATED_CHILD_REQUIREMENT',
  D_D_FAULT_ABORT_SCENARIOS: (r) => r.structuralType === 'FAULT_ABORT_SCENARIO',
  D_E_STRUCTURAL_DEFECTS: (r) => r.structuralType === 'DISCARDED_SIGNAL',
  D_F_AUTHORITY_DIFF_INVARIANTS: (r) => r.structuralType === 'AUTHORITY_DIFF_INVARIANT',
  D_G_CRYPTOGRAPHIC_ASSUMPTIONS: (r) => r.structuralType === 'CRYPTOGRAPHIC_TRUST_ASSUMPTION',
};
const denom = {}, member = {};
for (const [k, f] of Object.entries(DEN)) { const m = rows.filter(f); denom[k] = m.length; for (const r of m) (member[r.rowId] ??= []).push(k); }
for (const [id, ks] of Object.entries(member)) if (ks.length > 1) E('E08_DOUBLE_COUNTED', `${id} counted in ${ks.join(' AND ')}`);

// retired-but-active
for (const r of rows) if (RETIRED.has(r.disposition) && DEN.D_A_LOCAL_ACTIVE_PROPERTIES(r)) E('E09_DISPOSITION_CONFLICT', `${r.rowId}: retired (${r.disposition}) yet counted active`);

// 7 sums: every row lands in exactly one structural bucket
const structSum = Object.values(rows.reduce((a, r) => (a[r.structuralType] = (a[r.structuralType] || 0) + 1, a), {})).reduce((a, b) => a + b, 0);
if (structSum !== rows.length) E('E07_SUM_INCONSISTENT', `structuralType buckets sum to ${structSum}, rows=${rows.length}`);
const d1Counts = rows.reduce((a, r) => (a[r.d1Classification] = (a[r.d1Classification] || 0) + 1, a), {});
const d1Sum = Object.values(d1Counts).reduce((a, b) => a + b, 0);
if (d1Sum !== rows.length) E('E07_SUM_INCONSISTENT', `d1 classes sum to ${d1Sum}, rows=${rows.length}`);
// no declared totals may be believed
for (const k of ['totalRows', 'counts', 'denominators', 'totals']) if (k in reg) E('E07_SUM_INCONSISTENT', `registry stores a declared total "${k}" — totals are computed, never stored`);

// 19 markdown up to date
let mdState = 'ABSENT';
if (existsSync(rel(MATRIX))) {
  const md = readFileSync(rel(MATRIX), 'utf8');
  const m = /<!-- REGISTRY-SHA256 ([0-9a-f]{64}) -->/.exec(md);
  const regSha = sha(readFileSync(rel(REGISTRY)));
  if (!m) { E('E19_MARKDOWN_STALE', 'matrix has no REGISTRY-SHA256 provenance marker — it was not generated from the registry'); mdState = 'UNMARKED'; }
  else if (m[1] !== regSha) { E('E19_MARKDOWN_STALE', `matrix was generated from registry ${m[1].slice(0, 12)}…, current registry is ${regSha.slice(0, 12)}…`); mdState = 'STALE'; }
  else mdState = 'CURRENT';
} else E('E19_MARKDOWN_STALE', 'R3A5_BRANCH_MATRIX.md absent — must be generated from the registry');

// ---------------------------------------------------------------- report (all computed)
const testCounts = rows.reduce((a, r) => (a[r.testStatus] = (a[r.testStatus] || 0) + 1, a), {});
const activeRF = rows.filter((r) => DEN.D_A_LOCAL_ACTIVE_PROPERTIES(r) && r.d1Classification === 'REQUIRED_FALSIFIABLE');
const out = {
  registrySha256: sha(readFileSync(rel(REGISTRY))),
  totalInventoryRows: rows.length,
  legacyEntries: legacy.length,
  heterogeneousNote: 'totalInventoryRows is an inventory of HETEROGENEOUS obligations. It is NOT a branch count and the denominators below must never be summed.',
  denominators: denom,
  structuralTypeCounts: rows.reduce((a, r) => (a[r.structuralType] = (a[r.structuralType] || 0) + 1, a), {}),
  d1Counts,
  dispositionCounts: rows.reduce((a, r) => (a[r.disposition] = (a[r.disposition] || 0) + 1, a), {}),
  testStatusCounts: testCounts,
  legacyDispositionCounts: legacy.reduce((a, l) => (a[l.disposition] = (a[l.disposition] || 0) + 1, a), {}),
  localActiveRequiredFalsifiable: activeRF.length,
  withDiscriminatingTestVerified: rows.filter((r) => r.testStatus === 'DISCRIMINATING_VERIFIED').length,
  proofsVerified: rows.filter((r) => r.testStatus === 'PROOF_VERIFIED').length,
  proofsPending: rows.filter((r) => r.testStatus === 'PROOF_PENDING').length,
  delegatedGatesPending: rows.filter((r) => r.testStatus === 'DELEGATED_GATE_PENDING').length,
  defectsUnrepaired: rows.filter((r) => r.d1Classification === 'DEFECT_DISCARDED_SIGNAL').length,
  authorityApproved: rows.filter((r) => r.authorityStatus === 'CONTRACT_APPROVED').length,
  markdownState: mdState,
  errors: errs,
};
if (process.argv.includes('--json')) console.log(JSON.stringify(out, null, 2));
else {
  console.log(`registry ${out.registrySha256.slice(0, 16)}…  rows=${out.totalInventoryRows}  legacy=${out.legacyEntries}`);
  console.log('DENOMINATORS (never summed — heterogeneous units):');
  for (const [k, v] of Object.entries(denom)) console.log(`  ${k.padEnd(34)} ${v}`);
  console.log(`d1: ${JSON.stringify(d1Counts)}`);
  console.log(`testStatus: ${JSON.stringify(testCounts)}`);
  console.log(`legacy dispositions: ${JSON.stringify(out.legacyDispositionCounts)}`);
  console.log(`local active REQUIRED_FALSIFIABLE: ${out.localActiveRequiredFalsifiable}  ·  discriminating-verified: ${out.withDiscriminatingTestVerified}  ·  proofs verified: ${out.proofsVerified}  ·  defects unrepaired: ${out.defectsUnrepaired}`);
  console.log(`markdown: ${mdState}`);
  if (errs.length) { console.log(`\nBRANCH REGISTRY: FAIL (${errs.length})`); for (const e of errs) console.log(`  ${e.code}: ${e.msg}`); }
  else console.log('\nBRANCH REGISTRY: PASS');
}
process.exitCode = errs.length ? 1 : 0;
