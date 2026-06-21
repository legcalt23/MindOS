# Current `/anti` Subagent System Audit

This document provides a detailed current-state audit of the `/anti` subagent system, including command behavior, folder structure, discovered files, existing agent definitions, templates, review modes, limitations, missing components, and the current-state inventory table.

---

## 1. Current Command Behavior
The `/anti` command is configured as a structured planning command. 
- It reads the master guidelines and protocols located in `.antigravity-only/`.
- It performs a request classification to route proposals to appropriate teams (e.g., `ANTI_PLAN`, `ANTI_EXERCISE_REVIEW`).
- It enforces strict product scoping (`MindBoost` only) and prohibits production code modifications.
- It generates strategic planning deliverables (e.g., scientific summaries, financial assessments, risk registers, and Claude Code handoff prompts) rather than executing direct code changes.

## 2. Folder Structure
The current `/anti` configuration is stored within the following layout inside the repository:
- `.` (Root directory)
  - `.antigravity-only/`
    - `agents/` (agent bootstrapping and modes)
    - `blueprints/` (layout models)
    - `checklists/` (pre/post-flight validations)
    - `commands/` (custom command definitions)
    - `examples/` (reference usage examples)
    - `gates/` (quality gates and boundaries)
    - `protocols/` (division-specific review protocols)
    - `templates/` (deliverable output templates)
    - `plans/` (review output packages)

## 3. Discovered Files
A total of 42 files were discovered within `.antigravity-only/` during our current-state search. Key files include:
- `anti_config.yaml` (global settings)
- `03_COMMAND_ROUTER.md` (classification logic)
- `agents/BOOTSTRAP_FOUNDER_COUNCIL.md` (bootstrap configuration)
- `agents/FOUNDER_COUNCIL_REVIEW_MODES.md` (review mode lists)
- `agents/REALITY_STATUS.md` (status logs)
- `commands/anti.md` (routing syntax and instructions)

## 4. Existing Agent Definitions
The subagents are defined textually within:
- `BOOTSTRAP_FOUNDER_COUNCIL.md` which lists dynamic registration prompt templates.
- `REALITY_STATUS.md` which describes the status properties of the 36-agent council as `SESSION_REAL`.
- *Gap identified:* The file `agent_definitions_full.md`, referenced in bootstrap documents as containing the actual system prompts, is absent.

## 5. Existing Templates
Templates exist for generating standard deliverables under `.antigravity-only/templates/`, including:
- `claude_code_prompt_template.md` (formatting handoffs)
- `plan_pack_template.md` (review package structure)
- `risk_register_template.md` (risk formatting)
- `signoff_template.md` (vote collecting)

## 6. Existing Review Modes
Review modes are documented in `agents/FOUNDER_COUNCIL_REVIEW_MODES.md`. They specify the following modes:
- `FAST_REVIEW` (essential checks)
- `DEEP_REVIEW` (full squad audits)
- `WAR_ROOM_REVIEW` (critical incident drills)
- `SCIENTIFIC_REVIEW` (claims checks)
- `FINANCIAL_REVIEW` (business checks)
- `METRIC_REVIEW` (scoring and psychometrics checks)
- `COMPETITIVE_REVIEW` (integrity checks)
- `PLANNING_REVIEW` (default plan generation)
- `SECURITY_REVIEW` (privacy and boundaries checks)
- `OPERATIONAL_REVIEW` (release readiness checks)

## 7. Existing Output Directories
Output packages are generated under `.antigravity-only/plans/anti-system-review_YYYYMMDD_HHMMSS/` for systemic audit reviews, and under `plans/YYYY-MM-DD-short-name/` for product plans.

## 8. Existing Logs and Transcripts
Past run execution logs and temporary subagent interaction traces are captured in `.system_generated/logs/transcript.jsonl` within the app data directories. Local scratch summaries are stored in `/scratch/` (e.g. `anti_system_verdicts.txt`).

## 9. Current Limitations
- **Session Volatility:** Dynamic subagents are registered in-memory and lost upon session termination or workspace reload.
- **Over-privileged Sandbox:** Subagents inherit the parent's read boundary (`SHARED_PARENT_SCOPE`), allowing them to access sensitive config files.
- **Veto Crash Vulnerability:** If a subagent crashes or times out, the main orchestrator bypasses the veto gate.
- **Manual Verification:** No automated script runs to verify package structures or files.

## 10. Missing Components
- **`agent_definitions_full.md`:** The source file containing the 36 agent system prompts is missing.
- **JSON Manifests:** Individual agent schema instances do not exist.
- **Verification Scripts:** No executable validation scripts exist.
- **Override Log (`OVERRIDE_LOG.md`):** Safe override tracking is absent.

---

## 11. Current-State Inventory Table

| Component Path / Reference | Type | Status | Evidence Source | Notes |
|---|---|---|---|---|
| `.antigravity-only/README.md` | File | `FOUND` | File System | Readme documentation exists. |
| `.antigravity-only/00_MASTER_SYSTEM.md` | File | `FOUND` | File System | Master system instructions found. |
| `.antigravity-only/01_MINDBOOST_SCOPE.md` | File | `FOUND` | File System | Scope isolation guidelines found. |
| `.antigravity-only/02_NO_IMPLEMENTATION_RULE.md` | File | `FOUND` | File System | Core restriction rules found. |
| `.antigravity-only/03_COMMAND_ROUTER.md` | File | `FOUND` | File System | Command routing rules found. |
| `.antigravity-only/04_OPERATING_PROTOCOL.md` | File | `FOUND` | File System | Execution loop documentation found. |
| `.antigravity-only/anti_config.yaml` | File | `FOUND` | File System | Configuration parser file found. |
| `.antigravity-only/commands/anti.md` | File | `FOUND` | File System | Main custom command definition found. |
| `.antigravity-only/agents/BOOTSTRAP_FOUNDER_COUNCIL.md` | File | `FOUND` | File System | Dynamic setup instructions found. |
| `.antigravity-only/agents/FOUNDER_COUNCIL_REVIEW_MODES.md` | File | `FOUND` | File System | Roster mappings per mode found. |
| `.antigravity-only/agents/REALITY_STATUS.md` | File | `FOUND` | File System | Session-real parameters found. |
| `.antigravity-only/agents/agent_definitions_full.md` | File | `MISSING_OR_UNVERIFIED` | File System | Referenced in bootstrap but missing. |
| `.antigravity-only/agents/schemas/agent_manifest.schema.json` | File | `FOUND` | File System | Schema file is present. |
| `.antigravity-only/agents/schemas/*.json` (Manifest instances) | Directory | `MISSING_OR_UNVERIFIED` | File System | No active JSON instances exist. |
| `.antigravity-only/scripts/` | Directory | `MISSING_OR_UNVERIFIED` | File System | No executable verification scripts. |
| `.antigravity-only/agents/quarantine/` | Directory | `FOUND` | File System | Legacy simulated agents quarantined. |
| `OVERRIDE_LOG.md` | File | `MISSING_OR_UNVERIFIED` | File System | Veto override tracker is missing. |
| `evidence_ledger.md` (active) | File | `PARTIAL` | File System | Exists as an empty skeleton only. |
| `package_integrity.md` (active) | File | `PARTIAL` | File System | Exists as an empty skeleton only. |
| `run_manifest.json` (active) | File | `PARTIAL` | File System | Exists with empty/default fields. |
