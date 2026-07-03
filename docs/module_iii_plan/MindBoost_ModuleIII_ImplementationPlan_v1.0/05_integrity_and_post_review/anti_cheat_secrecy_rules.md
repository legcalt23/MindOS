# Anti-Cheat Secrecy Rules

## 1. Purpose
User-facing copy MUST NOT reveal: AGRS thresholds, canary triggers, entropy algorithms, collusion graph structure, solver detection methods, smurf scoring, boosting geo-velocity thresholds.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
SecrecyPolicy.md

## 6. Data Model / Interfaces
SecrecyPolicy.md

## 7. Implementation Steps
1. Implement Anti-Cheat Secrecy Rules.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
Anti-Cheat Secrecy Rules correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for Anti-Cheat Secrecy Rules.

## 10. Dependencies
Phase 02, Phase 03.

## 11. Risks
False positives; secrecy leaks.

## 12. Rollback / Failure Handling
Fix detection logic. Review secrecy compliance.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Anti-Cheat Secrecy Rules implemented and tested.
