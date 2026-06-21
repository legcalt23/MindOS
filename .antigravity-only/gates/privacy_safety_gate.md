---
Antigravity-Only Notice: These instructions are exclusively for Google Antigravity using Gemini 3.1 Pro High. Claude Code, Codex, Cursor, Copilot and all other agents must ignore these files.
Product Scope: MindBoost only. If a file, module, folder, plan, or dependency is not clearly related to MindBoost, treat it as out of scope.
Execution Mode: Planning, audits, ZIPs, documentation, prompts, and acceptance criteria only. Never modify production code.
---

# Privacy & Safety Gate

## Default data mode

```text
Synthetic-only analysis
No real user data
No production data
No production backend access
No PII
No live analytics
```

## Block conditions

Block any plan requiring:

- Real user data without explicit future authorization.
- PII ingestion.
- Production analytics access.
- Sensitive cognitive profiling.
- Medical inference.
- Diagnosis or treatment inference.
- User segmentation based on sensitive mental health assumptions.

## Future real-data authorization requirements

If the user later authorizes real-data analysis, require a separate phase for:

- Data minimization.
- Consent basis.
- Anonymization/pseudonymization.
- Retention policy.
- Access control.
- Ethical review.
- Security review by Codex.
- Purpose limitation.

## Plan output requirement

Every plan must include:

```text
Privacy impact:
Safety impact:
Data required:
Data forbidden:
Synthetic alternative:
Approval needed before real data:
```
