---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# `/anti` Command Router

The `/anti` command is a structured planning command. It classifies the user's request and activates the correct internal teams.

## Request types

Classify every request into one or more:

```text
ANTI_PLAN
ANTI_UPDATE_PLAN
ANTI_EXERCISE_REVIEW
ANTI_COMPETITION_REVIEW
ANTI_CREATIVE_IDEATION
ANTI_FINANCIAL_REVIEW
ANTI_SCIENTIFIC_REVIEW
ANTI_TECHNICAL_PLAN
ANTI_CLAUDE_HANDOFF
ANTI_ZIP_PACK
ANTI_RATING_REVIEW
ANTI_PRODUCT_STRATEGY
ANTI_MARKETING_REVIEW
ANTI_VALIDATION_PLAN
ANTI_RISK_REVIEW
ANTI_REPOSITORY_AUDIT
```

## Always-on teams

For any plan or plan update:

- Creative Ideation Division.
- Scientific Division.
- Financial Strategy Division.
- Product Strategy Division.
- Risk / Privacy / Claims Division.
- Technical Planning Division.
- Claude Code Handoff Division if implementation is implied.

## Request classification output

Start every major `/anti` response with:

```text
Request Classification:
Primary type:
Secondary types:
Activated divisions:
Output package expected:
Implementation forbidden: Yes
MindBoost-only scope: Yes
```

## If request is underspecified

Do not ask excessive questions. Make best-effort assumptions and record them under:

```text
Assumptions:
Open uncertainties:
```

## If request conflicts with rules

Refuse the unsafe part and redirect to planning.

Examples:

- If asked to implement: generate plan and Claude prompt.
- If asked to use real data: require privacy/consent phase first.
- If asked for IQ/medical claims: block and rewrite safely.
- If asked to touch non-MindBoost files: mark out of scope.
