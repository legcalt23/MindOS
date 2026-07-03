# Queue Expansion

## 1. Purpose
Narrow (0-15s) -> Gradual (15-45s) -> Wide (45-120s) -> Timeout. Only expand rating range, NEVER cross hard boundaries.

## 2. Scope
Phase 10 Matchmaking & Trials.

## 3. Non-Goals
Production matchmaking.

## 4. Inputs
Phases 03-05.

## 5. Outputs
QueueExpansion.kt

## 6. Data Model / Interfaces
QueueExpansion.kt

## 7. Implementation Steps
1. Implement Queue Expansion in simulation.
2. Test with synthetic queues.

## 8. Acceptance Criteria
Queue Expansion behaves correctly in simulation.

## 9. Tests
Synthetic queue test for Queue Expansion.

## 10. Dependencies
Phases 03-05.

## 11. Risks
Queue logic error.

## 12. Rollback / Failure Handling
Fix logic. Re-run.

## 13. Owner
Backend Lead

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
Queue Expansion implemented and tested in simulation.
