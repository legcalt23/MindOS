---
id: prompt-pattern-drive-a-claude-code-session-with-session-start-and-recall
type: PROMPT_PATTERN
title: Drive a Claude Code session with /session-start and /recall
tags: [memory-layer, workflow, claude-code]
source: docs/memory/README.md
importance: medium
created_at: 2026-06-30
updated_at: 2026-06-30
version: 1
encrypted: false
access_level: internal
related: []
supersedes: null
superseded_by: null
metadata: {}
content_hash: 3573a8bac5334351cb1b7f011c79ed4df8186a7abcf97de06753b15127a5908b
---

## When to use
At the start of any non-trivial task, and whenever you need prior decisions/standards for the area you are about to touch.

## The prompt
```
/session-start            (loads vision + architecture + active roadmap + recent sessions)
/recall <area or task>    (splices the most relevant entries, each with a source: anchor)
… do the work …
/session-end              (drafts a SESSION_SUMMARY for you to review and commit)
```

## Why it works here
Retrieval is keyword-led, so exact tokens (file paths, task ids, Detekt rule names) rank first. Every hit cites a verifiable source file, so you can confirm before trusting it.
