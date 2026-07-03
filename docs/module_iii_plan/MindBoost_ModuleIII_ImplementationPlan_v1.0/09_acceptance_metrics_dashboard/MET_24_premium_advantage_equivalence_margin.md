# MET_24 — Premium Advantage Equivalence Margin

## 1. Purpose
Define and track premium advantage equivalence margin.

## 2. Scope
Phase 09 Acceptance Metrics Dashboard.

## 3. Non-Goals
Production monitoring.

## 4. Inputs
Phase 08 Simulation Test outputs.

## 5. Outputs
Computed value of Premium Advantage Equivalence Margin.

## 6. Data Model / Interfaces
Definition: 95% CI for Premium vs Free win-rate delta. Pass if within ±0.5pp. Absence of significance is NOT sufficient.
Target: ±0.5 pp
Warning: ±1.0 pp
Failure: ±2.0 pp

## 7. Implementation Steps
1. Extract raw data from simulation output.
2. Compute Premium Advantage Equivalence Margin.
3. Compare against thresholds.
4. Render on dashboard.

## 8. Acceptance Criteria
Value meets target threshold: ±0.5 pp.

## 9. Tests
Automated threshold check for MET_24.

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
MET_24 computed and passing target threshold.
