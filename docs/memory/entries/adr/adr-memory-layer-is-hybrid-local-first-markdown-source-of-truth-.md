---
id: adr-memory-layer-is-hybrid-local-first-markdown-source-of-truth-
type: ADR
title: Memory Layer is hybrid local-first (Markdown source of truth + sqlite-vec index)
tags: [memory-layer, architecture, rag, local-first]
source: docs/memory/ARCHITECTURE.md
importance: high
created_at: 2026-06-30
updated_at: 2026-06-30
version: 1
encrypted: false
access_level: internal
related: []
supersedes: null
superseded_by: null
metadata: {}
content_hash: 6120af8e6b2186ab900f35625508bc6a467ea46eff2343b466f8b40ab7234681
---

**Status:** accepted  ·  **Date:** 2026-06-30

## Context
Claude Code loses context across sessions and across Vibe Kanban git worktrees. The machine has no Docker and no local Postgres, and the repo ethos is local-first + privacy-first.

## Decision
Markdown-with-frontmatter files in git (docs/memory/entries) are the source of truth. An embedded sqlite-vec + FTS5 index (a disposable, gitignored, per-worktree cache) provides hybrid keyword+vector retrieval. Embeddings are provider-decoupled with a local/offline default. Integration is via a CLI + slash commands; an MCP server is an optional later phase.

## Alternatives considered
- PostgreSQL + pgvector as source of truth — rejected: needs Docker/daemon or cloud egress; breaks local-first and worktree-travel.
- Harness file-memory only — rejected: does not travel across worktrees (one session dir per worktree path).

## Consequences
Versioning/audit/diff come free from git. The index is never load-bearing (rebuildable via `memory reindex`). Postgres remains a documented upgrade path with an explicit trigger threshold (corpus >100k chunks, multi-user, or server-side RAG).

## Security & privacy
A fail-closed redaction guard forbids secrets/PII from being persisted or spliced into prompts. v1 stores no sensitive content (no encryption subsystem yet).
