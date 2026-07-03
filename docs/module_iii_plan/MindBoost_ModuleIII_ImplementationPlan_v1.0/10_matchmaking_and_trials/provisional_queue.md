# Provisional Queue

## 1. Purpose
New players (<10 sessions) matched only with other provisionals. Prevents skill mismatch trauma.

## 2. Scope
Phase 10 Matchmaking & Trials.

## 3. Non-Goals
Production matchmaking.

## 4. Inputs
Phases 03-05.

## 5. Outputs
ProvisionalQueue.kt

## 6. Data Model / Interfaces
ProvisionalQueue.kt

## 7. Implementation Steps
1. Implement Provisional Queue in simulation.
2. Test with synthetic queues.

## 8. Acceptance Criteria
Provisional Queue behaves correctly in simulation.

## 9. Tests
Synthetic queue test for Provisional Queue.

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
Provisional Queue implemented and tested in simulation.
