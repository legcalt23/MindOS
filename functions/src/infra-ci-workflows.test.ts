/**
 * infra-ci-workflows.test.ts
 *
 * Structural smoke tests for the GitHub Actions workflows.
 *
 * We do NOT need a full YAML parser to assert the security-critical
 * properties — we treat the file as text and assert the substrings.
 *
 * This keeps the test stack zero-dependency and means a future edit that
 * removes (say) the scan-secrets step in CI will fail PR validation.
 */

import * as fs from 'fs';
import * as path from 'path';

const REPO_ROOT   = path.resolve(__dirname, '..', '..');
const CI_PATH     = path.join(REPO_ROOT, '.github', 'workflows', 'ci.yml');
const SEC_PATH    = path.join(REPO_ROOT, '.github', 'workflows', 'security.yml');

function read(p: string): string {
  return fs.readFileSync(p, 'utf8');
}

// ── ci.yml ───────────────────────────────────────────────────────────────────

describe('.github/workflows/ci.yml', () => {
  let ci: string;
  beforeAll(() => { ci = read(CI_PATH); });

  it('exists and is non-empty', () => {
    expect(ci.length).toBeGreaterThan(100);
  });

  it('triggers on push to main AND pull_request', () => {
    expect(ci).toMatch(/push:\s*\n\s*branches:\s*\[\s*main\s*\]/);
    expect(ci).toMatch(/pull_request:\s*\n\s*branches:\s*\[\s*main\s*\]/);
  });

  it('declares least-privilege permissions: contents: read', () => {
    expect(ci).toMatch(/permissions:\s*\n\s*contents:\s*read/);
  });

  it('pins Node to 18', () => {
    expect(ci).toContain("node-version: '18'");
  });

  it('runs scan:secrets step', () => {
    expect(ci).toContain('npm run scan:secrets');
  });

  it('runs typecheck step', () => {
    expect(ci).toContain('npm run typecheck');
  });

  it('runs the test suite', () => {
    expect(ci).toMatch(/npm test/);
  });

  it('runs npm audit on prod deps (audit:deps)', () => {
    expect(ci).toContain('npm run audit:deps');
  });

  it('uses persist-credentials: false on checkout', () => {
    expect(ci).toContain('persist-credentials: false');
  });

  it('uses npm ci (locked install), not npm install', () => {
    expect(ci).toContain('npm ci');
    expect(ci).not.toMatch(/\bnpm\s+install\b/);
  });

  it('declares a timeout-minutes on every job', () => {
    // Defensive: prevents a hung job from burning the runner budget.
    const matches = ci.match(/timeout-minutes:\s*\d+/g) ?? [];
    // At least one per job — currently three jobs.
    expect(matches.length).toBeGreaterThanOrEqual(3);
  });
});

// ── security.yml ─────────────────────────────────────────────────────────────

describe('.github/workflows/security.yml', () => {
  let sec: string;
  beforeAll(() => { sec = read(SEC_PATH); });

  it('exists and is non-empty', () => {
    expect(sec.length).toBeGreaterThan(100);
  });

  it('runs on schedule (weekly cron)', () => {
    expect(sec).toMatch(/schedule:/);
    expect(sec).toMatch(/cron:/);
  });

  it('supports manual workflow_dispatch', () => {
    expect(sec).toContain('workflow_dispatch:');
  });

  it('runs npm audit at fail-on-high level for prod deps', () => {
    expect(sec).toContain("npm audit --omit=dev --audit-level=high");
  });

  it('uploads the JSON audit report as an artifact for review', () => {
    expect(sec).toContain('upload-artifact');
    expect(sec).toContain('npm-audit-all.json');
  });

  it('runs the secret scanner', () => {
    expect(sec).toContain('scan-secrets.js');
  });

  it('declares least-privilege permissions: contents: read', () => {
    expect(sec).toMatch(/permissions:\s*\n\s*contents:\s*read/);
  });
});
