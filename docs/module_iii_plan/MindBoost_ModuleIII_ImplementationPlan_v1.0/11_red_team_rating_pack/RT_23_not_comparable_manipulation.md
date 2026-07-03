# RT_23 — Not Comparable Manipulation

## 1. Purpose
Threat model for not comparable manipulation.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Induce lag to avoid rating loss.
Impacted: ComparabilityDecision
Detection: Pattern detection
Prevention: NOT_COMP both sides
Expected: K=0

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-16.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-16 passes. Mitigation effective.

## 9. Tests
Covered by SIM-16.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated not comparable manipulation could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for not comparable manipulation.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_23 documented with effective mitigation.
