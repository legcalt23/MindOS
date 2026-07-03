# SessionEnvironmentSnapshot

## 1. Purpose
Per-session hardware/software/network state captured at session time. Never stored as player-level fixed property.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
data class SessionEnvironmentSnapshot(deviceClass: String, inputClass: String, fpsBand: Int, refreshRateBand: Int, screenSizeBand: String, latencyMs: Long, jitterMs: Long, networkInstability: Double, languageGroup: String, accessibilityClass: String, clientVersion: String, osVersion: String, appBuild: String, timestampMs: Long)

## 6. Data Model / Interfaces
data class SessionEnvironmentSnapshot(deviceClass: String, inputClass: String, fpsBand: Int, refreshRateBand: Int, screenSizeBand: String, latencyMs: Long, jitterMs: Long, networkInstability: Double, languageGroup: String, accessibilityClass: String, clientVersion: String, osVersion: String, appBuild: String, timestampMs: Long)

## 7. Implementation Steps
1. Create Kotlin data class for SessionEnvironmentSnapshot.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
SessionEnvironmentSnapshot compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for SessionEnvironmentSnapshot.

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
SessionEnvironmentSnapshot implemented, tested, serialization verified.
