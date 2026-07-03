# Deterministic Seed Support

## 1. Purpose
All random number generation must use seeded PRNGs for reproducibility. Each simulation run has a master seed that derives all sub-seeds.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
Random(seed) everywhere.

## 6. Data Model / Interfaces
Random(seed) everywhere.

## 7. Implementation Steps
1. Implement Deterministic Seed Support.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Deterministic Seed Support produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Deterministic Seed Support.

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
Deterministic Seed Support implemented and reproducible.
