// @ts-check
/**
 * memoryService.mjs — the WRITE path. Every mutation goes through here so the
 * redaction guard and audit log can never be bypassed.
 *
 * Invariants:
 *  - Fail-closed redaction: if the guard finds a secret/PII, NOTHING is written.
 *  - v1 refuses to persist access_level=sensitive content (no encryption
 *    subsystem yet — keep real secrets in your OS store / password manager).
 *  - No hard deletes (audit trail): supersede instead.
 *  - After a successful write the index is rebuilt (cheap: embedding cache means
 *    only changed chunks re-embed) so retrieval is never behind the files.
 */

import fs from 'node:fs';
import path from 'node:path';
import { entriesDir, ensureDir, relToRoot } from './paths.mjs';
import { normalizeEntry, validateEntry } from './schema.mjs';
import { entryToFile, fileToEntry } from './frontmatter.mjs';
import { guardEntry, findingPatternNames } from './redaction.mjs';
import { contentHash } from './hashing.mjs';
import { audit } from './audit.mjs';
import { loadConfig } from './config.mjs';
import { reindex } from './indexer.mjs';

function entryFilePath(entry) {
  const dir = path.join(entriesDir(), String(entry.type).toLowerCase());
  return path.join(dir, `${entry.id}.md`);
}

function walkMd(dir, fn) {
  if (!fs.existsSync(dir)) return;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name.startsWith('.')) continue;
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walkMd(full, fn);
    else if (e.name.toLowerCase().endsWith('.md')) fn(full);
  }
}

/** Locate an entry's file by its id (scans the entries tree). */
export function findEntryFileById(id) {
  let found = null;
  walkMd(entriesDir(), (file) => {
    if (found) return;
    try {
      const parsed = fileToEntry(fs.readFileSync(file, 'utf8'));
      if (parsed.id === id) found = file;
    } catch { /* skip */ }
  });
  return found;
}

function refuseSensitive(entry, cfg) {
  if (entry.access_level === 'sensitive' && cfg?.redaction?.forbidSensitivePersistence !== false) {
    throw new Error(
      'refused: v1 does not persist sensitive content. Keep real secrets in your OS store / password manager, ' +
      'and store only a non-secret pointer in memory (or raise the forbidSensitivePersistence flag deliberately).'
    );
  }
}

function runGuard(entry, actor, cfg) {
  const guard = guardEntry(entry);
  if (!guard.ok) {
    const patterns = findingPatternNames(guard.findings);
    audit({ op: 'write_rejected', entry_id: entry.id, entry_type: entry.type, actor, finding_patterns: patterns, finding_count: guard.findings.length }, cfg);
    throw new Error(`refused: redaction guard found ${patterns.join(', ')} — nothing was written. Remove the secret/PII and retry.`);
  }
}

/**
 * Add a new memory entry.
 * @param {Partial<import('./schema.mjs').MemoryEntry>} partial
 * @param {{ actor?: string, reindex?: boolean }} [opts]
 */
export async function addEntry(partial, opts = {}) {
  const cfg = loadConfig();
  const actor = opts.actor || 'cli';
  const entry = normalizeEntry(partial);
  const v = validateEntry(entry);
  if (!v.ok) throw new Error('invalid entry: ' + v.errors.join('; '));
  refuseSensitive(entry, cfg);
  runGuard(entry, actor, cfg);

  entry.content_hash = contentHash(entry.content);
  const file = entryFilePath(entry);
  if (fs.existsSync(file)) throw new Error(`entry already exists: ${entry.id} (use update)`);
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, entryToFile(entry), 'utf8');
  audit({ op: 'add', entry_id: entry.id, entry_type: entry.type, actor, version: entry.version }, cfg);

  let stats = null;
  if (opts.reindex !== false) stats = await reindex(cfg);
  return { id: entry.id, file: relToRoot(file), reindex: stats };
}

/**
 * Update an existing entry (bumps version, keeps created_at, rewrites the file).
 * @param {string} id
 * @param {Partial<import('./schema.mjs').MemoryEntry>} patch
 * @param {{ actor?: string, reindex?: boolean }} [opts]
 */
export async function updateEntry(id, patch, opts = {}) {
  const cfg = loadConfig();
  const actor = opts.actor || 'cli';
  const abs = findEntryFileById(id);
  if (!abs) throw new Error(`entry not found: ${id}`);
  const existing = fileToEntry(fs.readFileSync(abs, 'utf8'));
  const merged = normalizeEntry({
    ...existing,
    ...patch,
    id: existing.id,
    type: patch.type || existing.type,
    created_at: existing.created_at,
    version: (Number(existing.version) || 1) + 1,
    updated_at: undefined, // normalizeEntry stamps today
  });
  const v = validateEntry(merged);
  if (!v.ok) throw new Error('invalid entry: ' + v.errors.join('; '));
  refuseSensitive(merged, cfg);
  runGuard(merged, actor, cfg);

  merged.content_hash = contentHash(merged.content);
  const newFile = entryFilePath(merged);
  if (path.resolve(newFile) !== path.resolve(abs)) {
    fs.rmSync(abs, { force: true }); // type changed -> move file
  }
  ensureDir(path.dirname(newFile));
  fs.writeFileSync(newFile, entryToFile(merged), 'utf8');
  audit({ op: 'update', entry_id: merged.id, entry_type: merged.type, actor, version: merged.version }, cfg);

  let stats = null;
  if (opts.reindex !== false) stats = await reindex(cfg);
  return { id: merged.id, file: relToRoot(newFile), version: merged.version, reindex: stats };
}

/** Read a single entry by id (from the source of truth). */
export function getEntry(id) {
  const abs = findEntryFileById(id);
  if (!abs) return null;
  const entry = fileToEntry(fs.readFileSync(abs, 'utf8'));
  entry.file_path = relToRoot(abs);
  return entry;
}

/** List entry summaries from disk, optionally filtered. */
export function listEntries(filter = {}) {
  /** @type {Array<any>} */
  const out = [];
  walkMd(entriesDir(), (file) => {
    try {
      const e = fileToEntry(fs.readFileSync(file, 'utf8'));
      if (filter.type && e.type !== filter.type) return;
      if (filter.importance && e.importance !== filter.importance) return;
      if (filter.access_level && e.access_level !== filter.access_level) return;
      if (filter.tag && !(Array.isArray(e.tags) && e.tags.includes(filter.tag))) return;
      out.push({
        id: e.id, type: e.type, title: e.title, importance: e.importance,
        tags: e.tags || [], updated_at: e.updated_at, source: e.source || null,
        file: relToRoot(file),
      });
    } catch { /* skip */ }
  });
  return out.sort((a, b) => String(b.updated_at).localeCompare(String(a.updated_at)));
}
