# RT_12 — LLM Assistance

## 1. Purpose
Threat model for llm assistance.

## 2. Scope
Phase 11 Red-Team Rating Pack.

## 3. Non-Goals
Production threat response.

## 4. Inputs
Module III v1.1.1 Red-Team Matrix.

## 5. Outputs
Documented threat with mitigation.

## 6. Data Model / Interfaces
Attack: API solves logic tasks.
Impacted: ExpectedTaskPerformance
Detection: Supra-human latency
Prevention: Solver Resistance
Expected: INTEGRITY_HOLD

## 7. Implementation Steps
1. Document threat.
2. Map to simulation test SIM-05.
3. Verify mitigation.

## 8. Acceptance Criteria
Simulation test SIM-05 passes. Mitigation effective.

## 9. Tests
Covered by SIM-05.

## 10. Dependencies
Phases 03-07.

## 11. Risks
Unmitigated llm assistance could be exploited.

## 12. Rollback / Failure Handling
Implement additional detection for llm assistance.

## 13. Owner
Integrity Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
RT_12 documented with effective mitigation.
