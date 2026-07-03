# STRICT INSTRUCTIONS FOR MINDBOOST INTEGRATION

> **THIS IS THE MOST CRITICAL FILE IN THE ENTIRE PLAN.**

## 1. Exact Purpose
Build the offline simulation engine, data model prototype, and Bayesian Competitive Rating core for INTERNAL VALIDATION ONLY.

## 2. What Engineers MAY Implement
- Kotlin data classes in `com.mindos.app.features.arena.*`.
- Offline simulation harness (Python or Kotlin CLI).
- Unit tests with synthetic data.
- Feature flags (default OFF).
- Audit log infrastructure.
- Evidence Ledger gate logic.

## 3. What Engineers MUST NOT Implement
- ANY connection to production Firestore, Firebase, or real backends.
- ANY user-facing UI, screen, or notification.
- ANY real user data ingestion or processing.
- ANY monetization, prize, Pro Arena, or Cash Challenge logic.
- ANY live matchmaking queue or WebSocket connection.
- ANY deployment to staging or production environments.

## 4. Required Branch Name
`feature/module-iii-offline-simulation`

## 5. Required Package/Module Names
- `com.mindos.app.features.arena.rating`
- `com.mindos.app.features.arena.simulation`
- `com.mindos.app.features.arena.integrity`
- `com.mindos.app.features.arena.evidence`
- `com.mindos.app.features.arena.matchmaking`
- `com.mindos.app.features.arena.trials`
- `com.mindos.app.features.arena.audit`
- `com.mindos.app.features.arena.config`

## 6. Required Feature Flags (All Default OFF)
- `ARENA_RATING_ENGINE_ENABLED` = false
- `ARENA_SHADOW_RANKED_ENABLED` = false
- `ARENA_MENTAL_SPEED_RANKED_ENABLED` = false
- `ARENA_MATCHMAKING_ENABLED` = false
- `ARENA_TRIALS_ENABLED` = false

## 7. Required Environment Restrictions
- Code must compile but NEVER execute against live backends.
- All tests must use synthetic/mock data exclusively.
- No Firebase project ID references to production projects.

## 8. Required Synthetic-Only Data Policy
No real user IDs, session data, performance data, or device telemetry may be used in ANY simulation, test, or prototype.

## 9. Required Audit Log Behavior
Every rating mutation, freeze, rollback, or integrity decision must produce an audit log entry even in simulation mode.

## 10. Required Terminology Restrictions
**FORBIDDEN:** ELO, Elo, elo, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out, "gana dinero", "cerebro élite", "debilidad mental", "inteligencia general".

## 11. Required Tests Before Merge
- All unit tests pass.
- Terminology scanner finds zero violations.
- Feature flags confirmed default OFF in all build variants.
- No production endpoint references found by CI scanner.

## 12. Required Sign-offs Before Merge
- Engineering Lead
- Data Science Lead
- Product Owner

## 13. Required Sign-offs Before Live Shadow Ranked
- Science Lead
- Data Science Lead
- Integrity Lead
- Fairness Lead
- Privacy Lead
- UX Lead
- Product Owner
- Engineering Lead

## 14. Emergency Rollback Procedure
1. Delete the feature branch.
2. Revert any merged commits.
3. Confirm all feature flags are OFF in all environments.
4. Audit logs to verify no production data was accessed.

## 15. Final Statement
**This plan does NOT authorize production deployment, live Shadow Ranked, public Ranked, tournaments, prizes, Pro Arena, Cash Challenges, or user-facing activation of ANY kind.**
