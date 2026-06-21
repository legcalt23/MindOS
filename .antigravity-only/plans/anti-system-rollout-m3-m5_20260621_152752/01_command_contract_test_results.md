# Command Contract Test Results

This document records the execution testing of the `/anti` command contract.

---

## Command Matrix Verification

| Command | Input Fixture | Expected Result | Actual Result | Pass/Fail | Evidence / Notes |
|---|---|---|---|---|---|
| `/anti fast` | `valid_small_plan.md` | Route to Architect & QA | `SEMANTIC_ONLY` | `PASS` (Semantic) | Guided by `anti.md` and `review_modes.json`. |
| `/anti deep` | `missing_evidence_plan.md` | Route to all 7 agents | `SEMANTIC_ONLY` | `PASS` (Semantic) | Squad mapped dynamically in registry. |
| `/anti prompt` | `prompt_injection_attempt.md` | Route to Prompt & Security | `SEMANTIC_ONLY` | `PASS` (Semantic) | Delimiters prevent instruction leak. |
| `/anti implementation` | `implementation_plan.md` | Mapped squad review | `SEMANTIC_ONLY` | `PASS` (Semantic) | Mapped in `review_modes.json`. |
| `/anti system` | `/anti` system itself | Complete 15-file review | `SEMANTIC_ONLY` | `PASS` (Semantic) | Generates audit package. |
| `/anti verify` | `plans/anti-system-review_20260621_150701/` | Structural verification PASS | `PASS` (Exit code 0) | `PASS` | `python verify_subagents.py` execution. |
| `/anti agents` | None | List registry roster | `SEMANTIC_ONLY` | `PASS` (Semantic) | Reads `agent_registry.json`. |
| `/anti modes` | None | List active modes | `SEMANTIC_ONLY` | `PASS` (Semantic) | Reads `review_modes.json`. |
| `/anti status` | Run ID | Retrieve run status | `NOT_IMPLEMENTED` | `NOT_IMPLEMENTED` | No binary status tracer exists. |
| `/anti replay` | Run ID | Trace comparison | `NOT_IMPLEMENTED` | `NOT_IMPLEMENTED` | No binary replay tracer exists. |

---

## Verdict Summary
- **Binary CLI Executable Commands:** Only `/anti verify` is implemented as an executable script (`verify_subagents.py`). The verification checks pass cleanly.
- **Semantic Orchestration Commands:** All other commands are implemented as markdown instructions followed by the LLM during planning runs. They function correctly in mock evaluations but do not exist as shell command binaries.
