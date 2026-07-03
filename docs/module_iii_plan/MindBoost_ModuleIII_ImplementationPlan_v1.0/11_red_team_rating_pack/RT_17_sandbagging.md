# RT_17 — Sandbagging

## 1. Purpose
Threat model for sandbagging.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Lose deliberately to lower rank.
Impacted: Non-Compensatory
Detection: Mu volatility
Prevention: Soft-min rank drop
Expected: Badge only

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-22.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-22 passes. Mitigation effective.

## 9. Tests
Covered by SIM-22.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated sandbagging could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for sandbagging.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_17 documented with effective mitigation.
