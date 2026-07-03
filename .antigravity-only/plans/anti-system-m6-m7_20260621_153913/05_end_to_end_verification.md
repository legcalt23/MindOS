# End-to-End Verification

This document records the exact commands executed, exit codes, and results for Phase M6–M7.

## 1. End-to-End Execution Ledger

| Command | Exit Code | Result | Details / Failure Reason | Evidence File |
|---|---|---|---|---|
| `python .antigravity-only/scripts/test_sandbox_enforcement.py` | `0` | `PASS` | All 12 path-blocking and write-rooting tests passed. | `02_sandbox_test_results.md` |
| `python .antigravity-only/scripts/test_anti_cli.py` | `0` | `PASS` | All 10 CLI status/replay/verify tests passed. | `04_cli_replay_results.md` |
| `python .antigravity-only/scripts/anti_cli.py verify --package .antigravity-only/plans/anti-system-review_20260621_150701/` | `0` | `PASS` | Structural, placeholder, and claim checks passed. | `verify_subagents.py` |
| `python .antigravity-only/scripts/anti_cli.py status --manifest .antigravity-only/plans/anti-system-rollout-m3-m5_20260621_152752/run_manifest.json` | `0` | `PASS` | Status metrics displayed correctly. | `03_cli_status_results.md` |
| `python .antigravity-only/scripts/anti_cli.py replay --manifest .antigravity-only/plans/anti-system-rollout-m3-m5_20260621_152752/run_manifest.json` | `1` | `REPLAY_FAIL` | Fails cleanly because `evidence_ledger.md` is missing from the rollout package. | `04_cli_replay_results.md` |
| `python .antigravity-only/scripts/anti_cli.py replay --manifest .antigravity-only/plans/anti-system-review_20260621_150701/run_manifest.json` | `2` | `REPLAY_INCONCLUSIVE` | Inconcludes safely since subagent transcripts are not stored inside the package folder. | `04_cli_replay_results.md` |

---

## 2. Integrity Verification
- **LF Line Ending Normalization:** All SHA-256 hashes are recalculated using normalized LF line endings (`\n`) before comparison, preventing Windows/Posix casing and line ending conflicts.
- **Verification Suite Integrity:** The validation checks pass in strict mode, proving that package files are unmodified.
