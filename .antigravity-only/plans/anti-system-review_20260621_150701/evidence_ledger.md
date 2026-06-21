# Evidence Ledger

This ledger tracks every major strategic and technical claim made in the executive verdict and deliverables. Every claim must trace back to raw transcripts, files, or verified specifications.

| Claim ID | Claim | Source | Evidence Type | Verification Status | Agent | Confidence | Risk If Wrong |
|---|---|---|---|---|---|---|---|
| CLM-01 | Crashed subagents skip quality gates, bypassing mandatory safety vetoes. | `scratch/anti_system_verdicts.txt#L5-16` | Audit Transcript | VERIFIED | `agent_orchestration_architect` | 0.95 | `CRITICAL` - Unsafe plans approved. |
| CLM-02 | The orchestrator falls back to parent roleplay (`FALLBACK_PROMPT_SIMULATION`) and spoofs Conversation IDs when registration fails. | `scratch/anti_system_verdicts.txt#L68-96` | Audit Transcript | VERIFIED | `red_team_agent` | 0.95 | `CRITICAL` - Safety gates bypassed. |
| CLM-03 | The source registry file `agent_definitions_full.md` referenced in bootstrap docs is missing from the repository. | `.antigravity-only/agents/ file list` | File System | VERIFIED | `qa_evidence_agent` | 0.95 | `HIGH` - Non-deterministic bootstrap. |
| CLM-04 | All preflight checks and subagent verification are manual text-only smoke tests. | `.antigravity-only/ scripts search` | File System | VERIFIED | `qa_evidence_agent` | 0.95 | `HIGH` - Configuration drift. |
| CLM-05 | Subagents operate under `SHARED_PARENT_SCOPE` read access, exposing sensitive workspace environment files. | `agents/REALITY_STATUS.md#L12` | Config File | VERIFIED | `security_privacy_agent` | 0.95 | `HIGH` - Transcript credential leak. |
