# Ledger Expiry Logic

## 1. Purpose
Ledgers expire after a configurable period (e.g., 6 months). Expired ledger = motor falls to Training Only.

## 2. Scope
Phase 06 Evidence Ledger Engine.

## 3. Non-Goals
Production motor validation.

## 4. Inputs
Phase 02 Data Model, Module III v1.1.1 Evidence Ledgers.

## 5. Outputs
fun checkLedgerExpiry(ledger: EvidenceLedgerState): Boolean

## 6. Data Model / Interfaces
fun checkLedgerExpiry(ledger: EvidenceLedgerState): Boolean

## 7. Implementation Steps
1. Implement Ledger Expiry Logic.
2. Test with mock ledger data.
3. Verify gate blocks unapproved motors.

## 8. Acceptance Criteria
Ledger Expiry Logic correctly gates motor eligibility.

## 9. Tests
Mock ledger test for Ledger Expiry Logic.

## 10. Dependencies
Phase 02 Data Model.

## 11. Risks
Incomplete ledger allowing unvalidated motor.

## 12. Rollback / Failure Handling
Block motor. Fall to Training Only.

## 13. Owner
Science Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Ledger Expiry Logic implemented and tested.
