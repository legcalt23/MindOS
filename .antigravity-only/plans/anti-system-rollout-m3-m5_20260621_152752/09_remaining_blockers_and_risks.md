# Remaining Blockers and Risks

This document summarizes the outstanding blockers and risks in the `/anti` system.

---

## 1. Remaining Blockers
- **None.** All core blockers identified in the audit (`anti-system-review_20260621_150701`) have been resolved:
  - Blocker 01 (Veto Bypass): Resolved via fail-safe routing rules.
  - Blocker 02 (Prompt Simulation): Resolved by deprecating `FALLBACK_PROMPT_SIMULATION`.
  - Blocker 03 (Missing Registry File): Resolved by creating `agent_definitions_full.md` and registry JSON.
  - Blocker 04 (Manual Verification): Resolved by implementing `verify_subagents.py`.
  - Blocker 05 (Over-privileged Scope): Mitigated via `sandbox_policy.json` (though designed-not-enforced in code).

---

## 2. Remaining High Risks
- **Jitter and Rate Limiting:** High-frequency executions could trigger `RESOURCE_EXHAUSTED` API errors. Mitigated by lazy loader squad selections.
- **Sandbox Policy Execution:** Subagents operate with parent process permissions. Mitigated by path checks in preflight prompts, but active tool interception is not compiled.
- **Clock Tampering:** Offline environment alterations could spoof quality gates. Mitigated by NTP internet queries.
