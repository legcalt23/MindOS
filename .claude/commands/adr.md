---
description: Scaffold an Architectural Decision Record into the Memory Layer
argument-hint: "<short decision title>"
allowed-tools: Bash(node:*)
---

ADR scaffold for **"$ARGUMENTS"**:

!`node --no-warnings .claude/memory/cli.mjs adr --title "$ARGUMENTS"`

Fill in **Context / Decision / Alternatives considered / Consequences / Security & privacy** above, grounded in our discussion and the relevant code. Keep it honest about trade-offs.

When complete, persist it (the redaction guard runs on write):

```
node .claude/memory/cli.mjs add --type ADR --title "$ARGUMENTS" --importance high --file <filled-adr.md>
```

Cross-link any source docs with a `source:` field, and run `/recall $ARGUMENTS` afterward to confirm it's retrievable.
