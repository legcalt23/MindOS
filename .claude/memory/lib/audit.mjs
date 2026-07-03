// @ts-check
/**
 * audit.mjs — append-only, PII-free audit log of memory operations.
 *
 * Design choices (from the security review + pre-mortem):
 *  - ALLOWLISTED fields only. A fixed serializer drops anything not on the list,
 *    so a careless future caller can't turn the audit into a PII sink.
 *  - NEVER logs: raw query text, entry content, matched secret values, file
 *    contents. Only `query_hash` correlates a search without storing the query.
 *  - Monthly JSONL files under the gitignored index dir (per-worktree, machine
 *    -local evidence — not committed to git).
 *  - No hash-chain ceremony in v1 (git history is the tamper-evidence for a solo
 *    dev). Documented as a Phase-4 add-on.
 */

import fs from 'node:fs';
import path from 'node:path';
import { auditDir, ensureDir } from './paths.mjs';

const ALLOWED_FIELDS = new Set([
  'ts', 'op', 'entry_id', 'entry_type', 'actor', 'query_hash',
  'result_ids', 'finding_patterns', 'finding_count', 'version', 'reason',
]);

const ALLOWED_OPS = new Set([
  'add', 'update', 'get', 'search', 'reindex', 'write_rejected',
  'session_context', 'session_summary', 'restore', 'verify',
]);

/**
 * Append one audit event. Silently allowlist-filters keys; never throws into
 * the caller (audit failure must not break a memory op).
 * @param {Record<string, unknown>} event
 * @param {object} [cfg]
 */
export function audit(event, cfg) {
  try {
    if (cfg && cfg.audit && cfg.audit.enabled === false) return;
    const clean = sanitize(event);
    if (!clean) return;
    const dir = ensureDir(auditDir(cfg));
    const month = clean.ts.slice(0, 7).replace('-', ''); // YYYYMM
    const file = path.join(dir, `audit-${month}.jsonl`);
    fs.appendFileSync(file, JSON.stringify(clean) + '\n', 'utf8');
  } catch {
    /* audit must never break the operation it records */
  }
}

/** Build a PII-free record from an arbitrary event, dropping non-allowlisted keys. */
function sanitize(event) {
  if (!event || typeof event.op !== 'string' || !ALLOWED_OPS.has(event.op)) return null;
  /** @type {Record<string, unknown>} */
  const out = { ts: new Date().toISOString(), op: event.op };
  for (const [k, v] of Object.entries(event)) {
    if (k === 'ts' || k === 'op') continue;
    if (!ALLOWED_FIELDS.has(k)) continue; // drop anything not allowlisted
    if (k === 'result_ids' || k === 'finding_patterns') {
      out[k] = Array.isArray(v) ? v.map((x) => String(x)).slice(0, 50) : [];
    } else if (k === 'finding_count' || k === 'version') {
      out[k] = Number.isFinite(v) ? Number(v) : 0;
    } else {
      out[k] = String(v).slice(0, 200);
    }
  }
  return /** @type {{ts:string,op:string} & Record<string,unknown>} */ (out);
}

/** Read recent audit lines (newest last) for the `memory status`/`verify` views. */
export function readAudit(cfg, limit = 50) {
  try {
    const dir = auditDir(cfg);
    if (!fs.existsSync(dir)) return [];
    const files = fs.readdirSync(dir).filter((f) => f.endsWith('.jsonl')).sort();
    /** @type {object[]} */
    const rows = [];
    for (const f of files) {
      const lines = fs.readFileSync(path.join(dir, f), 'utf8').split('\n').filter(Boolean);
      for (const line of lines) {
        try { rows.push(JSON.parse(line)); } catch { /* skip corrupt line */ }
      }
    }
    return rows.slice(-limit);
  } catch {
    return [];
  }
}
