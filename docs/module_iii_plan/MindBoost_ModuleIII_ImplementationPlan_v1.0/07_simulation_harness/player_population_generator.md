# Player Population Generator

## 1. Purpose
Generate 10k+ synthetic players with: skill distributions (normal, bimodal, uniform), block profiles (balanced, specialist, weak-link), device classes, language groups, integrity profiles (clean, smurf, booster).

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun generatePopulation(config: PopConfig, seed: Long): List<SyntheticPlayer>

## 6. Data Model / Interfaces
fun generatePopulation(config: PopConfig, seed: Long): List<SyntheticPlayer>

## 7. Implementation Steps
1. Implement Player Population Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Player Population Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Player Population Generator.

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
Player Population Generator implemented and reproducible.
