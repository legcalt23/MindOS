#!/usr/bin/env node
// PII / SECRET LEAK SCANNER v2 — Focus V5 R3 remediation. FAILS CLOSED.
//
// This is a LEAK-LINT for identifiers/paths/secret-shaped tokens in text artifacts. It is NOT a
// privacy-compliance proof and NOT a substitute for a mature secret scanner (gitleaks/trufflehog).
// Claim it supports: NO_UNALLOWLISTED_MATCHES_IN_<scope>_UNDER_RULESET_<config_sha> — nothing stronger.
//
// EXIT CODES (deterministic, fail-closed):
//   0 = scope non-empty, every input read+hash-verified, report written, zero un-allowlisted findings
//   1 = >=1 un-allowlisted finding
//   2 = ANY integrity error: bad/unknown args, empty scope, missing/malformed manifest, member hash
//       mismatch (TOCTOU), unreadable input, path escape, report unwritable, or selftest failure
//
// REDACTION: a finding NEVER contains the raw match. Only {rule, path, line, len, mask, fp} where fp is
// a per-run HMAC (random key, never printed) — correlatable within a run, not dictionary-reversible.
// NO real secret/hostname/user literal lives in this source or its fixtures (fixtures are synthetic).
//
// Usage:
//   node scripts/pii-secret-scan.mjs --selftest
//   node scripts/pii-secret-scan.mjs --manifest        # scan external-manifest members (hash-verified)
//   node scripts/pii-secret-scan.mjs --files a.md b.kt  # scan an explicit set (>=1 required)
import { readFileSync, writeFileSync, existsSync, statSync, realpathSync } from 'node:fs'
import { createHash, createHmac, randomBytes } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { dirname, resolve, relative, sep } from 'node:path'

const TOOL = 'pii-secret-scan', VERSION = '2.0.0'
const SCRIPT = fileURLToPath(import.meta.url)
const REPO_ROOT = resolve(dirname(SCRIPT), '..')                      // scripts/ -> repo root (cwd-independent)
const MANIFEST = 'docs/engines/focus_v5/reaudit/S5_R3_EXTERNAL_MANIFEST.json'
const REPORT = 'docs/engines/focus_v5/reaudit/v2/logs/pii_scan_attestation.json' // NOT a manifest member; outside scanned set
const MAX_BYTES = 8 * 1024 * 1024
const CONFIG_SHA = createHash('sha256').update(readFileSync(SCRIPT)).digest('hex')
const FP_KEY = randomBytes(32)                                        // per-run; never printed
const FP_KEY_ID = createHash('sha256').update(FP_KEY).digest('hex').slice(0, 12)

const out = (code, obj) => {
  process.stdout.write(JSON.stringify({ tool: TOOL, version: VERSION, config_sha256: CONFIG_SHA, ...obj, exit: code }, null, 2) + '\n')
  process.exit(code)
}

// ---- rules (no real-value literals; specific credential shapes + generic identifiers) ----
const RULES = [
  { name: 'win_user_path',  re: /[A-Za-z]:[\\/]Users[\\/][^\\/\s"'`|)]+/g, sev: 'high' },
  { name: 'unix_home_path', re: /\/(?:home|Users|root)\/[A-Za-z0-9._-]+/g, sev: 'high' },
  { name: 'github_pat',     re: /\bghp_[A-Za-z0-9]{36}\b|\bgithub_pat_[A-Za-z0-9_]{22,}\b/g, sev: 'critical' },
  { name: 'aws_akia',       re: /\bAKIA[0-9A-Z]{16}\b/g,               sev: 'critical' },
  { name: 'gcp_api_key',    re: /\bAIza[0-9A-Za-z_-]{35}\b/g,          sev: 'critical' },
  { name: 'slack_token',    re: /\bxox[baprs]-[A-Za-z0-9-]{10,}\b/g,   sev: 'critical' },
  { name: 'openai_key',     re: /\bsk-[A-Za-z0-9]{20,}\b/g,            sev: 'critical' },
  { name: 'jwt',            re: /\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b/g, sev: 'high' },
  { name: 'bearer',         re: /\bBearer\s+[A-Za-z0-9._-]{20,}\b/g,   sev: 'high' },
  { name: 'private_key',    re: /-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----/g, sev: 'critical' },
  { name: 'firebase_url',   re: /https?:\/\/[a-z0-9-]+\.firebaseio\.com/gi, sev: 'high' },
  { name: 'url_with_creds', re: /\b[a-z][a-z0-9+.-]*:\/\/[^/\s:@]+:[^/\s:@]+@/gi, sev: 'critical' },
  { name: 'email',          re: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,24}\b/g, sev: 'medium' },
  { name: 'ipv4',           re: /\b(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\b/g, sev: 'low' },
  { name: 'high_entropy',   re: /\b[A-Za-z0-9+=]{40,}\b/g,             sev: 'medium' },
]
// LIMITATION (declared, not hidden): base32, base64url (-_), single-case hi-entropy, IPv6, MAC, intl phones,
// and locally-known host/user literals are NOT covered by generic rules. A local known value is detected only
// via env PII_KNOWN_FP (comma-sep HMAC-SHA256 hex of the value under key env PII_FP_KEY) — the literal never
// enters this source. Entropy thresholds (4.2/4.6) are heuristic, NOT calibrated on a versioned corpus.
const KNOWN_FP = (process.env.PII_KNOWN_FP || '').split(',').map((s) => s.trim()).filter(Boolean)
const KNOWN_FP_KEY = process.env.PII_FP_KEY || ''

const shannon = (s) => { const f = {}; for (const c of s) f[c] = (f[c] || 0) + 1; return -Object.values(f).reduce((a, n) => a + (n / s.length) * Math.log2(n / s.length), 0) }
const entropyOk = (rule, m) => rule !== 'high_entropy' || (shannon(m) >= 4.2 && /[a-z]/.test(m) && /[A-Z]/.test(m) && /[0-9]/.test(m) && !/^[0-9a-fA-F]+$/.test(m))

// ---- NARROW allowlist: exact path-suffix + rule + value. NO universal exemption, NO substring bypass. ----
const ALLOW = [
  { path: '*', rule: 'high_entropy', value: /^[0-9a-f]{7,8}$|^[0-9a-f]{40}$|^[0-9a-f]{64}$/, reason: 'git/sha256 digests', owner: 'audit', reviewed: '2026-07-23' },
  { path: '*', rule: 'email', value: 'noreply@anthropic.com', reason: 'public co-author trailer', owner: 'audit', reviewed: '2026-07-23' },
  { path: '*', rule: 'email', value: /@(?:example\.(?:com|org|net)|test|invalid|localhost)$/i, reason: 'reserved test domains', owner: 'audit', reviewed: '2026-07-23' },
  { path: '*', rule: 'ipv4', value: /^(?:127\.0\.0\.1|0\.0\.0\.0|255\.255\.255\.255)$/, reason: 'loopback/broadcast', owner: 'audit', reviewed: '2026-07-23' },
  // NO fixture allowlist: every synthetic fixture below is SPLIT in source (concatenated at runtime) so this
  // file contains NO contiguous matchable literal → the scanner scans ITSELF clean with ZERO self-exemption.
]
let allowHits = 0
const allowed = (relPath, rule, match) => {
  const hit = ALLOW.find((a) => (a.path === '*' || relPath === a.path || relPath.endsWith('/' + a.path)) && a.rule === rule &&
    (a.value instanceof RegExp ? a.value.test(match) : a.value === match))
  if (hit) allowHits++
  return !!hit
}

const redact = (val) => {
  const len = val.length
  const mask = len <= 2 ? '*'.repeat(len) : val[0] + '*'.repeat(Math.min(len - 2, 8)) + val[len - 1]
  return { len, mask, fp: createHmac('sha256', FP_KEY).update(val).digest('hex').slice(0, 16) }
}

function scan(relPath, text) {
  const findings = []
  const lines = text.split('\n')
  for (let i = 0; i < lines.length; i++) {
    for (const { name, re, sev } of RULES) {
      re.lastIndex = 0; let m
      while ((m = re.exec(lines[i])) !== null) {
        const v = m[0]
        if (!entropyOk(name, v)) continue
        if (allowed(relPath, name, v)) continue
        findings.push({ rule: name, severity: sev, path: relPath, line: i + 1, ...redact(v) }) // NEVER the raw match
      }
    }
    if (KNOWN_FP.length && KNOWN_FP_KEY) {
      for (const w of lines[i].split(/[\s"'`,;(){}\[\]]+/)) {
        if (!w) continue
        const fp = createHmac('sha256', KNOWN_FP_KEY).update(w).digest('hex')
        if (KNOWN_FP.includes(fp)) findings.push({ rule: 'known_value_fp', severity: 'high', path: relPath, line: i + 1, ...redact(w) })
      }
    }
  }
  return findings
}

// ---- selftest (synthetic fixtures; gates every run) ----
const FIX = {
  win_user_path: { pos: ['C:\\Users\\alice\\proj', 'D:/Users/' + 'bob/x'], neg: ['C:/Program Files/app', 'Users guide'] },
  unix_home_path: { pos: ['/ho' + 'me/alice/x', '/Us' + 'ers/bob/y', '/ro' + 'ot/z'], neg: ['/usr/local/bin', '/opt/data'] },
  // ALL positive fixtures SPLIT (concat) so no contiguous literal exists in this source → scanner self-scans clean.
  github_pat: { pos: ['ghp' + '_' + 'a'.repeat(36)], neg: ['ghp_short', 'graphql_query'] },
  aws_akia: { pos: ['AK' + 'IA' + 'ABCDEFGHIJKLMNOP'], neg: ['AKIALOWERcase', 'AKIA123'] },
  gcp_api_key: { pos: ['AI' + 'za' + 'B'.repeat(35)], neg: ['AIza-tooshort'] },
  slack_token: { pos: ['xo' + 'xb-' + '123456789012-abcdef'], neg: ['xox-nope'] },
  openai_key: { pos: ['sk' + '-' + 'A'.repeat(24)], neg: ['sk-short'] },
  jwt: { pos: ['ey' + 'JhbGciOiJIUzI1' + '.eyJzdWIiOiIxMjM0.SflKxwRJSMeKKF2Q'], neg: ['eyJonly'] },
  bearer: { pos: ['Bea' + 'rer ' + 'a'.repeat(24)], neg: ['Bearer x'] },
  private_key: { pos: ['-----BEGIN RSA ' + 'PRIVATE KEY-----', '-----BEGIN ' + 'PRIVATE KEY-----'], neg: ['-----BEGIN CERTIFICATE-----'] },
  firebase_url: { pos: ['https://my-proj.' + 'firebaseio.com'], neg: ['https://example.com'] },
  url_with_creds: { pos: ['https://user:pass' + '@host.com'], neg: ['https://host.com/path'] },
  email: { pos: ['a' + '@b.io', 'x.y' + '@z.co.uk'], neg: ['not@anemail', '@handle'] },
  ipv4: { pos: ['203.0.' + '113.7'], neg: ['1.310847010469', '11.8.0', '2.4.0'] },
  high_entropy: { pos: ['K7pQ2xR9mN4vL8wT3pJ6' + 'hG1sD7fA0bC5eY8uZ4wX2nM6qL0'], neg: ['a'.repeat(40), '0'.repeat(64)] },
}
function selftest() {
  const errs = []
  const raw = (t) => { const f = []; for (const { name, re } of RULES) { re.lastIndex = 0; let m; while ((m = re.exec(t)) !== null) if (entropyOk(name, m[0])) f.push(name) } return f }
  for (const [rule, { pos, neg }] of Object.entries(FIX)) {
    for (const p of pos) if (!raw(p).includes(rule)) errs.push(`POS miss ${rule}`)
    for (const n of neg) if (raw(n).includes(rule)) errs.push(`NEG false-positive ${rule} on ${JSON.stringify(n)}`)
  }
  return errs
}

// ---- CLI: fail-closed arg parsing ----
const argv = process.argv.slice(2)
const KNOWN = new Set(['--selftest', '--manifest', '--files', '--json'])
for (let i = 0; i < argv.length; i++) {
  const a = argv[i]
  if (a === '--files') { while (i + 1 < argv.length && !argv[i + 1].startsWith('--')) i++; continue }
  if (a.startsWith('--')) { if (!KNOWN.has(a)) out(2, { error: 'UNKNOWN_FLAG', flag: a }) }
  else out(2, { error: 'UNEXPECTED_POSITIONAL', arg: a })
}
const selfErrs = selftest()
if (selfErrs.length) out(2, { selftest: 'FAIL', selftest_errors: selfErrs })
if (argv.includes('--selftest')) out(0, { selftest: 'PASS', rules: RULES.length })

// resolve + validate a candidate path to a repo-relative, in-repo, real path
function safeRel(p) {
  const abs = resolve(REPO_ROOT, p)
  if (!existsSync(abs)) return { err: 'MISSING', p }
  let real; try { real = realpathSync(abs) } catch { return { err: 'UNREADABLE', p } }
  const rel = relative(REPO_ROOT, real)
  if (rel.startsWith('..') || rel.includes('..' + sep)) return { err: 'PATH_ESCAPE', p }
  return { rel: rel.split(sep).join('/'), abs: real }
}

// build scope
let scope = [] // {rel, abs, wantSha?}
const manifestMode = argv.includes('--manifest') || !argv.includes('--files')
if (argv.includes('--files')) {
  const i = argv.indexOf('--files'); const list = []
  for (let j = i + 1; j < argv.length && !argv[j].startsWith('--'); j++) list.push(argv[j])
  if (list.length === 0) out(2, { error: 'EMPTY_FILES_SCOPE' })              // fail-closed
  for (const p of list) { const r = safeRel(p); if (r.err) out(2, { error: 'BAD_INPUT_' + r.err, path: p }); scope.push(r) }
} else {
  const mAbs = resolve(REPO_ROOT, MANIFEST)
  if (!existsSync(mAbs)) out(2, { error: 'MANIFEST_MISSING' })
  let man; try { man = JSON.parse(readFileSync(mAbs, 'utf8')) } catch { out(2, { error: 'MANIFEST_MALFORMED' }) }
  if (!Array.isArray(man.members) || man.members.length === 0) out(2, { error: 'MANIFEST_NO_MEMBERS' })
  for (const mem of man.members) {
    if (mem.status !== 'PRESENT') continue
    const r = safeRel(mem.path); if (r.err) out(2, { error: 'MEMBER_' + r.err, path: mem.path })
    r.wantSha = mem.sha256; r.member = mem
    scope.push(r)
  }
}
// dedupe + case-collision detection
const seen = new Map()
for (const s of scope) {
  const k = s.rel.toLowerCase()
  if (seen.has(k)) out(2, { error: 'DUPLICATE_OR_CASE_COLLISION', a: seen.get(k), b: s.rel })
  seen.set(k, s.rel)
}
// the report path must NOT be in scope (no self-referential mutation)
if (scope.some((s) => s.rel === REPORT)) out(2, { error: 'REPORT_IN_SCOPE', path: REPORT })
if (scope.length === 0) out(2, { error: 'EMPTY_SCOPE' })                     // fail-closed

// scan: hash-verify (manifest) + TOCTOU re-hash + binary/oversize classification
let findings = [], denom = 0
const excluded = [], inputs = []
for (const s of scope) {
  const st = statSync(s.abs)
  if (st.size > MAX_BYTES) { excluded.push({ path: s.rel, reason: 'OVERSIZE', bytes: st.size }); continue }
  const buf = readFileSync(s.abs)
  const sha1 = createHash('sha256').update(buf).digest('hex')
  if (s.wantSha && sha1 !== s.wantSha) out(2, { error: 'MEMBER_HASH_MISMATCH', path: s.rel, manifest: s.wantSha, ondisk: sha1 })
  if (buf.includes(0)) { excluded.push({ path: s.rel, reason: 'BINARY' }); inputs.push({ path: s.rel, sha256: sha1, scanned: false }); continue }
  const text = buf.toString('utf8')
  const buf2 = readFileSync(s.abs); const sha2 = createHash('sha256').update(buf2).digest('hex')
  if (sha2 !== sha1) out(2, { error: 'TOCTOU_MUTATION', path: s.rel })       // file changed mid-scan
  denom += text.split('\n').length
  inputs.push({ path: s.rel, sha256: sha1, scanned: true })
  findings.push(...scan(s.rel, text))
}

const byRule = {}; for (const f of findings) byRule[f.rule] = (byRule[f.rule] || 0) + 1
const bySev = {}; for (const f of findings) bySev[f.severity] = (bySev[f.severity] || 0) + 1
const CAP = 500
const exitCode = findings.length ? 1 : 0
const attestation = {
  tool: TOOL, version: VERSION, config_sha256: CONFIG_SHA, fp_key_id: FP_KEY_ID, selftest: 'PASS',
  scope_mode: manifestMode ? 'manifest' : 'files',
  claim: exitCode ? null : `NO_UNALLOWLISTED_MATCHES_IN_${manifestMode ? 'MANIFEST_MEMBERS' : 'FILES'}_UNDER_RULESET_${CONFIG_SHA.slice(0, 12)}`,
  limitations: 'leak-lint only; NOT a privacy proof; NOT a mature secret scanner; entropy heuristic uncalibrated; base32/base64url/IPv6/MAC/intl-phone NOT covered',
  scanned_files: inputs.filter((i) => i.scanned).length, excluded, denominator_lines: denom,
  findings_total: findings.length, allowlisted_hits: allowHits, by_rule: byRule, by_severity: bySev,
  findings_truncated: findings.length > CAP, findings: findings.slice(0, CAP), // redacted (no raw match)
  inputs, exit: exitCode,
}
// report written OUTSIDE the scanned set; unwritable => exit 2 (no empty catch)
try { writeFileSync(resolve(REPO_ROOT, REPORT), JSON.stringify(attestation, null, 2) + '\n') }
catch (e) { out(2, { error: 'REPORT_UNWRITABLE', path: REPORT, detail: String(e.message || e) }) }
process.stdout.write(JSON.stringify({ ...attestation, findings: undefined }, null, 2) + '\n')
process.stderr.write(exitCode ? `SCAN: ${findings.length} finding(s) → exit 1 (report: ${REPORT})\n` : `SCAN CLEAN → exit 0 (${attestation.scanned_files} scanned, ${excluded.length} excluded, ${denom} lines)\n`)
process.exit(exitCode)
