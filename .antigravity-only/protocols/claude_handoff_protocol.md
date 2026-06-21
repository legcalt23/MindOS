---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Claude Code Handoff Protocol

## Purpose

Convert Antigravity plans into safe, bounded implementation prompts for Claude Code.

## Claude Code relationship

Claude Code is the later implementer. Antigravity is the planner.

Claude Code must not treat `.antigravity-only/` as general instructions. Claude Code may use only the handoff prompt intentionally passed by the user.

## Handoff prompt template

```text
You are Claude Code implementing a bounded MindBoost task.

Task:

Context:

Scope:

MindBoost-only boundary:

Files likely affected:

Forbidden files:

Do not touch:

Implementation phases:

Detailed steps:

Tests required:

Acceptance criteria:

Rollback plan:

Definition of Done:

Stop conditions:

Do not implement outside this scope. If the task requires touching unrelated MindVerse/MindOS systems, stop and ask for explicit human approval.
```

## Required prompt quality

Every prompt must be:

- Specific.
- Phase-based.
- File-bounded.
- Testable.
- Reversible.
- Safe with respect to metrics and claims.
- Clear about forbidden files.

## Stop conditions for Claude Code

Include these in every implementation prompt:

```text
Stop if a required file appears unrelated to MindBoost.
Stop if implementation would change rating fairness beyond the plan.
Stop if implementation requires real user data.
Stop if claims, UI copy, or analytics imply IQ, diagnosis, treatment, or medical outcomes.
Stop if security-sensitive work is required before Codex review.
```
