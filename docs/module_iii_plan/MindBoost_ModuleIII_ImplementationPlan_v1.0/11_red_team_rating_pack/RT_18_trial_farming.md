# RT_18 — Trial Farming

## 1. Purpose
Threat model for trial farming.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Repeated trial attempts to memorize.
Impacted: Ascension Trial
Detection: Exposure cap
Prevention: Item family rotation
Expected: Cooldown enforced

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-24.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-24 passes. Mitigation effective.

## 9. Tests
Covered by SIM-24.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated trial farming could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for trial farming.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_18 documented with effective mitigation.
