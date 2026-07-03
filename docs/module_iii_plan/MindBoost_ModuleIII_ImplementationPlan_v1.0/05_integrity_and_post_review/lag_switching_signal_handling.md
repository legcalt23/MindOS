# Lag Switching Signal Handling

## 1. Purpose
Detection: sudden jitter spike during losing position. Response: NOT_COMPARABLE for the session.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
LagSwitchDetector.kt

## 6. Data Model / Interfaces
LagSwitchDetector.kt

## 7. Implementation Steps
1. Implement Lag Switching Signal Handling.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
Lag Switching Signal Handling correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for Lag Switching Signal Handling.

## 10. Dependencies
Phase 02, Phase 03.

## 11. Risks
False positives; secrecy leaks.

## 12. Rollback / Failure Handling
Fix detection logic. Review secrecy compliance.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Lag Switching Signal Handling implemented and tested.
