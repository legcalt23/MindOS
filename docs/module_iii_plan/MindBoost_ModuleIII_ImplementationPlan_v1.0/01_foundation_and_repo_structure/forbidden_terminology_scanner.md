# Forbidden Terminology Scanner

## 1. Purpose
Automated scanner that fails CI if any source file, string resource, or documentation contains: ELO, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out, 'gana dinero', 'cerebro élite', 'debilidad mental'.

## 2. Scope
Phase 01 Foundation.

## 3. Non-Goals
Production deployment. User-facing features.

## 4. Inputs
Module III v1.1.1 specification.

## 5. Outputs
TerminologyScanner.kt + CI integration

## 6. Data Model / Interfaces
TerminologyScanner.kt + CI integration

## 7. Implementation Steps
1. Implement Forbidden Terminology Scanner.
2. Add unit test.
3. Integrate into CI.

## 8. Acceptance Criteria
Forbidden Terminology Scanner is implemented and CI-verified.

## 9. Tests
Unit test for Forbidden Terminology Scanner.

## 10. Dependencies
None (foundation phase).

## 11. Risks
Misconfiguration leading to production leak.

## 12. Rollback / Failure Handling
Revert commit. Confirm flags OFF.

## 13. Owner
Engineering Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Forbidden Terminology Scanner implemented, tested, CI-green.
