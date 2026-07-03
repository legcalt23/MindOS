# Skill Distribution Generator

## 1. Purpose
Generate skill (true mu) from configurable distributions. Default: N(1500, 300) per block.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun generateSkill(distribution: DistConfig, seed: Long): Double

## 6. Data Model / Interfaces
fun generateSkill(distribution: DistConfig, seed: Long): Double

## 7. Implementation Steps
1. Implement Skill Distribution Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Skill Distribution Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Skill Distribution Generator.

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
Skill Distribution Generator implemented and reproducible.
