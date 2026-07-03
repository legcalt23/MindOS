# Rollback Simulator

## 1. Purpose
Simulate rollback scenarios: invalidated session -> rating reversed.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun simulateRollback(session: SessionResult, player: PlayerRatingState): PlayerRatingState

## 6. Data Model / Interfaces
fun simulateRollback(session: SessionResult, player: PlayerRatingState): PlayerRatingState

## 7. Implementation Steps
1. Implement Rollback Simulator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Rollback Simulator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Rollback Simulator.

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
Rollback Simulator implemented and reproducible.
