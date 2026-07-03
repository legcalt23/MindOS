# MET_01 — Rating Convergence Speed

## 1. Purpose
Define and track rating convergence speed.

## 2. Scope
Phase 09 Acceptance Metrics Dashboard.

## 3. Non-Goals
Production monitoring.

## 4. Inputs
Phase 08 Simulation Test outputs.

## 5. Outputs
Computed value of Rating Convergence Speed.

## 6. Data Model / Interfaces
Definition: Sessions needed for mu to stabilize within 5% of true skill.
Target: < 30 sessions
Warning: > 40
Failure: > 60

## 7. Implementation Steps
1. Extract raw data from simulation output.
2. Compute Rating Convergence Speed.
3. Compare against thresholds.
4. Render on dashboard.

## 8. Acceptance Criteria
Value meets target threshold: < 30 sessions.

## 9. Tests
Automated threshold check for MET_01.

## 10. Dependencies
Phase 08 Simulation Tests.

## 11. Risks
Incorrect threshold could prematurely approve.

## 12. Rollback / Failure Handling
Adjust threshold. Re-run simulation.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_LIVE_SHADOW_RANKED

## 15. Definition of Done
MET_01 computed and passing target threshold.
