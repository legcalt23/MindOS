#!/usr/bin/env node
// MB-FOCUS-0083 — preserve a phase's evidence OUTSIDE the worktree before any reviewer is launched.
//
// Why this exists: a reviewer that can touch the tree can delete the artifacts it was launched to audit.
// That is not hypothetical — on 2026-08-28 an exit gate wiped 609 test-result XML, and the only reason the
// phase verdict survived is that its Gradle log happened to live outside the repo. A prohibition in a
// reviewer's prompt is not a mechanism; moving the bytes out of its reach is.
//
// Fail-closed on every axis that could make a "preserved" claim hollow:
//   - a source that does not exist            -> throw (you cannot preserve what is not there)
//   - a destination INSIDE the repo           -> throw (that is the whole defect being fixed)
//   - zero files actually copied              -> throw (anti-vacuity: a preserver that preserves nothing
//                                                would otherwise report success for every input)
//   - a copy whose sha256 differs from source -> throw (a corrupted copy is worse than no copy: it looks
//                                                like evidence)
//
// Related but NOT the same as scripts/portfolio/make-snapshot.mjs, which freezes an IDENTITY (hashes) and
// writes its output INSIDE the repo. That one answers "are we all reviewing the same bytes"; this one
// answers "do the bytes still exist after the reviewer ran".
import { createHash } from 'node:crypto';
import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { basename, join, relative, resolve, sep } from 'node:path';

const sha256 = (buf) => createHash('sha256').update(buf).digest('hex');

/** Every regular file under `root`, as paths relative to `root`. */
export function walk(root, prefix = '') {
  const out = [];
  for (const name of readdirSync(join(root, prefix))) {
    const rel = prefix ? join(prefix, name) : name;
    if (statSync(join(root, rel)).isDirectory()) out.push(...walk(root, rel));
    else out.push(rel);
  }
  return out.sort();
}

/** True when `child` is the repo root itself or lives inside it. */
export function isInside(child, parent) {
  const rel = relative(resolve(parent), resolve(child));
  return rel === '' || (!rel.startsWith('..') && !resolve(rel).startsWith(sep) && !/^[A-Za-z]:/.test(rel));
}

/**
 * Copy `sources` under `outDir`, verify every byte, and write a manifest.
 * Returns { manifestPath, entries }. Throws on any of the fail-closed conditions above.
 */
export function preserve({ sources, outDir, repoRoot, stamp }) {
  if (!Array.isArray(sources) || sources.length === 0) throw new Error('no sources given');
  if (isInside(outDir, repoRoot)) {
    throw new Error(`destination is inside the repo (${outDir}) - evidence there is deletable by a reviewer`);
  }
  const dest = join(outDir, `evidence-${stamp}`);
  mkdirSync(dest, { recursive: true });

  const entries = [];
  for (const src of sources) {
    if (!existsSync(src)) throw new Error(`source does not exist: ${src}`);
    const target = join(dest, basename(src));
    cpSync(src, target, { recursive: true });
    const files = statSync(src).isDirectory() ? walk(src) : [null];
    for (const rel of files) {
      const from = rel === null ? src : join(src, rel);
      const to = rel === null ? target : join(target, rel);
      const a = sha256(readFileSync(from));
      const b = sha256(readFileSync(to));
      if (a !== b) throw new Error(`copy mismatch for ${from}: ${a} != ${b}`);
      entries.push({ source: from, dest: to, sha256: a, bytes: statSync(to).size });
    }
  }
  if (entries.length === 0) throw new Error('anti-vacuity: zero files preserved');

  const manifestPath = join(dest, 'MANIFEST.json');
  writeFileSync(manifestPath, JSON.stringify({ stamp, repoRoot, count: entries.length, entries }, null, 2) + '\n');
  return { manifestPath, entries };
}

if (process.argv[1] && process.argv[1].endsWith('preserve-phase-evidence.mjs')) {
  const [outDir, ...sources] = process.argv.slice(2);
  if (!outDir || sources.length === 0) {
    console.error('usage: preserve-phase-evidence.mjs <outDirOutsideRepo> <source...>');
    process.exit(2);
  }
  const stamp = process.env.EVIDENCE_STAMP ?? new Date().toISOString().replace(/[:.]/g, '-');
  const { manifestPath, entries } = preserve({ sources, outDir, repoRoot: process.cwd(), stamp });
  console.log(`preserved ${entries.length} file(s) -> ${manifestPath}`);
}
