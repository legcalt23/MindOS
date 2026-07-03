# Simulation Output Format

## 1. Purpose
Output: player_ratings.csv, session_log.jsonl, acceptance_metrics.json, abuse_detection.json, dashboard.html.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
Output schema.

## 6. Data Model / Interfaces
Output schema.

## 7. Implementation Steps
1. Implement Simulation Output Format.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Simulation Output Format produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Simulation Output Format.

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
Simulation Output Format implemented and reproducible.
