// Regression tests for scripts/pii-secret-scan.mjs — each targets a DEMONSTRATED defect (mandate A–F, I).
// Run: node --test scripts/pii-secret-scan.test.mjs   (from repo root)
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { writeFileSync, rmSync, readFileSync, existsSync } from 'node:fs'

const SCANNER = 'scripts/pii-secret-scan.mjs'
const ATTEST = 'docs/engines/focus_v5/reaudit/v2/logs/pii_scan_attestation.json'
// run the CLI; return {code, stdout}. execFileSync throws on non-zero exit → capture code.
function run(args) {
  try { const stdout = execFileSync('node', [SCANNER, ...args], { encoding: 'utf8' }); return { code: 0, stdout } }
  catch (e) { return { code: e.status, stdout: (e.stdout || '') + (e.stderr || '') } }
}

test('defect-none: --selftest passes (exit 0)', () => {
  assert.equal(run(['--selftest']).code, 0)
})

test('defect-C: empty --files scope fails CLOSED (exit 2), not a vacuous CLEAN', () => {
  const r = run(['--files'])
  assert.equal(r.code, 2)
  assert.match(r.stdout, /EMPTY_FILES_SCOPE/)
})

test('defect-I: unknown flag rejected (exit 2)', () => {
  assert.equal(run(['--bogus']).code, 2)
})

test('defect-I: unexpected positional rejected (exit 2)', () => {
  assert.equal(run(['whatever.txt']).code, 2)
})

test('defect-6/7: path escape / missing input fails CLOSED (exit 2)', () => {
  assert.equal(run(['--files', '../outside.txt']).code, 2)
})

test('defect-B: scanner self-scans CLEAN (exit 0, 0 findings) and has NO universal self-exemption', () => {
  const r = run(['--files', SCANNER])
  assert.equal(r.code, 0)
  const rep = JSON.parse(readFileSync(ATTEST, 'utf8'))
  assert.equal(rep.findings_total, 0)
  const src = readFileSync(SCANNER, 'utf8')
  assert.ok(!/rule:\s*'\*'\s*,\s*value:\s*\/\.\*\//.test(src), 'must not contain a universal {rule:*, value:/.*/} exemption')
  assert.ok(!/file:\s*'pii-secret-scan'\s*,\s*rule:\s*'\*'/.test(src), 'must not exempt its own whole file')
})

test('defect-D: a real-shaped secret is DETECTED but never appears raw in the attestation (redaction)', () => {
  const tmp = 'docs/engines/focus_v5/reaudit/v2/logs/_TMP_redaction_fixture.txt'
  const synthetic = 'AKIA' + 'ABCDEFGHIJKLMNOP' // synthetic AWS-shaped key (not real)
  writeFileSync(tmp, 'leak here: ' + synthetic + '\n')
  try {
    const r = run(['--files', tmp])
    assert.equal(r.code, 1, 'must flag the secret')
    const raw = readFileSync(ATTEST, 'utf8')
    assert.ok(!raw.includes(synthetic), 'the raw secret must NOT be written to the report')
    assert.ok(!r.stdout.includes(synthetic), 'the raw secret must NOT be printed to stdout')
    const rep = JSON.parse(raw)
    assert.ok(rep.by_rule.aws_akia >= 1, 'aws_akia rule must fire')
    assert.ok(rep.findings.every((f) => !('match' in f) && 'fp' in f && 'mask' in f), 'findings carry fp+mask, never match')
  } finally { rmSync(tmp, { force: true }) }
})

test('defect-C: duplicate / case-collision input fails CLOSED (exit 2)', () => {
  assert.equal(run(['--files', SCANNER, SCANNER]).code, 2)
})

test('defect-F: the attestation path is written OUTSIDE the scanned set (never a scanned member)', () => {
  run(['--files', SCANNER])
  assert.ok(existsSync(ATTEST))
  const man = JSON.parse(readFileSync('docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json', 'utf8'))
  assert.ok(!man.members.some((m) => m.path.endsWith('pii_scan_attestation.json')), 'attestation must not be a manifest member')
})

test('defect-A: scanner source contains no known_hostname rule (no hardcoded real host literal)', () => {
  const src = readFileSync(SCANNER, 'utf8')
  assert.ok(!/known_hostname/.test(src), 'the known_hostname rule (which hardcoded a real host) must be gone')
})
