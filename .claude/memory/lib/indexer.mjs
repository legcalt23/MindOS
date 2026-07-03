// @ts-check
/**
 * indexer.mjs — (re)build the disposable index from the Markdown source of truth.
 *
 * Two sources feed the index:
 *  1. docs/memory/entries/**.md  — the authored memory entries.
 *  2. config.ingest.external      — existing brain files (CLAUDE.md, AGENTS.md)
 *     ingested READ-ONLY as searchable chunks (never copied/duplicated).
 *
 * Embeddings are cached by content-hash (provider+model+dims keyed), so a
 * reindex only re-embeds CHANGED chunks — cheap and deterministic.
 */

import fs from 'node:fs';
import path from 'node:path';
import { MemoryStore } from './store.mjs';
import { createEmbedder, vectorToJson } from './embedder.mjs';
import { chunkMarkdown, estimateTokens } from './chunker.mjs';
import { fileToEntry } from './frontmatter.mjs';
import { normalizeEntry, slugify } from './schema.mjs';
import { contentHash, embeddingCacheKey } from './hashing.mjs';
import { computeManifest, writeManifest } from './staleness.mjs';
import { entriesDir, repoRoot, relToRoot } from './paths.mjs';

/** Walk entriesDir and parse every *.md into a normalized entry + file_path. */
export function loadEntriesFromDisk() {
  const dir = entriesDir();
  /** @type {Array<any>} */
  const entries = [];
  walk(dir, (file) => {
    if (!file.toLowerCase().endsWith('.md')) return;
    try {
      const raw = fs.readFileSync(file, 'utf8');
      const parsed = fileToEntry(raw);
      if (!parsed.type || !parsed.title) return;
      const entry = normalizeEntry(parsed);
      entry.file_path = relToRoot(file);
      entries.push(entry);
    } catch { /* skip malformed */ }
  });
  return entries;
}

function walk(dir, fn) {
  if (!fs.existsSync(dir)) return;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name.startsWith('.')) continue;
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full, fn);
    else fn(full);
  }
}

/** Build synthetic entries for ingested external files (read-only chunks). */
function loadExternalEntries(cfg) {
  /** @type {Array<any>} */
  const out = [];
  for (const ext of cfg?.ingest?.external || []) {
    const abs = path.join(repoRoot(), ext.path);
    if (!fs.existsSync(abs)) continue;
    let text = '';
    try { text = fs.readFileSync(abs, 'utf8'); } catch { continue; }
    out.push({
      entry: normalizeEntry({
        id: `ext-${slugify(ext.path)}`,
        type: ext.type || 'RESEARCH_NOTE',
        title: `Ingested: ${ext.path}`,
        content: `(Read-only ingestion of ${ext.path}; not a copy. Source of truth is the file itself.)`,
        tags: ext.tags || ['ingested'],
        source: ext.path,
        importance: 'medium',
        access_level: 'internal',
        metadata: { ingested: true },
      }),
      file_path: ext.path,
      rawText: text,
      splitOnHeading: ext.splitOnHeading,
    });
  }
  return out;
}

/**
 * Embed an array of texts using the cache + batching uncached ones.
 * @returns {Promise<Array<string|null>>} JSON vectors aligned to `texts`, or nulls if no embedder.
 */
async function embedTexts(store, embedder, texts) {
  if (!embedder) return texts.map(() => null);
  const keys = texts.map((t) => embeddingCacheKey(embedder.id, embedder.model, embedder.dims, t));
  const result = new Array(texts.length).fill(null);
  const uncachedIdx = [];
  keys.forEach((k, i) => {
    const cached = store.getCachedEmbedding(k);
    if (cached) result[i] = cached;
    else uncachedIdx.push(i);
  });
  if (uncachedIdx.length) {
    const batchTexts = uncachedIdx.map((i) => texts[i]);
    const vectors = await embedder.embed(batchTexts, { inputType: 'document' });
    uncachedIdx.forEach((i, j) => {
      const json = vectorToJson(vectors[j]);
      store.putCachedEmbedding(keys[i], json);
      result[i] = json;
    });
  }
  return result;
}

/** Index one entry's chunks (shared by authored + external). */
async function indexEntry(store, embedder, entry, rawText, splitOnHeading, cfg) {
  const text = rawText != null ? rawText : entry.content;
  const chunks = chunkMarkdown(text, {
    targetTokens: cfg.chunking.targetTokens,
    maxTokens: cfg.chunking.maxTokens,
    overlapTokens: cfg.chunking.overlapTokens,
    splitOnHeading,
  });
  if (!chunks.length) return 0;
  const vectors = await embedTexts(store, embedder, chunks.map((c) => c.text));
  const tagStr = (entry.tags || []).join(' ');
  chunks.forEach((c, i) => {
    store.insertChunk({
      chunk_id: `${entry.id}#${c.ord}`,
      entry_id: entry.id,
      ord: c.ord,
      text: c.text,
      token_count: estimateTokens(c.text),
      anchor: c.anchor,
      content_hash: contentHash(c.text),
      title: entry.title,
      tags: tagStr,
    }, vectors[i]);
  });
  return chunks.length;
}

/**
 * Full rebuild of the index from disk. Returns stats.
 * @param {object} cfg
 * @param {{ onProgress?: (msg: string) => void }} [hooks]
 */
export async function reindex(cfg, hooks = {}) {
  const log = hooks.onProgress || (() => {});
  const store = new MemoryStore(cfg).open();
  const embedder = await createEmbedder(cfg);
  store.reset();

  const authored = loadEntriesFromDisk();
  let chunkCount = 0;
  for (const entry of authored) {
    store.upsertEntry({ ...entry, content_hash: contentHash(entry.content) });
    chunkCount += await indexEntry(store, embedder, entry, null, undefined, cfg);
  }
  log(`indexed ${authored.length} authored entries`);

  const externals = loadExternalEntries(cfg);
  for (const ext of externals) {
    store.upsertEntry({ ...ext.entry, file_path: ext.file_path, content_hash: contentHash(ext.rawText) });
    chunkCount += await indexEntry(store, embedder, { ...ext.entry, file_path: ext.file_path }, ext.rawText, ext.splitOnHeading, cfg);
  }
  if (externals.length) log(`ingested ${externals.length} external file(s)`);

  store.setMeta('embed_provider', embedder ? embedder.id : 'none');
  store.setMeta('embed_model', embedder ? embedder.model : 'none');
  store.setMeta('dims', String(cfg.dimensions));
  store.setMeta('reindexed_at', new Date().toISOString());

  const stats = store.counts();
  writeManifest(cfg, computeManifest(cfg));
  store.close();
  return {
    entries: authored.length,
    externals: externals.length,
    chunks: chunkCount,
    indexedChunks: stats.chunks,
    hasVec: stats.hasVec,
    embedder: embedder ? `${embedder.id}:${embedder.model}` : 'none (keyword-only)',
  };
}
