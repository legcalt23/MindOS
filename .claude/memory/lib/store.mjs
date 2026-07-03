// @ts-check
/**
 * store.mjs — the embedded index (a disposable, rebuildable cache).
 *
 * Backed by Node 24's built-in `node:sqlite` (no native compile). FTS5 (bundled
 * in SQLite) powers keyword/BM25 search. sqlite-vec is loaded OPTIONALLY; if it
 * is missing or fails to load, the store transparently degrades to keyword-only
 * (`hasVec === false`) and every vector call becomes a no-op. The Markdown files
 * in docs/memory are the source of truth, so a corrupt or stale index is always
 * fixable with `memory reindex`.
 */

import { DatabaseSync } from 'node:sqlite';
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { indexDbPath, indexDir, ensureDir } from './paths.mjs';

const require = createRequire(import.meta.url);

const SCHEMA_VERSION = '1';

export class MemoryStore {
  /** @param {object} cfg */
  constructor(cfg) {
    this.cfg = cfg;
    this.dims = Number(cfg?.dimensions || 384);
    this.hasVec = false;
    /** @type {import('node:sqlite').DatabaseSync | null} */
    this.db = null;
  }

  open() {
    ensureDir(indexDir(this.cfg));
    const dbPath = indexDbPath(this.cfg);
    this.db = new DatabaseSync(dbPath, { allowExtension: true });
    this.db.exec('PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA foreign_keys=ON;');
    this._tryLoadVec();
    this._ensureSchema();
    return this;
  }

  _tryLoadVec() {
    if (this.cfg?.embedder === 'none') return; // keyword-only by config
    try {
      this.db?.enableLoadExtension(true);
      // sqlite-vec is an optionalDependency; resolve its loadable path lazily.
      const sqliteVec = require('sqlite-vec');
      const loadable = sqliteVec.getLoadablePath();
      this.db?.loadExtension(loadable);
      this.hasVec = true;
    } catch {
      this.hasVec = false; // degrade to keyword-only
    } finally {
      try { this.db?.enableLoadExtension(false); } catch { /* ignore */ }
    }
  }

  _ensureSchema() {
    const db = this.db;
    if (!db) throw new Error('store not open');
    db.exec(`
      CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT);
      CREATE TABLE IF NOT EXISTS entries (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        tags TEXT NOT NULL DEFAULT '[]',
        source TEXT,
        importance TEXT NOT NULL DEFAULT 'medium',
        created_at TEXT,
        updated_at TEXT,
        version INTEGER NOT NULL DEFAULT 1,
        encrypted INTEGER NOT NULL DEFAULT 0,
        access_level TEXT NOT NULL DEFAULT 'internal',
        content_hash TEXT,
        file_path TEXT
      );
      CREATE INDEX IF NOT EXISTS idx_entries_type ON entries(type);
      CREATE INDEX IF NOT EXISTS idx_entries_importance ON entries(importance);
      CREATE INDEX IF NOT EXISTS idx_entries_access ON entries(access_level);
      CREATE TABLE IF NOT EXISTS chunks (
        rid INTEGER PRIMARY KEY AUTOINCREMENT,
        chunk_id TEXT UNIQUE NOT NULL,
        entry_id TEXT NOT NULL,
        ord INTEGER NOT NULL,
        text TEXT NOT NULL,
        token_count INTEGER NOT NULL DEFAULT 0,
        anchor TEXT,
        content_hash TEXT,
        FOREIGN KEY (entry_id) REFERENCES entries(id) ON DELETE CASCADE
      );
      CREATE INDEX IF NOT EXISTS idx_chunks_entry ON chunks(entry_id);
      CREATE VIRTUAL TABLE IF NOT EXISTS fts_chunks USING fts5(
        text, title, tags, rid UNINDEXED, tokenize='unicode61'
      );
      CREATE TABLE IF NOT EXISTS embedding_cache (
        key TEXT PRIMARY KEY,
        dims INTEGER NOT NULL,
        vector TEXT NOT NULL
      );
    `);
    if (this.hasVec) {
      try {
        db.exec(`CREATE VIRTUAL TABLE IF NOT EXISTS vec_chunks USING vec0(embedding float[${this.dims}] distance_metric=cosine);`);
      } catch {
        this.hasVec = false;
      }
    }
    this.setMeta('schema_version', SCHEMA_VERSION);
  }

  // ── meta ──────────────────────────────────────────────────────────────────
  setMeta(key, value) {
    this.db?.prepare('INSERT INTO meta(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value')
      .run(key, String(value));
  }
  getMeta(key) {
    const row = this.db?.prepare('SELECT value FROM meta WHERE key=?').get(key);
    return row ? String(/** @type {any} */ (row).value) : null;
  }

  // ── full rebuild ────────────────────────────────────────────────────────────
  reset() {
    const db = this.db;
    if (!db) return;
    db.exec('DELETE FROM fts_chunks; DELETE FROM chunks; DELETE FROM entries;');
    if (this.hasVec) { try { db.exec('DELETE FROM vec_chunks;'); } catch { /* ignore */ } }
  }

  // ── writes ──────────────────────────────────────────────────────────────────
  upsertEntry(e) {
    this.db?.prepare(`
      INSERT INTO entries(id,type,title,content,tags,source,importance,created_at,updated_at,version,encrypted,access_level,content_hash,file_path)
      VALUES(@id,@type,@title,@content,@tags,@source,@importance,@created_at,@updated_at,@version,@encrypted,@access_level,@content_hash,@file_path)
      ON CONFLICT(id) DO UPDATE SET
        type=excluded.type,title=excluded.title,content=excluded.content,tags=excluded.tags,
        source=excluded.source,importance=excluded.importance,created_at=excluded.created_at,
        updated_at=excluded.updated_at,version=excluded.version,encrypted=excluded.encrypted,
        access_level=excluded.access_level,content_hash=excluded.content_hash,file_path=excluded.file_path
    `).run({
      id: e.id, type: e.type, title: e.title, content: e.content,
      tags: JSON.stringify(e.tags || []), source: e.source || null,
      importance: e.importance || 'medium', created_at: e.created_at || null,
      updated_at: e.updated_at || null, version: e.version || 1,
      encrypted: e.encrypted ? 1 : 0, access_level: e.access_level || 'internal',
      content_hash: e.content_hash || null, file_path: e.file_path || null,
    });
  }

  /** Insert a chunk and (optionally) its embedding. Returns the integer rid. */
  insertChunk(chunk, embeddingJson) {
    const info = this.db?.prepare(`
      INSERT INTO chunks(chunk_id, entry_id, ord, text, token_count, anchor, content_hash)
      VALUES(?,?,?,?,?,?,?)
    `).run(chunk.chunk_id, chunk.entry_id, chunk.ord, chunk.text, chunk.token_count || 0, chunk.anchor || null, chunk.content_hash || null);
    const rid = Number(info?.lastInsertRowid);
    this.db?.prepare('INSERT INTO fts_chunks(text, title, tags, rid) VALUES(?,?,?,?)')
      .run(chunk.text, chunk.title || '', chunk.tags || '', rid);
    if (this.hasVec && embeddingJson) {
      try {
        this.db?.prepare('INSERT INTO vec_chunks(rowid, embedding) VALUES(?,?)').run(rid, embeddingJson);
      } catch { /* vector insert is best-effort */ }
    }
    return rid;
  }

  // ── embedding cache ─────────────────────────────────────────────────────────
  getCachedEmbedding(key) {
    const row = this.db?.prepare('SELECT vector FROM embedding_cache WHERE key=?').get(key);
    return row ? String(/** @type {any} */ (row).vector) : null;
  }
  putCachedEmbedding(key, vectorJson) {
    this.db?.prepare('INSERT INTO embedding_cache(key,dims,vector) VALUES(?,?,?) ON CONFLICT(key) DO UPDATE SET vector=excluded.vector')
      .run(key, this.dims, vectorJson);
  }

  // ── reads / search ──────────────────────────────────────────────────────────
  getEntry(id) {
    const row = this.db?.prepare('SELECT * FROM entries WHERE id=?').get(id);
    return row || null;
  }
  listEntries(filter = {}) {
    const where = [];
    const args = [];
    if (filter.type) { where.push('type=?'); args.push(filter.type); }
    if (filter.importance) { where.push('importance=?'); args.push(filter.importance); }
    if (filter.access_level) { where.push('access_level=?'); args.push(filter.access_level); }
    const sql = `SELECT id,type,title,tags,source,importance,created_at,updated_at,version,access_level,file_path
                 FROM entries ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY updated_at DESC, id`;
    return this.db?.prepare(sql).all(...args) || [];
  }
  counts() {
    const e = /** @type {any} */ (this.db?.prepare('SELECT COUNT(*) n FROM entries').get())?.n || 0;
    const c = /** @type {any} */ (this.db?.prepare('SELECT COUNT(*) n FROM chunks').get())?.n || 0;
    return { entries: Number(e), chunks: Number(c), hasVec: this.hasVec };
  }

  /**
   * Keyword (BM25) candidates. Returns rows joined to entry metadata for filtering.
   * @returns {Array<any>}
   */
  ftsSearch(query, limit = 40) {
    const match = toFtsQuery(query);
    if (!match) return [];
    try {
      return this.db?.prepare(`
        SELECT c.chunk_id, c.entry_id, c.ord, c.text, c.anchor,
               e.type, e.title, e.importance, e.access_level, e.source, e.updated_at, e.file_path,
               bm25(fts_chunks) AS score
        FROM fts_chunks f
        JOIN chunks c ON c.rid = f.rid
        JOIN entries e ON e.id = c.entry_id
        WHERE fts_chunks MATCH ?
        ORDER BY score
        LIMIT ?
      `).all(match, limit) || [];
    } catch {
      return [];
    }
  }

  /**
   * Vector KNN candidates (only if hasVec). embeddingJson is a JSON array string.
   * @returns {Array<any>}
   */
  vecSearch(embeddingJson, k = 40) {
    if (!this.hasVec || !embeddingJson) return [];
    try {
      const hits = this.db?.prepare(`
        SELECT rowid AS rid, distance FROM vec_chunks
        WHERE embedding MATCH ? ORDER BY distance LIMIT ?
      `).all(embeddingJson, k) || [];
      if (!hits.length) return [];
      const ridList = hits.map((h) => Number(/** @type {any} */ (h).rid));
      const placeholders = ridList.map(() => '?').join(',');
      const rows = this.db?.prepare(`
        SELECT c.rid, c.chunk_id, c.entry_id, c.ord, c.text, c.anchor,
               e.type, e.title, e.importance, e.access_level, e.source, e.updated_at, e.file_path
        FROM chunks c JOIN entries e ON e.id = c.entry_id
        WHERE c.rid IN (${placeholders})
      `).all(...ridList) || [];
      const distByRid = new Map(hits.map((h) => [Number(/** @type {any} */ (h).rid), Number(/** @type {any} */ (h).distance)]));
      return rows.map((r) => ({ ...(/** @type {any} */ (r)), distance: distByRid.get(Number(/** @type {any} */ (r).rid)) }))
        .sort((a, b) => a.distance - b.distance);
    } catch {
      return [];
    }
  }

  close() {
    try { this.db?.exec('PRAGMA wal_checkpoint(TRUNCATE);'); } catch { /* ignore */ }
    try { this.db?.close(); } catch { /* ignore */ }
    this.db = null;
  }
}

/** Convert a free-text query into a safe FTS5 MATCH expression (OR of terms + phrase). */
function toFtsQuery(query) {
  const terms = String(query)
    .toLowerCase()
    .match(/[a-z0-9_./:-]{2,}/g);
  if (!terms || !terms.length) return null;
  const uniq = [...new Set(terms)].slice(0, 24);
  // Quote each term so path/dot tokens don't break the FTS parser; OR them.
  return uniq.map((t) => `"${t.replace(/"/g, '')}"`).join(' OR ');
}

export function deleteIndexFiles(cfg) {
  const dir = indexDir(cfg);
  for (const f of ['index.db', 'index.db-wal', 'index.db-shm']) {
    try { fs.rmSync(path.join(dir, f), { force: true }); } catch { /* ignore */ }
  }
}
