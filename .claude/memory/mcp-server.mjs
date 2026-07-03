#!/usr/bin/env node
// @ts-check
/**
 * mcp-server.mjs — PHASE-2 STUB (opt-in; not wired by default).
 *
 * A local stdio MCP server exposing the Memory Layer as typed tools so Claude
 * Code can retrieve context as a tool call instead of a slash command. It wires
 * to the SAME engine functions the CLI uses, so behavior (redaction, staleness,
 * hybrid search) is identical.
 *
 * It is intentionally NOT auto-launched. To enable it, install the SDK and add an
 * opt-in entry to a project `.mcp.json` (see `.claude/README.md`'s opt-in pattern):
 *
 *   cd .claude/memory && npm install @modelcontextprotocol/sdk
 *
 *   // .mcp.json (repo root)
 *   {
 *     "mcpServers": {
 *       "mindos-memory": {
 *         "command": "node",
 *         "args": ["--no-warnings", ".claude/memory/mcp-server.mjs"]
 *       }
 *     }
 *   }
 *
 * Tools: memory_search, memory_session_context, memory_status.
 * (Write tools are intentionally left to the CLI so the fail-closed guard and a
 * human review stay in the loop — see the security model in docs/memory/ARCHITECTURE.md.)
 *
 * NOTE: This skeleton targets the high-level `McpServer` API. Confirm the method
 * names against your installed @modelcontextprotocol/sdk version before relying on
 * it — this stub is unverified until the SDK is installed (Phase 3 in MEMORY_STATUS).
 */

import { loadConfig } from './lib/config.mjs';
import { MemoryStore } from './lib/store.mjs';
import { createEmbedder } from './lib/embedder.mjs';
import { reindex } from './lib/indexer.mjs';
import { search, buildSplice } from './lib/retrieval.mjs';
import { checkStale } from './lib/staleness.mjs';
import { getEntry, listEntries } from './lib/memoryService.mjs';

async function ensureFresh(cfg) {
  const stale = checkStale(cfg);
  if (stale.stale) await reindex(cfg);
  return stale;
}

/** Run a hybrid search and return a prompt-ready splice (the core both tools share). */
async function recall(query, opts = {}) {
  const cfg = loadConfig();
  const stale = await ensureFresh(cfg);
  const store = new MemoryStore(cfg).open();
  const embedder = await createEmbedder(cfg);
  const hits = await search(query, opts, { store, embedder, cfg });
  store.close();
  return buildSplice(hits, cfg, { stale, query });
}

async function sessionContext(task) {
  const cfg = loadConfig();
  await ensureFresh(cfg);
  const pinnedIds = [
    ...listEntries({ type: 'PRODUCT_VISION' }).slice(0, 1),
    ...listEntries({ type: 'ARCHITECTURE' }).slice(0, 2),
    ...listEntries({ type: 'ROADMAP' }).slice(0, 1),
    ...listEntries({ type: 'SESSION_SUMMARY' }).slice(0, 2),
  ].map((r) => r.id);
  const pinned = pinnedIds.map(getEntry).filter(Boolean).map((e) => ({
    entry_id: e.id, type: e.type, title: e.title, importance: e.importance,
    access_level: e.access_level, source: e.source, file_path: e.file_path,
    anchor: '', text: String(e.content).slice(0, 800), score: 99,
  }));
  const store = new MemoryStore(cfg).open();
  const embedder = await createEmbedder(cfg);
  const taskHits = task ? await search(task, { k: 6 }, { store, embedder, cfg }) : [];
  store.close();
  const seen = new Set();
  const all = [...pinned, ...taskHits].filter((h) => (seen.has(h.entry_id) ? false : seen.add(h.entry_id)));
  return buildSplice(all, cfg, { query: task });
}

async function main() {
  let McpServer, StdioServerTransport, z;
  try {
    ({ McpServer } = await import('@modelcontextprotocol/sdk/server/mcp.js'));
    ({ StdioServerTransport } = await import('@modelcontextprotocol/sdk/server/stdio.js'));
    ({ z } = await import('zod'));
  } catch {
    process.stderr.write(
      'mindos-memory MCP server is not enabled: install the SDK first:\n' +
      '  cd .claude/memory && npm install @modelcontextprotocol/sdk zod\n' +
      'Then add the opt-in .mcp.json entry (see this file\'s header). Until then, use the CLI / slash commands.\n');
    process.exit(1);
    return;
  }

  const server = new McpServer({ name: 'mindos-memory', version: '0.1.0' });

  server.tool('memory_search',
    { query: z.string(), k: z.number().optional(), type: z.string().optional() },
    async ({ query, k, type }) => ({
      content: [{ type: 'text', text: await recall(query, { k, types: type ? [type.toUpperCase()] : undefined }) }],
    }));

  server.tool('memory_session_context',
    { task: z.string().optional() },
    async ({ task }) => ({ content: [{ type: 'text', text: await sessionContext(task || '') }] }));

  server.tool('memory_status', {}, async () => {
    const cfg = loadConfig();
    const store = new MemoryStore(cfg).open();
    const counts = store.counts();
    store.close();
    const stale = checkStale(cfg);
    return { content: [{ type: 'text', text: `entries=${counts.entries} chunks=${counts.chunks} vectors=${counts.hasVec ? 'on' : 'off'} stale=${stale.stale}` }] };
  });

  await server.connect(new StdioServerTransport());
}

main().catch((e) => { process.stderr.write(String(e) + '\n'); process.exit(1); });
