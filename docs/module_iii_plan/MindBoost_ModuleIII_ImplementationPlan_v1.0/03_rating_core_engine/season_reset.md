# SeasonReset

## 1. Purpose
Soft-reset: mu pushed toward population mean, sigma increased. Preserves relative ordering but compresses distribution.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun applySeasonReset(player: PlayerRatingState, seasonConfig: SeasonConfig): PlayerRatingState

## 6. Data Model / Interfaces
fun applySeasonReset(player: PlayerRatingState, seasonConfig: SeasonConfig): PlayerRatingState

## 7. Implementation Steps
1. Implement SeasonReset.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
SeasonReset passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for SeasonReset.

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
SeasonReset implemented, tested, guards verified.
