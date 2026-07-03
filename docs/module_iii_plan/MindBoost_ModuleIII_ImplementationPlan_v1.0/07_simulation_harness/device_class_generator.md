# Device Class Generator

## 1. Purpose
Assign device classes with configurable proportions. Default: 60% LOW_END, 30% MID, 10% HIGH_END.

## 2. Scope
Phase 07 Simulation Harness.

## 3. Non-Goals
Production simulation.

## 4. Inputs
Phases 02-06.

## 5. Outputs
fun assignDeviceClass(proportions: Map<DeviceClass, Double>, seed: Long): DeviceClass

## 6. Data Model / Interfaces
fun assignDeviceClass(proportions: Map<DeviceClass, Double>, seed: Long): DeviceClass

## 7. Implementation Steps
1. Implement Device Class Generator.
2. Test with small synthetic runs.
3. Scale to 100k.

## 8. Acceptance Criteria
Device Class Generator produces reproducible results with deterministic seeds.

## 9. Tests
Deterministic seed test for Device Class Generator.

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
Device Class Generator implemented and reproducible.
