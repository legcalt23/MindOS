# Simulation CLI

## 1. Purpose
Command-line interface: `./simulate --seed 42 --sessions 100000 --players 10000 --seasons 3 --output ./results`

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
CLI argument parsing.

## 6. Data Model / Interfaces
CLI argument parsing.

## 7. Implementation Steps
1. Implement Simulation CLI.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Simulation CLI produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Simulation CLI.

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
Simulation CLI implemented and reproducible.
