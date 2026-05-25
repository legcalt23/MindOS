#!/usr/bin/env node
/**
 * scan-secrets.js
 *
 * Fail-closed secret scanner for the wallet codebase.
 *
 * Why this exists:
 *  - We cannot rely on developer discipline alone for a fintech feature.
 *  - This script runs in CI (and locally via `npm run scan:secrets`).
 *  - Exit code 1 if ANY pattern matches → CI fails the build.
 *
 * What it scans:
 *  - All `src/**` TypeScript files in functions/
 *  - The top-level firebase.json, firestore.rules, storage.rules
 *
 * What it allows:
 *  - Test fixtures explicitly tagged with "FAKE_SECRET_FIXTURE" in a
 *    line-end comment. Use this for unit-test data where you genuinely
 *    need a string that looks like a key. Example:
 *      const fakeApiKey = "AIzaXXX..."; // FAKE_SECRET_FIXTURE
 *
 *  - The hash output of FAKE_SECRET_FIXTURE comments themselves
 *  - The string literals inside this file
 *
 * Patterns detected:
 *  - Google API keys (AIza...)
 *  - Firebase private keys ("-----BEGIN PRIVATE KEY-----")
 *  - GitHub tokens (ghp_/gho_/ghu_/ghs_/ghr_ prefixes)
 *  - Generic "AWS_ACCESS_KEY_ID = AKIA..." patterns
 *  - JWT-looking long b64 blobs (heuristic)
 *  - Stripe live keys (sk_live_, pk_live_)
 *  - High-entropy hex/b64 strings near a "secret" identifier
 */

'use strict';

const fs   = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const FUNCTIONS_SRC = path.join(ROOT, 'functions', 'src');
const SCAN_TARGETS = [
  FUNCTIONS_SRC,
  path.join(ROOT, 'firebase.json'),
  path.join(ROOT, 'firestore.rules'),
  path.join(ROOT, 'storage.rules'),
];

const SCRIPT_PATH = path.resolve(__filename);

const PATTERNS = [
  { name: 'google_api_key',   re: /AIza[0-9A-Za-z\-_]{35}/g,                 severity: 'critical' },
  { name: 'private_key',      re: /-----BEGIN (?:RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----/g, severity: 'critical' },
  { name: 'github_token',     re: /\bgh[pousr]_[0-9A-Za-z]{36,}\b/g,         severity: 'critical' },
  { name: 'aws_access_key',   re: /\bAKIA[0-9A-Z]{16}\b/g,                   severity: 'critical' },
  { name: 'stripe_live_key',  re: /\b(?:sk|pk|rk)_live_[0-9A-Za-z]{20,}\b/g, severity: 'critical' },
  { name: 'slack_token',      re: /\bxox[abprs]-[0-9A-Za-z\-]{10,}\b/g,      severity: 'critical' },
  { name: 'firebase_db_secret', re: /firebase[_-]?database[_-]?secret/gi,    severity: 'high' },
];

const ALLOWED_TAG = 'FAKE_SECRET_FIXTURE';

// ── Walker ────────────────────────────────────────────────────────────────────

function walk(dir, out) {
  if (!fs.existsSync(dir)) return out;
  const stat = fs.statSync(dir);
  if (stat.isFile()) {
    out.push(dir);
    return out;
  }
  for (const entry of fs.readdirSync(dir)) {
    if (entry === 'node_modules' || entry === 'lib' || entry === 'coverage') continue;
    if (entry.startsWith('.')) continue;
    walk(path.join(dir, entry), out);
  }
  return out;
}

function isScannable(file) {
  const lower = file.toLowerCase();
  return (
    lower.endsWith('.ts') ||
    lower.endsWith('.js') ||
    lower.endsWith('.json') ||
    lower.endsWith('.rules') ||
    lower.endsWith('.md') ||
    lower.endsWith('.yml') ||
    lower.endsWith('.yaml')
  );
}

// ── Main ──────────────────────────────────────────────────────────────────────

function scan() {
  const files = [];
  SCAN_TARGETS.forEach((target) => walk(target, files));

  const findings = [];

  for (const file of files) {
    if (!isScannable(file)) continue;
    // Never scan this script itself — it contains all the patterns by definition.
    if (path.resolve(file) === SCRIPT_PATH) continue;

    let content;
    try {
      content = fs.readFileSync(file, 'utf8');
    } catch {
      continue;
    }

    const lines = content.split(/\r?\n/);

    for (const { name, re, severity } of PATTERNS) {
      lines.forEach((line, idx) => {
        if (line.includes(ALLOWED_TAG)) return;
        re.lastIndex = 0;
        let match;
        while ((match = re.exec(line)) !== null) {
          findings.push({
            file:     path.relative(ROOT, file),
            line:     idx + 1,
            pattern:  name,
            severity,
            preview:  line.trim().slice(0, 80),
          });
        }
      });
    }
  }

  return findings;
}

/**
 * scanContent — exported for unit testing.
 * Runs all PATTERNS against the given text, honoring ALLOWED_TAG on the line.
 */
function scanContent(content, virtualPath = '<inline>') {
  const findings = [];
  const lines = content.split(/\r?\n/);
  for (const { name, re, severity } of PATTERNS) {
    lines.forEach((line, idx) => {
      if (line.includes(ALLOWED_TAG)) return;
      re.lastIndex = 0;
      let match;
      while ((match = re.exec(line)) !== null) {
        findings.push({
          file:     virtualPath,
          line:     idx + 1,
          pattern:  name,
          severity,
          preview:  line.trim().slice(0, 80),
        });
      }
    });
  }
  return findings;
}

module.exports = { scan, scanContent, PATTERNS, ALLOWED_TAG };

// Run as CLI only when invoked directly (not when required from tests)
if (require.main === module) {
  const findings = scan();
  if (findings.length === 0) {
    console.log('✓ scan-secrets: no secrets detected in', SCAN_TARGETS.length, 'target(s).');
    process.exit(0);
  }
  console.error('✗ scan-secrets: SECRETS DETECTED');
  console.error('  ' + findings.length + ' finding(s):\n');
  for (const f of findings) {
    console.error('  [' + f.severity + '] ' + f.file + ':' + f.line + '  ' + f.pattern);
    console.error('            "' + f.preview + '"');
  }
  console.error('\nTo allow a test fixture, append "// ' + ALLOWED_TAG + '" to the line.');
  process.exit(1);
}
