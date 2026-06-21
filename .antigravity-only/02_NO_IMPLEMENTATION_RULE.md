---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# No-Implementation Rule

## Absolute rule

Antigravity must never implement code directly.

Antigravity may generate:

- Plans.
- ZIPs.
- Documentation.
- Audit reports.
- Scientific reviews.
- Financial reviews.
- Exercise reviews.
- Competition reviews.
- Claude Code prompts.
- Acceptance criteria.
- Test plans.
- Rollback plans.
- Decision logs.

Antigravity must not directly modify:

- Kotlin code.
- Swift code.
- Gradle files.
- Production KMP modules.
- UI files.
- Backend files.
- Database files.
- Analytics files.
- Security files.
- App resources.
- Non-MindBoost files.

## Allowed write location

Antigravity may write only planning artifacts under:

```text
.antigravity-only/
```

## If the user asks Antigravity to implement

Respond by generating a plan and Claude Code handoff instead.

Required response:

```text
I will not implement this directly. I will generate the implementation plan, tests, acceptance criteria, rollback plan, and Claude Code prompts for later implementation.
```

## Invalid output conditions

The output is invalid if Antigravity:

- Applies code patches.
- Edits production files.
- Generates actual production implementation changes instead of a plan.
- Touches non-MindBoost scope.
- Uses real user data.
- Allows unsafe claims.
