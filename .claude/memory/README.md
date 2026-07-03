# Memory Layer — engine internals

The engine for the MindBoost Memory Layer. **User docs** live in
[`docs/memory/README.md`](../../docs/memory/README.md); **design rationale** in
[`docs/memory/ARCHITECTURE.md`](../../docs/memory/ARCHITECTURE.md). This file is for working on the
engine itself.

## Principles

- **Runs on built-in Node.** `node:sqlite`, FTS5, `node:crypto`, `node:test` — no build step, plain
  ESM `.mjs` with `@ts-check`/JSDoc. The only external deps (`sqlite-vec`, `@huggingface/transformers`)
  are **optional** and degrade to keyword-only.
- **Markdown is the source of truth.** The SQLite index is a disposable cache; never put anything in it
  that can't be rebuilt from `docs/memory/`.
- **Fail-closed writes.** Every mutation goes through `lib/memoryService.mjs` → redaction guard → audit.

## Module map

| File | Responsibility |
|---|---|
| `lib/schema.mjs` | 15 types, field model, validation, id derivation |
| `lib/frontmatter.mjs` | dependency-free frontmatter read/write |
| `lib/chunker.mjs` | Markdown → chunks (heading-aware, token-budgeted) |
| `lib/store.mjs` | `node:sqlite` + FTS5 + optional `sqlite-vec`; the index |
| `lib/embedder.mjs` | `Embedder` interface; local Transformers.js default; `none` |
| `lib/redaction.mjs` | fail-closed guard (write) + scrubber (retrieval) |
| `lib/audit.mjs` | append-only, allowlisted, PII-free JSONL |
| `lib/staleness.mjs` | content-hash manifest; is-index-behind-files? |
| `lib/indexer.mjs` | rebuild index from entries + ingested externals |
| `lib/retrieval.mjs` | hybrid RRF search + prompt splice |
| `lib/memoryService.mjs` | the write path (add/update/get/list) |
| `lib/config.mjs` / `lib/paths.mjs` / `lib/hashing.mjs` | config, worktree-aware paths, hashing |
| `cli.mjs` | the shared core every slash command calls |
| `mcp-server.mjs` | **phase-2 stub** (opt-in MCP; see header) |
| `scaffold.mjs` | (re)generate the 15 templates |
| `scripts/seed.mjs` | real MindBoost example entries |

## Run

```bash
# tests (built-in runner, no deps)
node --no-warnings --test test/unit.test.mjs test/integration.test.mjs

# self-check (redaction canary + index freshness)
node --no-warnings cli.mjs verify

# enable vectors (optional)
npm install && node --no-warnings cli.mjs reindex
```

## Config (`config.json`)

`embedder` (`local`|`none`|`api:<provider>`), `embedModel`, `dimensions`, `retrieval` (k, budgets, RRF
constants, keyword bias), `chunking`, `ingest.external` (files ingested read-only), `redaction`,
`audit`. Env override: `MINDOS_MEMORY_EMBEDDER=none` forces keyword-only (used by tests).

## Extending

- **New memory type:** add to `MEMORY_TYPES` + `TYPE_DESCRIPTIONS` in `lib/schema.mjs`, add a body in
  `scaffold.mjs`, re-run `node scaffold.mjs`.
- **New embedding provider:** implement the `Embedder` shape in `lib/embedder.mjs` behind an
  `api:<name>` branch; the cache key already includes provider+model+dims so spaces never mix.
- **Postgres backend (future):** implement the `store.mjs` surface against pgvector; the
  `entries/chunks/meta` schema is backend-agnostic. Keep Markdown as source of truth.

## Invariants (do not regress)

1. The index is rebuildable from `docs/memory/` alone. Never store unreconstructable state in it.
2. No write bypasses the redaction guard. The guard is fail-closed (errors ⇒ reject).
3. Search never answers from a known-stale index (auto-reindex or loud banner).
4. The query embedding space must match the index (`meta.dims`); mismatched spaces are rejected.
