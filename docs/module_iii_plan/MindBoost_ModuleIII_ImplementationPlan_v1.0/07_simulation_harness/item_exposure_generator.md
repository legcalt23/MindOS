# Item Exposure Generator

## 1. Purpose
Track item exposure counts per player and per population. Trigger retirement when exposure exceeds cap.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun trackItemExposure(playerId: String, itemId: String): Int

## 6. Data Model / Interfaces
fun trackItemExposure(playerId: String, itemId: String): Int

## 7. Implementation Steps
1. Implement Item Exposure Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Item Exposure Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Item Exposure Generator.

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
Item Exposure Generator implemented and reproducible.
