#!/usr/bin/env node
// Delivery-set authority (E). Writes DELIVERY_MANIFEST.json (exact file set + roles) and SHA256SUMS.txt
// (one filename+hash line per HASHED artifact). Roles: hashed (required, in SHA256SUMS), self-excluded
// (SHA256SUMS + this manifest — cannot hash themselves), historical (superseded, NOT required-hashed →
// resolves the prior MINDBOOST_121_EXERCISE_PORTFOLIO.md hash discrepancy). Run LAST, after the ledger.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const sha = (rel) => createHash('sha256').update(readFileSync(join(root, rel))).digest('hex');
const selfExcludingLedger = () => { const t = readFileSync(join(root, 'MINDBOOST_AUDIT_LEDGER.md'), 'utf8'); return createHash('sha256').update(t.split('\n').filter((l) => !l.startsWith('Canonical SHA-256')).join('\n'), 'utf8').digest('hex'); };

// V2d Phase 4: seed-portfolio.mjs is RETIRED and is no longer a required artifact — requiring it would keep
// the retired executable alive in the delivery authority. The V2c legacy claims are carried as
// HISTORICAL_EVIDENCE: hashed so tampering is detectable, but never a current-fact deliverable.
const HASHED = [
  // authorities
  'MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md',
  'PORTFOLIO_CODE_OBSERVATIONS.json', 'PORTFOLIO_RUNTIME_EVIDENCE.json', 'PORTFOLIO_DERIVED_MODEL.json',
  'PORTFOLIO_SOURCE_MANIFEST_V2.json', 'PORTFOLIO_AUTHORITY_MAP.md', 'PORTFOLIO_V2_EVIDENCE.md',
  'MINDBOOST_AUDIT_LEDGER.md', 'LOCK_SAFETY_RESULT.json',
  // Phase 4 evidence
  'PHASE4_FAIL_BEFORE_RESULT.json',
  // tooling — the exact chain that produces every published number
  'scripts/portfolio/observe-code.mjs', 'scripts/portfolio/derive-portfolio-model.mjs',
  'scripts/portfolio/block-adapters.mjs', 'scripts/portfolio/kotlin-code-scan.mjs', 'scripts/portfolio/authority-tests.mjs',
  'scripts/portfolio/build-portfolio.mjs', 'scripts/portfolio/render.mjs', 'scripts/portfolio/validate-portfolio.mjs',
  'scripts/portfolio/metrics.mjs', 'scripts/portfolio/portfolio-state-model.mjs', 'scripts/portfolio/migrate-legacy-columns.mjs',
  'scripts/portfolio/validate-evidence.mjs', 'scripts/portfolio/gen-source-manifest.mjs',
  'scripts/portfolio/evidence-mutation-tests.mjs', 'scripts/portfolio/phase4-mutation-tests.mjs',
  'scripts/portfolio/phase4-fail-before.mjs',
  'scripts/portfolio/mutation-test.mjs', 'scripts/portfolio/csv.mjs', 'scripts/portfolio/gen-manifest.mjs',
  'scripts/portfolio/gen-delivery.mjs', 'scripts/portfolio/lock-tests.mjs', 'scripts/portfolio/lock-race-worker.mjs',
  'scripts/ledger-lock.mjs', 'scripts/validate-audit-ledger.mjs', 'docs/decision_log/CAT_121_DIRECTIVE_PATCH.md',
];
const SELF_EXCLUDED = ['SHA256SUMS.txt', 'DELIVERY_MANIFEST.json'];
// Hashed for integrity, but explicitly NOT current facts. Nothing active may read them.
const HISTORICAL_EVIDENCE = [
  'PORTFOLIO_V2C_LEGACY_CLAIMS.csv', 'PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json',
  'scripts/portfolio/seed-portfolio.mjs.RETIRED',
];
const HISTORICAL = ['MINDBOOST_121_EXERCISE_PORTFOLIO.md']; // SUPERSEDED — kept for history, NOT a required-hashed deliverable

const missing = HASHED.filter((p) => !existsSync(join(root, p)));
if (missing.length) { console.error('DELIVERY: missing required artifacts:\n  ' + missing.join('\n  ')); process.exit(1); }

const artifacts = [
  ...HASHED.map((p) => ({ path: p, role: 'hashed', sha256: sha(p) })),
  ...HISTORICAL_EVIDENCE.filter((p) => existsSync(join(root, p))).map((p) => ({ path: p, role: 'HISTORICAL_EVIDENCE', sha256: sha(p),
    note: 'HISTORICAL_UNVERIFIED_CLAIMS — hashed so tampering is detectable. Never a current fact; no active tool may parse it.' })),
  ...SELF_EXCLUDED.map((p) => ({ path: p, role: 'self-excluded', note: 'hashes itself by convention → excluded from SHA256SUMS' })),
  ...HISTORICAL.map((p) => ({ path: p, role: 'historical', note: 'SUPERSEDED (V1); kept for history; not a required-hashed deliverable (resolves the prior hash discrepancy)' })),
];
const delivery = {
  artifact: 'MindBoost portfolio V2d Phase 4 delivery manifest', generatedNote: 'exact delivery file set + roles',
  legacyAuthorityRetired: 'scripts/portfolio/seed-portfolio.mjs is RETIRED and is no longer a required artifact. Its materialised status columns were moved to PORTFOLIO_V2C_LEGACY_CLAIMS.csv (HISTORICAL_EVIDENCE).',
  ledgerHashes: { fullFile: sha('MINDBOOST_AUDIT_LEDGER.md'), selfExcluding: selfExcludingLedger(),
    note: 'fullFile = sha256 of the whole file; selfExcluding = §0 canonical (excludes the Canonical SHA-256 line)' },
  externalRequired: ['all HASHED artifacts must be materially attached to the external handoff channel (MB-PORTFOLIO-0004)'],
  artifacts,
};
writeFileSync(join(root, 'DELIVERY_MANIFEST.json'), JSON.stringify(delivery, null, 2) + '\n', 'utf8');
const sums = artifacts.filter((a) => a.sha256).map((a) => `${a.sha256}  ${a.path}`).join('\n') + '\n';
writeFileSync(join(root, 'SHA256SUMS.txt'), sums, 'utf8');
console.log(`DELIVERY: ${artifacts.filter((a) => a.role === 'hashed').length} hashed, ${artifacts.filter((a) => a.role === 'HISTORICAL_EVIDENCE').length} historical-evidence, ${SELF_EXCLUDED.length} self-excluded, ${HISTORICAL.length} historical. SHA256SUMS written.`);
console.log(`  ledger fullFile=${delivery.ledgerHashes.fullFile.slice(0, 12)} selfExcluding=${delivery.ledgerHashes.selfExcluding.slice(0, 12)}`);
