---
description: Project Memory Layer — show status and how to use it
allowed-tools: Bash(node:*)
---

!`node --no-warnings .claude/memory/cli.mjs status`

**Memory Layer commands**
- `/recall <query>` — retrieve relevant context (RAG splice).
- `/session-start [area]` — load vision + architecture + roadmap + recent work.
- `/session-end [title]` — draft a SESSION_SUMMARY for review (never auto-writes).
- `/adr <title>` — scaffold an Architectural Decision Record.

**Direct CLI** (`node .claude/memory/cli.mjs <cmd>`): `add`, `update <id>`, `get <id>`, `list`, `reindex`, `verify`, `types`, `help`.

Source of truth = `docs/memory/entries/**` (git-versioned). The index is a disposable, gitignored cache rebuilt on demand. See `docs/memory/README.md`.
