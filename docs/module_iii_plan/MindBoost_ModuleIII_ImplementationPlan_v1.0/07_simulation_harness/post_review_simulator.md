# Post-Review Simulator

## 1. Purpose
Simulate review queue: sessions enter review -> random resolution -> rating applied or frozen.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun simulatePostReview(cases: List<PostReviewCase>): List<PostReviewCase>

## 6. Data Model / Interfaces
fun simulatePostReview(cases: List<PostReviewCase>): List<PostReviewCase>

## 7. Implementation Steps
1. Implement Post-Review Simulator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Post-Review Simulator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Post-Review Simulator.

## 10. Dependencies
Phases 02-06.

## 11. Risks
Simulator bugs producing invalid metrics.

## 12. Rollback / Failure Handling
Fix bug. Re-run with same seed.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Post-Review Simulator implemented and reproducible.
