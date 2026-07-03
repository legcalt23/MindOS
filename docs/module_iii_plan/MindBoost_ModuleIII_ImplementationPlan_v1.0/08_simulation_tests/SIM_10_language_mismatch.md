# SIM_10 — Language Mismatch

## 1. Purpose
Validate language mismatch behavior in synthetic simulation.

## 2. Scope
Phase 08 Simulation Tests.

## 3. Non-Goals
Production testing.

## 4. Inputs
Phase 07 Simulation Harness output.

## 5. Outputs
Simulation results for Language Mismatch.

## 6. Data Model / Interfaces
SessionResult records, PlayerRatingState snapshots.

## 7. Implementation Steps
1. Configure simulation for Language Mismatch scenario.
2. Run 100k sessions.
3. Extract metrics.
4. Evaluate pass/fail.

## 8. Acceptance Criteria
Language bias residual < 0.05 sigmas.

## 9. Tests
Automated assertion in simulation harness for SIM_10.

## 10. Dependencies
Phase 07 Simulation Harness.

## 11. Risks
Incorrect synthetic data leading to false pass.

## 12. Rollback / Failure Handling
Fix generator. Re-run with corrected data.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_LIVE_SHADOW_RANKED

## 15. Definition of Done
SIM_10 passes with documented evidence.
