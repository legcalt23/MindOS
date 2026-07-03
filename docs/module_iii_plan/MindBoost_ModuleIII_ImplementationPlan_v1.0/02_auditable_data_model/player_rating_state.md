# PlayerRatingState

## 1. Purpose
Core player rating object. Contains pseudonymized ID, global mu/sigma, block ratings map, mode ratings map, provisional/activity/verification/integrity/minor state, default profile, season ID.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
data class PlayerRatingState(playerIdPseudonymized: String, globalRatingMu: Double, globalRatingSigma: Double, blockRatings: Map<String, BlockRatingState>, modeRatings: Map<String, ModeRatingState>, provisionalState: Boolean, activityState: ActivityStateEnum, verificationState: VerificationStateEnum, integrityState: IntegrityStateEnum, minorState: MinorStateEnum, regionState: String, defaultProfile: PlayerDefaultProfile, lastRatingUpdateTimestampMs: Long, seasonId: String)

## 6. Data Model / Interfaces
data class PlayerRatingState(playerIdPseudonymized: String, globalRatingMu: Double, globalRatingSigma: Double, blockRatings: Map<String, BlockRatingState>, modeRatings: Map<String, ModeRatingState>, provisionalState: Boolean, activityState: ActivityStateEnum, verificationState: VerificationStateEnum, integrityState: IntegrityStateEnum, minorState: MinorStateEnum, regionState: String, defaultProfile: PlayerDefaultProfile, lastRatingUpdateTimestampMs: Long, seasonId: String)

## 7. Implementation Steps
1. Create Kotlin data class for PlayerRatingState.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
PlayerRatingState compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for PlayerRatingState.

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
PlayerRatingState implemented, tested, serialization verified.
