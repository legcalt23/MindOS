// @ts-check
/**
 * Integration tests run against a throwaway repo root (CLAUDE_PROJECT_DIR) so
 * they never touch the real docs/memory. Embedder is forced to 'none' (keyword
 * -only) for determinism and speed — the vector path is an optional enhancement
 * and is covered by the keyword path's contract.
 */
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'mindos-mem-'));
process.env.CLAUDE_PROJECT_DIR = TMP;
process.env.MINDOS_MEMORY_EMBEDDER = 'none';

// Realistic-but-fake key assembled at runtime; verbatim never in source.
const FAKE_AWS_KEY = 'AKIA' + 'IOSFODNN7EXAMPLE'; // FAKE_SECRET_FIXTURE

const { addEntry, getEntry, listEntries } = await import('../lib/memoryService.mjs');
const { reindex } = await import('../lib/indexer.mjs');
const { MemoryStore } = await import('../lib/store.mjs');
const { search, buildSplice } = await import('../lib/retrieval.mjs');
const { checkStale } = await import('../lib/staleness.mjs');
const { loadConfig } = await import('../lib/config.mjs');

before(() => {
  fs.mkdirSync(path.join(TMP, 'docs', 'memory', 'entries'), { recursive: true });
});
after(() => {
  try { fs.rmSync(TMP, { recursive: true, force: true }); } catch { /* ignore */ }
});

async function recall(query, opts = {}) {
  const cfg = loadConfig(true);
  const store = new MemoryStore(cfg).open();
  const hits = await search(query, opts, { store, embedder: null, cfg });
  store.close();
  return hits;
}

test('add → reindex → recall finds the entry by an exact marker token', async () => {
  await addEntry({
    type: 'CODING_STANDARD',
    title: 'Zzqq marker rule about FocusSessionStateHolder',
    content: 'A unique marker zzqqxx-token-9 lives here next to FocusSessionStateHolder details.',
    tags: ['marker'],
  }, { actor: 'test' });
  const hits = await recall('zzqqxx-token-9');
  assert.ok(hits.length >= 1, 'should retrieve at least one hit');
  assert.equal(hits[0].title, 'Zzqq marker rule about FocusSessionStateHolder');
});

test('redaction REJECTS a write containing a live-looking secret (fail-closed)', async () => {
  await assert.rejects(
    () => addEntry({
      type: 'SECURITY_NOTE',
      title: 'leak attempt',
      content: 'token = "' + FAKE_AWS_KEY + '"',
    }, { actor: 'test' }),
    /redaction guard found/i,
  );
});

test('refuses to persist access_level=sensitive content in v1', async () => {
  await assert.rejects(
    () => addEntry({
      type: 'SECURITY_NOTE', title: 'sensitive', content: 'nothing secret here', access_level: 'sensitive',
    }, { actor: 'test' }),
    /does not persist sensitive/i,
  );
});

test('staleness: an out-of-band file edit is detected', async () => {
  await reindex(loadConfig(true)); // stamp a fresh manifest
  assert.equal(checkStale(loadConfig(true)).stale, false);
  // Write a raw entry file directly (bypassing the service) -> index now behind.
  const dir = path.join(TMP, 'docs', 'memory', 'entries', 'roadmap');
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'roadmap-x.md'),
    '---\nid: roadmap-x\ntype: ROADMAP\ntitle: X\ntags: []\nimportance: medium\nversion: 1\naccess_level: internal\n---\nbody', 'utf8');
  assert.equal(checkStale(loadConfig(true)).stale, true);
});

test('canary: a fake secret/PII in a hit is scrubbed before it reaches the splice', () => {
  const cfg = loadConfig(true);
  const hit = {
    entry_id: 'x', type: 'RESEARCH_NOTE', title: 'note', importance: 'low',
    access_level: 'internal', source: 'docs/x.md', file_path: 'docs/x.md', anchor: 'Sec',
    text: 'leak ' + FAKE_AWS_KEY + ' and email a@b.com inside', score: 1,
  };
  const splice = buildSplice([hit], cfg, {});
  assert.doesNotMatch(splice, /AKIAIOSFODNN7EXAMPLE/);
  assert.doesNotMatch(splice, /a@b\.com/);
  assert.match(splice, /‹redacted:/);
});

test('sensitive entries (if any existed) are shown title-only in a splice', () => {
  const cfg = loadConfig(true);
  const hit = {
    entry_id: 's', type: 'SECURITY_NOTE', title: 'rotation runbook', importance: 'high',
    access_level: 'sensitive', source: null, file_path: 'docs/s.md', anchor: '',
    text: 'this body must never be spliced', score: 1,
  };
  const splice = buildSplice([hit], cfg, {});
  assert.doesNotMatch(splice, /must never be spliced/);
  assert.match(splice, /withheld from model/);
});

test('listEntries reflects the source of truth', async () => {
  const rows = listEntries({ type: 'CODING_STANDARD' });
  assert.ok(rows.some((r) => r.id === 'coding-standard-zzqq-marker-rule-about-focussessionstateholder'));
});
