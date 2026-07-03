# ObservedTaskPerformance

## 1. Purpose
Captures the objective, raw performance score from the cognitive motor. MUST NOT include fatigue, readiness, self-report, or metacognition.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun evaluateObservedTaskPerformance(rawInputs: List<RawInput>, motorId: String): Double

## 6. Data Model / Interfaces
fun evaluateObservedTaskPerformance(rawInputs: List<RawInput>, motorId: String): Double

## 7. Implementation Steps
1. Implement ObservedTaskPerformance.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
ObservedTaskPerformance passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for ObservedTaskPerformance.

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
ObservedTaskPerformance implemented, tested, guards verified.
