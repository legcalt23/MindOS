# Upgraded `/anti` Subagent System Plan

This document details the consolidated master plan for the upgraded Antigravity `/anti` subagent review system.

---

## 1. Executive Summary
The `/anti` system is an internal multi-agent review engine used by Google Antigravity. It enables structured, squad-based audits of strategic plans, design plans, and code interfaces. This plan details the architectural, verification, and governance upgrades required to harden `/anti` for reliable, tamper-resistant operations.

## 2. Goals
- Eliminate veto bypass vulnerabilities during subagent runtime crashes.
- Prevent spoofing and mock-agent fabrication through verifiable Conversation IDs.
- Automate package verification and placeholder detection without LLM dependencies.
- Standardize agent registries, review modes, and output packages using machine-readable JSON schemas.

## 3. Non-Goals
- Modifying production client apps, Android logic, or Firebase configurations.
- Expanding the active subagent roster beyond the 7 core members.
- Developing persistent multi-agent orchestration frameworks outside the current execution sandbox.

## 4. Current-State Findings
The current system contains the necessary files under `.antigravity-only/` but operates as an unverified, text-only developer helper. Critical gaps include missing JSON manifests, a missing system prompt registry (`agent_definitions_full.md`), manual smoke tests, and a lack of fail-safe crash routing.

## 5. Command Contract
The command interface is standardized around specific options (e.g. `/anti fast`, `/anti deep`, `/anti verify`, `/anti status`, `/anti replay`). Input contracts, squad requirements, output files, and exit codes are programmatically enforced. Detailed specifications are located in [06_updated_anti_command_contract.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/06_updated_anti_command_contract.md).

## 6. Review Modes
The system implements 10 review modes. These run either lightweight squads (e.g. `FAST_REVIEW` with 2 agents) or full-squad checks (e.g. `DEEP_REVIEW` with 7 agents) depending on the input triggers. Details are stored in [03_review_modes_audit.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/03_review_modes_audit.md).

## 7. Agent Registry
All agents must conform to the YAML/JSON schema defined in [07_updated_agent_registry_spec.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/07_updated_agent_registry_spec.md). Active runtime operations are limited to the 7 core subagents, with the other 29 designed-only roles disabled until manifests are generated.

## 8. Lazy Squad Bootstrap
To optimize token usage, the system implements a "Lazy Squad Bootstrap" protocol. Rather than loading all agents, the orchestrator registers only the squad members mapped to the active review mode. Bootstrap instructions are defined in [04_invocation_and_bootstrap_audit.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/04_invocation_and_bootstrap_audit.md).

## 9. Agent Invocation Lifecycle
The subagent lifecycle moves through squad fetching, registration, execution, and verification. Silent fallback to parent roleplay (`FALLBACK_PROMPT_SIMULATION`) is forbidden. If registration or communication fails, the execution fails closed.

## 10. Run Manifest
Every run generates a persistent `run_manifest.json` following the template schema. The manifest logs timestamps, conversation IDs, invoked/failed agents, and SHA-256 file hashes to ensure run reproducibility.

## 11. Invocation Matrix
The execution details of all squad members are logged in a structured matrix table. This matrix records the Conversation ID, agent status, and active fallbacks used during the run.

## 12. Evidence Ledger
Strategic and technical claims are mapped in `evidence_ledger.md` to prevent LLM hallucinations. Valid claims must cite specific line ranges in configuration files or step indices in active transcripts. Standards are specified in [09_evidence_ledger_and_audit_spec.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/09_evidence_ledger_and_audit_spec.md).

## 13. Output Package Structure
Deliverables are outputted to a new directory named `.antigravity-only/plans/anti-system-review_YYYYMMDD_HHMMSS/`. Previous packages are never overwritten or deleted.

## 14. Voting, Quorum and Veto Rules
- A quorum requires all squad members designated as `required` to successfully complete execution.
- If a critical agent votes `VETO`, the final run recommendation is overridden to `NO_GO_HIGH_STAKES`.
- Safety and reliability votes cannot be bypassed by mock simulation or manual edits.

## 15. Failure Handling
If an agent fails, the orchestrator executes a single retry. If a critical agent remains unresponsive, the orchestrator halts execution, records the failure, and returns exit code 1.

## 16. Anti-Fabrication Controls
- LLMs are prohibited from self-certifying their own verification checks.
- Preflight commands query API session logs to verify that conversation IDs represent distinct execution threads rather than spoofed mock statements.

## 17. Prompt Injection Protection
Input payloads are enclosed in strict JSON boundaries. Subagents execute instructions using system prompts separated by isolated delimiters, ignoring instructions embedded within payload text.

## 18. Verification Script
The deterministic verification script validates generated packages. The script checks file completeness, heading syntax, placeholder absence, and file integrity hashes. It evaluates runs using the Golden Fixtures (`FIX-01` to `FIX-06`) specified in [10_verification_script_spec.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/10_verification_script_spec.md).

## 19. Package Integrity
File integrity is tracked in `package_integrity.md` using computed SHA-256 hashes of all generated files. Verification status must be logged for each file.

## 20. Token and Cost Controls
The system implements a token budgeting adapter. Budgets are assigned per mode (e.g. 50,000 for fast, 250,000 for deep). Invocation hooks abort executions if the cumulative token counts exceed limits.

## 21. Human Override Policy
Manual overrides must be documented in `OVERRIDE_LOG.md`. The log records override ID, gate bypassed, reason, and risk accepted. Human overrides cannot change a failed verification check to passed; they only record approved deviations.

## 22. Operational Runbook
Operating protocols, troubleshooting steps for crash recovery, and registry upgrade actions are detailed in [11_operational_runbook.md](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/anti-system-review_20260621_150701/11_operational_runbook.md).

## 23. Roadmap
- **Phase 0:** Current audit, inventory table, and deliverables specification. (COMPLETED)
- **Phase 1:** Registry manifest generation, prompt source creation, and token budget enforcement. (PLANNED)
- **Phase 2:** Fail-safe routing integration, sandbox restriction, and verification runner deployment. (PLANNED)

## 24. Final Signoff
This plan represents the hardened blueprint for the `/anti` subagent system. Execution of Phase 1 and Phase 2 changes will begin upon founder signoff.
