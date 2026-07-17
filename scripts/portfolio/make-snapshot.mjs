#!/usr/bin/env node
// R3-A3 §2 — freeze the snapshot BEFORE any review.
//
// The reviewers must all cite the same bytes. A verdict against a tree that has since moved is not a verdict, so
// the snapshot id is a hash over (path, sha256) for every in-scope file — including the untracked ones, which is
// where every R3-A change lives. A conventional `git diff` shows none of them.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, statSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, resolve } from 'node:path';

const REPO = resolve(process.argv[2] ?? '.');
const sh = (args) => execFileSync('git', args, { cwd: REPO, encoding: 'utf8' });
const sha = (buf) => createHash('sha256').update(buf).digest('hex');

// In-scope = the portfolio toolchain and its artifacts. Foreign WIP (shared/**, functions/**, .claude/**) is
// deliberately excluded from the SNAPSHOT ID but recorded separately as preserved-foreign, so a reviewer can see
// it was neither staged nor touched.
// Three-way classification. The first version had only two buckets and got it wrong twice over: it filed my own
// artifacts (CLEAN_ROOM_RUNBOOK.md, DELIVERY_MANIFEST.json, SHA256SUMS.txt, the candidate ZIP) as FOREIGN, and it
// had no answer to the circularity that running the gates rewrites the result files — which would change the very
// id those results are bound to, so no snapshot could ever survive its own verification.
//
//   INPUTS  → define the snapshotId. Source + artifacts. What a reviewer reviews.
//   OUTPUTS → produced BY a run over those inputs. Hashed and recorded, but EXCLUDED from the id, because a run
//             cannot be an input to itself. They carry `snapshotId` internally (see atomic-report.mjs) — that is
//             what binds them, not membership in the hash.
//   FOREIGN → someone else's WIP. Recorded to prove it was neither staged nor touched.
const OUTPUTS = (p) =>
  /^R3A_FINAL_SNAPSHOT\.(json|sha256sums\.txt)$/.test(p) ||
  /^(PHASE\d|PHASE\d+_|R03_|LOCK_)/.test(p) && p.endsWith('.json') ||
  p === 'MUTATION_REGISTRY.json' || p === 'E1_GAP_ANALYSIS.json' ||
  /^PORTFOLIO_(AUTHORITY_TESTS_RESULT|EVIDENCE_MUTATIONS_RESULT|EVIDENCE_VALIDATION|RUNTIME_EVIDENCE)\.json$/.test(p);

const INPUTS = (p) =>
  !OUTPUTS(p) &&
  (p.startsWith('scripts/portfolio/') ||
    p.startsWith('docs/portfolio/') ||
    /^(PORTFOLIO_|BUNDLE_|MINDBOOST_EXERCISE|MINDBOOST_121|V2D_|R3A_)/.test(p) ||
    p === 'CLEAN_ROOM_RUNBOOK.md' || p === 'DELIVERY_MANIFEST.json' || p === 'SHA256SUMS.txt' ||
    p === 'MINDBOOST_V2D_CANDIDATE_BUNDLE.zip' || /^MINDBOOST_PORTFOLIO_V2C_HANDOFF_[0-9a-f]{64}\.zip$/.test(p));

const IN_SCOPE = (p) => INPUTS(p) || OUTPUTS(p);

const status = sh(['status', '--porcelain=v2', '-z']).split('\0').filter(Boolean);
const untracked = [], trackedModified = [], foreign = [];
for (const line of status) {
  if (line.startsWith('? ')) {
    const p = line.slice(2);
    (IN_SCOPE(p) ? untracked : foreign).push(p);
  } else if (line.startsWith('1 ') || line.startsWith('2 ')) {
    const p = line.split(' ').slice(8).join(' ');
    (IN_SCOPE(p) ? trackedModified : foreign).push(p);
  }
}

// Expand untracked directories — git reports `scripts/portfolio/` as one entry, not 43 files.
const expand = (entries) => {
  const out = [];
  for (const e of entries) {
    if (e.endsWith('/')) {
      const listed = sh(['ls-files', '--others', '--exclude-standard', '-z', '--', e]).split('\0').filter(Boolean);
      out.push(...listed);
    } else out.push(e);
  }
  return out.sort();
};

const hashOf = (p) => ({ path: p, bytes: statSync(join(REPO, p)).size, sha256: sha(readFileSync(join(REPO, p))) });
const all = [...new Set([...expand(untracked), ...trackedModified])].sort();
const inputEntries = all.filter(INPUTS).map(hashOf);
const outputEntries = all.filter(OUTPUTS).map(hashOf);

// Snapshot id: sha256 over the canonical "sha256  path\n" listing of the INPUTS ONLY. Reproducible by any reviewer
// with sha256sum over the same list.
const canonical = inputEntries.map((e) => `${e.sha256}  ${e.path}`).join('\n') + '\n';
const snapshotId = sha(canonical);
const entries = inputEntries;

const snapshot = {
  snapshotSchemaVersion: '1.0.0',
  snapshotId,
  head: sh(['rev-parse', 'HEAD']).trim(),
  headSubject: sh(['log', '-1', '--format=%s']).trim(),
  detached: (() => { try { sh(['symbolic-ref', '-q', '--short', 'HEAD']); return false; } catch { return true; } })(),
  idCoversRule: 'snapshotId = sha256 over the "sha256  path" listing of INPUT files only. Outputs are produced BY a run over these inputs and are bound to it by carrying snapshotId internally; including them would make the id depend on its own verification.',
  inputFileCount: inputEntries.length,
  outputFileCount: outputEntries.length,
  outputs: outputEntries,
  inScopeFileCount: entries.length,
  inScopeTrackedModified: trackedModified.sort(),
  inScopeUntracked: expand(untracked).filter(IN_SCOPE),
  deletedOrRenamed: status.filter((l) => l.startsWith('2 ') || / D /.test(l)).map((l) => l),
  preservedForeignCount: expand(foreign).length,
  preservedForeign: expand(foreign),
  files: entries,
  canonicalListingSha256: snapshotId,
};

writeFileSync(join(REPO, 'R3A_FINAL_SNAPSHOT.json'), JSON.stringify(snapshot, null, 2) + '\n');
writeFileSync(join(REPO, 'R3A_FINAL_SNAPSHOT.sha256sums.txt'), canonical);
console.log(`snapshotId : ${snapshotId}`);
console.log(`head       : ${snapshot.head} (detached=${snapshot.detached})`);
console.log(`in-scope   : ${entries.length} files (${trackedModified.length} tracked-modified, ${snapshot.inScopeUntracked.length} untracked)`);
console.log(`foreign    : ${snapshot.preservedForeignCount} preserved, untouched`);
