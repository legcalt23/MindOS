# ModeRatingState

## 1. Purpose
Per-mode rating with isolation and contamination risk tracking via enums.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
data class ModeRatingState(modeId: String, mu: Double, sigma: Double, sampleSize: Int, measurementConfidence: Double, lastUpdatedTimestampMs: Long, eligibilityState: MetricEligibilityStateEnum, driftState: DriftStateEnum, isolationState: IsolationState, contaminationRiskState: ContaminationRiskState, modeConfigVersion: String, lastSimulationValidatedVersion: String)

## 6. Data Model / Interfaces
data class ModeRatingState(modeId: String, mu: Double, sigma: Double, sampleSize: Int, measurementConfidence: Double, lastUpdatedTimestampMs: Long, eligibilityState: MetricEligibilityStateEnum, driftState: DriftStateEnum, isolationState: IsolationState, contaminationRiskState: ContaminationRiskState, modeConfigVersion: String, lastSimulationValidatedVersion: String)

## 7. Implementation Steps
1. Create Kotlin data class for ModeRatingState.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
ModeRatingState compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for ModeRatingState.

## 10. Dependencies
Phase 01 (enums, audit log).

## 11. Risks
Schema changes after simulation begins require migration.

## 12. Rollback / Failure Handling
Revert data class changes.

## 13. Owner
Engineering

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
ModeRatingState implemented, tested, serialization verified.
