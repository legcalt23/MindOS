# MET_02 — Rating Inflation Per Season

## 1. Purpose
Define and track rating inflation per season.

## 2. Scope
Phase 09 Acceptance Metrics Dashboard.

## 3. Non-Goals
Production monitoring.

## 4. Inputs
Phase 08 Simulation Test outputs.

## 5. Outputs
Computed value of Rating Inflation Per Season.

## 6. Data Model / Interfaces
Definition: Global mu shift across one simulated season.
Target: ± 1%
Warning: ± 3%
Failure: ± 5%

## 7. Implementation Steps
1. Extract raw data from simulation output.
2. Compute Rating Inflation Per Season.
3. Compare against thresholds.
4. Render on dashboard.

## 8. Acceptance Criteria
Value meets target threshold: ± 1%.

## 9. Tests
Automated threshold check for MET_02.

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
MET_02 computed and passing target threshold.
