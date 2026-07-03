# Integrity Abuse Generator

## 1. Purpose
Generate abuse profiles: CLEAN (90%), SMURF (3%), BOOSTER (2%), COLLUDER (2%), LAG_SWITCHER (1%), CV_SOLVER (1%), ACCOUNT_SHARER (1%).

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun assignAbuseProfile(proportions: Map<AbuseType, Double>, seed: Long): AbuseProfile

## 6. Data Model / Interfaces
fun assignAbuseProfile(proportions: Map<AbuseType, Double>, seed: Long): AbuseProfile

## 7. Implementation Steps
1. Implement Integrity Abuse Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Integrity Abuse Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Integrity Abuse Generator.

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
Integrity Abuse Generator implemented and reproducible.
