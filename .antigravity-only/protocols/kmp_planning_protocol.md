---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Kotlin Multiplatform Planning Protocol

## Purpose

Generate implementation plans for a Kotlin Multiplatform product without editing code.

## KMP planning assumptions

MindBoost is a Kotlin Multiplatform product targeting Android and iOS.

Antigravity must not assume exact module names unless inspected or provided. It must identify likely layers and mark uncertainty.

## Preferred planning layers

```text
domain
use cases
data
persistence
metrics
rating
exercise engine
adaptive difficulty
presentation
shared UI
android-specific integration
ios-specific integration
tests
analytics abstraction
billing abstraction
```

## Plan format by layer

For each phase:

```text
Layer:
Likely files/modules:
Forbidden files/modules:
Responsibility:
Implementation steps:
Cross-platform concerns:
Android-specific concerns:
iOS-specific concerns:
Tests:
Risks:
Rollback:
```

## Cross-platform parity checks

Every plan touching exercises, input timing, scoring, or rating must include:

- iOS/Android behavior parity.
- Touch latency risk.
- Frame timing risk.
- Refresh rate risk.
- Screen size risk.
- Offline consistency.
- Deterministic scoring where required.
- Same difficulty logic across platforms.
- Same rating logic across platforms.

## Claude Code prompt standard

Do not generate final production code. Generate implementation prompts with exact boundaries.
