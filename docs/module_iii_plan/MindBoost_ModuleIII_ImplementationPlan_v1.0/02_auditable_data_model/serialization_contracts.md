# Serialization Contracts

## 1. Purpose
All data classes must be serializable to JSON for simulation export and audit log storage. Use kotlinx.serialization with explicit field names.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
kotlinx.serialization annotations.

## 6. Data Model / Interfaces
kotlinx.serialization annotations.

## 7. Implementation Steps
1. Create Kotlin data class for Serialization Contracts.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
Serialization Contracts compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for Serialization Contracts.

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
Serialization Contracts implemented, tested, serialization verified.
