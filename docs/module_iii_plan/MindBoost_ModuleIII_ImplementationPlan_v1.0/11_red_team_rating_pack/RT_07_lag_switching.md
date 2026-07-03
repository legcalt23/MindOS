# RT_07 — Lag Switching

## 1. Purpose
Threat model for lag switching.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Induce lag when losing.
Impacted: ComparabilityDecision
Detection: Jitter spike
Prevention: NOT_COMPARABLE
Expected: K=0

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-08.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-08 passes. Mitigation effective.

## 9. Tests
Covered by SIM-08.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated lag switching could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for lag switching.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_07 documented with effective mitigation.
