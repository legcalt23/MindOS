# Updated Output Templates

This document details the standardized templates required for all generated deliverables and reports within the `/anti` subagent system.

---

## 1. Agent Review Output Template
```markdown
# [Agent Name]

## Verdict
[APPROVE | CONDITIONAL_APPROVE | VETO | INCONCLUSIVE_BLOCKED]

## Confidence
[0.00 - 1.00]

## Scope Reviewed
- [Scope item 1]
- [Scope item 2]

## Strong Points
1. [Strong point 1]
2. [Strong point 2]
3. [Strong point 3]

## Critical Issues
| Severity | Issue | Evidence | Impact | Required Fix | Owner | Acceptance Criteria |
|---|---|---|---|---|---|---|
| [CRITICAL|HIGH] | [Issue description] | [File reference] | [Risk impact] | [Remediation action] | [Role] | [Verifiable outcome] |

## Missing Evidence
- [Missing evidence description]

## Recommended Changes
1. [Recommended change 1]
2. [Recommended change 2]

## Required Plan Updates
- [Plan updates required]

## Runtime Implementation Implications
- [Implications details]

## Final Agent Decision
[Detailed final decision paragraph]
```

---

## 2. Executive Verdict Template
```markdown
# Executive Verdict: [Project Name]

## 00A Plan Gap Analysis
[Detailed gap analysis checklist]

## Final Status
**Overall Status:** [GO_INTERNAL | CONDITIONAL_GO_INTERNAL | NO_GO_HIGH_STAKES | INCONCLUSIVE_BLOCKED]

*Explanation:* [Overall run verdict reasoning]

## Status by Plane
### [Plane A/B/C/etc.]
- **Verdict:** [Status]
- **Confidence:** [Score]
- **Blockers:** [Blocker description]
- **High Risks:** [High risk description]
- **Missing Evidence:** [Missing evidence reference]
- **Required Fixes:** [Required fix description]
- **Acceptance Criteria:** [Acceptance criteria description]

## Agent Vote Matrix
| Agent | Role | Criticality | Invoked | Conversation ID | Verdict | Confidence | Critical Blockers | Fallback Used |
| :--- | :--- | :---: | :---: | :--- | :---: | :---: | :--- | :---: |
| [Name] | [Role] | [Level] | [Yes/No] | `[UUID]` | [Verdict] | [Score] | [List] | [Yes/No] |

## Blockers
1. [Blocker description]

## High Risks
1. [Risk description]

## Internal Readiness
- **Verdict:** [GO_INTERNAL | CONDITIONAL_GO_INTERNAL | NO_GO]
- **Reason:** [Justification]

## High-Stakes Readiness
- **Verdict:** [GO_HIGH_STAKES | NO_GO_HIGH_STAKES]
- **Reason:** [Justification]

## Founder Decision Required
[Decision options for the founder]
```

---

## 3. Critical Findings Template
```markdown
# Critical Findings Report

## [Blocker / High Risk ID] - [Title]
- **Severity:** [CRITICAL | HIGH]
- **Owner:** [Role]
- **Trigger Condition:** [Trigger details]
- **Impact Analysis:** [Impact details]
- **Acceptance Criteria:** [Acceptance criteria details]
- **Mitigation Path:** [Step-by-step resolution path]
```

---

## 4. Evidence Ledger Template
```markdown
# Evidence Ledger

| Claim ID | Claim | Source | Evidence Type | Verification Status | Agent | Confidence | Risk If Wrong |
|---|---|---|---|---|---|---|---|
| [CLM-01] | [Strategic/technical statement] | [File path / Transcript] | [File / Log / Code] | [VERIFIED | UNVERIFIED | INCONCLUSIVE | CONTRADICTED | NOT_APPLICABLE] | [Agent ID] | [Score] | [Risk impact if wrong] |
```

---

## 5. Invocation Matrix Template
```markdown
# Invocation Matrix

| Agent | Role | Criticality | Required | Invoked | Conversation ID | Status | Verdict | Confidence | Failure Reason | Fallback Used |
|---|---|---|---|---|---|---|---|---|---|---|
| [Agent ID] | [Title] | [critical/standard] | [true/false] | [true/false] | [UUID / UNVERIFIED] | [Status] | [Verdict / NOT_INVOKED] | [Score] | [Details] | [Fallback Name] |
```

---

## 6. Founder Spanish Summary Template
```text
Estado final: [GO_INTERNAL | CONDITIONAL_GO_INTERNAL | NO_GO_HIGH_STAKES | INCONCLUSIVE_BLOCKED]
Si /anti está listo para uso interno: [SÍ | NO | CONDICIONAL]
Si /anti está listo para revisiones críticas: [SÍ | NO]
Número de blockers: [X]
Número de high risks: [Y]
Agentes críticos invocados: [Lista de agentes]
Agentes críticos fallidos: [Lista de agentes fallidos]
Verificación ejecutada o diseñada/no ejecutada: [EJECUTADA | DISEÑADA_NO_EJECUTADA]
Package integrity generado o no: [GENERADO | NO]
Principales mejoras aplicadas:
1. [Mejora 1]
2. [Mejora 2]
Top 5 correcciones obligatorias:
1. [Corrección 1]
2. [Corrección 2]
3. [Corrección 3]
4. [Corrección 4]
5. [Corrección 5]
Ruta del paquete generado: [Ruta absoluta]
Siguiente decisión exacta del fundador: [Opción 1 o Opción 2]
```

---

## 7. Verification Failure Report Template
```markdown
# Verification Failure Report

**Run ID:** `[Run ID]`  
**Execution Timestamp:** `[Timestamp]`  
**Verification Result:** `FAIL`

## Failure Summary
The package checks failed because of the following structural or logic violations:

| Check Category | Violated Rule | Details | File Affected |
|---|---|---|---|
| [Structural/Evidence/Registry] | [Rule violated] | [Failure details] | [File path] |

## Required Resolution Actions
- [Required action 1]
- [Required action 2]
```

---

## 8. Package Integrity Report Template
```markdown
# Package Integrity Ledger

**Run ID:** `[Run ID]`  
**Timestamp:** `[Timestamp]`  
**Verification Status:** `[Status]`  
**Safe for High-Stakes Use:** `[Yes/No]`

This document tracks all generated files, their SHA-256 integrity hashes, and their verification status.

## Generated Files & Integrity Hashes

| File Name | SHA-256 Hash | Verification Status | Verification Failures | Complete |
|---|---|---|---|---|
| [File Name 1] | [SHA-256 Hash] | [Status] | - | [Yes/No] |
```

---

## 9. Run Manifest Template
```json
{
  "run_id": "[Run ID]",
  "started_at": "[Timestamp]",
  "completed_at": "[Timestamp | null]",
  "command": "/anti system",
  "mode": "[Mode Name]",
  "input_hash": "[Hash]",
  "package_path": "[Path]",
  "required_agents": [
    "[Agent 1]",
    "[Agent 2]"
  ],
  "optional_agents": [],
  "invoked_agents": [
    "[Agent 1]"
  ],
  "failed_agents": [],
  "fallbacks_used": [],
  "final_status": "[Status]",
  "verification_status": "[Status]",
  "file_hashes": {
    "[File Name 1]": "[Hash]"
  },
  "token_usage": {
    "estimated": "[Tokens]",
    "actual_if_available": "[Tokens]",
    "baseline_if_available": "[Tokens]"
  }
}
```
