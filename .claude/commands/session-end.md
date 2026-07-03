---
description: Draft a SESSION_SUMMARY of this session for review (does NOT auto-write — you approve before it persists)
argument-hint: "[optional: short title for the summary]"
allowed-tools: Bash(node:*)
---

Draft a `SESSION_SUMMARY` memory entry for **this** session. Structure (from the template):

!`cat docs/memory/_templates/SESSION_SUMMARY.md`

Write the draft grounded ONLY in what actually happened this session — what changed, decisions made, files touched, how it was verified, and concrete next steps. Be specific (file paths, ids). **Do not include any secret, token, API key, or PII** — the redaction guard will reject the write if you do.

Then **STOP and show me the draft**. Do not persist anything yet. After I approve, save it with:

```
node .claude/memory/cli.mjs add --type SESSION_SUMMARY --title "$ARGUMENTS" --file <path-to-reviewed-draft.md>
```

(`/session-end` never auto-writes model-summarized content — that's the deliberate guard against leaking on-screen secrets.)
