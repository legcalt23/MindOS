# Blockers Resolved

This document lists the runtime blockers that have been resolved during the M0–M2 runtime hardening.

---

## 1. Blocker 01 (Veto Bypass via Crash)
- **Status:** `RESOLVED`
- **Correction Applied:** Appended fail-safe routing rules to `04_OPERATING_PROTOCOL.md`. The orchestrator must immediately halt and mark the final run status as `INCONCLUSIVE_BLOCKED` if any critical squad member crashes or times out.

## 2. Blocker 02 (Prompt Simulation Fallback)
- **Status:** `RESOLVED`
- **Correction Applied:** Removed the dynamic simulation fallback (`FALLBACK_PROMPT_SIMULATION`) from `SESSION_BOOTSTRAP_REQUIRED.md`. Silent roleplaying bypasses are now strictly forbidden.

## 3. Blocker 03 (Missing Registry File)
- **Status:** `RESOLVED`
- **Correction Applied:** Created `.antigravity-only/agents/agent_definitions_full.md` containing the canonical definitions of the 7 core subagents. Generated schema-conforming manifest structures inside `.antigravity-only/agents/agent_registry.json`.

## 4. Blocker 04 (Manual Verification Only)
- **Status:** `RESOLVED`
- **Correction Applied:** Created `verify_subagents.py` which automates structural verification, placeholder checks, matrices validation, claim citations, and file integrity hashes.

## 5. Blocker 05 (Over-privileged Scope)
- **Status:** `MITIGATED`
- **Correction Applied:** Configured `sandbox_policy.json` establishing strict path denylists (e.g. `.env`, `.pem`, `google-services.json`) and exfiltration controls. Any attempted access to denied paths by subagents is logged as `SECURITY_BLOCKED` and halts execution.
