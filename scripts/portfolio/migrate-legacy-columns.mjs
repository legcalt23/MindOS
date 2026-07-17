#!/usr/bin/env node
// V2d PHASE 4 — AUTHORITY MIGRATION (run once; kept as reproducible provenance).
//
// The V2c CSV mixed two incompatible things in one file:
//   * editorial facts the human authority genuinely owns (which slot exists, what it is called, what it
//     is assigned to, whether a human reviewed its distinctness), and
//   * technical statuses the CSV has no way to know (implemented? reachable? tested? measurable?), which
//     the retired seed hand-asserted and which then flowed into render/validate as if they were current.
//
// This script splits them. The technical columns are NOT deleted — deleting them would destroy the
// provenance of the published "43 live / 44 mechanic" numbers. They move to an explicitly HISTORICAL
// artifact that no active tool may read.
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsv, headerOf, toCsv } from './csv.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);

// Editorial authority KEEPS only what a human editor actually decides.
//
// P4-R1: `entityType` and `countsTowardTarget` are NOT here. Target membership is a property of the
// CONTAINER, not a claim the row makes about itself. While `entityType` was an active column it was
// filtered BEFORE any invariant family ran, so one cell switched off every invariant on the row
// (reproduced: 4 diagnostics → 0, and the validator then reported the row as "missing" while it sat in the
// file carrying its defects). `MINDBOOST_EXERCISE_PORTFOLIO.csv` is BY DEFINITION a file of target slots:
// every physical row in it is a target slot and is validated. Their V2c values are preserved in
// PORTFOLIO_V2C_LEGACY_CLAIMS.csv.
export const ACTIVE_COLUMNS = [
  'slotId', 'canonicalExerciseId', 'codeExerciseId', 'designExerciseId', 'aliases',
  'block', 'canonicalFamilyId', 'primarySubskill',
  'distinctnessRationale', 'distinctnessReviewStatus', 'distinctnessEvidence',
  'definitionStatus', 'owner', 'blockers',
];

// Everything whose REAL authority is code-observation, runtime/QA or scientific review.
// Any of these appearing in the active CSV is a red gate (PORTFOLIO_ACTIVE_SCHEMA_GATE).
export const RETIRED_COLUMNS = [
  // P4-R1 — self-asserted membership discriminators. A row may not decide whether it is validated.
  'entityType', 'countsTowardTarget',
  // seed-asserted implementation claims — the origin of "43 live / 44 mechanic"
  'mechanicImplemented', 'implementationStatus', 'verificationStatus', 'launchReadiness', 'measurementStatus',
  'routeStatus',
  // platform / QA — authority is runtime/QA, which has no runs
  'androidStatus', 'iosStatus', 'offlineStatus', 'tutorialStatus', 'practiceStatus', 'completionStatus',
  'accessibilityStatus', 'reducedMotionStatus', 'permissionFallbackStatus', 'difficultyStatus',
  'scoringStatus', 'persistenceStatus', 'dossierStatus', 'generatorOrBankStatus',
  // evidence statuses derived from the above
  'identityEvidenceStatus', 'codeDeclarationEvidenceStatus', 'mechanicEvidenceStatus', 'routeEvidenceStatus',
  'runtimeEvidenceStatus', 'timelineEvidenceStatus', 'measurementEvidenceStatus',
  // runtime-derived / code-observation facts
  'currentSnapshotTestRun', 'evidencePaths', 'symbol', 'existedAtPlanStart', 'twoWeekDelta', 'lastVerifiedRef',
];

const CSV = rel('MINDBOOST_EXERCISE_PORTFOLIO.csv');
const LEGACY = rel('PORTFOLIO_V2C_LEGACY_CLAIMS.csv');
const LEGACY_META = rel('PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json');

// The migration is a PURE FUNCTION OF THE V2c SNAPSHOT, not of whatever the CSV happens to be now.
// Re-running it against an already-migrated CSV would emit a legacy artifact full of empty cells and
// silently destroy the provenance of the V2c numbers, so the source is pinned by hash and verified.
export const V2C_SOURCE_SHA256 = '7f84eff3a70ee684893e52dd31ae05b7500085a0d7e07de225050212ace5a219';

if (process.argv[1]?.endsWith('migrate-legacy-columns.mjs')) {
  const fromArg = process.argv.indexOf('--from');
  const src = fromArg > 0 ? process.argv[fromArg + 1] : CSV;
  const raw = readFileSync(src, 'utf8');
  const sourceHash = createHash('sha256').update(raw).digest('hex');
  if (sourceHash !== V2C_SOURCE_SHA256) {
    console.error(`REFUSING: migration source ${src}`);
    console.error(`  sha256 ${sourceHash}`);
    console.error(`  wanted ${V2C_SOURCE_SHA256} (the pristine V2c CSV — extract it from the immutable V2c handoff ZIP)`);
    console.error('  Migrating from an already-migrated CSV would emit empty legacy claims and destroy the provenance of the V2c numbers.');
    process.exit(1);
  }
  const hdr = headerOf(raw);
  const rows = parseCsv(raw);

  const unknown = hdr.filter((h) => !ACTIVE_COLUMNS.includes(h) && !RETIRED_COLUMNS.includes(h));
  if (unknown.length) { console.error(`REFUSING: unclassified columns (neither active nor retired): ${unknown.join(', ')}`); process.exit(1); }
  const missing = [...ACTIVE_COLUMNS, ...RETIRED_COLUMNS].filter((c) => !hdr.includes(c));
  if (missing.length) { console.error(`REFUSING: declared columns absent from source CSV: ${missing.join(', ')}`); process.exit(1); }

  // 1. HISTORICAL artifact — every retired claim preserved, keyed by slot, stamped as unverified.
  const legacyCols = ['slotId', 'disclaimer', ...RETIRED_COLUMNS];
  const legacyRows = rows.map((r) => ({ slotId: r.slotId, disclaimer: 'HISTORICAL_UNVERIFIED_CLAIMS',
    ...Object.fromEntries(RETIRED_COLUMNS.map((c) => [c, r[c]])) }));
  writeFileSync(LEGACY, toCsv(legacyRows, legacyCols), 'utf8');

  const legacyHash = createHash('sha256').update(readFileSync(LEGACY)).digest('hex');
  writeFileSync(LEGACY_META, JSON.stringify({
    artifact: 'V2c legacy portfolio claims — HISTORICAL EVIDENCE ONLY',
    schemaVersion: '1.0.0',
    role: 'HISTORICAL_EVIDENCE',
    disclaimer: 'HISTORICAL_UNVERIFIED_CLAIMS',
    meaning: 'These columns were hand-asserted by the now-retired scripts/portfolio/seed-portfolio.mjs and were ' +
      'never verified against code, runtime or science. They are the provenance of the published V2c numbers ' +
      '"Live-reachable 43/121" and "Mechanic-implemented 44/121". They are preserved so those numbers can be ' +
      'traced, NOT so they can be believed.',
    mayNotFeed: ['render', 'validator', 'code observations', 'derived model', 'any published metric'],
    enforcedBy: 'PORTFOLIO_ACTIVE_SCHEMA_GATE (active CSV must not contain a retired column) and ' +
      'LEGACY_ISOLATION_GATE (no active tool may read this artifact)',
    sourceCsv: { path: 'MINDBOOST_EXERCISE_PORTFOLIO.csv', sha256AtMigration: sourceHash },
    v2cSnapshot: { handoffZip: 'MINDBOOST_PORTFOLIO_V2C_HANDOFF_8795736d0b1afbb6da1b319d16b862536850ee39f711d6236f4900238dc548fd.zip',
      csvSha256: '7f84eff3a70ee684893e52dd31ae05b7500085a0d7e07de225050212ace5a219',
      mdSha256: '1679c7d6d13939dff75f1671565833b2fb85799840b4820671497c533508df95' },
    retiredColumns: RETIRED_COLUMNS,
    activeColumns: ACTIVE_COLUMNS,
    rowCount: legacyRows.length,
    legacyCsvSha256: legacyHash,
  }, null, 2) + '\n', 'utf8');

  // 2. Active CSV — editorial facts only.
  writeFileSync(CSV, toCsv(rows.map((r) => Object.fromEntries(ACTIVE_COLUMNS.map((c) => [c, r[c]]))), ACTIVE_COLUMNS), 'utf8');

  console.log(`MIGRATED`);
  console.log(`  source CSV sha256      ${sourceHash}`);
  console.log(`  active columns         ${ACTIVE_COLUMNS.length} (was ${hdr.length})`);
  console.log(`  retired to historical  ${RETIRED_COLUMNS.length}`);
  console.log(`  legacy claims          ${LEGACY.replace(root, '.')} (${legacyRows.length} rows, sha256 ${legacyHash})`);
  console.log(`  new active CSV sha256  ${createHash('sha256').update(readFileSync(CSV)).digest('hex')}`);
}
