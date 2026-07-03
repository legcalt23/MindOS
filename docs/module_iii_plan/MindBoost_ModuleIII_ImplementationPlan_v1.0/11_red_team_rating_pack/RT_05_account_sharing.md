# RT_05 — Account Sharing

## 1. Purpose
Threat model for account sharing.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: Multiple people use one account.
Impacted: SessionResult
Detection: Input cadence, Device ID
Prevention: Device Verification
Expected: INTEGRITY_HOLD

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-07.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-07 passes. Mitigation effective.

## 9. Tests
Covered by SIM-07.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated account sharing could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for account sharing.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_05 documented with effective mitigation.
