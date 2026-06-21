# Round 12: Migration & Implementation Plan

This document defines the implementation steps, order of operations, and rollback strategies for the 36-agent system.

## 1. Migration Steps

### Step 1: Active Configuration File Upgrades (under `.antigravity-only/agents/`)
- Update `README.md` to document the 36-agent Extended Council.
- Update `REALITY_STATUS.md` to register the new status.
- Update `ROUTING_MAP.md` mapping task types to the 36 agents.
- Create `FOUNDER_COUNCIL_REVIEW_MODES.md` with active squad constraints.
- Create `FOUNDER_COUNCIL_OUTPUT_TEMPLATE.md` with the V2 template.
- Create `BOOTSTRAP_FOUNDER_COUNCIL.md` with define block and smoke-test protocols.
- Create `FOUNDER_COUNCIL_USAGE_GUIDE.md` with copying templates.
- Create `SESSION_BOOTSTRAP_REQUIRED.md` warning page.

### Step 2: Quarantine Retention
- Ensure the original 20 simulated agents remain in `.antigravity-only/agents/quarantine/` untouched. Do not delete them.

### Step 3: Registration Status
- Declare the runtime status of the 36 subagents as `DESIGNED_NOT_EXECUTED` in active configurations, as registering all 36 in one runtime turn exceeds token-size boundaries.

---

## 2. Rollback Strategy
If the founder wants to restore the original 20 simulated agents:
1. Run the commands in `ROLLBACK.md`.
2. Move the 20 files back from `.antigravity-only/agents/quarantine/` to `.antigravity-only/agents/`.
3. Revert `README.md` and delete the new V2 files using:
   ```powershell
   Remove-Item -Path .antigravity-only/agents/FOUNDER_COUNCIL_REVIEW_MODES.md -Force
   Remove-Item -Path .antigravity-only/agents/FOUNDER_COUNCIL_OUTPUT_TEMPLATE.md -Force
   Remove-Item -Path .antigravity-only/agents/BOOTSTRAP_FOUNDER_COUNCIL.md -Force
   Remove-Item -Path .antigravity-only/agents/FOUNDER_COUNCIL_USAGE_GUIDE.md -Force
   Remove-Item -Path .antigravity-only/agents/SESSION_BOOTSTRAP_REQUIRED.md -Force
   ```
