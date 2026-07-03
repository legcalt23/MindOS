---
description: Load session-start project context from the Memory Layer (vision, architecture, roadmap, recent work + task-relevant entries)
argument-hint: "[optional: the area/task you're about to work on]"
allowed-tools: Bash(node:*), Bash(git rev-parse:*)
---

Loading project memory for this session (branch-aware)…

!`node --no-warnings .claude/memory/cli.mjs session-context --branch "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)" --task "$ARGUMENTS"`

The `<memory-context>` above is the current project state: vision, architecture, active roadmap, recent session summaries, and entries relevant to "$ARGUMENTS". Use it as authoritative grounding for this session. Before any non-trivial change, also run `/recall <specific area>` for that area's prior decisions and standards. If the block shows `index-stale`, run `node .claude/memory/cli.mjs reindex`.
