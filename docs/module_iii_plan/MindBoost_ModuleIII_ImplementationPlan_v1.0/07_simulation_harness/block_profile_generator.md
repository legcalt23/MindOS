# Block Profile Generator

## 1. Purpose
Generate multi-block profiles: BALANCED (all blocks similar), SPECIALIST (one block very high), WEAK_LINK (one block very low), RANDOM.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
enum class BlockProfileType

## 6. Data Model / Interfaces
enum class BlockProfileType

## 7. Implementation Steps
1. Implement Block Profile Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Block Profile Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Block Profile Generator.

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
Block Profile Generator implemented and reproducible.
