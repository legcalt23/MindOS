# Verification Script Spec

This document specifies the deterministic validation logic and test constraints for auditing `/anti` review packages. It defines the validation check list, failure policies, and golden test fixtures.

---

## 1. Deterministic Verification Logic

The verification engine executes static and structural checks on the package directory without calling external LLM APIs. The script must run the following checks:

### 1. File Completeness Check
- Verify that all 15 required files exist in the target package directory:
  - `00_executive_verdict.md`
  - `01_current_anti_system_audit.md`
  - `02_agent_registry_audit.md`
  - `03_review_modes_audit.md`
  - `04_invocation_and_bootstrap_audit.md`
  - `05_failure_modes_and_risk_register.md`
  - `06_updated_anti_command_contract.md`
  - `07_updated_agent_registry_spec.md`
  - `08_updated_output_templates.md`
  - `09_evidence_ledger_and_audit_spec.md`
  - `10_verification_script_spec.md`
  - `11_operational_runbook.md`
  - `12_final_upgraded_anti_system_plan.md`
  - `run_manifest.json`
  - `evidence_ledger.md`
  - `package_integrity.md`

### 2. Header and Structure Check
- Parse each markdown file and check that all required headers (defined in output templates) are present.
- Ensure that all sections under headers contain at least one non-empty line of text.

### 3. Placeholder Detection Check
- Scan all generated files for any of the following placeholder patterns:
  - `[` `Placeholder` `...]`, `[` `insert` `...]`, `[` `TODO` `...]`, `[` `TBD` `]`, `IN_PROGRESS`, `UNVERIFIED` (except inside raw mock templates).
- Identify any instances and log them as structural failures.

### 4. Findings and Owner Validation
- Audit `00_executive_verdict.md` and `05_failure_modes_and_risk_register.md` to ensure that every `BLOCKER` or `HIGH` severity finding contains:
  - An assigned owner (e.g. `Orchestration Architect`, `QA Lead`).
  - Verifiable acceptance criteria.

### 5. Agent Invocation Integrity Check
- Cross-check `run_manifest.json` and `00_executive_verdict.md` to ensure:
  - No agent marked as `NOT_INVOKED` has a real verdict or conversation ID.
  - All critical squad members are marked as `invoked = true`, or a valid fallback agent is documented.
  - No duplicate agent verdicts exist in the matrix.

### 6. Evidence Ledger Mapping Check
- Ensure that every major claim in `00_executive_verdict.md` corresponds to a row in `evidence_ledger.md`.
- Validate that every row in `evidence_ledger.md` has a verification status (e.g. `VERIFIED`, `UNVERIFIED`).

### 7. Package Integrity Hashing Check
- Recompute the SHA-256 hash of each file in the package.
- Verify that the recomputed hashes match the records inside `package_integrity.md` and `run_manifest.json`.

### 8. Claim Cautiousness Check
- Scan all text files for prohibited absolute claims:
  - `fully audited`, `100% reliable`, `production-ready`, `validated`, `proved`, `unbreakable`.
- Enforce the use of cautious qualifiers instead (e.g. `review package generated`, `internal readiness assessed`).

---

## 2. Failure Policy & Recovery
If any check fails:
1.  **Generate Failure Logs:** Write a detailed report to `13_verification_failures.md` explaining the specific violation.
2.  **Force Status Downgrade:** Override the final run status inside `run_manifest.json` and `00_executive_verdict.md` to `INCONCLUSIVE_BLOCKED`.
3.  **Halt Release Gate:** Return exit code 1 to block CI/CD pipeline integration.

---

## 3. Golden Test Fixtures

The verification script uses the following synthetic test fixtures to validate routing, security boundaries, and validation safety:

| Fixture ID | Fixture Name | Purpose | Expected Mode | Expected Status | Expected Verification Result |
|---|---|---|---|---|---|
| **FIX-01** | **Valid small plan** | Baseline routing and output verification. Tests standard `/anti fast` commands. | `FAST_REVIEW` | `GO_INTERNAL` | `PASS` (All 3 files exist and are verified). |
| **FIX-02** | **Missing evidence plan**| Verifies that missing claims are flagged. Tests evidence ledger enforcement. | `DEEP_REVIEW` | `INCONCLUSIVE_BLOCKED` | `FAIL` (Generates `13_verification_failures.md` due to missing references). |
| **FIX-03** | **Prompt injection attempt**| Tests system prompt isolation. Payload contains system-override attacks. | `SECURITY_REVIEW` | `NO_GO_HIGH_STAKES` | `FAIL` (Flagged by security agent; unsafe content blocks execution). |
| **FIX-04** | **Fake non-invoked verdict**| Verifies that fake votes are blocked. Matrix contains fake verdicts for non-invoked agents. | `FAST_REVIEW` | `NO_GO_HIGH_STAKES` | `FAIL` (Detected by orchestrator; blocks run integrity check). |
| **FIX-05** | **Critical agent failure** | Tests crash recovery. A critical squad member fails to respond or timeouts. | `DEEP_REVIEW` | `INCONCLUSIVE_BLOCKED` | `FAIL` (Orchestrator blocks output due to incomplete squad quorum). |
| **FIX-06** | **Placeholder output package**| Verifies placeholder scanning. Package contains `TODO` or `IN_PROGRESS` tags. | `VERIFY` | `INCONCLUSIVE_BLOCKED` | `FAIL` (Scans detect raw placeholder strings). |
