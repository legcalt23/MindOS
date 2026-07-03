# Rollback After Review

## 1. Purpose
When a session is confirmed invalid after review, rollback the rating delta and create RollbackReference.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
fun rollbackAfterReview(caseId: String): RollbackReference

## 6. Data Model / Interfaces
fun rollbackAfterReview(caseId: String): RollbackReference

## 7. Implementation Steps
1. Implement Rollback After Review.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
Rollback After Review correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for Rollback After Review.

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
Rollback After Review implemented and tested.
