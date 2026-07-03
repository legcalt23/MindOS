# Review Required Workflow

## 1. Purpose
When gateState = REVIEW_REQUIRED: freeze rating, create PostReviewCase, assign to queue, notify user with safe copy.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
Workflow steps.

## 6. Data Model / Interfaces
Workflow steps.

## 7. Implementation Steps
1. Implement Review Required Workflow.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
Review Required Workflow correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for Review Required Workflow.

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
Review Required Workflow implemented and tested.
