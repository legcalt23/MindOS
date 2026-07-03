# CLI Replay Results

This document records the testing of the `/anti replay` command-line behaviors.

## 1. Replay Command & Execution
- **Command:** `python .antigravity-only/scripts/anti_cli.py replay --manifest .antigravity-only/plans/anti-system-review_20260621_150701/run_manifest.json`
- **Result:** `REPLAY_INCONCLUSIVE`
- **Exit Code:** `2` (interpreted as non-zero inconclusive exit)
- **Output:** `REPLAY_INCONCLUSIVE: transcripts are missing (UNVERIFIED)`

---

## 2. Replay Verification Checks
The replay interpreter checks the following constraints:

1. **Manifest and Package Existence:** Checks that the manifest and target package path are valid and present.
2. **File Casing and Hashes:** Computes normalized LF line-ending hashes for all package files and compares them with manifest records, capturing any drift or modification.
3. **Evidence and Integrity Presence:** Assures `evidence_ledger.md` and `package_integrity.md` exist and are structured properly.
4. **Transcript Matching:** Scans the package for `.jsonl` files. If transcripts are absent, it safely falls back to `REPLAY_INCONCLUSIVE` and does not invent any missing evidence or transcripts.
5. **Vote Matrix Safety:** Scans the agent vote matrix in `00_executive_verdict.md` to ensure no non-invoked agent is treated as invoked.

All CLI tests in `test_anti_cli.py` (including hash mismatch detection and missing transcript fallback tests) have passed successfully.
