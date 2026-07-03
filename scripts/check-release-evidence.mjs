import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

export const REQUIRED_RELEASE_CHECKS = Object.freeze([
  'iap',
  'data_safety',
  'privacy_url',
  'store_listing_iarc',
  'ugc_report_appeal',
]);

export function validateReleaseSigningPolicy(buildScript) {
  const requiredFragments = [
    'publishableReleaseRequested',
    'signingConfig = signingConfigs.getByName("release")',
    'throw GradleException(',
  ];
  const missing = requiredFragments.filter((fragment) => !buildScript.includes(fragment));
  if (missing.length > 0 || buildScript.includes('signingConfig = signingConfigs.getByName("debug")')) {
    throw new Error('Android release signing policy is not fail-closed.');
  }
}

export function validateReleaseEvidence(manifest, { requireGo = false } = {}) {
  if (manifest?.schemaVersion !== 1 || !Array.isArray(manifest.checks)) {
    throw new Error('Release evidence must use schemaVersion 1 with a checks array.');
  }

  const checksById = new Map(manifest.checks.map((check) => [check.id, check]));
  const missing = REQUIRED_RELEASE_CHECKS.filter((id) => !checksById.has(id));
  if (missing.length > 0) throw new Error(`Missing release evidence checks: ${missing.join(', ')}`);

  const pending = [];
  for (const id of REQUIRED_RELEASE_CHECKS) {
    const check = checksById.get(id);
    if (!check.owner || !['pending', 'approved'].includes(check.status)) {
      throw new Error(`Release evidence ${id} has an invalid owner or status.`);
    }
    if (check.status === 'approved' && (!check.evidence || !check.verifiedAt)) {
      throw new Error(`Approved release evidence ${id} requires evidence and verifiedAt.`);
    }
    if (check.status !== 'approved') pending.push(id);
  }

  if (manifest.releaseStatus === 'GO' && pending.length > 0) {
    throw new Error(`releaseStatus GO is inconsistent with pending checks: ${pending.join(', ')}`);
  }
  if (requireGo && (manifest.releaseStatus !== 'GO' || pending.length > 0)) {
    throw new Error(`Release remains NO-GO; pending evidence: ${pending.join(', ')}`);
  }
  return { pending };
}

async function main() {
  const requireGo = process.argv.includes('--require-go');
  const manifest = JSON.parse(
    await readFile(new URL('../docs/release/RELEASE_EVIDENCE.json', import.meta.url), 'utf8'),
  );
  validateReleaseSigningPolicy(
    await readFile(new URL('../androidApp/build.gradle.kts', import.meta.url), 'utf8'),
  );
  const result = validateReleaseEvidence(manifest, { requireGo });
  process.stdout.write(
    result.pending.length === 0
      ? 'release-evidence: GO\n'
      : `release-evidence: valid NO-GO; pending=${result.pending.join(',')}\n`,
  );
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch((error) => {
    process.stderr.write(`release-evidence: ${error.message}\n`);
    process.exitCode = 1;
  });
}
