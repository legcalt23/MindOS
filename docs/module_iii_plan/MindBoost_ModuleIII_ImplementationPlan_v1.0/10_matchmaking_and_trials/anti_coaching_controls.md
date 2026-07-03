# Anti-Coaching Controls

## 1. Purpose
No repeated trial item family. Suspicious pause patterns trigger INTEGRITY_HOLD. Trial structure not disclosed publicly.

## 2. Scope
Phase 10 Matchmaking & Trials.

## 3. Non-Goals
Production matchmaking.

## 4. Inputs
Phases 03-05.

## 5. Outputs
AntiCoaching.kt

## 6. Data Model / Interfaces
AntiCoaching.kt

## 7. Implementation Steps
1. Implement Anti-Coaching Controls in simulation.
2. Test with synthetic queues.

## 8. Acceptance Criteria
Anti-Coaching Controls behaves correctly in simulation.

## 9. Tests
Synthetic queue test for Anti-Coaching Controls.

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
Anti-Coaching Controls implemented and tested in simulation.
