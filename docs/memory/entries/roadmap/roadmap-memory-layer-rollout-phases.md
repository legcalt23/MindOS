---
id: roadmap-memory-layer-rollout-phases
type: ROADMAP
title: Memory Layer rollout phases
tags: [memory-layer, roadmap]
source: docs/memory/MEMORY_STATUS.md
importance: high
created_at: 2026-06-30
updated_at: 2026-06-30
version: 2
encrypted: false
access_level: internal
related: []
supersedes: null
superseded_by: null
metadata: {}
content_hash: af74d4950b1556bca9fe043cf1deeb9403251af89ee1631b1b9cb4aa3c79cb5e
---

## Now (v1 — shipped)
FTS5 keyword hybrid search over docs/memory + ingested CLAUDE.md/AGENTS.md, CLI + slash commands, fail-closed redaction, audit log, 15 types, per-worktree auto-reindex with staleness banner.

## Next
Enable local vectors (npm install in .claude/memory → Transformers.js), then measure for two weeks whether retrieval beats reading CLAUDE.md.

## Later
Optional MCP server (typed memory.* tools). Travel-safe field encryption only if a real need appears.

## Explicitly out of scope (and why)
Knowledge graph (no ROII at this corpus size); Postgres/pgvector (no Docker; local-first).
