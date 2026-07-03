# Low Population Fallback

## 1. Purpose
After 120s timeout: cancel with fairness-preserving message. Better to cancel than force asymmetric match.

## 2. Scope
Phase 10 Matchmaking & Trials.

## 3. Non-Goals
Production matchmaking.

## 4. Inputs
Phases 03-05.

## 5. Outputs
LowPopFallback.kt

## 6. Data Model / Interfaces
LowPopFallback.kt

## 7. Implementation Steps
1. Implement Low Population Fallback in simulation.
2. Test with synthetic queues.

## 8. Acceptance Criteria
Low Population Fallback behaves correctly in simulation.

## 9. Tests
Synthetic queue test for Low Population Fallback.

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
Low Population Fallback implemented and tested in simulation.
