#!/usr/bin/env node
// @ts-check
/**
 * seed.mjs — real MindBoost example entries, so the memory layer ships useful
 * out of the box (and doubles as worked examples of each type). Idempotent:
 * entries that already exist are skipped. Each goes through the redaction guard.
 *
 *   node .claude/memory/scripts/seed.mjs
 */

import { addEntry } from '../lib/memoryService.mjs';
import { reindex } from '../lib/indexer.mjs';
import { loadConfig } from '../lib/config.mjs';

/** @type {Array<Partial<import('../lib/schema.mjs').MemoryEntry>>} */
const SEEDS = [
  {
    type: 'PRODUCT_VISION',
    title: 'MindBoost — functional cognitive trainer inside Mindverse',
    tags: ['vision', 'safety', 'positioning'],
    importance: 'critical',
    access_level: 'public',
    source: 'CLAUDE.md',
    content: [
      '## Vision',
      'MindBoost is the cognitive intelligence trainer inside Mindverse. It trains functional cognitive abilities — attention, working memory, processing speed, reasoning, creativity, metacognition, decision-making, and emotional regulation applied to performance.',
      '',
      '## Value proposition',
      'Premium, credible, privacy-first training measured against the user\'s own baseline with adaptive difficulty — not generic brain-game claims.',
      '',
      '## Boundaries (what this is NOT)',
      '- Not a medical, clinical, diagnostic, or therapeutic product.',
      '- Does not diagnose any health condition.',
      '- Does not promise guaranteed IQ increases or clinical improvement.',
      '- Prefer language: "functional cognitive profile", "practice", "training", "progress against your own baseline", "adaptive difficulty".',
    ].join('\n'),
  },
  {
    type: 'ADR',
    title: 'Memory Layer is hybrid local-first (Markdown source of truth + sqlite-vec index)',
    tags: ['memory-layer', 'architecture', 'rag', 'local-first'],
    importance: 'high',
    access_level: 'internal',
    source: 'docs/memory/ARCHITECTURE.md',
    content: [
      '**Status:** accepted  ·  **Date:** 2026-06-30',
      '',
      '## Context',
      'Claude Code loses context across sessions and across Vibe Kanban git worktrees. The machine has no Docker and no local Postgres, and the repo ethos is local-first + privacy-first.',
      '',
      '## Decision',
      'Markdown-with-frontmatter files in git (docs/memory/entries) are the source of truth. An embedded sqlite-vec + FTS5 index (a disposable, gitignored, per-worktree cache) provides hybrid keyword+vector retrieval. Embeddings are provider-decoupled with a local/offline default. Integration is via a CLI + slash commands; an MCP server is an optional later phase.',
      '',
      '## Alternatives considered',
      '- PostgreSQL + pgvector as source of truth — rejected: needs Docker/daemon or cloud egress; breaks local-first and worktree-travel.',
      '- Harness file-memory only — rejected: does not travel across worktrees (one session dir per worktree path).',
      '',
      '## Consequences',
      'Versioning/audit/diff come free from git. The index is never load-bearing (rebuildable via `memory reindex`). Postgres remains a documented upgrade path with an explicit trigger threshold (corpus >100k chunks, multi-user, or server-side RAG).',
      '',
      '## Security & privacy',
      'A fail-closed redaction guard forbids secrets/PII from being persisted or spliced into prompts. v1 stores no sensitive content (no encryption subsystem yet).',
    ].join('\n'),
  },
  {
    type: 'ARCHITECTURE',
    title: 'Project Memory Layer — components and data flow',
    tags: ['memory-layer', 'architecture'],
    importance: 'high',
    access_level: 'internal',
    source: 'docs/memory/ARCHITECTURE.md',
    content: [
      '## Overview',
      'A persistent "project brain" for Claude Code: authored Markdown entries + ingested existing brain files (CLAUDE.md, AGENTS.md), indexed for hybrid retrieval and spliced into prompts on demand.',
      '',
      '## Modules & boundaries',
      '- lib/schema, lib/frontmatter — the 15-type data model + file format.',
      '- lib/store — node:sqlite (FTS5) + optional sqlite-vec; a rebuildable cache.',
      '- lib/embedder — provider-decoupled embeddings (local default).',
      '- lib/redaction — fail-closed write guard + retrieval scrubber.',
      '- lib/indexer / lib/retrieval — build index; hybrid RRF search + splice.',
      '- lib/memoryService — the only write path (guard + audit).',
      '- cli.mjs — the shared core all slash commands call.',
      '',
      '## Key invariants',
      '- Markdown is the only source of truth; the .db is disposable.',
      '- Every write passes the redaction guard (fail-closed).',
      '- Search never answers from an index known to be behind the files (auto-reindex on staleness; loud banner otherwise).',
    ].join('\n'),
  },
  {
    type: 'SECURITY_NOTE',
    title: 'Memory never stores secrets; redaction is fail-closed at write and retrieval',
    tags: ['memory-layer', 'security', 'redaction', 'privacy'],
    importance: 'critical',
    access_level: 'internal',
    source: 'functions/scripts/scan-secrets.js',
    content: [
      '## Decision',
      'The memory layer must never persist or surface secrets/PII. v1 does not encrypt — it REFUSES sensitive content instead (keep real secrets in the OS store / password manager).',
      '',
      '## Control',
      'lib/redaction reuses the repo secret scanner (scanContent + high-entropy heuristic) plus PII patterns (email, phone, IPv4, credit-card via Luhn, JWT). The write guard is fail-closed: a finding rejects the write and nothing is persisted. The retrieval scrubber re-scans every snippet before it reaches the model.',
      '',
      '## Boundary (what must never cross)',
      'No secret/token/API key/PII may enter an entry, the index, an embedding, or a prompt splice. Sensitive entries (if ever allowed) surface as title+metadata only.',
      '',
      '## Verification',
      'A canary test seeds a realistic fake key and asserts it never appears in any splice; `node .claude/memory/cli.mjs verify` runs the guard checks.',
    ].join('\n'),
  },
  {
    type: 'ROADMAP',
    title: 'Memory Layer rollout phases',
    tags: ['memory-layer', 'roadmap'],
    importance: 'medium',
    access_level: 'internal',
    source: 'docs/memory/MEMORY_STATUS.md',
    content: [
      '## Now (v1 — shipped)',
      'FTS5 keyword hybrid search over docs/memory + ingested CLAUDE.md/AGENTS.md, CLI + slash commands, fail-closed redaction, audit log, 15 types, per-worktree auto-reindex with staleness banner.',
      '',
      '## Next',
      'Enable local vectors (npm install in .claude/memory → Transformers.js), then measure for two weeks whether retrieval beats reading CLAUDE.md.',
      '',
      '## Later',
      'Optional MCP server (typed memory.* tools). Travel-safe field encryption only if a real need appears.',
      '',
      '## Explicitly out of scope (and why)',
      'Knowledge graph (no ROII at this corpus size); Postgres/pgvector (no Docker; local-first).',
    ].join('\n'),
  },
  {
    type: 'PROMPT_PATTERN',
    title: 'Drive a Claude Code session with /session-start and /recall',
    tags: ['memory-layer', 'workflow', 'claude-code'],
    importance: 'medium',
    access_level: 'internal',
    source: 'docs/memory/README.md',
    content: [
      '## When to use',
      'At the start of any non-trivial task, and whenever you need prior decisions/standards for the area you are about to touch.',
      '',
      '## The prompt',
      '```',
      '/session-start            (loads vision + architecture + active roadmap + recent sessions)',
      '/recall <area or task>    (splices the most relevant entries, each with a source: anchor)',
      '… do the work …',
      '/session-end              (drafts a SESSION_SUMMARY for you to review and commit)',
      '```',
      '',
      '## Why it works here',
      'Retrieval is keyword-led, so exact tokens (file paths, task ids, Detekt rule names) rank first. Every hit cites a verifiable source file, so you can confirm before trusting it.',
    ].join('\n'),
  },
  {
    type: 'CODING_STANDARD',
    title: 'Adding a member to FeatureFlags requires updating every implementation',
    tags: ['kmp', 'feature-flags', 'modules', 'compile'],
    importance: 'high',
    access_level: 'internal',
    source: 'CLAUDE.md',
    content: [
      '## Rule',
      'When you add a member to the FeatureFlags interface, update ALL implementations in the same change: DefaultFeatureFlags, StaticFeatureFlags (in :shared) and RemoteConfigFeatureFlags (in androidApp).',
      '',
      '## Why',
      'RemoteConfigFeatureFlags lives in androidApp, outside :shared. Compiling only :shared:compileAndroidMain will NOT catch the missing override — the gap appears only when compiling :androidApp.',
      '',
      '## How to apply',
      'Add the override with the existing boolOrDefault(KEY_*, *Enabled) pattern and verify with `:androidApp:compileDebugKotlin`, not just `:shared`.',
    ].join('\n'),
  },
];

async function main() {
  const cfg = loadConfig();
  let added = 0;
  let skipped = 0;
  for (const seed of SEEDS) {
    try {
      const res = await addEntry(seed, { actor: 'seed', reindex: false });
      process.stdout.write(`  + ${res.id}\n`);
      added++;
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      if (/already exists/.test(msg)) { skipped++; continue; }
      process.stderr.write(`  ! ${seed.title}: ${msg}\n`);
    }
  }
  const stats = await reindex(cfg);
  process.stdout.write(`✓ seeded ${added} (skipped ${skipped}) · index: ${stats.entries} entries / ${stats.indexedChunks} chunks (${stats.embedder})\n`);
}

main().catch((e) => { process.stderr.write(String(e) + '\n'); process.exit(1); });
