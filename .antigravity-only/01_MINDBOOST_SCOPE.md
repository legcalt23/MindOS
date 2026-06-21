---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# MindBoost Repository Scope Rules

## Known repository root

```text
C:\Users\curro\AndroidStudioProjects\MindOS
```

The user supplied a Windows search URI, not a technical project path. Therefore the only safe repository assumption is:

```text
Repository root: C:\Users\curro\AndroidStudioProjects\MindOS
Product scope: MindBoost only
Context: MindBoost is currently inside MindVerse / MindOS but will be extracted or released separately first.
```

## Boundary rule

Antigravity may analyze only files, folders, modules, documentation, plans, exercises, metrics, and architecture that are clearly related to MindBoost.

If a file is not clearly related to MindBoost, treat it as forbidden.

## Allowed planning scope

- MindBoost product strategy.
- MindBoost exercises.
- MindBoost cognitive metrics.
- MindBoost rating systems.
- MindBoost KMP architecture planning.
- MindBoost UI/UX planning.
- MindBoost monetization planning.
- MindBoost scientific validation planning.
- MindBoost Claude Code handoff prompts.

## Forbidden scope

- Unrelated MindVerse systems.
- Other applications inside MindOS.
- Root-wide refactors.
- Shared infrastructure changes unless strictly required for MindBoost and explicitly marked as dependency-only.
- Secrets.
- Production user data.
- Production analytics.
- Production backend access.
- Implementation patches.

## Scope verification protocol

Before producing any plan, include:

```text
Scope Confirmation:
- Product: MindBoost
- Repository root considered: C:\Users\curro\AndroidStudioProjects\MindOS
- Files/folders considered:
- Files/folders forbidden:
- Uncertainties:
- Out-of-scope assumptions:
```

If uncertain, do not plan edits to that area.
