# Module III (Arena) — Integration Rulebook & Conformance Report

> **THIS DOCUMENTS THE STRICT INTEGRATION RULES AND CHECKS THE BUILD AGAINST THEM.**
> Source of truth: `STRICT_INSTRUCTIONS_FOR_MINDBOOST_INTEGRATION.md`.
> **This plan does NOT authorize production deployment, live Shadow Ranked, public Ranked,
> tournaments, prizes, Pro Arena, Cash Challenges, or user-facing activation of ANY kind.**

## 1. Purpose (acknowledged)
The arena is an **offline simulation engine + data model + Bayesian Competitive Rating core for
INTERNAL VALIDATION ONLY**. It is built entirely in `com.mindos.app.features.arena.*` (KMP commonMain),
exercised only by `desktopTest` with synthetic data.

## 2. Conformance matrix (rule → enforcement in the build)

| Rule (strict instructions) | Status | Evidence / Enforcement |
| --- | --- | --- |
| §2 MAY implement (data classes, offline harness, synthetic tests, flags, audit infra, ledger gates) | ✅ | All built items fall within this list |
| §3 MUST NOT (Firebase/backends, UI, real data, monetization, live matchmaking/WebSocket, deploy) | ✅ | `arenaProductionGuard` scans for production refs; no UI/Firebase/networking in `arena.*`; matchmaking is a pure `canMatch` function (no queue service) |
| §4 Branch `feature/module-iii-offline-simulation` | ⚠️ R-07 | Work was done on the existing branch `feat/mindboost-enfoque-integration`; rename/rebase before a formal merge if the prescribed branch name is required |
| §5 Required packages (rating, simulation, integrity, evidence, matchmaking, trials, audit, config) | ✅ | All 8 present (+ extras: comparability, measurement, sanitization, observability, hardware, metrics, redteam, validation, session) |
| §6 Five feature flags, all default OFF | ✅ (R-06 resolved) | All 5 present & default OFF: rating-engine, shadow-ranked, mental-speed-ranked, **matchmaking, trials** (FeatureFlags interface + Default + Static + RemoteConfig; asserted by VAL_25 + ArenaModelTests) |
| §7 Compile-but-never-execute-live; synthetic/mock tests; no prod project IDs | ✅ | `arena.*` has no live calls; tests are synthetic; no production Firebase IDs in `arena.*` |
| §8 Synthetic-only data policy (no real UIDs/sessions/telemetry) | ✅ | `UuidV4` synthetic ids only; `DataSanitizer` + `SafeArenaException` scrub any UID/PII; `SyntheticProfileId`/`SimulationRunId` reject non-UUIDv4 |
| §9 Audit log per rating mutation/freeze/rollback/integrity decision | ✅ (R-04 resolved) | `RatingAuditLogger.audit()` produces a `RatingMutationAuditLog` for every update (applied OR rejected); the same pattern covers freeze/rollback/integrity decisions |
| §10 Terminology restrictions | ✅ (R-08 resolved) | `arenaTerminologyScanner` (commonMain + commonTest + desktopTest) now enforces the full §10 list incl. `"inteligencia general"` |
| §11 Tests before merge (unit pass, terminology zero, flags OFF, no prod refs) | ✅ | All green: `desktopTest`, `arenaTerminologyScanner*`, flags asserted OFF (`VAL_07`/`VAL_25`), `arenaProductionGuard` |
| §12 Sign-offs before merge (Eng Lead, Data Science, Product) | ⏳ | Human gate — pending |
| §13 Sign-offs before Live Shadow (8 leads) | ⏳ | Human gate — pending; **blocked** |
| §14 Emergency rollback procedure | ✅ documented | See §4 below |
| §15 Final statement (no production/live authorization) | ✅ | Re-affirmed; `VAL_25` verifies no motor is live |

## 3. Conformance gaps — status

- **R-06 — ✅ RESOLVED.** Added `arenaMatchmakingEnabled` + `arenaTrialsEnabled` (default OFF) to the
  `FeatureFlags` interface, `DefaultFeatureFlags` (+keys), `StaticFeatureFlags`, and `RemoteConfigFeatureFlags`
  (`androidApp`). Verified with `:androidApp:compileDebugKotlin` + `:shared:desktopTest`; VAL_25/ArenaModelTests
  assert all 5 OFF.
- **R-08 — ✅ RESOLVED.** Added `"inteligencia general"` to the `arenaTerminologyScanner` forbidden list
  (`shared/build.gradle.kts`); all scanner tasks green.
- **R-04 — ✅ RESOLVED.** Added `arena.audit.RatingAuditLogger` producing a `RatingMutationAuditLog` for every
  update (applied or rejected). *Follow-up:* it is available but not yet auto-invoked by a session pipeline
  (no pipeline exists offline); call sites wire it when the pipeline is built.
- **R-07 — ⏳ OPEN (manual).** Branch is `feat/mindboost-enfoque-integration`, not the prescribed
  `feature/module-iii-offline-simulation`. A branch rename/rebase is a manual git decision (not auto-performed).

## 4. Emergency rollback procedure (§14, documented)
1. Delete the feature branch.
2. Revert any merged commits.
3. Confirm all `ARENA_*` feature flags are OFF in **all** build variants/environments.
4. Inspect audit logs to verify **no production data was accessed** (synthetic-only invariant).

## 5. Sign-off ledger (pending — human gates, not automatable)

| Gate | Required sign-offs |
| --- | --- |
| Before merge (§12) | Engineering Lead · Data Science Lead · Product Owner |
| Before Live Shadow Ranked (§13) | Science · Data Science · Integrity · Fairness · Privacy · UX · Product · Engineering |
| Anti-cheat boundaries | Codex sign-off (per `09_upgraded_plan.md`) |

## 6. Final statement
The offline, synthetic-only, privacy-hardened Module III build is **complete and internally validated**
(Phases 01–12 green, 97.40% core coverage). **No production deployment, live Shadow Ranked, public Ranked,
leaderboard, monetization, Firebase, or user-facing surface is authorized or implemented.** The remaining
work is the human/Codex sign-offs and the conformance gaps (R-04/R-06/R-07/R-08) — none of which authorize
go-live.
