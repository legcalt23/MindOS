# MET_23 — Non-Compensatory Rank Correctness

## 1. Purpose
Define and track non-compensatory rank correctness.

## 2. Scope
Phase 09 Acceptance Metrics Dashboard.

## 3. Non-Goals
Production monitoring.

## 4. Inputs
Phase 08 Simulation Test outputs.

## 5. Outputs
Computed value of Non-Compensatory Rank Correctness.

## 6. Data Model / Interfaces
Definition: Unifocal players incorrectly reaching Elite.
Target: 0%
Warning: > 0.1%
Failure: > 1%

## 7. Implementation Steps
1. Extract raw data from simulation output.
2. Compute Non-Compensatory Rank Correctness.
3. Compare against thresholds.
4. Render on dashboard.

## 8. Acceptance Criteria
Value meets target threshold: 0%.

## 9. Tests
Automated threshold check for MET_23.

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
MET_23 computed and passing target threshold.
