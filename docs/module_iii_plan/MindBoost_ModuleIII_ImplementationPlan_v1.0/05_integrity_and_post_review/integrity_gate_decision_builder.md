# IntegrityGateDecision Builder

## 1. Purpose
Constructs IntegrityGateDecision from multiple signals (AGRS, smurf, boosting, collusion, solver). If ANY signal triggers, gateState != OPEN.

## 2. Scope
Phase 05 Integrity & Post-Review.

## 3. Non-Goals
Live integrity operations.

## 4. Inputs
Phase 02 Data Model, Phase 03 Rating Core.

## 5. Outputs
fun buildIntegrityGate(signals: IntegritySignals): IntegrityGateDecision

## 6. Data Model / Interfaces
fun buildIntegrityGate(signals: IntegritySignals): IntegrityGateDecision

## 7. Implementation Steps
1. Implement IntegrityGateDecision Builder.
2. Test with synthetic abuse scenarios.
3. Verify audit trail.

## 8. Acceptance Criteria
IntegrityGateDecision Builder correctly handles synthetic integrity scenarios.

## 9. Tests
Synthetic abuse test for IntegrityGateDecision Builder.

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
IntegrityGateDecision Builder implemented and tested.
