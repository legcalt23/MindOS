# User-Safe Explanation Mapping

## 1. Purpose
Maps internal reason codes to user-facing copy. MUST NOT reveal thresholds, algorithms, or anti-cheat signals.

## 2. Scope
Phase 04 Comparability & Measurement Confidence.

## 3. Non-Goals
Production fairness deployment.

## 4. Inputs
Phase 02 Data Model.

## 5. Outputs
Map<InternalReasonCode, SafeUserCopy>

## 6. Data Model / Interfaces
Map<InternalReasonCode, SafeUserCopy>

## 7. Implementation Steps
1. Implement User-Safe Explanation Mapping.
2. Unit test with synthetic device/network profiles.
3. Verify audit log.

## 8. Acceptance Criteria
User-Safe Explanation Mapping correctly classifies synthetic sessions.

## 9. Tests
Synthetic test for User-Safe Explanation Mapping.

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
User-Safe Explanation Mapping implemented and tested.
