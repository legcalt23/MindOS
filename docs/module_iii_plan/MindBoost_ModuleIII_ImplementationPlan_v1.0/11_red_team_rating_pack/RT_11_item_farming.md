# RT_11 — Item Farming

## 1. Purpose
Threat model for item farming.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Memorize/record items for advantage.
Impacted: ExpectedTaskPerformance
Detection: Exposure cap
Prevention: Procedural generation
Expected: Performance drops

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-11.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-11 passes. Mitigation effective.

## 9. Tests
Covered by SIM-11.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated item farming could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for item farming.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_11 documented with effective mitigation.
