/**
 * infra-secret-scan.test.ts
 *
 * Smoke tests for scripts/scan-secrets.js.
 *
 * These are the build-time guardrails that prevent a secret leak from being
 * merged. The scanner must:
 *   - DETECT each pattern with a representative payload
 *   - ALLOW lines tagged with FAKE_SECRET_FIXTURE
 *   - REPORT zero findings on clean content
 *   - SURFACE file path and line number for each finding
 */

/* eslint-disable @typescript-eslint/no-var-requires */
// The scanner is plain JS to keep CI surface minimal — load via require().
const {
  scanContent,
  isAllowedFinding,
  PATTERNS,
  ALLOWED_TAG,
}: {
  scanContent: (content: string, virtualPath?: string) => Array<{
    file: string;
    line: number;
    pattern: string;
    severity: string;
    preview: string;
  }>;
  isAllowedFinding: (patternName: string, file: string, line: string) => boolean;
  PATTERNS: Array<{ name: string; severity: string }>;
  ALLOWED_TAG: string;
} = require('../scripts/scan-secrets.js');

// ── Detection coverage ───────────────────────────────────────────────────────

describe('scan-secrets — detection', () => {
  // Each entry: pattern name + a realistic-shaped sample that MUST match.
  // The literal payloads below are dummy random data, NOT real keys.
  // Built at runtime via concatenation so the raw line in source does not
  // itself contain the full match (so scan-secrets does not flag this file).
  const A = 'A' + 'I' + 'z' + 'a' + 'Sy';
  const samples: Array<[string, string]> = [
    ['google_api_key',     `const k = "${A}Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";`],
    ['private_key',        'const pem = "-----BEGIN ' + 'PRIVATE KEY-----\\nMIIE...";'],
    ['github_token',       'const t = "gh' + 'p_AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIII";'],
    ['aws_access_key',     'const k = "AK' + 'IAIOSFODNN7EXAMPLE";'],
    ['stripe_live_key',    'const s = "sk' + '_live_AAAABBBBCCCCDDDDEEEEFFFFGG";'],
    ['slack_token',        'const t = "xo' + 'xb-1234567890-AAAABBBBCCCC";'],
    ['firebase_db_secret', 'const v = "FIREBASE_DATABASE_' + 'SECRET=abc";'],
  ];

  it.each(samples)('detects %s pattern', (name, content) => {
    const findings = scanContent(content, '<test>');
    const matched = findings.find((f) => f.pattern === name);
    expect(matched).toBeDefined();
  });

  it('reports every PATTERNS entry above is exercised by at least one sample', () => {
    const sampleNames = new Set(samples.map(([n]) => n));
    for (const p of PATTERNS) {
      expect(sampleNames.has(p.name)).toBe(true);
    }
  });
});

// ── Allow-tag whitelisting ───────────────────────────────────────────────────

describe('scan-secrets — fixture allow-tag', () => {
  // Built at runtime so the raw line never contains a complete match.
  const A = 'AI' + 'za' + 'Sy';

  it('allows AIza... when the line carries the FAKE_SECRET_FIXTURE tag', () => {
    const content = `const fakeKey = "${A}Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // ${ALLOWED_TAG}`;
    expect(scanContent(content, '<test>')).toHaveLength(0);
  });

  it('still flags subsequent lines without the tag', () => {
    const content = [
      `const ok  = "${A}Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // ${ALLOWED_TAG}`,
      `const bad = "${A}Dyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy";`,
    ].join('\n');
    const findings = scanContent(content, '<test>');
    expect(findings).toHaveLength(1);
    expect(findings[0].line).toBe(2);
  });
});

// ── Clean content ────────────────────────────────────────────────────────────

describe('scan-secrets — Firebase public config allowlist', () => {
  const A = 'AI' + 'za' + 'Sy';
  const publicKeyLine = `api_key: [{ current_key: "${A}Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" }]`;

  it('allows Firebase public API keys only in platform config files', () => {
    expect(isAllowedFinding('google_api_key', 'androidApp/google-services.json', publicKeyLine)).toBe(true);
    expect(isAllowedFinding('google_api_key', 'shared/src/iosMain/resources/GoogleService-Info.plist', publicKeyLine)).toBe(true);
  });

  it('still flags the same key outside Firebase platform config files', () => {
    const findings = scanContent(publicKeyLine, 'docs/leaked-key.md');
    expect(findings).toHaveLength(1);
    expect(findings[0].pattern).toBe('google_api_key');
  });
});

describe('scan-secrets — clean content', () => {
  it('returns zero findings for a normal function file', () => {
    const content = `
      import * as functions from 'firebase-functions';
      export const fn = functions.https.onCall((data, ctx) => {
        return { ok: true };
      });
    `;
    expect(scanContent(content, '<test>')).toHaveLength(0);
  });

  it('returns zero findings for empty content', () => {
    expect(scanContent('', '<test>')).toHaveLength(0);
  });
});

// ── Surface (file/line) ──────────────────────────────────────────────────────

describe('scan-secrets — finding surface', () => {
  it('reports virtual file path and 1-based line number', () => {
    const ghp = 'gh' + 'p_AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHIIII';
    const content = [
      'line one',
      'line two',
      `const bad = "${ghp}";`,
    ].join('\n');
    const findings = scanContent(content, 'fake/path.ts');
    expect(findings).toHaveLength(1);
    expect(findings[0].file).toBe('fake/path.ts');
    expect(findings[0].line).toBe(3);
    expect(findings[0].pattern).toBe('github_token');
  });

  it('caps preview to 80 chars', () => {
    const longLine = 'AK' + 'IA' + 'X'.repeat(16) + ' /* ' + 'A'.repeat(200) + ' */';
    const findings = scanContent(longLine, '<test>');
    expect(findings[0].preview.length).toBeLessThanOrEqual(80);
  });
});

// ── High-entropy / hardcoded-fallback heuristic (M15) ────────────────────────
describe('scan-secrets — high-entropy heuristic', () => {
  // Built at runtime so this test file never contains a full flaggable literal.
  const HI = 'k3J9xQ2mZ' + '7pLvR8nWqA' + '6tY1bD5cF0gH'; // ~31 chars, near-unique

  it('flags a high-entropy literal assigned to a secret-like identifier', () => {
    const findings = scanContent(`const apiSecret = "${HI}";`, '<test>');
    expect(findings.some((f) => f.pattern === 'high_entropy_secret')).toBe(true);
  });

  it('does NOT flag a low-entropy dictionary fallback', () => {
    const phrase = 'mindos-account' + '-deletion-local' + '-fallback';
    expect(scanContent(`const FALLBACK_SECRET = "${phrase}";`, '<test>')).toHaveLength(0);
  });

  it('does NOT flag an env-read fallback (process.env on the line)', () => {
    expect(scanContent(`const s = process.env.X || "${HI}";`, '<test>')).toHaveLength(0);
  });

  it('does NOT flag a high-entropy value on a non-secret identifier', () => {
    expect(scanContent(`const requestId = "${HI}";`, '<test>')).toHaveLength(0);
  });

  it('honors the FAKE_SECRET_FIXTURE allow-tag', () => {
    expect(scanContent(`const apiSecret = "${HI}"; // ${ALLOWED_TAG}`, '<test>')).toHaveLength(0);
  });
});
