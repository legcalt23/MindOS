# BlendingConfig

## 1. Purpose
Versioned configuration that determines the blend ratio between TaskPerformanceDelta and MatchOutcomeDelta per mode. In pure task-based modes, match weight = 0. In PvP, blend is mode-specific and auditable.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
data class BlendingConfig(version: String, taskWeight: Double, matchWeight: Double)

## 6. Data Model / Interfaces
data class BlendingConfig(version: String, taskWeight: Double, matchWeight: Double)

## 7. Implementation Steps
1. Implement BlendingConfig.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
BlendingConfig passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for BlendingConfig.

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
BlendingConfig implemented, tested, guards verified.
