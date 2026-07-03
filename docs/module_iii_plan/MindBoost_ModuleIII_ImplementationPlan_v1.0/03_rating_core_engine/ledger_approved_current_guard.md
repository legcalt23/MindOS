# Ledger-Approved-Current Guard

## 1. Purpose
Prevents rating updates for motors whose Evidence Ledger is not APPROVED_CURRENT.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
if (ledger.status != APPROVED_CURRENT) return 0.0

## 6. Data Model / Interfaces
if (ledger.status != APPROVED_CURRENT) return 0.0

## 7. Implementation Steps
1. Implement Ledger-Approved-Current Guard.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
Ledger-Approved-Current Guard passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for Ledger-Approved-Current Guard.

## 10. Dependencies
Phase 02 Data Model.

## 11. Risks
Mathematical error propagation.

## 12. Rollback / Failure Handling
Fix formula. Re-run simulation.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Ledger-Approved-Current Guard implemented, tested, guards verified.
