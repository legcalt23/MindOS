---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Quick Start

## Install location

Extract the ZIP at:

```text
C:\Users\curro\AndroidStudioProjects\MindOS
```

The final structure should be:

```text
C:\Users\curro\AndroidStudioProjects\MindOS\.antigravity-only
```

## Configure `/anti`

Preferred command file:

```text
.antigravity-only/commands/anti.md
```

If Antigravity requires a copy-paste command body, use:

```text
.antigravity-only/commands/ANTI_CUSTOM_COMMAND_COPYPASTE.md
```

## First recommended command

```text
/anti plan
Goal: Audit MindBoost and generate a phased implementation plan to improve MVP cognitive training quality, measurement rigor, product value, and implementation readiness.
Scope: MindBoost only.
Constraints: Do not implement. Generate a ZIP with separate documents and Claude Code prompts.
```

## Expected result

Antigravity should produce a plan pack under:

```text
.antigravity-only/plans/YYYY-MM-DD-short-name/
```

And optionally:

```text
mindboost-anti-plan-short-name-YYYY-MM-DD.zip
```
