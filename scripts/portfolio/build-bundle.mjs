#!/usr/bin/env node
// V2d PHASE 5 — CANDIDATE BUNDLE. Builds the handoff container and its manifest.
// Every file has exactly ONE role. The source archive carries the bytes the manifest hashes, so the
// clean-room never needs the original repository (MB-PORTFOLIO-0017).
import { readFileSync, writeFileSync, existsSync, lstatSync, mkdirSync, rmSync, copyFileSync, readdirSync, utimesSync } from 'node:fs';

// A frozen artifact must not embed the moment it was built. SOURCE_DATE_EPOCH honoured; default 0.
const FIXED_EPOCH = Number(process.env.SOURCE_DATE_EPOCH ?? 0);
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative } from 'node:path';
import { buildZip } from './deterministic-zip.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => createHash('sha256').update(readFileSync(p)).digest('hex');

// ROLES — exactly one per file. A file with no role, or two incompatible roles, is a red gate.
export const ROLES = {
  ACTIVE_AUTHORITY: ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'PORTFOLIO_AUTHORITY_MAP.md'],
  RUNTIME_EVIDENCE: ['PORTFOLIO_RUNTIME_EVIDENCE.json'],
  SOURCE_EVIDENCE: ['PORTFOLIO_SOURCE_ARCHIVE.mbsrc', 'PORTFOLIO_SOURCE_INDEX.json', 'PORTFOLIO_SOURCE_MANIFEST_V2.json', 'PORTFOLIO_CODE_OBSERVATIONS.json'],
  GENERATED_VIEW: ['MINDBOOST_EXERCISE_PORTFOLIO.md', 'PORTFOLIO_DERIVED_MODEL.json'],
  VALIDATOR: [
    'scripts/portfolio/validate-portfolio.mjs', 'scripts/portfolio/validate-evidence.mjs',
    'scripts/portfolio/derive-portfolio-model.mjs', 'scripts/portfolio/build-portfolio.mjs',
    'scripts/portfolio/render.mjs', 'scripts/portfolio/metrics.mjs', 'scripts/portfolio/portfolio-state-model.mjs',
    'scripts/portfolio/block-adapters.mjs', 'scripts/portfolio/kotlin-code-scan.mjs', 'scripts/portfolio/observe-code.mjs',
    'scripts/portfolio/csv.mjs', 'scripts/portfolio/migrate-legacy-columns.mjs',
    // validate-portfolio.mjs imports decideMode from here. Omitting it shipped a validator without its
    // dependency: the extract failed at module resolution. Phase 8 caught it — which is what "reject required
    // files that were omitted" (MB-V2D-R08) is for. A hash proves the integrity of what IS listed; only the
    // freeze's regenerate-from-the-extract step proves the list is complete.
    'scripts/portfolio/validation-mode.mjs',
    'scripts/portfolio/build-source-archive.mjs', 'scripts/portfolio/gen-source-manifest.mjs',
    'scripts/portfolio/validate-bundle.mjs', 'scripts/validate-audit-ledger.mjs',
  ],
  MUTATION_ORACLE: [
    'scripts/portfolio/authority-tests.mjs', 'scripts/portfolio/evidence-mutation-tests.mjs',
    'scripts/portfolio/phase4-mutation-tests.mjs', 'scripts/portfolio/phase4-fail-before.mjs',
    'scripts/portfolio/phase4-r1-fail-before.mjs', 'scripts/portfolio/phase5-mutation-tests.mjs',
    'scripts/portfolio/r03-mode-mutation-tests.mjs',
  ],
  HISTORICAL_EVIDENCE: ['PORTFOLIO_V2C_LEGACY_CLAIMS.csv', 'PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json', 'scripts/portfolio/seed-portfolio.mjs.RETIRED'],
  RAW_OUTPUT: ['PHASE4_FAIL_BEFORE_RESULT.json', 'PHASE4_R1_FAIL_BEFORE_RESULT.json', 'PHASE4_MUTATION_RESULT.json',
    'PORTFOLIO_AUTHORITY_TESTS_RESULT.json', 'PORTFOLIO_EVIDENCE_MUTATIONS_RESULT.json', 'PORTFOLIO_EVIDENCE_VALIDATION.json',
    'LOCK_SAFETY_RESULT.json', 'LOCK_LIVENESS_RESULT.json'],
  LEDGER: ['MINDBOOST_AUDIT_LEDGER.md'],
  RUNBOOK: ['CLEAN_ROOM_RUNBOOK.md'],
  HASH_MANIFEST: ['BUNDLE_MANIFEST.json', 'BUNDLE_SHA256SUMS.txt'],
};
// Self-exclusion is NOT a property of the HASH_MANIFEST role — it is a property of genuine self-reference,
// and only ONE file has it.
//
// The previous rule excluded everything in ROLES.HASH_MANIFEST and stamped both entries with the same note,
// "cannot hash itself". That note is true of SHA256SUMS.txt with respect to ITS OWN list, and it is a
// non-sequitur with respect to the MANIFEST's list — they are two different lists. Checked, not assumed:
// SHA256SUMS.txt does not list BUNDLE_MANIFEST.json and does not list itself, so hashing SHA256SUMS.txt from
// BUNDLE_MANIFEST.json closes no cycle. It was simply left unhashed by anything except the container digest —
// a real root-of-trust hole that the "self-attestation" convention concealed by copy-pasting a justification
// from the one file where it applies onto one where it does not.
//
// BUNDLE_MANIFEST.json is the sole irreducible case: a file cannot contain its own digest.
const SELF_EXCLUDED = new Set(['BUNDLE_MANIFEST.json']);

export function roleOf(p) {
  const hits = Object.entries(ROLES).filter(([, ps]) => ps.includes(p)).map(([r]) => r);
  if (hits.length === 0) return { role: null, error: `no role declared for "${p}" — every bundled file needs exactly one` };
  if (hits.length > 1) return { role: null, error: `"${p}" claims multiple roles: ${hits.join(', ')}` };
  return { role: hits[0], error: null };
}

if (process.argv[1]?.endsWith('build-bundle.mjs')) {
  const stage = join(root, '.bundle-stage');
  rmSync(stage, { recursive: true, force: true });
  mkdirSync(stage, { recursive: true });

  // HASH_MANIFEST files are generated by THIS script, so they are not members of the copied input set. That is
  // an ordering fact, NOT a licence to leave them unhashed: SHA256SUMS.txt is generated first and then hashed
  // INTO the manifest below (see the two-pass note there). Only BUNDLE_MANIFEST.json ends up uncovered, and
  // only because a file cannot contain its own digest.
  const all = Object.entries(ROLES).filter(([r]) => r !== 'HASH_MANIFEST').flatMap(([, ps]) => ps).sort();
  const missing = all.filter((p) => !existsSync(rel(p)));
  if (missing.length) { console.error('REFUSING: declared bundle files missing:\n  ' + missing.join('\n  ')); process.exit(1); }

  const artifacts = [];
  for (const p of all) {
    const { role, error } = roleOf(p);
    if (error) { console.error('REFUSING: ' + error); process.exit(1); }
    const abs = rel(p);
    const st = lstatSync(abs);
    if (st.isSymbolicLink()) { console.error(`REFUSING symlink in bundle: ${p}`); process.exit(1); }
    if (st.size === 0) { console.error(`REFUSING zero-byte required file: ${p}`); process.exit(1); }
    const dst = join(stage, p);
    mkdirSync(dirname(dst), { recursive: true });
    copyFileSync(abs, dst);
    artifacts.push({ path: p, role, sha256: sha(abs), byteLength: st.size, required: true });
  }
  // case-fold collision across the whole bundle
  const lower = new Map();
  for (const a of artifacts) {
    const k = a.path.toLowerCase();
    if (lower.has(k)) { console.error(`REFUSING case-fold collision: ${a.path} vs ${lower.get(k)}`); process.exit(1); }
    lower.set(k, a.path);
  }

  const srcIndex = JSON.parse(readFileSync(rel('PORTFOLIO_SOURCE_INDEX.json'), 'utf8'));
  const srcManifest = JSON.parse(readFileSync(rel('PORTFOLIO_SOURCE_MANIFEST_V2.json'), 'utf8'));
  const manifestEntries = (srcManifest.entries ?? srcManifest.sources ?? []);

  const manifest = {
    // 2.0.0 — a MAJOR bump, because the meaning of the document changed, not just its shape:
    //   • selfExcluded narrowed from "every HASH_MANIFEST file" to exactly [BUNDLE_MANIFEST.json]. A 1.x
    //     manifest excluding SHA256SUMS.txt is VALID 1.x and INVALID 2.0.0 — that is a semantic break, so it
    //     must not be silently reinterpreted under the new rules.
    //   • SHA256SUMS.txt moved into artifacts[] and is now hashed.
    // A 1.x manifest is REJECTED by validateContext rather than migrated in place: adapting an old document to
    // new rules would rewrite history. The correct path is to regenerate the bundle with this generator, which
    // is what happens here.
    artifact: 'MindBoost V2d CANDIDATE bundle manifest', schemaVersion: '2.0.0',
    status: 'CANDIDATE — Phase 8 freezes and re-reproduces this. Not yet transmitted.',
    selfAttestationNote: 'This manifest is NOT evidence of itself. validate-bundle.mjs recomputes every field from disk: existence, regular-file, symlink, byteLength, sha256, role, SHA256SUMS membership, source-archive membership, duplicates, case collisions and extra files.',
    containerHashNote: 'The ZIP hash is published OUTSIDE the ZIP (a container cannot contain its own hash).',
    sourceCompleteness: {
      claim: 'The bytes this bundle hashes are IN this bundle. The clean-room never reads the original repository.',
      manifestActiveEntries: manifestEntries.filter((e) => e.role !== 'HISTORICAL_EVIDENCE').length,
      manifestTotalEntries: manifestEntries.length,
      sourceIndexEntries: srcIndex.entries.length,
      packagedSourceEntries: srcIndex.archive.entryCount,
      archive: srcIndex.archive,
    },
    roles: Object.fromEntries(Object.entries(ROLES).map(([r, ps]) => [r, ps.length])),
    roleContract: {
      ACTIVE_AUTHORITY: 'owns a class of fact; may be read by the model',
      GENERATED_VIEW: 'derived; never an input',
      VALIDATOR: 'recomputes and gates',
      MUTATION_ORACLE: 'proves the gates can go red',
      SOURCE_EVIDENCE: 'the hashed bytes themselves',
      RUNTIME_EVIDENCE: 'executed runs only',
      HISTORICAL_EVIDENCE: 'HISTORICAL_UNVERIFIED_CLAIMS — hashed for integrity, never a current fact, never satisfies an active evidence reference',
      RAW_OUTPUT: 'captured transcripts of the suites',
      LEDGER: 'the append-only audit register',
      RUNBOOK: 'clean-room instructions',
      HASH_MANIFEST: 'carries digests of other files. Only BUNDLE_MANIFEST.json is self-excluded (a file cannot contain its own digest); SHA256SUMS.txt IS hashed by BUNDLE_MANIFEST.json, because it does not reference the manifest and so closes no cycle',
    },
    // Emitted from SELF_EXCLUDED, not from the role list: exclusion tracks self-reference, not a role label.
    // validation-mode.mjs enforces the same one-element set from code, so a manifest cannot widen it.
    selfExcluded: [...SELF_EXCLUDED].map((p) => ({ path: p, role: 'HASH_MANIFEST',
      note: 'A file cannot contain its own digest, so this is the one irreducible exclusion. Its integrity comes from the container hash, published outside the ZIP. Every other file — SHA256SUMS.txt included — is hashed in artifacts[].' })),
    artifacts,
  };
  // TWO PASSES, and the order is the whole point.
  //
  // Pass 1: SHA256SUMS.txt lists the copied artifacts. It cannot list itself (that would be circular) and it
  // does not list BUNDLE_MANIFEST.json.
  // Pass 2: BECAUSE SHA256SUMS.txt exists and references neither itself nor the manifest, the manifest can now
  // hash it. No cycle is closed. Writing the manifest first — as this script used to — made that impossible and
  // left SHA256SUMS.txt covered by nothing but the container digest, which the "cannot hash itself" note then
  // presented as a law of nature rather than a build-order artefact.
  const sums = artifacts.map((a) => `${a.sha256}  ${a.path}`).join('\n') + '\n';
  // Written to BOTH stage and repo, exactly like BUNDLE_MANIFEST.json below. The drift check compares repo
  // copies against the manifest's hashes, so a bundle artifact that exists only in the stage has no
  // counterpart to compare and reports as permanent drift.
  writeFileSync(join(stage, 'BUNDLE_SHA256SUMS.txt'), sums, 'utf8');
  writeFileSync(rel('BUNDLE_SHA256SUMS.txt'), sums, 'utf8');
  manifest.artifacts = [...artifacts, {
    path: 'BUNDLE_SHA256SUMS.txt', role: 'HASH_MANIFEST',
    sha256: createHash('sha256').update(sums, 'utf8').digest('hex'),
    byteLength: Buffer.byteLength(sums, 'utf8'), required: true,
  }].sort((a, b) => (a.path < b.path ? -1 : 1));
  writeFileSync(join(stage, 'BUNDLE_MANIFEST.json'), JSON.stringify(manifest, null, 2) + '\n', 'utf8');
  writeFileSync(rel('BUNDLE_MANIFEST.json'), JSON.stringify(manifest, null, 2) + '\n', 'utf8');

  // Deterministic container. bsdtar was tried first and rejected: its zip writer embeds an NTFS extra field
  // carrying ctime, which utimes cannot set, so two builds of identical content differed
  // (28cba698… → e4f1ab45…). buildZip() takes only the sorted path and the exact bytes.
  const files = [];
  const walk = (d) => { for (const e of readdirSync(d, { withFileTypes: true }).sort((a, b) => (a.name < b.name ? -1 : 1))) { const p = join(d, e.name); e.isDirectory() ? walk(p) : files.push(relative(stage, p).split('\\').join('/')); } };
  walk(stage);
  files.sort();
  const { buffer: zipBuf, sha256: zipSha, entryCount, order } = buildZip(files.map((f) => ({ path: f, data: readFileSync(join(stage, f)) })));
  const zip = rel('MINDBOOST_V2D_CANDIDATE_BUNDLE.zip');
  writeFileSync(zip, zipBuf);
  writeFileSync(rel('BUNDLE_CONTAINER_HASH.txt'),
    `# Published OUTSIDE the container — a ZIP cannot contain its own hash.\n${zipSha}  MINDBOOST_V2D_CANDIDATE_BUNDLE.zip\n`, 'utf8');
  rmSync(stage, { recursive: true, force: true });

  console.log(`CANDIDATE BUNDLE: ${artifacts.length} hashed files · ${entryCount} zip entries (store-only, sorted, no extra fields)`);
  console.log(`  zip sha256   ${zipSha}`);
  console.log(`  source bytes ${srcIndex.archive.entryCount} entries in ${srcIndex.archive.filename} (${srcIndex.archive.byteLength} bytes)`);
  for (const [r, n] of Object.entries(manifest.roles)) console.log(`  ${r.padEnd(20)} ${n}`);
}
