---
id: architecture-project-memory-layer-components-and-data-flow
type: ARCHITECTURE
title: Project Memory Layer — components and data flow
tags: [memory-layer, architecture]
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
content_hash: d031417081842264d03cde3c5aacb46521f77ccf70f90c55a154725ea9ecc14a
---

## Overview
A persistent "project brain" for Claude Code: authored Markdown entries + ingested existing brain files (CLAUDE.md, AGENTS.md), indexed for hybrid retrieval and spliced into prompts on demand.

## Modules & boundaries
- lib/schema, lib/frontmatter — the 15-type data model + file format.
- lib/store — node:sqlite (FTS5) + optional sqlite-vec; a rebuildable cache.
- lib/embedder — provider-decoupled embeddings (local default).
- lib/redaction — fail-closed write guard + retrieval scrubber.
- lib/indexer / lib/retrieval — build index; hybrid RRF search + splice.
- lib/memoryService — the only write path (guard + audit).
- cli.mjs — the shared core all slash commands call.

## Key invariants
- Markdown is the only source of truth; the .db is disposable.
- Every write passes the redaction guard (fail-closed).
- Search never answers from an index known to be behind the files (auto-reindex on staleness; loud banner otherwise).
