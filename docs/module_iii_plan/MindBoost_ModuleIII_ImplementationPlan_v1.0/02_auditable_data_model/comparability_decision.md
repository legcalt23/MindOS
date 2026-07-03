# ComparabilityDecision

## 1. Purpose
Auditable comparability decision with per-dimension reason codes.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
Fields: comparabilityState (COMPARABLE/PARTIALLY_COMPARABLE/NOT_COMPARABLE), updateMultiplier, deviceReasonCode, latencyReasonCode, fpsReasonCode, languageReasonCode, accessibilityReasonCode, internalAuditReason, publicExplanationCode, auditLogReference.

## 6. Data Model / Interfaces
Fields: comparabilityState (COMPARABLE/PARTIALLY_COMPARABLE/NOT_COMPARABLE), updateMultiplier, deviceReasonCode, latencyReasonCode, fpsReasonCode, languageReasonCode, accessibilityReasonCode, internalAuditReason, publicExplanationCode, auditLogReference.

## 7. Implementation Steps
1. Create Kotlin data class for ComparabilityDecision.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
ComparabilityDecision compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for ComparabilityDecision.

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
ComparabilityDecision implemented, tested, serialization verified.
