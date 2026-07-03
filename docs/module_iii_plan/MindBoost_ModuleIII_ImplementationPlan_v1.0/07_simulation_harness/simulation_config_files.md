# Simulation Config Files

## 1. Purpose
YAML/JSON config files for: population proportions, skill distributions, device classes, abuse profiles, rating parameters, thresholds.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
Config file format.

## 6. Data Model / Interfaces
Config file format.

## 7. Implementation Steps
1. Implement Simulation Config Files.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Simulation Config Files produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Simulation Config Files.

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
Simulation Config Files implemented and reproducible.
