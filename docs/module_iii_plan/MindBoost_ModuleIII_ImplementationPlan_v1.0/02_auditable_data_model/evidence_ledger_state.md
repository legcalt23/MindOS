# EvidenceLedgerState

## 1. Purpose
Tracks the approval state of a motor's Evidence Ledger.

## 2. Scope
Phase 02 Auditable Data Model.

## 3. Non-Goals
Production database deployment.

## 4. Inputs
Module III v1.1.1 Data Model specification.

## 5. Outputs
Fields: motorId, version, ledgerState (DRAFT/UNDER_REVIEW/APPROVED_CURRENT/EXPIRED/REVOKED), approvedDate, expiryDate, signOffs, rollbackPolicy, revalidationTriggers.

## 6. Data Model / Interfaces
Fields: motorId, version, ledgerState (DRAFT/UNDER_REVIEW/APPROVED_CURRENT/EXPIRED/REVOKED), approvedDate, expiryDate, signOffs, rollbackPolicy, revalidationTriggers.

## 7. Implementation Steps
1. Create Kotlin data class for EvidenceLedgerState.
2. Add serialization annotations.
3. Write unit tests.
4. Document in this file.

## 8. Acceptance Criteria
EvidenceLedgerState compiles, serializes, and deserializes correctly.

## 9. Tests
Unit test: serialize/deserialize roundtrip for EvidenceLedgerState.

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
EvidenceLedgerState implemented, tested, serialization verified.
