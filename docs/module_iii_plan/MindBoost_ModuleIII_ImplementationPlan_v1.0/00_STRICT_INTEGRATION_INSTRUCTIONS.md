# Strict Integration Instructions

> **CRITICAL:** This plan does NOT authorize production, live Shadow Ranked, public Ranked, or user-facing activation.

## 1. Exact Purpose
Build the offline simulation engine, data model prototype, and Bayesian Competitive Rating core for internal validation only.

## 2. What Engineers MAY Implement
- Data model classes (Kotlin data classes) in a dedicated module.
- Offline simulation harness (Python or Kotlin).
- Unit tests against synthetic data.
- Feature flags (default OFF).
- Audit log infrastructure.

## 3. What Engineers MUST NOT Implement
- Any connection to production Firestore/Firebase.
- Any user-facing UI surface.
- Any real user data ingestion.
- Any monetization, prize, Pro Arena or Cash Challenge logic.
- Any live matchmaking queue.

## 4. Required Branch Name
`feature/module-iii-offline-simulation`

## 5. Required Package Names
`com.mindos.app.features.arena.rating` — Rating core.
`com.mindos.app.features.arena.simulation` — Simulation harness.
`com.mindos.app.features.arena.integrity` — Integrity decisions.
`com.mindos.app.features.arena.evidence` — Evidence Ledger engine.

## 6. Required Feature Flags (Default OFF)
- `ARENA_RATING_ENGINE_ENABLED` = false
- `ARENA_SHADOW_RANKED_ENABLED` = false
- `ARENA_MENTAL_SPEED_RANKED_ENABLED` = false

## 7. Required Environment Restrictions
- Code must compile but never execute against live backends.
- All tests must use synthetic/mock data exclusively.

## 8. Synthetic-Only Data Policy
No real user IDs, session data, performance data, or device telemetry may be used. All simulation data must be generated from deterministic seeds.

## 9. Audit Log Behavior
Every rating update, freeze, rollback, or integrity decision must produce an audit log entry even in simulation mode.

## 10. Terminology Restrictions
Forbidden: ELO, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out, "gana dinero", "cerebro élite", "debilidad mental".

## 11. Required Tests Before Merge
- All unit tests pass.
- Terminology scanner finds zero violations.
- Feature flags confirmed OFF.
- No production endpoint references.

## 12. Required Sign-offs Before Merge
Engineering Lead, Data Science Lead, Product Owner.

## 13. Required Sign-offs Before Live Shadow Ranked
Science, Data, Integrity, Fairness, Privacy, UX, Product, Engineering.

## 14. Emergency Rollback Procedure
Delete the feature branch. Revert any merged commits. Confirm feature flags remain OFF in all environments.

## 15. Final Statement
This plan does not authorize production, live Shadow Ranked, public Ranked, or user-facing activation.
