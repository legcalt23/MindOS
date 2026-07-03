# SEM Conditional Handling

## 1. Purpose
Compute conditional SEM by rating band. Lower SEM = higher confidence = larger update multiplier.

## 2. Scope
Phase 04 Comparability & Measurement Confidence.

## 3. Non-Goals
Production fairness deployment.

## 4. Inputs
Phase 02 Data Model.

## 5. Outputs
fun computeConditionalSEM(ratingBand: String, motorId: String): Double

## 6. Data Model / Interfaces
fun computeConditionalSEM(ratingBand: String, motorId: String): Double

## 7. Implementation Steps
1. Implement SEM Conditional Handling.
2. Unit test with synthetic device/network profiles.
3. Verify audit log.

## 8. Acceptance Criteria
SEM Conditional Handling correctly classifies synthetic sessions.

## 9. Tests
Synthetic test for SEM Conditional Handling.

## 10. Dependencies
Phase 02 Data Model.

## 11. Risks
Incorrect thresholds.

## 12. Rollback / Failure Handling
Adjust thresholds. Re-run simulation.

## 13. Owner
Fairness Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
SEM Conditional Handling implemented and tested.
