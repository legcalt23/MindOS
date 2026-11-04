// node:test suite for the phase-evidence preserver (MB-FOCUS-0083).
// Run: node --test scripts/preserve-phase-evidence.test.mjs
//
// Every fail-closed branch gets its DISCRIMINATING half: without them, an implementation that simply
// threw on everything would pass, and one that preserved nothing would report success.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, sep } from 'node:path';
import { preserve, isInside, walk } from './preserve-phase-evidence.mjs';

const tmp = () => mkdtempSync(join(tmpdir(), 'mb-evidence-'));

test('preserves files outside the repo and records a verifiable manifest', () => {
  const repo = tmp(), out = tmp();
  writeFileSync(join(repo, 'run.log'), 'BUILD SUCCESSFUL\n');
  mkdirSync(join(repo, 'results'));
  writeFileSync(join(repo, 'results', 'a.xml'), '<a/>');
  writeFileSync(join(repo, 'results', 'b.xml'), '<b/>');

  const { manifestPath, entries } = preserve({
    sources: [join(repo, 'run.log'), join(repo, 'results')],
    outDir: out, repoRoot: repo, stamp: 'T1',
  });

  assert.equal(entries.length, 3, 'one log + two xml must be preserved');
  assert.ok(existsSync(manifestPath));
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  assert.equal(manifest.count, 3);
  // The copies must be readable AND identical - a manifest over bytes that no longer match is the
  // failure mode this whole script exists to prevent.
  for (const e of manifest.entries) {
    assert.equal(readFileSync(e.dest, 'utf8'), readFileSync(e.source, 'utf8'), e.source);
  }
});

test('refuses a destination inside the repo - the defect being fixed', () => {
  const repo = tmp();
  writeFileSync(join(repo, 'run.log'), 'x');
  assert.throws(
    () => preserve({ sources: [join(repo, 'run.log')], outDir: join(repo, 'build', 'evidence'), repoRoot: repo, stamp: 'T2' }),
    /inside the repo/,
  );
  // DISCRIMINATING HALF: the very same call with a destination OUTSIDE must succeed, or "refuses"
  // would just mean "never works".
  const out = tmp();
  const { entries } = preserve({ sources: [join(repo, 'run.log')], outDir: out, repoRoot: repo, stamp: 'T2b' });
  assert.equal(entries.length, 1);
});

test('refuses a source that does not exist', () => {
  const repo = tmp(), out = tmp();
  assert.throws(() => preserve({ sources: [join(repo, 'missing.log')], outDir: out, repoRoot: repo, stamp: 'T3' }), /does not exist/);
});

test('refuses an empty source list', () => {
  const out = tmp();
  assert.throws(() => preserve({ sources: [], outDir: out, repoRoot: tmp(), stamp: 'T4' }), /no sources/);
});

test('anti-vacuity: an empty directory preserves nothing and must NOT report success', () => {
  const repo = tmp(), out = tmp();
  mkdirSync(join(repo, 'empty'));
  assert.throws(() => preserve({ sources: [join(repo, 'empty')], outDir: out, repoRoot: repo, stamp: 'T5' }), /anti-vacuity/);
});

test('isInside answers both directions', () => {
  const repo = tmp(), sibling = tmp();
  assert.equal(isInside(join(repo, 'build'), repo), true, 'a child is inside');
  assert.equal(isInside(repo, repo), true, 'the root itself counts as inside');
  assert.equal(isInside(sibling, repo), false, 'a sibling temp dir is NOT inside');
});

test('walk finds nested files and sorts them', () => {
  const root = tmp();
  mkdirSync(join(root, 'deep', 'er'), { recursive: true });
  writeFileSync(join(root, 'b.txt'), '');
  writeFileSync(join(root, 'a.txt'), '');
  writeFileSync(join(root, 'deep', 'er', 'c.txt'), '');
  const found = walk(root).map((p) => p.split(sep).join('/'));
  assert.deepEqual(found, ['a.txt', 'b.txt', 'deep/er/c.txt']);
});
