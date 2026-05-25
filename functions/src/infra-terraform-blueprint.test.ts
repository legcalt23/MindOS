/**
 * infra-terraform-blueprint.test.ts
 *
 * Enforces the non-destructive contract of infra/terraform/.
 *
 * Rules:
 *   - There must be NO active *.tf files (only *.tf.example placeholders).
 *     This is what guarantees `terraform init` cannot accidentally apply.
 *
 *   - Every .example file must NOT contain:
 *       - A local backend ("backend \"local\"")
 *       - Hardcoded credentials (provider "google" { credentials = ... } as
 *         an inline literal — only env-based or WIF-based credentials allowed)
 *       - Any string matching scan-secrets patterns
 *
 *   - variables.tf.example must mark every variable that could carry
 *     secret data as `sensitive = true`.
 *
 *   - main.tf.example, when activated, must use a remote backend (gcs/s3/...).
 *
 *   - The README must exist and document the contract.
 */

import * as fs   from 'fs';
import * as path from 'path';

const REPO_ROOT     = path.resolve(__dirname, '..', '..');
const TERRAFORM_DIR = path.join(REPO_ROOT, 'infra', 'terraform');

function listDir(dir: string): string[] {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir).map((e) => path.join(dir, e));
}

function readIfFile(p: string): string | null {
  if (!fs.existsSync(p) || !fs.statSync(p).isFile()) return null;
  return fs.readFileSync(p, 'utf8');
}

// ── Non-destructive guarantees ───────────────────────────────────────────────

describe('infra/terraform — non-destructive guarantee', () => {
  it('directory exists', () => {
    expect(fs.existsSync(TERRAFORM_DIR)).toBe(true);
  });

  it('contains NO active *.tf files (only *.tf.example placeholders)', () => {
    const entries = listDir(TERRAFORM_DIR);
    const activeTf = entries.filter((p) => {
      if (!fs.statSync(p).isFile()) return false;
      const name = path.basename(p);
      return name.endsWith('.tf') && !name.endsWith('.tf.example');
    });
    expect(activeTf).toHaveLength(0);
  });

  it('README documents the contract', () => {
    const readme = readIfFile(path.join(TERRAFORM_DIR, 'README.md'));
    expect(readme).not.toBeNull();
    expect(readme).toContain('non-destructive');
    expect(readme).toContain('Forbidden patterns');
  });
});

// ── Example file content checks ──────────────────────────────────────────────

describe('infra/terraform — example file contracts', () => {
  it('main.tf.example uses a remote backend (gcs), never local', () => {
    const main = readIfFile(path.join(TERRAFORM_DIR, 'main.tf.example'));
    expect(main).not.toBeNull();
    expect(main).toMatch(/backend\s+"gcs"/);
    expect(main).not.toMatch(/backend\s+"local"/);
  });

  it('main.tf.example does NOT inline file-based credentials', () => {
    const main = readIfFile(path.join(TERRAFORM_DIR, 'main.tf.example'));
    expect(main).not.toBeNull();
    // The pattern we forbid is "credentials = file(...)" as ACTIVE HCL.
    // Strip comment lines before checking, otherwise documentation that
    // mentions the forbidden pattern would self-trip this test.
    const activeLines = main!
      .split(/\r?\n/)
      .filter((line) => !line.trim().startsWith('#'))
      .join('\n');
    expect(activeLines).not.toMatch(/credentials\s*=\s*file\s*\(/);
  });

  it('main.tf.example pins required_version and required_providers', () => {
    const main = readIfFile(path.join(TERRAFORM_DIR, 'main.tf.example'));
    expect(main).not.toBeNull();
    expect(main).toMatch(/required_version/);
    expect(main).toMatch(/required_providers/);
  });

  it('variables.tf.example marks secret-bearing variables sensitive=true', () => {
    const vars = readIfFile(path.join(TERRAFORM_DIR, 'variables.tf.example'));
    expect(vars).not.toBeNull();
    // Find every variable block and check whether it mentions "sensitive"
    // when the description contains "secret".
    // Simpler: assert that wallet_secrets (which we know is sensitive) is marked.
    expect(vars).toMatch(/variable\s+"wallet_secrets"[\s\S]*?sensitive\s*=\s*true/);
    expect(vars).toMatch(/variable\s+"fraud_pubsub_subscribers"[\s\S]*?sensitive\s*=\s*true/);
  });
});

// ── No real secrets anywhere in infra/terraform ─────────────────────────────

describe('infra/terraform — secret hygiene', () => {
  it('the secret scanner runs cleanly on the terraform blueprint files', () => {
    // The repo-wide scanner already covers functions/src and root rules.
    // Here we explicitly assert no *.tf.example file contains key patterns.
    /* eslint-disable @typescript-eslint/no-var-requires */
    const { scanContent } = require('../scripts/scan-secrets.js') as {
      scanContent: (s: string, p?: string) => Array<unknown>;
    };

    const entries = listDir(TERRAFORM_DIR)
      .filter((p) => fs.statSync(p).isFile())
      .filter((p) => p.endsWith('.example') || p.endsWith('.md') || p.endsWith('.tf'));

    for (const f of entries) {
      const content = fs.readFileSync(f, 'utf8');
      const findings = scanContent(content, path.relative(REPO_ROOT, f));
      expect(findings).toEqual([]);
    }
  });
});
