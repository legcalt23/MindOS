// @ts-check
/**
 * staleness.mjs — keep the disposable index honest about the Markdown source of
 * truth. The #1 risk from the design pre-mortem was SILENT drift: the index
 * serving a chunk whose source line no longer exists. We defend with a content
 * -hash manifest: every reindex stamps a hash of all source files; search
 * compares the stored hash against the live one and reindexes (or warns) when
 * behind. Never answer from an index known to be behind the files.
 */

import fs from 'node:fs';
import path from 'node:path';
import { entriesDir, manifestPath, repoRoot, ensureDir, indexDir } from './paths.mjs';
import { sha256 } from './hashing.mjs';

/** Recursively list *.md files under a dir. */
function listMarkdown(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name.startsWith('.')) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...listMarkdown(full));
    else if (entry.name.toLowerCase().endsWith('.md')) out.push(full);
  }
  return out;
}

/** All files whose content the index depends on (entries + ingested externals). */
export function sourceFiles(cfg) {
  const files = listMarkdown(entriesDir());
  for (const ext of cfg?.ingest?.external || []) {
    const p = path.join(repoRoot(), ext.path);
    if (fs.existsSync(p)) files.push(p);
  }
  return files.sort();
}

/**
 * Compute a manifest hash over the source files (content-addressed, so cosmetic
 * mtime changes don't churn but real edits do).
 * @returns {{ hash: string, count: number }}
 */
export function computeManifest(cfg) {
  const files = sourceFiles(cfg);
  const parts = [];
  for (const f of files) {
    try {
      const content = fs.readFileSync(f, 'utf8');
      parts.push(path.relative(repoRoot(), f).split(path.sep).join('/') + ':' + sha256(content));
    } catch { /* skip unreadable */ }
  }
  return { hash: sha256(parts.join('\n')), count: files.length };
}

export function readManifest(cfg) {
  try {
    return JSON.parse(fs.readFileSync(manifestPath(cfg), 'utf8'));
  } catch {
    return null;
  }
}

export function writeManifest(cfg, manifest) {
  ensureDir(indexDir(cfg));
  fs.writeFileSync(manifestPath(cfg), JSON.stringify({ ...manifest, stampedAt: new Date().toISOString() }, null, 2), 'utf8');
}

/**
 * Is the index behind the source files?
 * @returns {{ stale: boolean, changed: number, liveCount: number }}
 */
export function checkStale(cfg) {
  const live = computeManifest(cfg);
  const stored = readManifest(cfg);
  if (!stored || stored.hash !== live.hash) {
    const changed = stored ? Math.abs(live.count - (stored.count || 0)) : live.count;
    return { stale: true, changed, liveCount: live.count };
  }
  return { stale: false, changed: 0, liveCount: live.count };
}
