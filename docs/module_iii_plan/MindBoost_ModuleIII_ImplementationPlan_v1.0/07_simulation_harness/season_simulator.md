# Season Simulator

## 1. Purpose
Simulate multiple seasons: soft-reset, rank decay, sigma increase.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun simulateSeason(players: List<PlayerRatingState>, seasonConfig: SeasonConfig): List<PlayerRatingState>

## 6. Data Model / Interfaces
fun simulateSeason(players: List<PlayerRatingState>, seasonConfig: SeasonConfig): List<PlayerRatingState>

## 7. Implementation Steps
1. Implement Season Simulator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Season Simulator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Season Simulator.

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
Season Simulator implemented and reproducible.
