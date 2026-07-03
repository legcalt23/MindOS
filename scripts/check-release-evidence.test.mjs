import test from 'node:test';
import assert from 'node:assert/strict';
import {
  REQUIRED_RELEASE_CHECKS,
  validateReleaseEvidence,
  validateReleaseSigningPolicy,
} from './check-release-evidence.mjs';

const manifest = (status = 'pending') => ({
  schemaVersion: 1,
  releaseStatus: status === 'approved' ? 'GO' : 'NO_GO',
  checks: REQUIRED_RELEASE_CHECKS.map((id) => ({
    id,
    owner: 'owner',
    status,
    evidence: status === 'approved' ? `evidence/${id}` : '',
    verifiedAt: status === 'approved' ? '2026-06-19' : '',
  })),
});

test('validates NO-GO without treating pending work as complete', () => {
  assert.deepEqual(validateReleaseEvidence(manifest()).pending, REQUIRED_RELEASE_CHECKS);
});

test('fails the production gate while any evidence is pending', () => {
  assert.throws(() => validateReleaseEvidence(manifest(), { requireGo: true }), /NO-GO/);
});

test('accepts GO only when every required check has evidence', () => {
  assert.deepEqual(validateReleaseEvidence(manifest('approved'), { requireGo: true }).pending, []);
});

test('rejects approved checks without evidence', () => {
  const candidate = manifest('approved');
  candidate.checks[0].evidence = '';
  assert.throws(() => validateReleaseEvidence(candidate), /requires evidence/);
});

test('rejects a debug signing fallback for the publishable release', () => {
  assert.throws(
    () => validateReleaseSigningPolicy('signingConfig = signingConfigs.getByName("debug")'),
    /fail-closed/,
  );
});

test('accepts the explicit fail-closed release policy', () => {
  assert.doesNotThrow(() => validateReleaseSigningPolicy([
    'publishableReleaseRequested',
    'signingConfig = signingConfigs.getByName("release")',
    'throw GradleException(',
  ].join('\n')));
});
