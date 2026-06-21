# MindBoost Antigravity Team System — Install Guide

This ZIP installs a full Antigravity-only planning system for MindBoost.

## Install

1. Extract this ZIP at the root of:

```text
C:\Users\curro\AndroidStudioProjects\MindOS
```

2. Confirm the folder exists:

```text
C:\Users\curro\AndroidStudioProjects\MindOS\.antigravity-only
```

3. In Google Antigravity, create or map the custom command `/anti` to:

```text
.antigravity-only/commands/anti.md
```

4. Use `/anti` only with structured requests.

## Non-negotiable rules

- Antigravity is a planning brain, not an implementer.
- Antigravity must never directly modify Kotlin, Swift, Gradle, backend, analytics, database, UI, or production app files.
- Antigravity may generate documents, ZIPs, plans, prompts, audits, and handoff packages inside `.antigravity-only/`.
- Claude Code implements later from explicitly generated handoff prompts.
- Codex owns cybersecurity/security reviews.
- MindBoost only. Non-MindBoost scope is forbidden by default.

## Recommended first command

```text
/anti plan
Goal: Audit the current MindBoost KMP project and generate a phased implementation plan to improve cognitive training quality, measurement rigor, and MVP readiness.
Scope: MindBoost only.
Output: ZIP pack with separate documents and Claude Code prompts.
Do not implement anything.
```
