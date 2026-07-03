# Evidence Ledger

This ledger tracks every major claim made in the M8 subagent verification package.

| Claim ID | Claim | Source | Evidence Type | Verification Status | Agent | Confidence | Risk If Wrong |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| CLM-M8-01 | Registry contains exactly 36 canonical subagents. | `00_real_subagent_registry.md` | File System | VERIFIED | `chief_orchestrator_agent` | 1.00 | `CRITICAL` |
| CLM-M8-02 | Registry matches surgery and upgraded plan source documents. | `00_real_subagent_registry.md` | File System | VERIFIED | `chief_orchestrator_agent` | 1.00 | `CRITICAL` |
| CLM-M8-03 | Python sandbox enforces path separation and denied secrets. | `01_sandbox_enforcement_audit.md` | File System | VERIFIED | `security_privacy_agent` | 0.95 | `HIGH` |
| CLM-M8-04 | CLI status and verify commands enforce reconciliation modes. | `02_cli_status_reconciliation_audit.md` | File System | VERIFIED | `qa_evidence_agent` | 0.95 | `HIGH` |
| CLM-M8-05 | CLI replay command rejects tampered, fake or uninvoked agent matrix votes. | `03_cli_replay_audit.md` | File System | VERIFIED | `qa_evidence_agent` | 0.95 | `HIGH` |

## 2. Readiness Confirmation Statements

The following confirmation statements are issued for the Phase M8 implementation:

* **CANONICAL_36_CONFIRMED**: The roster of exactly 36 subagents matches the surgery log and council plan repository evidence.
* **GO_INTERNAL_VERIFIED**: The system is verified for controlled, local/internal pair-programming review operations.
* **NO_GO_HIGH_STAKES**: The system is NOT approved for unsupervised high-stakes autonomous execution.
* **SANDBOX_ENFORCEMENT_PARTIAL**: Sandbox enforcement is restricted to Python process-level API interception and does not contain lower-level subprocess or native binary executions.
