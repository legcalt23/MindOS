#!/usr/bin/env node
// validate-authority-entrypoints.mjs — Focus V5 authority-chain validator (V2, registry-driven).
//
// REPLACES the V1 substring-includes scanner, which passed a known contraejemplo (a marker quoted as
// "obsolete", a legacy doc claiming authority in prose, an index NEGATING a quarantine). This version:
//   - honors a machine authority declaration ONLY inside a sentinel-delimited block (prose mentions ignored);
//   - validates the tree AGAINST a structured registry (authority-registry.json), not free substrings;
//   - emits STRUCTURED errors { code, path, rule, message } and JSON;
//   - is IMPORT-SAFE: importing scanContent does not run main or call process.exit;
//   - exposes rules individually so mutation adequacy can be proven (see the .test.mjs).
//
//   node scripts/validate-authority-entrypoints.mjs            # scan repo; exit 0 PASS / 1 FAIL
//   node scripts/validate-authority-entrypoints.mjs --json     # machine-readable result to stdout
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, basename } from 'node:path';
import { createHash } from 'node:crypto';

// ---- pure helpers (no fs) ----------------------------------------------------
/** Trimmed lines that appear strictly BETWEEN the sentinel block markers (prose outside is ignored). */
export function authorityBlockLines(content, reg) {
  const out = [];
  let inside = false;
  for (const raw of content.split(/\r?\n/)) {
    const t = raw.trim();
    if (t === reg.block_start) { inside = true; continue; }
    if (t === reg.block_end) { inside = false; continue; }
    if (inside) out.push(t);
  }
  return out;
}
const declaresMarker = (c, reg) => authorityBlockLines(c, reg).includes(reg.marker);
const declaresRole = (c, reg) => authorityBlockLines(c, reg).includes(reg.current_role_line);

// ---- rules: each returns errors[] and is individually disableable --------------
// A rule reads { files, reg, dirMd } where files=Map<path,content>, dirMd=Map<basename,content>.
const RULES = [
  {
    id: 'registry_wellformed',
    run({ reg, files }) {
      const e = [];
      const seen = new Set();
      for (const d of reg.docs) {
        if (seen.has(d.path)) e.push({ code: 'DUPLICATE_REGISTRY_RECORD', path: d.path, rule: 'registry_wellformed', message: `duplicate registry record: ${d.path}` });
        seen.add(d.path);
        if (d.superseded_by && !reg.docs.some((x) => x.path === d.superseded_by) && d.superseded_by !== reg.current_index) {
          e.push({ code: 'SUPERSEDED_BY_TARGET_MISSING', path: d.path, rule: 'registry_wellformed', message: `superseded_by target not registered: ${d.superseded_by}` });
        }
      }
      if (!files.has(reg.current_index)) e.push({ code: 'MISSING_INDEX_FILE', path: reg.current_index, rule: 'registry_wellformed', message: `current index file absent: ${reg.current_index}` });
      return e;
    },
  },
  {
    id: 'entrypoint_marker',
    run({ reg, files }) {
      const e = [];
      for (const ep of reg.entrypoints) {
        const c = files.get(ep);
        if (c == null) { e.push({ code: 'MISSING_ENTRYPOINT_FILE', path: ep, rule: 'entrypoint_marker', message: `entrypoint file absent: ${ep}` }); continue; }
        if (!declaresMarker(c, reg)) e.push({ code: 'ENTRYPOINT_MARKER_MISSING', path: ep, rule: 'entrypoint_marker', message: `entrypoint lacks the marker inside the authority block (prose/negated mentions do not count): ${ep}` });
      }
      return e;
    },
  },
  {
    id: 'index_role_unique',
    run({ reg, dirMd, files }) {
      const e = [];
      const claimers = [...dirMd.entries()].filter(([, c]) => declaresRole(c, reg)).map(([n]) => n);
      const idxBase = basename(reg.current_index);
      if (claimers.length === 0) e.push({ code: 'INDEX_ROLE_MISSING', path: reg.current_index, rule: 'index_role_unique', message: 'no file declares AUTHORITY_INDEX_ROLE: CURRENT inside an authority block' });
      else if (claimers.length > 1) e.push({ code: 'INDEX_ROLE_DUPLICATE', path: claimers.join(','), rule: 'index_role_unique', message: `CURRENT role declared by ${claimers.length} files: ${claimers.join(', ')}` });
      else if (claimers[0] !== idxBase) e.push({ code: 'INDEX_ROLE_WRONG_FILE', path: claimers[0], rule: 'index_role_unique', message: `CURRENT role declared by the wrong file: ${claimers[0]} (expected ${idxBase})` });
      // also require the index file to actually be the declaring one when present
      const idx = files.get(reg.current_index);
      if (idx != null && !declaresRole(idx, reg) && claimers.length === 0) { /* already reported */ }
      return e;
    },
  },
  {
    id: 'legacy_no_marker_no_role',
    run({ reg, files }) {
      const e = [];
      for (const d of reg.docs.filter((x) => x.role === 'LEGACY')) {
        const c = files.get(d.path);
        if (c == null) continue;
        if (declaresMarker(c, reg)) e.push({ code: 'LEGACY_CARRIES_MARKER', path: d.path, rule: 'legacy_no_marker_no_role', message: `legacy doc carries the index marker in an authority block: ${d.path}` });
        if (declaresRole(c, reg)) e.push({ code: 'LEGACY_CARRIES_ROLE', path: d.path, rule: 'legacy_no_marker_no_role', message: `legacy doc declares AUTHORITY_INDEX_ROLE: CURRENT: ${d.path}` });
      }
      return e;
    },
  },
  {
    id: 'legacy_no_forbidden_vocab',
    run({ reg, files }) {
      const e = [];
      for (const d of reg.docs.filter((x) => x.role === 'LEGACY')) {
        const c = files.get(d.path);
        if (c == null) continue;
        for (const pat of reg.forbidden_legacy_vocab) {
          if (new RegExp(pat, 'i').test(c)) { e.push({ code: 'LEGACY_FORBIDDEN_VOCAB', path: d.path, rule: 'legacy_no_forbidden_vocab', message: `legacy doc asserts authority vocabulary (/${pat}/i): ${d.path}` }); break; }
        }
      }
      return e;
    },
  },
  {
    id: 'quarantine_positive',
    run({ reg, files }) {
      const e = [];
      const idx = files.get(reg.current_index) || '';
      for (const d of reg.docs.filter((x) => x.status === 'QUARANTINED')) {
        if (!d.superseded_by) { e.push({ code: 'QUARANTINE_SUCCESSOR_MISSING', path: d.path, rule: 'quarantine_positive', message: `quarantined doc has no successor: ${d.path}` }); continue; }
        // a NEGATION phrase referencing this doc's basename in the index is a fake disposition
        const base = basename(d.path);
        for (const neg of reg.quarantine_negation_patterns) {
          const re = new RegExp(`${neg}[^\\n]{0,80}${base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`, 'i');
          if (re.test(idx)) { e.push({ code: 'QUARANTINE_NEGATED', path: d.path, rule: 'quarantine_positive', message: `index NEGATES the disposition of a quarantined doc ("${neg}" … ${base})` }); break; }
        }
      }
      return e;
    },
  },
  {
    id: 'hash_pin',
    run({ reg, files }) {
      const e = [];
      for (const d of reg.docs) {
        if (!d.worktree_sha256) continue;
        const c = files.get(d.path);
        if (c == null) continue;
        const h = createHash('sha256').update(Buffer.from(c, 'utf8')).digest('hex');
        if (h !== d.worktree_sha256) e.push({ code: 'REGISTRY_HASH_MISMATCH', path: d.path, rule: 'hash_pin', message: `pinned worktree hash mismatch for ${d.path}: registry=${d.worktree_sha256.slice(0, 12)}… actual=${h.slice(0, 12)}…` });
      }
      return e;
    },
  },
  {
    id: 'no_unregistered_authority_doc',
    run({ reg, dirMd }) {
      const e = [];
      // Allowed to appear in the dir without being flagged: registered docs, entrypoints (they legitimately
      // CARRY the marker pointing to the index), and the index itself. A rogue doc is one that CLAIMS the
      // CURRENT role or asserts authority vocabulary — merely pointing to the index (marker) is not a claim.
      const allowed = new Set([
        ...reg.docs.map((d) => basename(d.path)),
        ...reg.entrypoints.map((p) => basename(p)),
        basename(reg.current_index),
      ]);
      for (const [name, c] of dirMd.entries()) {
        if (allowed.has(name)) continue;
        const vocab = reg.forbidden_legacy_vocab.some((p) => new RegExp(p, 'i').test(c));
        if (declaresRole(c, reg) || vocab) {
          e.push({ code: 'UNREGISTERED_AUTHORITY_DOC', path: `${reg.authority_dir}/${name}`, rule: 'no_unregistered_authority_doc', message: `unregistered doc claims authority (CURRENT role or authority vocabulary): ${name}` });
        }
      }
      return e;
    },
  },
];

/**
 * Pure core. IMPORT-SAFE: no fs, no process.exit.
 * @param files Map<repoRelativePath, content> for entrypoints + current index (+ legacy docs).
 * @param reg   parsed authority-registry.json
 * @param opts  { dirMd?: Map<basename, content>, disabledRules?: Set<string> }
 */
export function scanContent(files, reg, opts = {}) {
  const dirMd = opts.dirMd || new Map();
  const disabled = opts.disabledRules || new Set();
  const errors = [];
  for (const rule of RULES) {
    if (disabled.has(rule.id)) continue;
    errors.push(...rule.run({ files, reg, dirMd }));
  }
  return { ok: errors.length === 0, errors };
}

export const RULE_IDS = RULES.map((r) => r.id);

// ---- fs wrapper (only runs as CLI) -------------------------------------------
export function scanRepo(root) {
  const reg = JSON.parse(readFileSync(join(root, 'docs/engines/focus_v5/authority-registry.json'), 'utf8'));
  const files = new Map();
  for (const p of [...reg.entrypoints, reg.current_index, ...reg.docs.map((d) => d.path)]) {
    const abs = join(root, p);
    if (existsSync(abs)) files.set(p, readFileSync(abs, 'utf8'));
  }
  const dirMd = new Map();
  const dir = join(root, reg.authority_dir);
  if (existsSync(dir)) for (const n of readdirSync(dir)) if (n.endsWith('.md')) dirMd.set(n, readFileSync(join(dir, n), 'utf8'));
  return { reg, ...scanContent(files, reg, { dirMd }) };
}

const isMain = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMain) {
  const root = join(dirname(fileURLToPath(import.meta.url)), '..');
  const asJson = process.argv.includes('--json');
  const r = scanRepo(root);
  if (asJson) { process.stdout.write(JSON.stringify({ ok: r.ok, errors: r.errors }, null, 2) + '\n'); process.exit(r.ok ? 0 : 1); }
  if (r.ok) { console.log('AUTHORITY CHAIN OK — registry validated: entrypoints declare the index inside the authority block; one CURRENT index; legacy docs quarantined with valid successors, no authority vocabulary, no marker; no unregistered authority doc; pinned hashes match.'); process.exit(0); }
  console.error('AUTHORITY CHAIN FAILED:');
  for (const e of r.errors) console.error(`  - [${e.code}] ${e.path}: ${e.message}`);
  process.exit(1);
}
