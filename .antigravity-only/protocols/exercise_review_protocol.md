---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Exercise Review Protocol

## Exercise status model

Allowed statuses:

```text
Research concept
Prototype
Training only
Offline Simulation
Shadow Candidate
Live Shadow Ranked Beta
Private Ranked Beta
Public Ranked
Tournament Eligible
Rejected
```

Default for existing current motors:

```text
Focus Motor 01 — Interference Control: Offline Simulation + Shadow Candidate
Working Memory Motor 01 — Sequence Updating: Training + Shadow Candidate
Mental Speed Motor 01 — Choice Reaction: Training + Offline Simulation Only
```

Public ranked and tournaments are forbidden until validation gates pass.

## Exercise review card

```text
Exercise name:
Cognitive block:
Primary construct:
Secondary construct:
Current status:
Allowed contexts:
Forbidden contexts:
Task mechanics:
Difficulty model:
Scoring model:
Rating impact:
Main metrics:
Confounds:
Bias risks:
Device risks:
Fatigue risks:
Learning effects:
Safety risks:
Claims risks:
Evidence level:
Validation plan:
Tests required:
Acceptance criteria:
Decision: GO / REVISE / NO-GO / RESEARCH ONLY
```

## Critic rules

Reject or revise exercises that:

- Mix too many constructs without separating metrics.
- Reward motor speed when claiming memory or logic.
- Are highly device-dependent.
- Are easy to farm.
- Are vulnerable to memorized patterns.
- Produce unstable scores.
- Cannot be validated synthetically or offline.
- Encourage unsafe claims.
