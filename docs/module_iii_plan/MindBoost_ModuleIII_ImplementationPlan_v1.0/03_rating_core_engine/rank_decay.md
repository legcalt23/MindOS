# RankDecay

## 1. Purpose
Increases sigma gradually during inactivity. Does NOT reduce mu punitively. Decay rate configurable per tier.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun applyRankDecay(player: PlayerRatingState, daysSinceLastSession: Int): PlayerRatingState

## 6. Data Model / Interfaces
fun applyRankDecay(player: PlayerRatingState, daysSinceLastSession: Int): PlayerRatingState

## 7. Implementation Steps
1. Implement RankDecay.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
RankDecay passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for RankDecay.

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
RankDecay implemented, tested, guards verified.
