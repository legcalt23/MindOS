---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Example Claude Code Handoff Usage

After Antigravity generates a plan, copy only the specific phase prompt from:

```text
.antigravity-only/plans/YYYY-MM-DD-short-name/10_claude_prompts.md
```

Do not tell Claude Code to read all `.antigravity-only/` instructions.

Claude Code should receive a bounded prompt like:

```text
Implement Phase 01 only.
Use the exact scope and forbidden files listed below.
Do not touch non-MindBoost files.
Do not alter metric integrity beyond this phase.
Run the listed tests.
Stop if scope is ambiguous.
```
