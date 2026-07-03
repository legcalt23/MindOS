# Session Generator

## 1. Purpose
Generate complete SessionResult objects including: motor selection, difficulty, observed performance (from true skill + noise), environment snapshot, all decision objects.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun generateSession(player: SyntheticPlayer, opponent: SyntheticPlayer?, config: SimConfig, seed: Long): SessionResult

## 6. Data Model / Interfaces
fun generateSession(player: SyntheticPlayer, opponent: SyntheticPlayer?, config: SimConfig, seed: Long): SessionResult

## 7. Implementation Steps
1. Implement Session Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Session Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Session Generator.

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
Session Generator implemented and reproducible.
