#!/usr/bin/env node
// PHASE 0 — pre-correction inventory + preimage preservation for the S5 R3 V2 reaudit.
// Records for every package file: repo-relative path, tracked/untracked, HEAD blob hash (if tracked),
// worktree SHA-256, bytes, EOL, purpose, included/excluded + reason. Copies current bytes to preimages/.
// Run from repo root: node docs/engines/focus_v5/reaudit/v2/phase0_inventory.mjs
import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { execSync } from 'node:child_process'

const FILES = [
  // corrected authority docs (untracked)
  ['docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md', 'authority index (edited V1)', 'INCLUDE'],
  ['docs/engines/focus_v5/S5_R3_P7_HANDOFF.md', 'handoff (rewritten V1)', 'INCLUDE'],
  ['docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.json', 'evidence capsule json (edited V1)', 'INCLUDE'],
  ['docs/engines/focus_v5/S5_R3_EVIDENCE_CAPSULE.md', 'evidence capsule md', 'INCLUDE'],
  ['docs/engines/focus_v5/PROVENANCE_INCIDENT_V5_BUNDLE.md', 'provenance incident', 'INCLUDE'],
  ['docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md', 'R3 contract (normative)', 'INCLUDE'],
  ['docs/engines/focus_v5/S5_REOPEN_STATUS.md', 'reopen status', 'INCLUDE'],
  ['docs/engines/focus_v5/ADR_REGISTER_V5.md', 'ADR register (tracked, worktree-modified)', 'INCLUDE'],
  // tooling
  ['scripts/validate-authority-entrypoints.mjs', 'authority scanner V1 (to be redesigned in V2)', 'INCLUDE'],
  ['scripts/validate-audit-ledger.mjs', 'ledger validator', 'INCLUDE'],
  // ledger
  ['MINDBOOST_AUDIT_LEDGER.md', 'cumulative ledger (tracked, worktree-modified)', 'INCLUDE'],
  // reaudit V1 evidence
  ['docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs', 'R2 harness (Node reimpl)', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/r2_falsification_result.json', 'R2 harness result', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/r2_falsification_summary.txt', 'R2 harness summary', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/build_external_manifest.mjs', 'manifest builder', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json', 'external manifest json', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.md', 'external manifest md', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_MASTER_REPORT.md', 'V1 master report', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_FINDINGS.json', 'V1 findings (has b-suffix defect)', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_MULTIDIMENSIONAL_VERDICTS.md', 'V1 verdicts', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_ELIGIBILITY_CONTRACT_SPEC.md', 'eligibility spec', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_DATAFLOW_THREATMODEL.md', 'data-flow/threat model', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_BLOCKERS_MATRIX.md', 'V1 blockers (stale: lacks 0049-0055)', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_COMMAND_LOG.md', 'V1 command log', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_LENS_OUTPUTS.json', 'raw 7-lens outputs', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_REAUDIT_REDLINE.md', 'V1 redline', 'INCLUDE'],
  ['docs/engines/focus_v5/reaudit/S5_R3_CEO_MEMO.md', 'V1 CEO memo', 'INCLUDE'],
  // process artifacts EXCLUDED (absolute usernames)
  ['docs/engines/focus_v5/S5_R4_2_PASO7_EXECUTION_PLAN.md', 'process plan (absolute paths)', 'EXCLUDE'],
  ['docs/engines/focus_v5/S5_R4_2_AUDIT_ANNEX.md', 'process annex (absolute paths)', 'EXCLUDE'],
]

function eol(buf) {
  const s = buf.toString('latin1')
  const crlf = (s.match(/\r\n/g) || []).length
  const lf = (s.match(/(^|[^\r])\n/g) || []).length
  return crlf && lf ? 'MIXED' : crlf ? 'CRLF' : s.includes('\n') ? 'LF' : 'NONE'
}
function headBlob(path) {
  try { return execSync(`git rev-parse HEAD:${path}`, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim() }
  catch { return null }
}
function tracked(path) {
  try { execSync(`git ls-files --error-unmatch ${path}`, { stdio: 'ignore' }); return true } catch { return false }
}

const items = []
for (const [path, purpose, disp] of FILES) {
  if (!existsSync(path)) { items.push({ path, purpose, disposition: disp, status: 'MISSING' }); continue }
  const buf = readFileSync(path)
  const tr = tracked(path)
  const sha = createHash('sha256').update(buf).digest('hex')
  // save preimage (flatten path)
  const flat = path.replace(/[\/\\]/g, '__')
  writeFileSync(`docs/engines/focus_v5/reaudit/v2/preimages/${flat}`, buf)
  items.push({
    path, purpose, disposition: disp, tracked: tr,
    head_blob: tr ? headBlob(path) : null,
    worktree_sha256: sha, bytes: buf.length, eol: eol(buf),
    preimage_saved: `reaudit/v2/preimages/${flat}`,
  })
}

const inv = {
  phase: 0,
  task: 'TASK-2026-07-22-FOCUS-V5-S5-R3-REAUDIT-V2',
  head: '8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5',
  branch: 'product/focus2-v5',
  tools: { node: process.version, jdk: '21.0.10', git: '2.54.0.windows.1', os: 'Windows/MINGW64' },
  preimage_policy: {
    note: 'These are V1-STATE preimages (bytes as they stood after the V1 reaudit, before V2 edits). '
      + 'The PRE-V1 (Paso 7) bytes of the untracked authority docs (handoff/index/capsule) were OVERWRITTEN in '
      + 'V1 without a saved preimage.',
    pre_v1_untracked_authority_docs: 'PREIMAGE_IRRECOVERABLE on disk; content exists only in the session '
      + 'transcript (.jsonl). Declared honestly per V2 mandate Phase 0.3.',
    pre_v1_tracked_docs: 'RECOVERABLE via HEAD blob (ledger/ADR_REGISTER/05/09/CLAUDE.md — git ls-tree).',
  },
  items,
}
writeFileSync('docs/engines/focus_v5/reaudit/v2/phase0_inventory.json', JSON.stringify(inv, null, 2))
const inc = items.filter((i) => i.disposition === 'INCLUDE' && i.status !== 'MISSING').length
const miss = items.filter((i) => i.status === 'MISSING').length
process.stdout.write(`inventory: ${items.length} entries, ${inc} INCLUDE present, ${miss} MISSING, preimages saved\n`)
