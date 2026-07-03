# Appeal Allowed Logic

## 1. Purpose
Appeals allowed for: INVALIDATED, INTEGRITY_HOLD resolutions. Rate-limited to prevent abuse. 7-day window.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
fun isAppealAllowed(case: PostReviewCase): Boolean

## 6. Data Model / Interfaces
fun isAppealAllowed(case: PostReviewCase): Boolean

## 7. Implementation Steps
1. Implement Appeal Allowed Logic.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
Appeal Allowed Logic correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for Appeal Allowed Logic.

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
Appeal Allowed Logic implemented and tested.
