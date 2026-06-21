# Subagent System Limitations & Failure Modes

This document details the technical limits, operational constraints, and failure modes observed during the validation of the **MindOS Founder Council Extended V2** system.

---

## 1. Technical Scope & Sandbox Limitations

All custom subagents launched via the `invoke_subagent` tool operate under severe constraints that restrict them from executing autonomously as real software engineers:

1.  **Shared Parent Sandbox (`SHARED_PARENT_SCOPE`):**
    Subagents share the exact container environment, filesystem permissions, and workspace directory as the parent agent. They do not run in isolated virtual machines or separate secure containers. If a subagent modifies a file, it modifies the parent's source workspace directly. To maintain safety, subagents must have their write tools disabled.
2.  **Session-Only Scopes (`SESSION_REAL`):**
    Subagents are created dynamically during the active execution turn. They do not survive workspace reloads, runtime container restarts, or session restarts. Once the parent conversation turns idle or resets, the subagent instances are destroyed. They cannot maintain independent state across days or weeks.
3.  **Manual / Tool-Assisted Routing:**
    There is no autonomous routing daemon or system dispatcher. The orchestrator (or the parent agent) must manually call `define_subagent` and `invoke_subagent` to trigger reviews. If a developer submits a proposal, the system cannot automatically choose the right squad without explicit pre-programmed rules.

---

## 2. Observed Failure Modes

### A. Quota Exhaustion (`RESOURCE_EXHAUSTED` Crashes)
During Run 3 (Android Timing Review), the `qa_evidence_agent` crashed on step 114 with a `RESOURCE_EXHAUSTED` error.
- **Cause:** Invoking multiple subagents sequentially or running complex files (like reading multi-thousand line files) consumes massive amounts of the shared API rate limits.
- **Impact:** The crash left the technical review without QA verification.
- **Mitigation:** The Orchestrator must enforce strict Word Count limits (e.g., max 200 words per response) and prevent subagents from recursively reading files or launching their own subagents.

### B. Client-First Clock Tampering (Daily Streaks)
Standard daily streaks assume client-side time measurements or local system clocks.
- **Vulnerability:** A user can easily cheat the daily streak by manually altering their device's local clock/timezone settings.
- **Impact:** Validating streaks offline without server authority makes consistency metrics highly gameable, corrupting baseline statistics.

### C. Over-Calibration and Volatility traps (Consistency Ratio)
The formula for consistency relies on comparing standard deviations directly.
- **Failure Vector:** If a user is highly precise, a tiny deviation causes a massive score reduction. Conversely, a noisy user is never penalized for minor jitter. This creates an unfair measurement paradigm that penalizes high performers.
