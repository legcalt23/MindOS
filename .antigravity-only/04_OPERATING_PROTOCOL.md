---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Operating Protocol

## Standard execution sequence

For every significant `/anti` request, follow this sequence:

1. Parse request.
2. Confirm MindBoost scope.
3. Classify request type.
4. Activate required agents.
5. Run creative ideation.
6. Run scientific review.
7. Run exercise/competition review if relevant.
8. Run product review.
9. Run financial review.
10. Run technical planning review.
11. Run privacy, safety, claims, metric integrity, and repository boundary gates.
12. Produce phased plan.
13. Produce Claude Code handoff prompts.
14. Produce test plan.
15. Produce rollback plan.
16. Produce risk register.
17. Produce final scoring and recommendation.
18. Package into structured documents or ZIP when requested.

## Default output folder

```text
.antigravity-only/plans/YYYY-MM-DD-short-plan-name/
```

Recommended files:

```text
00_summary.md
01_scope.md
02_ideas.md
03_science.md
04_exercises.md
05_finance.md
06_product.md
07_technical_plan.md
08_tests.md
09_risks.md
10_claude_prompts.md
11_signoff.md
```

## Length discipline

Use multiple well-named documents instead of one overlong file.

Each document must be complete but not bloated.

## Required final response to user

When a ZIP or plan package is generated, respond with:

```text
Generated pack:
[path or link]

Summary:
- What it contains
- How to use it
- Any critical warning
```

Do not paste the full ZIP contents into chat unless asked.

## Fail-safe Execution & Agent Lifecycle

To ensure runtime reliability, the orchestrator must track and enforce these states for each registry agent:
- `PENDING`: Registration successful, awaiting invocation.
- `INVOKED`: Active LLM execution thread started.
- `COMPLETED`: Run finished successfully with a signed verdict.
- `FAILED`: Invocation failed due to timeout or quota limits.
- `RETRIED`: Invocation re-attempted.
- `FALLBACK_USED`: Critical failure recovered using a designated fallback agent.
- `BLOCKED`: Veto gate triggered.
- `NOT_INVOKED`: Ignored due to lazy squad selection rules.

### Strict Routing Rules
1. **Critical Agent Failure:** If any critical agent enters the `FAILED` state and no fallback agent is successfully executed, the entire run must fail closed immediately, returning **INCONCLUSIVE_BLOCKED**.
2. **Missing Transcript:** Any critical agent lacking a corresponding transcript trace triggers **INCONCLUSIVE_BLOCKED**.
3. **Missing Conversation ID:** Any critical agent lacking a verified Conversation ID triggers a maximum status of **CONDITIONAL_GO_INTERNAL** (unless explicitly marked as `UNVERIFIED` for test cases).
4. **Non-Invoked Agent Verdict:** If an agent marked `NOT_INVOKED` is found to have a verdict or conversation ID, the run is flagged as compromised and sets status to **NO_GO_HIGH_STAKES**.
5. **Fake Transcripts:** If verification scans detect simulated/mock transcripts, status is forced to **INCONCLUSIVE_BLOCKED**.
6. **Veto Autonomy:** The orchestrator cannot override QA, Red Team, or Security vetoes unless a valid, founder-signed block is logged under the Human Override Policy (`WAIVED_BY_HUMAN`).

