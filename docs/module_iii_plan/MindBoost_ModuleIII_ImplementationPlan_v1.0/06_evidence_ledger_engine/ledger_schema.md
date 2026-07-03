# Ledger Schema

## 1. Purpose
Complete schema with all fields from Module III v1.1.1: Motor ID, Version, Cognitive block, Current status, Eligible/Ineligible contexts, Constructs, Non-target variance, Calibration requirements, Min sample sizes, IRT model, IRT fit, TIF, SEM, reliability, Omega, G-Theory, DIF plans, Practice Effect, Holdout Transfer, Item exposure, Solver resistance, Device/Language/Accessibility sensitivity, Revalidation, Rollback, Model Card, Safe copy, Sign-offs, Expiry, Blocking conditions.

## 2. Scope
Phase 06 Evidence Ledger Engine.

## 3. Non-Goals
Production motor validation.

## 4. Inputs
Phase 02 Data Model, Module III v1.1.1 Evidence Ledgers.

## 5. Outputs
EvidenceLedger data class

## 6. Data Model / Interfaces
EvidenceLedger data class

## 7. Implementation Steps
1. Implement Ledger Schema.
2. Test with mock ledger data.
3. Verify gate blocks unapproved motors.

## 8. Acceptance Criteria
Ledger Schema correctly gates motor eligibility.

## 9. Tests
Mock ledger test for Ledger Schema.

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
Ledger Schema implemented and tested.
