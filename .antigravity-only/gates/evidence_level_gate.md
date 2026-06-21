---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Evidence Level Gate

## Mission

Ensure no scientific, exercise, rating, or product recommendation enters implementation planning without evidence classification.

## Required fields

Every scientific recommendation must include:

```text
Evidence Level:
Scientific Rationale:
Expected Cognitive Impact:
Limits of Evidence:
Risk of Overclaiming:
Measurement Plan:
Validation Test:
Required Metrics:
Decision:
```

## Decision rules

- High evidence can support implementation if all other gates pass.
- Medium evidence can support controlled implementation with validation.
- Low evidence requires cautious pilot framing.
- Experimental hypothesis requires research/prototype status.
- Creative unvalidated idea cannot enter MVP without review.
- Rejected must not be implemented.
