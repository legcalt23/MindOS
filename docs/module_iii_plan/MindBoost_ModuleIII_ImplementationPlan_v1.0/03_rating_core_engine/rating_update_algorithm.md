# RatingUpdateAlgorithm

## 1. Purpose
Core function: RatingUpdateDelta = Blend(TaskDelta, MatchDelta, BlendingConfig) × MeasurementConfidenceDecision.updateMultiplier × ComparabilityDecision.updateMultiplier × SessionValidityDecision.updateMultiplier × IntegrityGateDecision.updateMultiplier × ModeScalingFactor. Hard gates: if Integrity != OPEN, return 0. If Validity == NOT_COMPARABLE, return 0. If Confidence == INSUFFICIENT, return 0. If Ledger != APPROVED_CURRENT, return 0.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun calculateRatingUpdate(session: SessionResult): Double

## 6. Data Model / Interfaces
fun calculateRatingUpdate(session: SessionResult): Double

## 7. Implementation Steps
1. Implement RatingUpdateAlgorithm.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
RatingUpdateAlgorithm passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for RatingUpdateAlgorithm.

## 10. Dependencies
Phase 02 Data Model.

## 11. Risks
Mathematical error propagation.

## 12. Rollback / Failure Handling
Fix formula. Re-run simulation.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RatingUpdateAlgorithm implemented, tested, guards verified.
