// @ts-check
/**
 * retrieval.mjs — hybrid keyword(BM25)+vector search fused with Reciprocal Rank
 * Fusion, then a compact, citeable splice block for prompts.
 *
 * Design choices baked in (from the pre-mortem):
 *  - KEYWORD-LED: this corpus is full of exact tokens (file paths, task ids,
 *    Detekt rule names, error strings). FTS gets a bias and exact-token matches
 *    are boosted to the top — pure vectors blur exactly what makes Claude useful.
 *  - DIVERSITY: at most one (best) chunk per entry, so a near-duplicate flood of
 *    similar "Aprendizaje" entries can't crowd out the one useful hit.
 *  - SAFE SPLICE: every snippet is re-run through the redaction guard; sensitive
 *    entries are surfaced as title+stub only; output is budget-capped and every
 *    hit carries a verifiable `source:` anchor.
 */

import { vectorToJson } from './embedder.mjs';
import { redactForModel } from './redaction.mjs';
import { estimateTokens } from './chunker.mjs';

/**
 * @param {string} query
 * @param {{ k?: number, types?: string[], tags?: string[], importance?: string, accessLevel?: string }} opts
 * @param {{ store: import('./store.mjs').MemoryStore, embedder: any, cfg: object }} ctx
 */
export async function search(query, opts, ctx) {
  const { store, embedder, cfg } = ctx;
  const rcfg = cfg.retrieval || {};
  const rrfK = rcfg.rrfK || 60;
  const keywordBias = rcfg.keywordBias || 1.15;

  const ftsRows = store.ftsSearch(query, 48);
  let vecRows = [];
  if (embedder && store.hasVec) {
    try {
      const qvec = await embedder.embed([query], { inputType: 'query' });
      vecRows = store.vecSearch(vectorToJson(qvec[0]), 48);
    } catch { vecRows = []; }
  }

  /** @type {Map<string, any>} */
  const byChunk = new Map();
  const fuse = (rows, weight) => {
    rows.forEach((row, rank) => {
      const id = row.chunk_id;
      const prev = byChunk.get(id) || { row, score: 0 };
      prev.score += weight * (1 / (rrfK + rank + 1));
      prev.row = row;
      byChunk.set(id, prev);
    });
  };
  fuse(ftsRows, keywordBias);
  fuse(vecRows, 1.0);

  // Exact-token boost: file paths / task-ids / dotted names in the query that
  // appear verbatim in a chunk get a strong nudge.
  if (rcfg.exactTokenBoost !== false) {
    const exactTokens = (String(query).match(/[A-Za-z0-9_./-]*[/_.][A-Za-z0-9_./-]*|[A-Za-z]+\d[\w-]*/g) || [])
      .filter((t) => t.length >= 4);
    if (exactTokens.length) {
      for (const item of byChunk.values()) {
        const text = String(item.row.text || '');
        if (exactTokens.some((t) => text.includes(t))) item.score += 0.5;
      }
    }
  }

  // Filter by metadata, then keep the best chunk per entry.
  /** @type {Map<string, any>} */
  const bestPerEntry = new Map();
  for (const item of byChunk.values()) {
    const r = item.row;
    if (!passesFilter(r, opts)) continue;
    const cur = bestPerEntry.get(r.entry_id);
    if (!cur || item.score > cur.score) bestPerEntry.set(r.entry_id, item);
  }

  const k = opts.k || rcfg.k || 8;
  const ranked = [...bestPerEntry.values()].sort((a, b) => b.score - a.score).slice(0, k);
  return ranked.map((it) => ({
    entry_id: it.row.entry_id,
    type: it.row.type,
    title: it.row.title,
    importance: it.row.importance,
    access_level: it.row.access_level,
    source: it.row.source,
    file_path: it.row.file_path,
    anchor: it.row.anchor,
    updated_at: it.row.updated_at,
    text: it.row.text,
    score: Number(it.score.toFixed(6)),
  }));
}

function passesFilter(row, opts) {
  if (opts.types && opts.types.length && !opts.types.includes(row.type)) return false;
  if (opts.importance && row.importance !== opts.importance) return false;
  if (opts.accessLevel && row.access_level !== opts.accessLevel) return false;
  if (opts.tags && opts.tags.length) {
    const hay = String(row.text || '').toLowerCase();
    if (!opts.tags.some((t) => hay.includes(String(t).toLowerCase()))) return false;
  }
  return true;
}

/**
 * Build the prompt-ready splice block. Budget-capped, redacted, citeable.
 * @param {Array<any>} hits
 * @param {object} cfg
 * @param {{ stale?: { stale: boolean, changed: number }, query?: string }} [meta]
 */
export function buildSplice(hits, cfg, meta = {}) {
  const rcfg = cfg.retrieval || {};
  const perHitCharCap = rcfg.perHitCharCap || 600;
  const budget = rcfg.spliceTokenBudget || 1500;
  const lines = [];
  const banner = meta.stale && meta.stale.stale
    ? ` index-stale="${meta.stale.changed} changed file(s) — run: node .claude/memory/cli.mjs reindex"`
    : '';
  lines.push(`<memory-context retrieved="${hits.length}"${banner}>`);
  let used = estimateTokens(lines[0]);
  let shown = 0;
  for (let i = 0; i < hits.length; i++) {
    const h = hits[i];
    const head = `[${i + 1}] type=${h.type} importance=${h.importance} id=${h.entry_id}${h.updated_at ? ' updated=' + h.updated_at : ''}`;
    const src = `    source: ${citation(h)}`;
    let bodyLine;
    if (h.access_level === 'sensitive') {
      bodyLine = '    [encrypted/sensitive — body withheld from model; ask the human]';
    } else {
      const safe = redactForModel(String(h.text || '')).replace(/\s+/g, ' ').trim().slice(0, perHitCharCap);
      bodyLine = '    > ' + safe + (String(h.text || '').length > perHitCharCap ? ' …(trimmed)' : '');
    }
    const block = `${head}\n    title: ${h.title}\n${src}\n${bodyLine}`;
    const cost = estimateTokens(block);
    if (used + cost > budget && shown > 0) {
      lines.push(`… (${hits.length - shown} more hits omitted for token budget)`);
      break;
    }
    lines.push(block);
    used += cost;
    shown++;
  }
  lines.push('</memory-context>');
  return lines.join('\n');
}

function citation(h) {
  const file = h.file_path || h.source || `entries/${h.entry_id}.md`;
  let anchor = '';
  if (h.anchor) anchor = '#' + (/^L\d+$/.test(h.anchor) ? h.anchor : anchorSlug(h.anchor));
  return `${file}${anchor}`;
}

function anchorSlug(text) {
  return String(text).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 50);
}
