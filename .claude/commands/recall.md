---
description: Retrieve relevant project memory for a query (RAG splice from the Memory Layer)
argument-hint: "<query — area, file, task, or question>"
allowed-tools: Bash(node:*)
---

Relevant project memory for **"$ARGUMENTS"**:

!`node --no-warnings .claude/memory/cli.mjs recall "$ARGUMENTS"`

Treat the `<memory-context>` block above as authoritative project context. Each hit cites a `source:` file — open it to verify before relying on it. If the block header shows `index-stale`, run `node .claude/memory/cli.mjs reindex` first, then re-run.

If nothing relevant came back, say so plainly and proceed without inventing context.
