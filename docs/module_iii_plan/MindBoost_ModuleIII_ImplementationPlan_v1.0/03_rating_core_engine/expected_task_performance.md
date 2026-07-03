# ExpectedTaskPerformance

## 1. Purpose
Calculates expected performance of a player against a task/motor difficulty. Formula: f(player_block_mu, task_context_difficulty, motor_model, evidence_ledger_status). This is independent of any opponent.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun calculateExpectedTaskPerformance(blockMu: Double, taskDifficulty: Double, motorModel: String): Double

## 6. Data Model / Interfaces
fun calculateExpectedTaskPerformance(blockMu: Double, taskDifficulty: Double, motorModel: String): Double

## 7. Implementation Steps
1. Implement ExpectedTaskPerformance.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
ExpectedTaskPerformance passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for ExpectedTaskPerformance.

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
ExpectedTaskPerformance implemented, tested, guards verified.
