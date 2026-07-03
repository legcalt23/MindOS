# Latency/Jitter Generator

## 1. Purpose
Generate latency and jitter per session from distributions correlated with device class. High-end = low latency. Add abuse profiles for lag switching.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun generateLatency(deviceClass: DeviceClass, abuseProfile: AbuseProfile, seed: Long): Pair<Long, Long>

## 6. Data Model / Interfaces
fun generateLatency(deviceClass: DeviceClass, abuseProfile: AbuseProfile, seed: Long): Pair<Long, Long>

## 7. Implementation Steps
1. Implement Latency/Jitter Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Latency/Jitter Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Latency/Jitter Generator.

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
Latency/Jitter Generator implemented and reproducible.
