# RT_16 — Rollback Abuse

## 1. Purpose
Threat model for rollback abuse.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Exploit appeal system for rollbacks.
Impacted: SessionResult
Detection: High appeal rate
Prevention: Rate limit appeals
Expected: Appeal denied

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-15.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-15 passes. Mitigation effective.

## 9. Tests
Covered by SIM-15.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated rollback abuse could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for rollback abuse.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_16 documented with effective mitigation.
