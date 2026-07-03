# Review Modes Audit

This document audits the review modes within the `/anti` subagent system, establishing their activation triggers, required squads, critical gates, expected outputs, and token budgets.

---

## 1. Audited Review Modes

The system defines 10 review modes. Their performance and configuration constraints are detailed below:

### 1. FAST_REVIEW
- **Trigger:** `/anti fast [input]`
- **Required Squad:** `agent_orchestration_architect`, `qa_evidence_agent`.
- **Critical Agents:** `agent_orchestration_architect` (Veto rights).
- **Expected Output:** Executive verdict, basic risk register, and single-paragraph checks.
- **Failure Behavior:** Terminate with exit code 1 if any agent is unreachable. No fallback allowed.
- **Token Budget:** 50,000 tokens.

### 2. DEEP_REVIEW
- **Trigger:** `/anti deep [input]`
- **Required Squad:** All 7 core agents.
- **Critical Agents:** `agent_orchestration_architect`, `qa_evidence_agent`, `security_privacy_agent`, `red_team_agent`.
- **Expected Output:** Complete 15-file package directory, full evidence ledger, and signed integrity manifest.
- **Failure Behavior:** Force status to `INCONCLUSIVE_BLOCKED` on agent failure.
- **Token Budget:** 250,000 tokens.

### 3. WAR_ROOM_REVIEW
- **Trigger:** Activated on high-risk strategic updates or emergency vetoes.
- **Required Squad:** All 7 core agents + Senior Strategic Advisors.
- **Critical Agents:** All squad members hold veto rights.
- **Expected Output:** Remediation plan, rollback check, and signed consensus matrix.
- **Failure Behavior:** Fail closed. Terminate run immediately.
- **Token Budget:** 400,000 tokens.

### 4. SCIENTIFIC_REVIEW
- **Trigger:** `/anti prompt [input]` (when clinical or cognitive claims are made)
- **Required Squad:** `qa_evidence_agent`, `prompt_engineering_agent`, `security_privacy_agent`.
- **Critical Agents:** `qa_evidence_agent`.
- **Expected Output:** Scientific Recommendation template, evidence-level checks.
- **Failure Behavior:** Downgrade claims to "UNVERIFIED" and mark recommendation as `REVISE`.
- **Token Budget:** 100,000 tokens.

### 5. SECURITY_REVIEW
- **Trigger:** Activated by security warnings or access-level updates.
- **Required Squad:** `security_privacy_agent`, `red_team_agent`, `agent_orchestration_architect`.
- **Critical Agents:** `security_privacy_agent`, `red_team_agent`.
- **Expected Output:** Vulnerability index, scope compliance audit, DLP validation report.
- **Failure Behavior:** Fail closed. Reject all plan modifications.
- **Token Budget:** 120,000 tokens.

---

## 2. Gaps and Missing Modes
Our audit identified several missing modes required for complete operational coverage:
- **`PROMPT_REVIEW` (Missing):** Specialized mode for testing system prompts, input sanitization, and instruction isolation defenses.
- **`VERIFY` (Missing):** Mode activated by `/anti verify [package_path]` to execute the deterministic validation script over a generated folder.
- **`REPLAY` (Missing):** Mode activated by `/anti replay [run_id]` to reconstruct and verify previous execution paths.

---

## 3. Review Mode Configurations and Budgets

| Mode Name | Trigger Command | Required Squad Size | Critical Agents (Veto Rights) | Token Budget (Max) | Failure Policy |
|---|---|---|---|---|---|
| **FAST_REVIEW** | `/anti fast` | 2 | Orchestrator | 50,000 | Fail open to standard review |
| **DEEP_REVIEW** | `/anti deep` | 7 | Orchestrator, QA, Security, Red Team | 250,000 | Fail closed (Blocked) |
| **WAR_ROOM_REVIEW** | (Triggered) | 7+ | All active agents | 400,000 | Fail closed (Immediate halt) |
| **SCIENTIFIC_REVIEW**| `/anti prompt` | 3 | QA | 100,000 | Downgrade claims to Unverified |
| **SECURITY_REVIEW** | (Triggered) | 3 | Security, Red Team | 120,000 | Fail closed (Immediate halt) |
| **VERIFY** | `/anti verify` | 1 | QA | 20,000 | Output failure report |

---

## 4. Minimum Useful Agents Rule
To prevent token waste and cognitive noise, we establish the "Minimum Useful Agents" rule:
- A review mode must not invoke more than 3 agents unless it is a `DEEP_REVIEW` or `WAR_ROOM_REVIEW`.
- If an input plan only touches a specific plane (e.g., UX layout adjustments), the Orchestrator must restrict the squad to `prompt_engineering_agent` and `qa_evidence_agent`, bypass the other 5 agents, and document the bypass in the run manifest.

## 5. Mode Failure and Veto Handling
If a critical agent fails to respond during mode execution:
1.  **Retry Protocol:** The orchestrator retries the invocation once.
2.  **Fail-Closed Rule:** If the retry fails, the mode terminates with `INCONCLUSIVE_BLOCKED` status. The system is prohibited from falling back to simulated roleplay or skipping the veto.
3.  **Optional Agent Failure:** If an optional agent fails, the run may proceed, but its verdict is recorded as `FAILED_BY_TIMEOUT` and its confidence is marked as `0.00`.
