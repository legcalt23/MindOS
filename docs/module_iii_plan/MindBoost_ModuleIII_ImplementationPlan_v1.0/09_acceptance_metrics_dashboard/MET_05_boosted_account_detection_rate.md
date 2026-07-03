# MET_05 — Boosted Account Detection Rate

## 1. Purpose
Define and track boosted account detection rate.

## 2. Scope
Phase 09 Acceptance Metrics Dashboard.

## 3. Non-Goals
Production monitoring.

## 4. Inputs
Phase 08 Simulation Test outputs.

## 5. Outputs
Computed value of Boosted Account Detection Rate.

## 6. Data Model / Interfaces
Definition: Recall of synthetically boosted accounts.
Target: > 95%
Warning: < 90%
Failure: < 80%

## 7. Implementation Steps
1. Extract raw data from simulation output.
2. Compute Boosted Account Detection Rate.
3. Compare against thresholds.
4. Render on dashboard.

## 8. Acceptance Criteria
Value meets target threshold: > 95%.

## 9. Tests
Automated threshold check for MET_05.

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
MET_05 computed and passing target threshold.
