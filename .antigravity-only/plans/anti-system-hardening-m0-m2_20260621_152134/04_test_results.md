# Test Results

This document records the results of the verification script execution tests on the `/anti` review package.

---

## 1. Executed Verification Commands

```bash
python .antigravity-only/scripts/verify_subagents.py --package .antigravity-only/plans/anti-system-review_20260621_150701/
```

- **Exit Code:** `0`
- **Result:** `PASS`
- **JSON Output:**
```json
{
  "status": "PASS",
  "failures": [],
  "warnings": [],
  "final_status_allowed": true
}
```

---

## 2. Verification of Golden Fixtures (Fail-Closed Tests)

```bash
python .antigravity-only/scripts/verify_subagents.py --package .antigravity-only/scripts/fixtures/placeholder_output_package/
```
- **Exit Code:** `1`
- **Result:** `FAIL` (Correctly blocked due to missing files and placeholder checks).

```bash
python .antigravity-only/scripts/verify_subagents.py --package .antigravity-only/scripts/fixtures/fake_non_invoked_verdict_package/
```
- **Exit Code:** `1`
- **Result:** `FAIL` (Correctly blocked due to non-invoked agent verdict).

---

## 3. Scope of Files Checked
The script verified the following files under `plans/anti-system-review_20260621_150701/`:
1. `00_executive_verdict.md`
2. `01_current_anti_system_audit.md`
3. `02_agent_registry_audit.md`
4. `03_review_modes_audit.md`
5. `04_invocation_and_bootstrap_audit.md`
6. `05_failure_modes_and_risk_register.md`
7. `06_updated_anti_command_contract.md`
8. `07_updated_agent_registry_spec.md`
9. `08_updated_output_templates.md`
10. `09_evidence_ledger_and_audit_spec.md`
11. `10_verification_script_spec.md`
12. `11_operational_runbook.md`
13. `12_final_upgraded_anti_system_plan.md`
14. `evidence_ledger.md`
15. `package_integrity.md`
16. `run_manifest.json`

## 4. Failures Found
- **Initial Run:** Flagged formatting violations in 7 files (empty subheadings and raw placeholders in spec examples).
- **Post-Correction Run:** 0 failures found.

## 5. Remaining Blockers
- None. All blockers are resolved.
