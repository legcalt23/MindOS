# Agent Registry Audit

This audit evaluates the status, roles, criticality settings, and gaps within the `/anti` agent registry, focusing on the core review squad and the proposed 36-agent Extended Founder Council.

---

## 1. Discovered and Active Registry Roster
The active registry contains the following 7 core subagents that were registered and invoked for the system review:
1.  **Agent Orchestration Architect** (`agent_orchestration_architect`): Orchestration schema and veto integrity checker. (Criticality: `CRITICAL`)
2.  **Prompt Engineering Agent** (`prompt_engineering_agent`): Instruction isolation and injection safety. (Criticality: `CRITICAL`)
3.  **Runtime Reliability Agent** (`runtime_reliability_agent`): Crash handling and runbook validity. (Criticality: `CRITICAL`)
4.  **QA / Evidence Agent** (`qa_evidence_agent`): Evidence checking and verification runner. (Criticality: `CRITICAL`)
5.  **Red Team Agent** (`red_team_agent`): Threat modeling and clock-tamper auditor. (Criticality: `CRITICAL`)
6.  **Security / Privacy Agent** (`security_privacy_agent`): Scope validation and transcript privacy. (Criticality: `CRITICAL`)
7.  **DevOps / Operations Agent** (`devops_operations_agent`): Build/deploy hooks and telemetry constraints. (Criticality: `CRITICAL`)

Additionally, 20 legacy simulated agents are quarantined in `.antigravity-only/agents/quarantine/`. 

The remaining 29 agents designed for the 36-agent Extended Founder Council are defined textually but have status `DESIGNED_NOT_EXECUTED` as they lack active code registry configurations, manifest JSON files, or live session initialization blocks.

## 2. Agent Metadata Gap Analysis
Our audit identified significant structural gaps in agent definition records:
- **No JSON Manifest Files:** Although `agent_manifest.schema.json` is defined, there are zero actual JSON manifest instances. All definitions reside in static markdown.
- **Missing System Prompt Source (`agent_definitions_full.md`):** Dynamic registration templates attempt to reference this file, which does not exist.
- **Undefined Execution Parameters:** Parameters such as `max_retries`, `fallback_agent_id`, `token_budget`, and `timeout_policy` are omitted in current descriptions.

## 3. Duplicate and Overlapping Roles
The proposed 36-agent roster introduces significant overlap that complicates routing:
- **Psychometrics vs. Scoring vs. Metacognition:** These three roles evaluate highly overlapping cognitive scoring aspects. In practice, this causes redundant evaluations.
- **Red Team vs. Red Team Devil's Advocate:** A single Red Team Agent with multiple prompt modes is more efficient than maintaining two distinct runtime agents.
- **DevOps vs. Runtime Reliability:** Roles clash on deployment vs. crash recovery parameters.

## 4. Registry Gaps Summary

| Gap ID | Description | Severity | Impact | Required Action |
|---|---|---|---|---|
| **REG-01** | Missing manifest instances | `HIGH` | Registry cannot be statically checked or loaded. | Generate 7 core JSON manifests matching the schema. |
| **REG-02** | Missing `agent_definitions_full.md` | `CRITICAL` | Subagent prompts cannot be loaded programmatically. | Create the master prompts file with exact role boundaries. |
| **REG-03** | Lack of execution metadata | `MEDIUM` | Fallbacks and budgets default to system-wide hardcodes. | Add retry and budget fields to the registry spec. |

## 5. Recommended Registry Upgrades
1.  **Harden to Core-7:** Restrict the active runtime registry to the 7 core subagents. Keep the remaining 29 agents in a planning-only schema until manifests are built.
2.  **Define Fallback Anchors:** Map every critical agent to an explicit fallback agent. For example, if `security_privacy_agent` fails, route to `red_team_agent`. Do not allow fallback to mock simulation.
3.  **Strict Schema Enforcements:** Force a preflight validation of all registry manifests against `agent_manifest.schema.json` before any `/anti` execution begins.
