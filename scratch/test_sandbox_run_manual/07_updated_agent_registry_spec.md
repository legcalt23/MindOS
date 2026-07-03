# Updated Agent Registry Spec

This document specifies the schema definitions and core entries for the `/anti` agent registry. All agents must conform to this schema to be dynamically bootstrapped.

---

## 1. Machine-Readable Registry Schema (YAML)

```yaml
agent_id:
  type: string
  pattern: "^[a-z0-9_]+$"
  description: "Unique snake_case identifier for the agent."
display_name:
  type: string
  description: "Human-readable name."
role:
  type: string
  description: "Role or job title."
description:
  type: string
  description: "Detailed description of the agent's main mission."
criticality:
  type: string
  enum: [critical, standard, optional]
  description: "Critical agents block approvals if they fail or crash."
supported_modes:
  type: array
  items:
    type: string
  description: "Review modes this agent is allowed to join."
default_modes:
  type: array
  items:
    type: string
  description: "Modes where this agent is activated by default."
input_contract:
  type: string
  description: "Description of expected inputs."
output_contract:
  type: string
  description: "Required sections and formatting of output reports."
tools_allowed:
  type: array
  items:
    type: string
  description: "Allowlist of tools. Usually read-only for planning subagents."
tools_forbidden:
  type: array
  items:
    type: string
  description: "Denylist of tools. Must include write and MCP execution tools."
max_retries:
  type: integer
  minimum: 0
  maximum: 3
  description: "Number of invocation retries on crash before failing."
fallback_agent_id:
  type: string
  description: "Identifier of the agent to route to if this agent fails."
token_budget:
  type: integer
  description: "Maximum token limit per invocation."
timeout_policy:
  type: string
  description: "Timeout handling strategy (e.g. HALT, DOWNGRADE)."
evidence_required:
  type: boolean
  description: "If true, agent must cite references in its audit report."
veto_rights:
  type: boolean
  description: "If true, a veto (NO-GO) blocks the entire run."
version:
  type: string
  description: "Version identifier."
owner:
  type: string
  description: "Owner ID responsible for maintenance."
last_updated:
  type: string
  format: date-time
  description: "ISO-8601 timestamp of last change."
```

---

## 2. Core Agent Registry Entries

### 1. Agent Orchestration Architect
```yaml
agent_id: agent_orchestration_architect
display_name: Agent Orchestration Architect
role: Orchestration Schema & Veto Auditor
description: Audits routing maps, bootstrap protocols, quorum rules, and veto execution.
criticality: critical
supported_modes: [FAST_REVIEW, DEEP_REVIEW, WAR_ROOM_REVIEW, SECURITY_REVIEW, OPERATIONAL_REVIEW]
default_modes: [FAST_REVIEW, DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "Review packages and registry plan maps."
output_contract: "Verdict (APPROVE/VETO), confidence level, and list of critical orchestration issues."
tools_allowed: [view_file, list_dir]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: none
token_budget: 40000
timeout_policy: HALT
evidence_required: true
veto_rights: true
version: "2.1.0"
owner: dev-team
last_updated: "2026-06-21T15:00:00Z"
```

### 2. Prompt Engineering Agent
```yaml
agent_id: prompt_engineering_agent
display_name: Prompt Engineering Agent
role: Prompt Optimizer and Instruction Auditor
description: Audits prompt safety, system prompts, role boundaries, and injection vulnerability.
criticality: critical
supported_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW, SCIENTIFIC_REVIEW]
default_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "System prompt declarations and test payloads."
output_contract: "Prompt strength analysis, instruction leak assessment, and security suggestions."
tools_allowed: [view_file]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: none
token_budget: 35000
timeout_policy: HALT
evidence_required: true
veto_rights: false
version: "1.8.0"
owner: security-team
last_updated: "2026-06-21T15:00:00Z"
```

### 3. Runtime Reliability Agent
```yaml
agent_id: runtime_reliability_agent
display_name: Runtime Reliability Agent
role: Runtime and Crash Resilience Auditor
description: Audits crash recovery rules, execution limits, and rollback protocols.
criticality: critical
supported_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW, OPERATIONAL_REVIEW]
default_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "Rollback plans, recovery logs, and YAML configurations."
output_contract: "Resilience checklist score, crash impact table, and retry parameters."
tools_allowed: [view_file, list_dir]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: devops_operations_agent
token_budget: 30000
timeout_policy: DOWNGRADE
evidence_required: true
veto_rights: false
version: "1.2.0"
owner: reliability-team
last_updated: "2026-06-21T15:00:00Z"
```

### 4. QA / Evidence Agent
```yaml
agent_id: qa_evidence_agent
display_name: QA / Evidence Agent
role: Evidence Ledger and Verification Auditor
description: Audits evidence ledgers, checks citation accuracy, and verifies file structures.
criticality: critical
supported_modes: [FAST_REVIEW, DEEP_REVIEW, WAR_ROOM_REVIEW, SCIENTIFIC_REVIEW, OPERATIONAL_REVIEW]
default_modes: [FAST_REVIEW, DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "Generated review package directories and evidence ledgers."
output_contract: "Verification report, ledger citation checks, and placeholder warning list."
tools_allowed: [view_file, list_dir]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: none
token_budget: 30000
timeout_policy: HALT
evidence_required: true
veto_rights: true
version: "2.0.0"
owner: qa-team
last_updated: "2026-06-21T15:00:00Z"
```

### 5. Red Team Agent
```yaml
agent_id: red_team_agent
display_name: Red Team Agent
role: Adversarial Threat Auditor
description: Attacks configurations, checks for verification bypasses and clock cheats.
criticality: critical
supported_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW, SECURITY_REVIEW]
default_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "Configuration scopes, clocks, and security boundaries."
output_contract: "Threat matrix table, clock vulnerability analysis, and bypass proof."
tools_allowed: [view_file, list_dir]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: none
token_budget: 45000
timeout_policy: HALT
evidence_required: true
veto_rights: true
version: "2.3.0"
owner: security-team
last_updated: "2026-06-21T15:00:00Z"
```

### 6. Security / Privacy Agent
```yaml
agent_id: security_privacy_agent
display_name: Security / Privacy Agent
role: Transcript Privacy and DLP Auditor
description: Audits transcript privacy, credentials path boundaries, and data minimization.
criticality: critical
supported_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW, SECURITY_REVIEW]
default_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "Transcript files, scope configurations, and environment paths."
output_contract: "Data leakage risk assessment, sandbox isolation verification, and path audit."
tools_allowed: [view_file]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: none
token_budget: 35000
timeout_policy: HALT
evidence_required: true
veto_rights: true
version: "2.0.0"
owner: security-team
last_updated: "2026-06-21T15:00:00Z"
```

### 7. DevOps / Operations Agent
```yaml
agent_id: devops_operations_agent
display_name: DevOps / Operations Agent
role: Operations and Observability Auditor
description: Audits build pipelines, deployment gates, rollback metrics, and telemetry constraints.
criticality: critical
supported_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW, OPERATIONAL_REVIEW]
default_modes: [DEEP_REVIEW, WAR_ROOM_REVIEW]
input_contract: "CI/CD workbooks, release reports, and operational telemetry."
output_contract: "Telemetry readiness check, pipeline metrics, and recovery step signoff."
tools_allowed: [view_file, list_dir]
tools_forbidden: [write_to_file, run_command]
max_retries: 2
fallback_agent_id: runtime_reliability_agent
token_budget: 30000
timeout_policy: DOWNGRADE
evidence_required: true
veto_rights: false
version: "1.5.0"
owner: devops-team
last_updated: "2026-06-21T15:00:00Z"
```
