# Updated `/anti` Command Contract

This document establishes the updated command contract for the `/anti` CLI subagent system. It defines input syntax, required squads, output deliverables, failure policies, and verification criteria for each command option.

---

## 1. Commands Specification

The following commands are defined under the updated CLI contract:

### `/anti fast [input]`
- **Purpose:** Perform a rapid strategic check on a proposed plan to detect immediate design flaws.
- **Input Contract:** Raw text input or path to a markdown plan.
- **Required Agents:** `agent_orchestration_architect`, `qa_evidence_agent`.
- **Optional Agents:** None.
- **Output Contract:** Generates a lightweight review package with 3 files (`run_manifest.json`, `evidence_ledger.md`, `00_executive_verdict.md`).
- **Failure Behavior:** Terminate run immediately on agent timeout.
- **Final Status Rules:** `GO_INTERNAL` if no blockers; `INCONCLUSIVE_BLOCKED` on crash.
- **Example Usage:** `/anti fast plans/daily_streak_plan.md`
- **Acceptance Criteria:** Response is returned within 10 seconds; no placeholder strings remain.

### `/anti deep [input]`
- **Purpose:** Execute a comprehensive multi-agent audit on strategic or complex plans.
- **Input Contract:** Path to a complete planning directory or plan markdown file.
- **Required Agents:** All 7 core agents.
- **Optional Agents:** None.
- **Output Contract:** Generates the complete 15-file review package under `.antigravity-only/plans/anti-system-review_YYYYMMDD_HHMMSS/`.
- **Failure Behavior:** Terminate run immediately on critical agent timeout/failure. No fallback allowed.
- **Final Status Rules:** Evaluated dynamically based on agent verdicts and blockers.
- **Example Usage:** `/anti deep plans/timing_architecture.md`
- **Acceptance Criteria:** All 7 reviews are completed independently; verification checks pass.

### `/anti implementation [input]`
- **Purpose:** Review technical implementation specs, generate detailed Claude Code handoff prompts, and review coding boundaries.
- **Input Contract:** Path to a coding or architecture specification.
- **Required Agents:** `agent_orchestration_architect`, `prompt_engineering_agent`, `runtime_reliability_agent`, `qa_evidence_agent`.
- **Optional Agents:** `security_privacy_agent`, `devops_operations_agent`.
- **Output Contract:** Generates technical planning summaries and Claude Code prompts.
- **Failure Behavior:** Fail-closed if any required agent crashes.
- **Final Status Rules:** `CONDITIONAL_GO_INTERNAL` if implementation risks remain.
- **Example Usage:** `/anti implementation docs/timing_spec.md`
- **Acceptance Criteria:** Generated prompts contain exact file boundaries, tests required, and rollback steps.

### `/anti prompt [input]`
- **Purpose:** Audit system prompts, role boundaries, instruction isolation, and prompt injection vulnerabilities.
- **Input Contract:** Raw prompt text or path to a prompt template file.
- **Required Agents:** `prompt_engineering_agent`, `security_privacy_agent`, `red_team_agent`.
- **Optional Agents:** `qa_evidence_agent`.
- **Output Contract:** Injection vulnerability score, sanitization suggestions, and isolated prompt templates.
- **Failure Behavior:** Fail closed.
- **Final Status Rules:** `NO_GO_HIGH_STAKES` if injection escape is detected.
- **Example Usage:** `/anti prompt agents/prompts/cognitive_science.txt`
- **Acceptance Criteria:** Output includes prompt sanitization check results.

### `/anti system [input]`
- **Purpose:** Perform a full audit of the `/anti` subagent system itself (this command was used to execute the current run).
- **Input Contract:** Path to the `/anti` system specification and implementation plan.
- **Required Agents:** All 7 core agents.
- **Optional Agents:** None.
- **Output Contract:** Generates the complete 15-file system audit package.
- **Failure Behavior:** Fail-closed on critical agent crash.
- **Final Status Rules:** Derived strictly from independent votes and blockers.
- **Example Usage:** `/anti system .antigravity-only/`
- **Acceptance Criteria:** All 15 required deliverables are populated and verify checks execute successfully.

### `/anti verify [package_path]`
- **Purpose:** Run deterministic structural and evidence checks over a generated package directory.
- **Input Contract:** Absolute path to a generated review package.
- **Required Agents:** `qa_evidence_agent`.
- **Optional Agents:** None.
- **Output Contract:** Generates a verification status log. If checks fail, outputs `13_verification_failures.md`.
- **Failure Behavior:** Returns exit code 1 if files are missing or placeholders remain.
- **Final Status Rules:** Sets `verification_status` to `PASS` or `FAIL`.
- **Example Usage:** `/anti verify .antigravity-only/plans/anti-system-review_20260621_150701/`
- **Acceptance Criteria:** Deterministic check executes without LLM calls.

### `/anti status [run_id]`
- **Purpose:** Retrieve the status and progress of a background review run.
- **Input Contract:** Valid Run ID string.
- **Required Agents:** None.
- **Optional Agents:** None.
- **Output Contract:** Console log with current state (e.g. `IN_PROGRESS`, `COMPLETED`, `INCONCLUSIVE_BLOCKED`) and elapsed time.
- **Failure Behavior:** Returns "Run ID not found" if invalid.
- **Final Status Rules:** Read-only command.
- **Example Usage:** `/anti status run-20260621-150701-anti-harden`
- **Acceptance Criteria:** Status query resolves instantly.

### `/anti replay [run_id]`
- **Purpose:** Reconstruct and verify previous execution paths based on the saved run manifest and logs.
- **Input Contract:** Valid Run ID string.
- **Required Agents:** `agent_orchestration_architect`.
- **Optional Agents:** None.
- **Output Contract:** Step-by-step trace comparison report.
- **Failure Behavior:** Terminate run if previous logs are missing or tampered.
- **Final Status Rules:** `GO_INTERNAL` if trace matches; `INCONCLUSIVE_BLOCKED` on mismatch.
- **Example Usage:** `/anti replay run-20260621-150701-anti-harden`
- **Acceptance Criteria:** Output shows complete timeline.

### `/anti agents`
- **Purpose:** List all active and designed agents in the registry.
- **Input Contract:** None.
- **Required Agents:** None.
- **Optional Agents:** None.
- **Output Contract:** Markdown table listing Agent ID, Role, Criticality, Supported Modes, and Version.
- **Failure Behavior:** None.
- **Final Status Rules:** Read-only command.
- **Example Usage:** `/anti agents`
- **Acceptance Criteria:** Table matches current-state JSON manifests.

### `/anti modes`
- **Purpose:** List all available review modes and their assigned agent squads.
- **Input Contract:** None.
- **Required Agents:** None.
- **Optional Agents:** None.
- **Output Contract:** Markdown list of modes, triggers, and squads.
- **Failure Behavior:** None.
- **Final Status Rules:** Read-only command.
- **Example Usage:** `/anti modes`
- **Acceptance Criteria:** Output matches `FOUNDER_COUNCIL_REVIEW_MODES.md`.
