#!/usr/bin/env node
// @ts-check
/**
 * cli.mjs — MindBoost Memory Layer command line.
 *
 * The single source of logic that slash commands and (future) the MCP server
 * both call. Runs on built-in Node; vector search activates automatically once
 * the optional deps are installed (see .claude/memory/README.md).
 *
 * Usage:
 *   node .claude/memory/cli.mjs <command> [args] [--flags]
 *
 * Commands:
 *   recall <query>          Retrieve relevant context (prints a prompt-ready splice)
 *   search <query>          Alias of recall; add --json or --raw for other formats
 *   add                     Add an entry (--type --title --tags --importance --source --access, content via --content/--file/stdin)
 *   update <id>             Update an entry (same flags as add)
 *   get <id>                Print one entry
 *   list                    List entries (--type --importance --tag --json)
 *   session-context         Build session-start context (--task "..." --branch X --out FILE)
 *   reindex                 Rebuild the index from docs/memory (full)
 *   status                  Index health, staleness, embedder, audit tail
 *   verify                  Self-test: redaction canary + index integrity
 *   types                   List the 15 memory types
 *   adr --title "..."       Scaffold an ADR entry to stdout (add --add to persist)
 *   help
 */

import fs from 'node:fs';
import path from 'node:path';
import { loadConfig } from './lib/config.mjs';
import { MemoryStore } from './lib/store.mjs';
import { createEmbedder } from './lib/embedder.mjs';
import { reindex } from './lib/indexer.mjs';
import { search, buildSplice } from './lib/retrieval.mjs';
import { checkStale } from './lib/staleness.mjs';
import { addEntry, updateEntry, getEntry, listEntries } from './lib/memoryService.mjs';
import { audit, readAudit } from './lib/audit.mjs';
import { guardText } from './lib/redaction.mjs';
import { MEMORY_TYPES, TYPE_DESCRIPTIONS, todayIso } from './lib/schema.mjs';
import { repoRoot, indexDir, ensureDir } from './lib/paths.mjs';

function parseArgs(argv) {
  const _ = [];
  /** @type {Record<string, string|boolean>} */
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const next = argv[i + 1];
      if (next === undefined || next.startsWith('--')) flags[key] = true;
      else { flags[key] = next; i++; }
    } else _.push(a);
  }
  return { _, flags };
}

function out(s) { process.stdout.write(s + '\n'); }
function err(s) { process.stderr.write(s + '\n'); }

async function ensureFresh(cfg) {
  const stale = checkStale(cfg);
  if (stale.stale) await reindex(cfg);
  return stale;
}

function readContent(flags) {
  if (typeof flags.content === 'string') return flags.content;
  if (typeof flags.file === 'string') return fs.readFileSync(path.resolve(repoRoot(), flags.file), 'utf8');
  // Read stdin only when explicitly asked (--stdin), so metadata-only updates never block.
  if (flags.stdin) {
    try { return fs.readFileSync(0, 'utf8'); } catch { return ''; }
  }
  return '';
}

function entryFromFlags(flags) {
  return {
    type: String(flags.type || '').toUpperCase(),
    title: String(flags.title || ''),
    content: readContent(flags),
    tags: flags.tags ? String(flags.tags).split(',').map((t) => t.trim()).filter(Boolean) : [],
    importance: flags.importance ? String(flags.importance) : 'medium',
    source: flags.source ? String(flags.source) : null,
    access_level: flags.access ? String(flags.access) : 'internal',
  };
}

// ── commands ───────────────────────────────────────────────────────────────
async function cmdRecall(query, flags, cfg) {
  if (!query) { err('usage: recall <query>'); process.exitCode = 1; return; }
  const stale = await ensureFresh(cfg);
  const store = new MemoryStore(cfg).open();
  const embedder = await createEmbedder(cfg);
  const opts = {
    k: flags.k ? Number(flags.k) : undefined,
    types: flags.type ? [String(flags.type).toUpperCase()] : undefined,
    importance: flags.importance ? String(flags.importance) : undefined,
  };
  const hits = await search(query, opts, { store, embedder, cfg });
  store.close();
  audit({ op: 'search', actor: 'cli', query_hash: undefined, result_ids: hits.map((h) => h.entry_id), finding_count: hits.length }, cfg);
  if (flags.json) { out(JSON.stringify(hits, null, 2)); return; }
  if (flags.raw) {
    if (!hits.length) { out('(no results)'); return; }
    hits.forEach((h, i) => out(`${i + 1}. [${h.type}] ${h.title}  (${h.file_path}) score=${h.score}`));
    return;
  }
  out(buildSplice(hits, cfg, { stale, query }));
}

async function cmdAdd(flags) {
  const entry = entryFromFlags(flags);
  if (!entry.type || !entry.title) { err('usage: add --type TYPE --title "..." [--content "..."|--file F|stdin]'); process.exitCode = 1; return; }
  try {
    const res = await addEntry(entry, { actor: 'cli' });
    out(`✓ added ${res.id}\n  file: ${res.file}` + (res.reindex ? `\n  index: ${res.reindex.entries} entries / ${res.reindex.indexedChunks} chunks (${res.reindex.embedder})` : ''));
  } catch (e) { err('✗ ' + message(e)); process.exitCode = 1; }
}

async function cmdUpdate(id, flags) {
  if (!id) { err('usage: update <id> [flags]'); process.exitCode = 1; return; }
  const patch = {};
  if (flags.title) patch.title = String(flags.title);
  if (flags.type) patch.type = String(flags.type).toUpperCase();
  if (flags.tags) patch.tags = String(flags.tags).split(',').map((t) => t.trim()).filter(Boolean);
  if (flags.importance) patch.importance = String(flags.importance);
  if (flags.source) patch.source = String(flags.source);
  if (flags.access) patch.access_level = String(flags.access);
  if (flags.content || flags.file || flags.stdin) patch.content = readContent(flags);
  try {
    const res = await updateEntry(id, patch, { actor: 'cli' });
    out(`✓ updated ${res.id} -> v${res.version}\n  file: ${res.file}`);
  } catch (e) { err('✗ ' + message(e)); process.exitCode = 1; }
}

function cmdGet(id, flags) {
  if (!id) { err('usage: get <id>'); process.exitCode = 1; return; }
  const e = getEntry(id);
  if (!e) { err(`not found: ${id}`); process.exitCode = 1; return; }
  if (flags.json) out(JSON.stringify(e, null, 2));
  else out(`# ${e.title}\ntype: ${e.type}  importance: ${e.importance}  v${e.version}  (${e.file_path})\n\n${e.content}`);
}

function cmdList(flags) {
  const filter = {};
  if (flags.type) filter.type = String(flags.type).toUpperCase();
  if (flags.importance) filter.importance = String(flags.importance);
  if (flags.tag) filter.tag = String(flags.tag);
  const rows = listEntries(filter);
  if (flags.json) { out(JSON.stringify(rows, null, 2)); return; }
  if (!rows.length) { out('(no entries)'); return; }
  for (const r of rows) out(`${r.updated_at}  [${r.type}] ${r.title}  · ${r.id}`);
  out(`\n${rows.length} entr${rows.length === 1 ? 'y' : 'ies'}`);
}

async function cmdSessionContext(flags, cfg) {
  const stale = await ensureFresh(cfg);
  const store = new MemoryStore(cfg).open();
  const embedder = await createEmbedder(cfg);
  const pinned = [];
  const pick = (type, n) => listEntries({ type }).slice(0, n).map((r) => r.id);
  const pinnedIds = [
    ...pick('PRODUCT_VISION', 1),
    ...pick('ARCHITECTURE', 2),
    ...pick('ROADMAP', 1),
    ...pick('SESSION_SUMMARY', 2),
  ];
  for (const id of pinnedIds) {
    const e = getEntry(id);
    if (e) pinned.push({ entry_id: e.id, type: e.type, title: e.title, importance: e.importance, access_level: e.access_level, source: e.source, file_path: e.file_path, anchor: '', text: String(e.content).slice(0, 800), score: 99 });
  }
  let taskHits = [];
  const task = flags.task ? String(flags.task) : (flags.branch ? String(flags.branch).replace(/[-_/]/g, ' ') : '');
  if (task) taskHits = await search(task, { k: 6 }, { store, embedder, cfg });
  store.close();
  const seen = new Set();
  const all = [...pinned, ...taskHits].filter((h) => (seen.has(h.entry_id) ? false : seen.add(h.entry_id)));
  const splice = buildSplice(all, cfg, { stale, query: task });
  audit({ op: 'session_context', actor: 'cli', result_ids: all.map((h) => h.entry_id), finding_count: all.length }, cfg);
  if (flags.out) {
    const dest = path.resolve(repoRoot(), String(flags.out));
    ensureDir(path.dirname(dest));
    fs.writeFileSync(dest, splice + '\n', 'utf8');
    out(`✓ session context -> ${flags.out} (${all.length} entries)`);
  } else {
    out(splice);
  }
}

async function cmdReindex(cfg) {
  const t0 = Date.now();
  const stats = await reindex(cfg, { onProgress: (m) => err('  ' + m) });
  audit({ op: 'reindex', actor: 'cli', finding_count: stats.indexedChunks }, cfg);
  out(`✓ reindexed ${stats.entries} entries + ${stats.externals} external → ${stats.indexedChunks} chunks in ${Date.now() - t0}ms`);
  out(`  vectors: ${stats.hasVec ? 'on' : 'OFF (keyword-only)'} · embedder: ${stats.embedder}`);
}

function cmdStatus(cfg) {
  const store = new MemoryStore(cfg).open();
  const counts = store.counts();
  const provider = store.getMeta('embed_provider');
  const reAt = store.getMeta('reindexed_at');
  store.close();
  const stale = checkStale(cfg);
  out('MindBoost Memory Layer — status');
  out(`  source of truth : docs/memory/entries (git-versioned)`);
  out(`  index           : ${path.relative(repoRoot(), indexDir(cfg)).split(path.sep).join('/')}/index.db (gitignored)`);
  out(`  entries/chunks  : ${counts.entries} / ${counts.chunks}`);
  out(`  vectors         : ${counts.hasVec ? 'on' : 'OFF (keyword-only — run npm install in .claude/memory to enable)'}`);
  out(`  embedder        : ${provider || 'unknown'}`);
  out(`  last reindex    : ${reAt || 'never'}`);
  out(`  staleness       : ${stale.stale ? `STALE (${stale.changed} changed file(s) — run reindex)` : 'fresh'}`);
  const tail = readAudit(cfg, 5);
  if (tail.length) { out('  recent audit    :'); for (const r of tail) out(`    ${r.ts}  ${r.op}  ${r.entry_id || r.op}`); }
}

function cmdTypes() {
  out('Memory types (15):');
  for (const t of MEMORY_TYPES) out(`  ${t.padEnd(18)} ${TYPE_DESCRIPTIONS[t]}`);
}

function cmdAdr(flags) {
  const title = flags.title ? String(flags.title) : 'TITLE';
  const body = adrTemplate(title);
  if (flags.add) {
    addEntry({ type: 'ADR', title, content: body, tags: ['adr'], importance: 'high' }, { actor: 'cli' })
      .then((r) => out(`✓ ADR added ${r.id}\n  file: ${r.file}`))
      .catch((e) => { err('✗ ' + message(e)); process.exitCode = 1; });
  } else {
    out(body);
  }
}

function cmdVerify(cfg) {
  // 1) Redaction canary: a realistic-but-fake secret MUST be caught.
  const canary = 'const k = "AKIA' + 'IOSFODNN7EXAMPLE";'; // FAKE_SECRET_FIXTURE-shaped
  const r1 = guardText(canary.replace('// FAKE_SECRET_FIXTURE', ''), 'canary');
  const guardOk = !r1.ok && r1.findings.some((f) => f.pattern === 'aws_access_key');
  // 2) Clean prose MUST pass.
  const r2 = guardText('A normal coding standard about Compose state hoisting.', 'canary');
  const cleanOk = r2.ok;
  // 3) Index integrity.
  const stale = checkStale(cfg);
  audit({ op: 'verify', actor: 'cli', finding_count: guardOk && cleanOk ? 0 : 1 }, cfg);
  out(`redaction guard (catches secret) : ${guardOk ? 'PASS' : 'FAIL'}`);
  out(`redaction guard (allows prose)   : ${cleanOk ? 'PASS' : 'FAIL'}`);
  out(`index fresh                      : ${stale.stale ? 'STALE (run reindex)' : 'PASS'}`);
  if (!guardOk || !cleanOk) process.exitCode = 1;
}

function adrTemplate(title) {
  return [
    `# ADR: ${title}`,
    '',
    `**Status:** proposed  ·  **Date:** ${todayIso()}`,
    '',
    '## Context',
    'What problem/force prompts this decision? Constraints, prior art, links to docs/.',
    '',
    '## Decision',
    'The decision, stated plainly.',
    '',
    '## Alternatives considered',
    '- Option A — why not.',
    '- Option B — why not.',
    '',
    '## Consequences',
    'Trade-offs, follow-ups, what this makes easier/harder.',
    '',
    '## Security & privacy',
    'Any data, secret, or boundary impact (or "none").',
  ].join('\n');
}

function message(e) { return e instanceof Error ? e.message : String(e); }

function help() {
  out(`MindBoost Memory Layer
  recall <query> [--type T --k N --json --raw]   retrieve relevant context (splice)
  add --type T --title "..." [--tags a,b --importance high --source path --content "..."|--file F]
  update <id> [flags]                            edit an entry (bumps version)
  get <id> [--json]                              print one entry
  list [--type T --importance I --tag X --json]  list entries
  session-context [--task "..." --branch X --out FILE]   session-start context
  reindex                                        rebuild index from docs/memory
  status                                         health, staleness, embedder
  verify                                         redaction canary + index check
  types                                          the 15 memory types
  adr --title "..." [--add]                      scaffold an ADR`);
}

async function main() {
  const { _, flags } = parseArgs(process.argv.slice(2));
  const cmd = _[0] || 'help';
  const cfg = loadConfig();
  try {
    switch (cmd) {
      case 'recall':
      case 'search': await cmdRecall(_.slice(1).join(' '), flags, cfg); break;
      case 'add': await cmdAdd(flags); break;
      case 'update': await cmdUpdate(_[1], flags); break;
      case 'get': cmdGet(_[1], flags); break;
      case 'list': cmdList(flags); break;
      case 'session-context':
      case 'session-start': await cmdSessionContext(flags, cfg); break;
      case 'reindex': await cmdReindex(cfg); break;
      case 'status': cmdStatus(cfg); break;
      case 'verify': cmdVerify(cfg); break;
      case 'types': cmdTypes(); break;
      case 'adr': cmdAdr(flags); break;
      case 'help': default: help();
    }
  } catch (e) {
    err('✗ ' + message(e));
    process.exitCode = 1;
  }
}

main();
