#!/usr/bin/env node
// EXTERNAL MANIFEST BUILDER — Focus V5 · S5 · R3 REAUDIT package closure.
// Emits FULL SHA-256 (64 hex), byte length, EOL type, last byte, relative path for every package
// member, resolving the handoff self-reference paradox (§3.1.5): the handoff cannot hash itself,
// but THIS manifest hashes it. Run from repo root:
//   node docs/engines/focus_v5/reaudit/build_external_manifest.mjs
// Output: docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json (+ .md)
//
// Hashes are of WORKTREE BYTES as-on-disk. EOL is reported so an external checkout under a
// different core.autocrlf can reconcile (see .gitattributes: the ledger is pinned eol=lf).

import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync, existsSync, statSync } from 'node:fs'

// Package closure: the R3 external-audit set. Process/host artifacts (S5_R4_*) are EXCLUDED
// (absolute usernames; §6 of the handoff). The legacy V5 bundle is delivered only via the index.
const MEMBERS = [
  // R3 authored/corrected docs
  'docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md',
  'docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md',
  'docs/engines/focus_v5/PROVENANCE_INCIDENT_V5_BUNDLE.md',
  'docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md',
  'docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.json',
  'docs/engines/focus_v5/S5_R3_P7_HANDOFF.md',
  'docs/engines/focus_v5/S5_REOPEN_STATUS.md',
  'docs/engines/focus_v5/ADR_REGISTER_V5.md',
  // reaudit primary evidence
  'docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_MASTER_REPORT.md',
  'docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_FINDINGS.json',
  'docs/engines/focus_v5/reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md',
  'docs/engines/focus_v5/reaudit/S5_R3_ELIGIBILITY_CONTRACT_SPEC.md',
  'docs/engines/focus_v5/reaudit/S5_R3_DATAFLOW_THREATMODEL.md',
  'docs/engines/focus_v5/reaudit/S5_R3_BLOCKERS_MATRIX.md',
  'docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs',
  'docs/engines/focus_v5/reaudit/r2_falsification_result.json',
  'docs/engines/focus_v5/reaudit/r2_falsification_summary.txt',
  'scripts/validate-authority-entrypoints.mjs',
  // V2 reaudit tooling + evidence
  'docs/engines/focus_v5/authority-registry.json',
  'scripts/validate-authority-entrypoints.test.mjs',
  'docs/engines/focus_v5/reaudit/v2/findings_v2.json',
  'docs/engines/focus_v5/reaudit/v2/validate_cross_artifact.mjs',
  'docs/engines/focus_v5/reaudit/v2/phase0_inventory.mjs',
  'docs/engines/focus_v5/reaudit/v2/phase0_inventory.json',
  'docs/engines/focus_v5/reaudit/v2/logs/command_log.jsonl',
  'shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus2/domain/scoring/DescriptiveScorerGoldenReauditTest.kt',
  // phase-0 tracked members carried into the closure (tooling + V1 reaudit docs)
  'scripts/validate-audit-ledger.mjs',
  'docs/engines/focus_v5/reaudit/build_external_manifest.mjs',
  'docs/engines/focus_v5/reaudit/S5_R3_CEO_MEMO.md',
  'docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_REDLINE.md',
  'docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_COMMAND_LOG.md',
  'docs/engines/focus_v5/reaudit/S5_R3_LENS_OUTPUTS.json',
  // V2 COMPLETION artifacts (2026-07-23): execution evidence, lineage, root-of-trust, domain, decision packets
  'docs/engines/focus_v5/reaudit/v2/S5_R3_REAUDIT_V2_MASTER_REPORT.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_KOTLIN_EXECUTION_REPORT.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_FINDINGS_LINEAGE.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_LENS_COVERAGE.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_ROOT_OF_TRUST_AUDIT.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_R2_DOMAIN_SPEC.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_CEO_DECISION_PACKET.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_PSYCHOMETRIC_PROTOCOL.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_PRIVACY_DATA_MAP.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_ACCESSIBILITY_DECISION_RECORD.md',
  'docs/engines/focus_v5/reaudit/v2/S5_R3_LOG_AND_PII.md',
  'docs/engines/focus_v5/reaudit/v2/README_VERIFY.md',
  'scripts/authority-scanner.mutation.mjs',
  'scripts/pii-secret-scan.mjs',
  'scripts/pii-secret-scan.test.mjs',
  'docs/engines/focus_v5/reaudit/v2/logs/focus2_full_suite_result.json',
  // NOTE: pii_scan_attestation.json is INTENTIONALLY NOT a member — the scanner writes it, and a scanned
  // member must be immutable during the scan (mandate defect F). The old pii_scan_report.json is superseded.
  'docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorerGoldenReauditTest.xml',
  'docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibilityTest.xml',
  'docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-com.mindos.app.features.mindboost.focus2.domain.evidence.FocusEvidenceStatePolicyTest.xml',
  // authoritative cumulative ledger (its own §0 self-excluding seal is separate; here we pin full bytes)
  'MINDBOOST_AUDIT_LEDGER.md',
]

function eolType(buf) {
  const s = buf.toString('latin1')
  const crlf = (s.match(/\r\n/g) || []).length
  const lfOnly = (s.match(/(^|[^\r])\n/g) || []).length
  if (crlf > 0 && lfOnly > 0) return 'MIXED'
  if (crlf > 0) return 'CRLF'
  if (s.includes('\n')) return 'LF'
  return 'NONE'
}

const members = []
let missing = 0
for (const rel of MEMBERS) {
  if (!existsSync(rel)) { members.push({ path: rel, status: 'MISSING' }); missing += 1; continue }
  const buf = readFileSync(rel)
  members.push({
    path: rel,
    status: 'PRESENT',
    bytes: buf.length,
    sha256: createHash('sha256').update(buf).digest('hex'),
    eol: eolType(buf),
    last_byte_hex: buf.length ? buf[buf.length - 1].toString(16).padStart(2, '0') : null,
    mtime: statSync(rel).mtimeMs, // informational only (not part of the digest)
  })
}

const manifest = {
  manifest: 'S5_R3_EXTERNAL_MANIFEST',
  purpose: 'External-audit package closure for Focus V5 S5 R3 (reaudit). Full SHA-256, bytes, EOL, last byte.',
  review_class: 'AI_ASSISTED_INDEPENDENT_REVIEW (not a human/institutional EXTERNAL_AUDIT)',
  hash_algorithm: 'sha256 (64 hex) over worktree bytes as-on-disk',
  eol_note: 'EOL reported per member; ledger pinned eol=lf via .gitattributes. A checkout under a different '
    + 'core.autocrlf will change CRLF/MIXED member hashes — reconcile via the reported eol + byte length.',
  canonicalization: {
    algorithm: 'package_root_sha256 = sha256( utf8( sort_asc( "<relpath>\\t<member_sha256>\\t<bytes>" for each PRESENT member ).join("\\n") ) )',
    path_order: 'ascending byte sort of the joined triple lines (order-independent of MEMBERS[])',
    member_hash: 'sha256 of the file bytes as-on-disk (worktree)',
    eol_policy: 'not normalized; EOL reported per member for external reconciliation',
    symlinks: 'none in the package (all members are regular files)',
    manifest_self_treatment: 'the manifest is the ROOT and is NOT a member of itself (no self-hash paradox)',
    anchor: 'FAIL_UNANCHORED — a self-contained manifest proves integrity BETWEEN members, not authenticity; '
      + 'the root must be anchored externally (signed commit/tag or immutable CI artifact) before PASS.',
  },
  head: null, // filled below
  member_count_present: members.filter((m) => m.status === 'PRESENT').length,
  member_count_missing: missing,
  members,
}

// Root digest = sha256 over the sorted (path, sha256, bytes) triples — stable regardless of member order.
const rootBasis = members
  .filter((m) => m.status === 'PRESENT')
  .map((m) => `${m.path}\t${m.sha256}\t${m.bytes}`)
  .sort()
  .join('\n')
manifest.package_root_sha256 = createHash('sha256').update(rootBasis, 'utf8').digest('hex')

const json = JSON.stringify(manifest, null, 2)
writeFileSync('docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json', json)

// Human-readable companion.
const lines = []
lines.push('# S5_R3_EXTERNAL_MANIFEST — Focus V5 · S5 · R3 (reaudit) package closure')
lines.push('')
lines.push('```')
lines.push('REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW (not a human/institutional external audit)')
lines.push('HASH: sha256 (full 64 hex) over worktree bytes as-on-disk')
lines.push(`PACKAGE_ROOT_SHA256: ${manifest.package_root_sha256}`)
lines.push(`MEMBERS_PRESENT: ${manifest.member_count_present}   MISSING: ${manifest.member_count_missing}`)
lines.push('```')
lines.push('')
lines.push('> The manifest resolves the handoff self-reference paradox: the handoff cannot contain its own hash, ')
lines.push('> but this sibling manifest hashes it. To verify: checkout the same HEAD, run this script, compare ')
lines.push('> `package_root_sha256` and each member sha256. EOL differences (core.autocrlf) shift CRLF/MIXED hashes.')
lines.push('')
lines.push('| Member | Status | Bytes | EOL | last | sha256 (full) |')
lines.push('|---|---|---|---|---|---|')
for (const m of members) {
  if (m.status !== 'PRESENT') { lines.push(`| \`${m.path}\` | **${m.status}** | — | — | — | — |`); continue }
  lines.push(`| \`${m.path}\` | ok | ${m.bytes} | ${m.eol} | ${m.last_byte_hex} | \`${m.sha256}\` |`)
}
lines.push('')
writeFileSync('docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.md', lines.join('\n') + '\n')

process.stdout.write(`manifest built: ${manifest.member_count_present} present, ${missing} missing\n`)
process.stdout.write(`package_root_sha256=${manifest.package_root_sha256}\n`)
if (missing > 0) process.stdout.write(`WARN: ${missing} member(s) MISSING (expected until reaudit artifacts are written)\n`)
