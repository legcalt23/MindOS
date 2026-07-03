# MindBoost Memory Layer

A private, versioned, auditable **persistent "project brain"** for Claude Code. It lets Claude
recover the *relevant* context for any task — architecture, decisions, roadmap, bugs, coding
standards, research, prompts — without re-reading the whole repo and without losing context between
sessions or across git worktrees.

**Design in one line:** Markdown files in git are the source of truth; an embedded, disposable
`sqlite-vec` + FTS5 index gives hybrid keyword+vector retrieval; a fail-closed redaction guard makes
sure no secret or PII is ever stored or sent to the model.

> Full rationale: [`ARCHITECTURE.md`](ARCHITECTURE.md) · rollout status: [`MEMORY_STATUS.md`](MEMORY_STATUS.md)
> · engine internals: [`../../.claude/memory/README.md`](../../.claude/memory/README.md)

---

## Quick start

```bash
# Status (entries, index health, staleness, embedder)
node .claude/memory/cli.mjs status

# Retrieve relevant context for a task (this is what Claude splices into a prompt)
node .claude/memory/cli.mjs recall "firestore rules limit query"

# Add an entry
node .claude/memory/cli.mjs add --type CODING_STANDARD --title "…" --tags a,b --content "…"

# Rebuild the index from the Markdown source of truth (always safe)
node .claude/memory/cli.mjs reindex
```

In Claude Code, prefer the slash commands:

| Command | What it does |
|---|---|
| `/session-start [area]` | Loads vision + architecture + active roadmap + recent sessions (+ area-relevant entries). Run at the top of a session. |
| `/recall <query>` | Splices the most relevant entries for a task. Each hit cites a `source:` file you can verify. |
| `/session-end [title]` | **Drafts** a `SESSION_SUMMARY` for you to review and commit. Never auto-writes (guards against leaking on-screen secrets). |
| `/adr <title>` | Scaffolds an Architectural Decision Record. |
| `/memory` | Status + command help. |

---

## What lives where

```
docs/memory/                      ← SOURCE OF TRUTH (committed to git)
  README.md  ARCHITECTURE.md  MEMORY_STATUS.md
  _templates/<TYPE>.md            ← one fill-in template per memory type
  entries/<type>/<id>.md          ← the actual memory entries (one file each)
.claude/memory/                   ← the engine (committed; runs on built-in Node)
  cli.mjs  lib/*  test/*  scaffold.mjs  scripts/seed.mjs
.claude/.memory/                  ← runtime cache (GITIGNORED, per-worktree, disposable)
  index.db  manifest.json  audit/  models/
```

The index and downloaded models are **never committed** — they rebuild from the Markdown files with
`reindex`. That is what makes memory survive worktree switches: the *files* travel in git; the cache
is local.

---

## The 15 memory types

`PRODUCT_VISION` · `ARCHITECTURE` · `ADR` · `FEATURE_SPEC` · `ROADMAP` · `BUG_REPORT` ·
`SESSION_SUMMARY` · `CODING_STANDARD` · `SECURITY_NOTE` · `PROMPT_PATTERN` · `RESEARCH_NOTE` ·
`USER_PERSONA` · `API_CONTRACT` · `DATA_MODEL` · `DEPLOYMENT_NOTE`

`node .claude/memory/cli.mjs types` lists them with descriptions. Each entry is a Markdown file with
this frontmatter:

```yaml
---
id: adr-virtual-currency-ledger   # stable, unique, kebab — drives the file path
type: ADR
title: Virtual-currency ledger is server-authoritative
tags: [wallet, security]
source: docs/architecture/virtual-currency-adr.md   # backref to a source doc (never a copy)
importance: high                  # low | medium | high | critical
created_at: 2026-06-30
updated_at: 2026-06-30
version: 1                        # bumped on edit; git holds the full chain
encrypted: false                  # reserved; always false in v1
access_level: internal            # public | internal | sensitive
metadata: {}
---
(markdown body)
```

---

## Daily flow

1. **Start** — `/session-start` (or `… session-context --out .claude/.memory/session-context.md`).
2. **During** — `/recall <area>` before non-trivial work; `/adr <title>` to record decisions.
3. **End** — `/session-end` drafts a summary; review it, then persist with `add`.

---

## Privacy & safety

- **Never stores secrets/PII.** Every write passes a fail-closed redaction guard (reuses the repo's
  `scan-secrets` heuristics + email/phone/card/JWT patterns). A finding rejects the write entirely.
- **Selective retrieval.** Only the top-k relevant entries are spliced into a prompt — never the whole
  store — and each snippet is scrubbed again before it reaches the model.
- **No vendor lock-in.** Embeddings sit behind a small interface; the default is on-device and offline.
- **v1 refuses `access_level: sensitive` content.** Keep real secrets in your OS store / password
  manager; store only a non-secret pointer in memory.

It is **robust, not absolute** — a determined attacker running as your user, or a novel secret format,
can defeat any local tool. Treat it as strong hygiene, not a vault.

---

## Enabling vector search (optional)

Keyword search works with zero install. To add semantic vectors (on-device, $0, no API key):

```bash
cd .claude/memory && npm install   # pulls sqlite-vec + @huggingface/transformers (optional deps)
node cli.mjs reindex                # first run downloads a small embedding model once
node cli.mjs status                 # "vectors: on"
```

If install fails on your machine, nothing breaks — the engine stays in keyword-only mode.
