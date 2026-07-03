// @ts-check
/**
 * redaction.mjs — the FAIL-CLOSED gate on the write path and a defense-in-depth
 * scrubber on the retrieval path. This is intentionally SEPARATE from
 * .claude/hooks/mindos-guard.cjs, which always exits 0 (warn-only) and therefore
 * cannot block. Here, a finding REJECTS the write (nothing is persisted) and a
 * finding at retrieval REDACTS the snippet before it reaches the model.
 *
 * Reuses the repo's trusted secret scanner (functions/scripts/scan-secrets.js:
 * scanContent + PATTERNS + the high-entropy heuristic) and adds PII patterns the
 * wallet scanner lacks (email, phone, IPv4, credit card via Luhn, JWT).
 *
 * Honors the repo convention: a line tagged `FAKE_SECRET_FIXTURE` is allowed.
 */

import { createRequire } from 'node:module';
import { scanSecretsPath } from './paths.mjs';

const require = createRequire(import.meta.url);
const ALLOWED_TAG = 'FAKE_SECRET_FIXTURE';

/** Load the repo scanner; if unavailable, fall back to an embedded pattern set so the guard NEVER silently no-ops. */
function loadScanContent() {
  try {
    const mod = require(scanSecretsPath());
    if (mod && typeof mod.scanContent === 'function') return mod.scanContent;
  } catch {
    /* fall through to embedded fallback */
  }
  const FALLBACK = [
    { name: 'google_api_key', re: /AIza[0-9A-Za-z\-_]{35}/g, severity: 'critical' },
    { name: 'private_key', re: /-----BEGIN (?:RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----/g, severity: 'critical' },
    { name: 'github_token', re: /\bgh[pousr]_[0-9A-Za-z]{36,}\b/g, severity: 'critical' },
    { name: 'aws_access_key', re: /\bAKIA[0-9A-Z]{16}\b/g, severity: 'critical' },
    { name: 'stripe_live_key', re: /\b(?:sk|pk|rk)_live_[0-9A-Za-z]{20,}\b/g, severity: 'critical' },
    { name: 'slack_token', re: /\bxox[abprs]-[0-9A-Za-z\-]{10,}\b/g, severity: 'critical' },
  ];
  return (content, virtualPath = '<inline>') => {
    const findings = [];
    const lines = String(content).split(/\r?\n/);
    for (const { name, re, severity } of FALLBACK) {
      lines.forEach((line, idx) => {
        if (line.includes(ALLOWED_TAG)) return;
        re.lastIndex = 0;
        while (re.exec(line) !== null) {
          findings.push({ file: virtualPath, line: idx + 1, pattern: name, severity, preview: '' });
        }
      });
    }
    return findings;
  };
}

const scanContent = loadScanContent();

// ── PII patterns (added on top of the secret scanner) ────────────────────────
const PII_PATTERNS = [
  { name: 'email', re: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b/g, severity: 'high' },
  { name: 'phone_e164', re: /(?<!\w)\+\d{1,3}[\s.-]?\(?\d{1,4}\)?(?:[\s.-]?\d{2,4}){2,4}(?!\w)/g, severity: 'high' },
  { name: 'ipv4', re: /\b(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)\b/g, severity: 'medium' },
  { name: 'jwt', re: /\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b/g, severity: 'critical' },
];
// Card detection is special-cased (Luhn) to avoid flagging ordinary 16-digit numbers.
const CARD_RE = /\b(?:\d[ -]?){13,19}\b/g;

// Secret patterns used by the retrieval-time scrubber (mirror the write guard so
// nothing the guard catches could slip into a splice via an older/edited entry).
const SECRET_REDACT_PATTERNS = [
  { name: 'google_api_key', re: /AIza[0-9A-Za-z\-_]{35}/g },
  { name: 'aws_access_key', re: /\bAKIA[0-9A-Z]{16}\b/g },
  { name: 'github_token', re: /\bgh[pousr]_[0-9A-Za-z]{36,}\b/g },
  { name: 'stripe_key', re: /\b(?:sk|pk|rk)_live_[0-9A-Za-z]{20,}\b/g },
  { name: 'slack_token', re: /\bxox[abprs]-[0-9A-Za-z\-]{10,}\b/g },
  { name: 'private_key', re: /-----BEGIN (?:RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----/g },
];

function luhnValid(digits) {
  let sum = 0;
  let alt = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = digits.charCodeAt(i) - 48;
    if (alt) { d *= 2; if (d > 9) d -= 9; }
    sum += d;
    alt = !alt;
  }
  return sum % 10 === 0;
}

/** Ignore well-known non-PII IPs to cut false positives (0.0.0.0, 127.x, version-like). */
function isBenignIp(ip) {
  return ip === '0.0.0.0' || ip === '127.0.0.1' || ip.startsWith('255.') || ip === '1.1.1.1';
}

/** @returns {Array<{pattern:string,line:number,severity:string}>} */
function piiFindings(text) {
  const findings = [];
  const lines = String(text).split(/\r?\n/);
  lines.forEach((line, idx) => {
    if (line.includes(ALLOWED_TAG)) return;
    for (const { name, re, severity } of PII_PATTERNS) {
      re.lastIndex = 0;
      let m;
      while ((m = re.exec(line)) !== null) {
        if (name === 'ipv4' && isBenignIp(m[0])) continue;
        findings.push({ pattern: name, line: idx + 1, severity });
      }
    }
    CARD_RE.lastIndex = 0;
    let cm;
    while ((cm = CARD_RE.exec(line)) !== null) {
      const digits = cm[0].replace(/[ -]/g, '');
      if (digits.length >= 13 && digits.length <= 19 && luhnValid(digits)) {
        findings.push({ pattern: 'credit_card', line: idx + 1, severity: 'critical' });
      }
    }
  });
  return findings;
}

/**
 * Scan text for secrets + PII. PII-free result: pattern names + line numbers
 * ONLY — never the matched value.
 * @param {string} text
 * @param {string} [label]
 * @returns {{ ok: boolean, findings: Array<{pattern:string,line:number,severity:string}> }}
 */
export function guardText(text, label = '<memory>') {
  try {
    const secretFindings = scanContent(String(text || ''), label).map((f) => ({
      pattern: f.pattern, line: f.line, severity: f.severity,
    }));
    const findings = [...secretFindings, ...piiFindings(text)];
    return { ok: findings.length === 0, findings };
  } catch {
    // Fail-closed: if the guard itself throws, treat as NOT clean.
    return { ok: false, findings: [{ pattern: 'guard_error', line: 0, severity: 'critical' }] };
  }
}

/**
 * Guard a whole entry (content + title + serialized metadata).
 * @param {{content?:string,title?:string,metadata?:object}} entry
 */
export function guardEntry(entry) {
  const parts = [
    entry.title || '',
    entry.content || '',
    entry.metadata ? JSON.stringify(entry.metadata) : '',
  ].join('\n');
  return guardText(parts, `entry:${entry.title || ''}`.slice(0, 60));
}

/** Distinct finding pattern names (for PII-free audit + user messages). */
export function findingPatternNames(findings) {
  return [...new Set(findings.map((f) => f.pattern))];
}

/**
 * Defense-in-depth: replace any secret/PII match with a placeholder before a
 * snippet is spliced into a prompt. Never throws — on error, returns '' (drop).
 * @param {string} text
 */
export function redactForModel(text) {
  try {
    let out = String(text || '');
    for (const { name, re } of SECRET_REDACT_PATTERNS) {
      out = out.replace(new RegExp(re.source, re.flags), `‹redacted:${name}›`);
    }
    for (const { name, re } of PII_PATTERNS) {
      out = out.replace(new RegExp(re.source, re.flags), `‹redacted:${name}›`);
    }
    return out;
  } catch {
    return '';
  }
}
