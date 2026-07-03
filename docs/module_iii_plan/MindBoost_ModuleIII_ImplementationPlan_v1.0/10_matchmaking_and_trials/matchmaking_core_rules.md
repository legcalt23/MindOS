# Matchmaking Core Rules

## 1. Purpose
Match players by: rating_mu, rating_sigma, block_profile, mode_rating, device_class, input_class, fps_band, latency_band, language_group, accessibility_class, integrity_state, smurf_suspicion, minor_safety.

## 2. Scope
Phase 10 Matchmaking & Trials.

## 3. Non-Goals
Production matchmaking.

## 4. Inputs
Phases 03-05.

## 5. Outputs
MatchmakingEngine.kt

## 6. Data Model / Interfaces
MatchmakingEngine.kt

## 7. Implementation Steps
1. Implement Matchmaking Core Rules in simulation.
2. Test with synthetic queues.

## 8. Acceptance Criteria
Matchmaking Core Rules behaves correctly in simulation.

## 9. Tests
Synthetic queue test for Matchmaking Core Rules.

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
Matchmaking Core Rules implemented and tested in simulation.
