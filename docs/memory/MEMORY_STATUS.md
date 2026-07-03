# Memory Layer — Rollout Status

Mirrors the repo's `IMPLEMENTATION_STATUS.md` convention.

## Phase 1 — Minimal core · **DONE** (2026-06-30)

- [x] Data model: 15 types, frontmatter schema, validation (`lib/schema.mjs`, `lib/frontmatter.mjs`)
- [x] Index: `node:sqlite` + FTS5 keyword search, optional `sqlite-vec` (`lib/store.mjs`)
- [x] Chunking by heading with token budget (`lib/chunker.mjs`)
- [x] Hybrid retrieval (FTS+vec RRF, keyword-led, exact-token boost) + prompt splice (`lib/retrieval.mjs`)
- [x] Fail-closed redaction guard (write + retrieval) reusing `scan-secrets` (`lib/redaction.mjs`)
- [x] PII-free append-only audit log (`lib/audit.mjs`)
- [x] Staleness manifest + auto-reindex-on-staleness (`lib/staleness.mjs`)
- [x] Write path with guard + audit (`lib/memoryService.mjs`)
- [x] CLI: recall/add/update/get/list/session-context/reindex/status/verify/types/adr (`cli.mjs`)
- [x] Slash commands: `/recall` `/session-start` `/session-end` `/adr` `/memory`
- [x] 15 type templates (`scaffold.mjs`) + 7 real seed entries (`scripts/seed.mjs`)
- [x] Tests: 21 passing (`test/unit.test.mjs`, `test/integration.test.mjs`)
- [x] Docs + `.gitignore` (index/models/audit excluded; source of truth committed)

**Verify:** `node .claude/memory/cli.mjs verify && node --no-warnings --test .claude/memory/test/unit.test.mjs .claude/memory/test/integration.test.mjs`

## Phase 2 — Vectors + relevance measurement · **NEXT (opt-in)**

- [ ] `cd .claude/memory && npm install` (sqlite-vec + Transformers.js), `reindex`, confirm `vectors: on`
- [ ] Instrument `/recall` for 2 weeks: does retrieval beat reading CLAUDE.md? Keep or cut on evidence.

## Phase 3 — MCP server · **STUBBED**

- [ ] Implement `mcp-server.mjs` tools (memory.search/add/update/summarize/session_context)
- [ ] Opt-in `.mcp.json` per the `.claude/README.md` pattern (no auto-`npx`)

## Phase 4 — Encryption & richer audit · **DEFERRED (only if a real need appears)**

- [ ] Travel-safe field encryption (wrapped DEK in git frontmatter + passphrase KEK) — see ARCHITECTURE §5
- [ ] Hash-chained audit (git history suffices for a solo dev today)

## Known limitations (honest)

- Vector search is OFF until optional deps are installed (keyword-only by default).
- Redaction is robust, **not absolute** (novel secret formats / low-entropy human passwords can slip).
- Audit is per-worktree (under the gitignored index dir), not aggregated across worktrees.
- No on-device "does this actually speed me up" measurement yet — that's the Phase-2 gate.
