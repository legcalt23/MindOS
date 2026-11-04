#!/usr/bin/env node
// PHASE 4 (V2 COMPLETION) — BIDIRECTIONAL cross-artifact integrity validator for the S5 R3 reaudit package.
// Fails (exit 1) if the package is internally incoherent. Structured JSON errors.
// Run from repo root: node docs/engines/focus_v5/reaudit/v2/validate_cross_artifact.mjs [--json]
//
// Bidirectional means: not only "every finding → a real ledger row", but also "every OPEN reaudit-scope ledger
// row → a finding", "every blocker → finding+ledger", "every V1 ledger ref → real row or recorded mislabel",
// "no ledger ID maps to two findings", "manifest ⊆ inventory". A one-directional subset check (12 ⊂ 130) cannot
// catch a dropped finding, a dangling blocker, or a silently-reused ID — those need the reverse direction.
import { readFileSync, existsSync } from 'node:fs'

const err = []
const E = (code, where, msg) => err.push({ code, where, message: msg })
const read = (p) => (existsSync(p) ? readFileSync(p, 'utf8') : null)

const findings = JSON.parse(readFileSync('docs/engines/focus_v5/reaudit/v2/findings_v2.json', 'utf8'))
const v1raw = read('docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_FINDINGS.json')
const v1 = v1raw ? JSON.parse(v1raw) : { findings: [] }
const ledger = read('MINDBOOST_AUDIT_LEDGER.md') || ''
const blockers = read('docs/engines/focus_v5/reaudit/S5_R3_BLOCKERS_MATRIX.md') || ''
const manifestMd = read('docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.md') || ''
const lineage = read('docs/engines/focus_v5/reaudit/v2/S5_R3_FINDINGS_LINEAGE.md') || ''
const inv = JSON.parse(read('docs/engines/focus_v5/reaudit/v2/phase0_inventory.json') || '{}')

// ---- register: ID -> row text (first backticked cell of a §2 table row with >=12 columns) ----
const ledgerRow = new Map()
for (const l of ledger.split('\n')) {
  const m = l.match(/^\|\s*`(MB-[A-Z]+-\d{4})`\s*\|/)
  if (m && l.split('|').length >= 12) ledgerRow.set(m[1], l)
}
const ledgerIds = new Set(ledgerRow.keys())

// The reaudit scope: the register rows this R3 reaudit owns (0035 pre-existing + 0047..0057 created V1/V2).
const SCOPE = new Set(['MB-FOCUS-0035', ...Array.from({ length: 11 }, (_, i) => `MB-FOCUS-00${47 + i}`)])

// V1 ledger refs that are KNOWN-corrected mislabels/bogus-suffixes, recorded in the lineage doc. A V1 ref that is
// NOT a real row must appear here (and the lineage must document it) or it is drift.
const V1_CORRECTED = new Map([
  ['MB-FOCUS-0050b', 'MB-FOCUS-0050'],
  ['MB-FOCUS-0051b', 'MB-FOCUS-0056'],
  ['MB-FOCUS-0053b', 'MB-FOCUS-0054'],
  ['MB-FOCUS-0054', 'MB-FOCUS-0057'], // V1 findings.json mislabel: 0054 register row is the orphan constant; population is 0057
])

const EN = findings.enums
const byLedger = new Map()

function pathOf(ev) {
  const m = ev.match(/([\w./-]+\.(?:kt|mjs|json|md|txt|xml))/)
  if (!m) return null
  let p = m[1]
  if (/^(docs|shared|scripts|functions)\//.test(p)) return p
  if (p.startsWith('reaudit/')) return 'docs/engines/focus_v5/' + p
  return p
}

// ============ FORWARD: every V2 finding is well-formed and grounded ============
for (const f of findings.findings) {
  for (const [k, list] of Object.entries({
    impact_severity: EN.impact_severity, current_reachability: EN.current_reachability, likelihood: EN.likelihood,
    launch_priority: EN.launch_priority, confidence: EN.confidence, epistemic_class: EN.epistemic_class,
    product_risk_status: EN.product_risk_status, documentation_status: EN.documentation_status,
  })) {
    if (!list.includes(f[k])) E('ENUM_INVALID', f.id, `${k}="${f[k]}" not in enum`)
  }
  if (/b$/.test(f.ledger)) E('LEDGER_B_SUFFIX', f.id, `ledger id has a non-schema 'b' suffix: ${f.ledger}`)
  if (!ledgerIds.has(f.ledger)) E('LEDGER_ID_DANGLING', f.id, `ledger id not found in register: ${f.ledger}`)
  // uniqueness: no two findings claim the same register row
  if (byLedger.has(f.ledger)) E('LEDGER_ID_NONUNIQUE', f.id, `register row ${f.ledger} also claimed by ${byLedger.get(f.ledger)}`)
  else byLedger.set(f.ledger, f.id)
  if (f.product_risk_status === 'OPEN' && !blockers.includes(f.ledger)) E('OPEN_NOT_IN_BLOCKERS', f.id, `product_risk OPEN but ${f.ledger} absent from blockers matrix`)
  if (f.impact_severity === 'CRITICAL' && f.current_reachability === 'LIVE_WIRED' && f.launch_priority !== 'BLOCKER_BEFORE_LAUNCH') E('SEVERITY_MODEL', f.id, 'CRITICAL+LIVE_WIRED must be BLOCKER_BEFORE_LAUNCH')
  if (f.epistemic_class === 'DOCUMENT_ASSERTION_ONLY' && f.documentation_status === 'CORRECTED' && f.product_risk_status === 'N_A_DOCS_ONLY') E('PASS_ON_DOC_ASSERTION', f.id, 'a corrected/closed status cannot rest solely on DOCUMENT_ASSERTION_ONLY')
  for (const ev of f.evidence || []) {
    const p = pathOf(ev)
    if (p && !existsSync(p)) E('EVIDENCE_PATH_MISSING', f.id, `evidence path does not exist: ${p}`)
  }
}

// ============ REVERSE 1: every OPEN reaudit-scope register row has a finding OR an explicit disposition ============
for (const id of SCOPE) {
  if (!ledgerIds.has(id)) { E('SCOPE_ROW_MISSING', id, `reaudit-scope register row absent from ledger`); continue }
  const row = ledgerRow.get(id)
  const hasFinding = byLedger.has(id)
  const dispositioned = /SUPERSEDED|CLOSED|VERIFIED|WITHDRAWN/.test(row)
  if (!hasFinding && !dispositioned) E('SCOPE_ROW_UNMAPPED', id, `scope row has neither a V2 finding nor a CLOSED/SUPERSEDED disposition`)
}

// ============ REVERSE 2: every blocker ledger ref resolves to a real row AND a finding ============
for (const m of blockers.matchAll(/`(MB-FOCUS-\d{4})`/g)) {
  const id = m[1]
  if (!ledgerIds.has(id)) E('BLOCKER_DANGLING_LEDGER', 'blockers', `blocker cites ${id} not in register`)
  else if (!byLedger.has(id) && SCOPE.has(id)) E('BLOCKER_WITHOUT_FINDING', 'blockers', `blocker cites scope row ${id} with no V2 finding`)
}

// ============ REVERSE 3: V1 lineage — every V1 ledger ref is a real row or a recorded corrected mislabel ============
for (const f of (v1.findings || [])) {
  const ref = (f.ledger || '').replace(/\s*\(.*\)\s*$/, '') // strip "(reframed)" etc.
  if (ledgerIds.has(ref)) continue
  if (V1_CORRECTED.has(ref)) {
    // the correction target must be a real row AND the lineage doc must document this ref
    const tgt = V1_CORRECTED.get(ref)
    if (!ledgerIds.has(tgt)) E('V1_CORRECTION_TARGET_MISSING', f.id, `${ref} corrected→${tgt} but ${tgt} not in register`)
    if (!lineage.includes(ref)) E('V1_DRIFT_UNDOCUMENTED', f.id, `V1 ref ${ref} corrected but not documented in lineage doc`)
  } else {
    E('V1_LEDGER_DRIFT', f.id, `V1 ledger ref ${ref} is neither a real register row nor a recorded corrected mislabel`)
  }
}

// ============ REVERSE 4: manifest ↔ files. The phase-0 inventory is a PRE snapshot; the manifest is POST
// (it legitimately includes V2 tooling created after phase 0). So the honest checks are: (a) every manifest
// member exists on disk; (b) nothing the inventory TRACKED (INCLUDE, present) was DROPPED from the manifest. ==
const manifestMembers = new Set([...manifestMd.matchAll(/^\|\s*`([^`]+)`\s*\|/gm)].map((m) => m[1]).filter((p) => /\.(kt|mjs|json|md|txt|xml)$/.test(p)))
for (const p of manifestMembers) {
  if (!existsSync(p) && !p.includes('EXTERNAL_MANIFEST')) E('MANIFEST_MEMBER_MISSING_ON_DISK', 'manifest', `manifest member missing on disk: ${p}`)
}
for (const it of (inv.items || [])) {
  // the manifest's OWN output files cannot be members of themselves (self-reference paradox — see
  // manifest.canonicalization.manifest_self_treatment). Exempt them from the dropped-check.
  if (it.path.includes('S5_R3_EXTERNAL_MANIFEST')) continue
  if (it.disposition === 'INCLUDE' && it.status !== 'MISSING' && !manifestMembers.has(it.path)) {
    E('INVENTORY_ITEM_DROPPED_FROM_MANIFEST', 'manifest', `phase-0 INCLUDE item not carried into the manifest: ${it.path}`)
  }
}

// ============ cross-doc R3 state agreement (nothing approved/frozen/mounted/parity) ============
const stateDocs = {
  handoff: read('docs/engines/focus_v5/S5_R3_P7_HANDOFF.md') || '',
  memo: read('docs/engines/focus_v5/reaudit/S5_R3_CEO_MEMO.md') || '',
  verdicts: read('docs/engines/focus_v5/reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md') || '',
}
for (const [name, c] of Object.entries(stateDocs)) {
  if (/R3_CONTRACT_APPROVED|R3\s+APPROVED\b/.test(c)) E('STATE_DISCREPANCY', name, 'asserts R3 approved')
  if (!/CANDIDATA/i.test(c)) E('STATE_DISCREPANCY', name, 'does not state R3 CANDIDATA')
}

// ============ manifest must declare canonicalization ============
if (!/package_root_sha256|canonical|sorted|hash_algorithm/i.test(manifestMd)) E('MANIFEST_NO_CANONICALIZATION', 'manifest', 'external manifest does not declare a canonicalization/root algorithm')

const ok = err.length === 0
const counts = {
  raw_lens_source: 'reaudit/S5_R3_LENS_OUTPUTS.json',
  v1_canonical: (v1.findings || []).length,
  v2_canonical: findings.findings.length,
  ledger_ids_total: ledgerIds.size,
  reaudit_scope_rows: SCOPE.size,
  blockers_ids: new Set([...blockers.matchAll(/`(MB-FOCUS-\d{4})`/g)].map((m) => m[1])).size,
  mappings_1to1: byLedger.size,
  unmapped_scope_rows: [...SCOPE].filter((id) => !byLedger.has(id) && !/SUPERSEDED|CLOSED|VERIFIED/.test(ledgerRow.get(id) || '')).length,
}
const out = { ok, counts, errors: err }
process.stdout.write(JSON.stringify(out, null, 2) + '\n')
if (!ok) { process.stderr.write(`CROSS-ARTIFACT (BIDIRECTIONAL) FAILED: ${err.length} error(s)\n`); process.exit(1) }
process.stderr.write('CROSS-ARTIFACT (BIDIRECTIONAL) OK\n')
