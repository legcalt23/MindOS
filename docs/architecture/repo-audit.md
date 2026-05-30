# Repo Audit — MindOS

Date: 2026-05-24
Phase: 0 — Preparation
Prompt: 00_PREPARACION_01_AUDITAR_REPO

---

## 1. Project Overview

MindOS is a Kotlin Multiplatform social app (Android, Desktop, iOS) with Firebase backend (Cloud Functions + Firestore + Storage). The app includes social graph, profiles, DMs, waves, global feed, notifications, privacy controls, and media handling.

## 2. Modules

| Module | Path | Purpose |
|---|---|---|
| `shared` | `shared/` | KMP shared code (commonMain, androidMain, desktopMain, iosMain). Contains all domain, data, presentation, and UI. |
| `androidApp` | `androidApp/` | Android application entry point. Thin shell — delegates to `shared`. |
| `desktopApp` | `desktopApp/` | Desktop (JVM) application entry point. Thin shell. |
| `app` | `app/` | Legacy/scaffolded Android module (not included in `settings.gradle.kts`). Unused. |
| `functions` | `functions/` | Firebase Cloud Functions backend (TypeScript, Node 18). |

## 3. Stack Detected

### Backend
- **Runtime**: Firebase Cloud Functions v2 (TypeScript)
- **Database**: Cloud Firestore (NoSQL, document-based)
- **Auth**: Firebase Auth (email/password) with App Check (Play Integrity / debug)
- **Storage**: Firebase Storage with server-enforced quotas
- **Tests**: Jest (86 tests, 85 passing, 1 flaky due to mock init ordering)

### KMP Client
- **Language**: Kotlin 2.3.10
- **UI**: Compose Multiplatform 1.10.3 + Material3
- **Targets**: Android (minSdk 29, targetSdk 36), Desktop (JVM 17), iOS (arm64 + simulator)
- **Serialization**: kotlinx.serialization 1.9.0
- **Coroutines**: kotlinx.coroutines 1.10.2
- **Firebase KMP SDK**: dev.gitlive:firebase-* 2.4.0
- **Tests**: kotlin-test + coroutines-test (18 test files, all passing)

### Build
- **Gradle**: 9.3.1 with Kotlin DSL
- **AGP**: 9.1.0
- **Version catalog**: `gradle/libs.versions.toml`
- **No CI pipeline detected** (no `.github/workflows/`, no `.gitlab-ci.yml`, no `Jenkinsfile`)

## 4. Feature Map

```
features/
  auth/              — Auth screen (email/password login/register)
  element_selection/  — Element type selection (Orb UI)
  social/            — Social graph, profiles, privacy, feed, publisher, media viewer, notifications
  search/            — Username search with radar mode
  dm/                — Direct messages (1-to-1 chat, media, audio)
  waves/             — Wave requests (pending interactions)
  privacy/           — Privacy settings screen + overlay
  home/              — Home screen (navigation hub)
```

## 5. Navigation

Custom state-machine routing in `App.kt` using `AppRoute` sealed interface. No Jetpack Navigation or Decompose. Routes:
- Loading, Auth, ElementSelection, Main, GlobalFeed, Settings, MediaViewer, DMList, DMChat, DMChatUserInfo, DMCajonera

**No existing route for "Moneda" / wallet / coins.** Will need to add `AppRoute.Moneda` and wire it into `HomeScreen`.

## 6. Backend Security Posture

Cloud Functions follow a strict security envelope:
```
requireAppCheck → requireAuth → validateInput → checkRateLimit → business logic
```

- All write operations go through Cloud Functions (Admin SDK bypasses Firestore Rules).
- Firestore Rules are defense-in-depth for reads + client-initiated writes.
- Rate limiting via Firestore atomic counters per (uid, action).
- Input validation is strict: safe IDs, no control chars, path traversal protection, MIME whitelist.
- Block checks, privacy checks, and storage suspend checks are enforced server-side.
- The catch-all rule `match /{document=**} { allow read, write: if false; }` denies all unmatched paths.

## 7. Database (Firestore)

Current collections detected in rules/functions:
- `users`, `profiles`, `privacy`, `usernames`, `profile_visuals`
- `follows`, `followRequests`, `blocks`, `counters`
- `observations`, `notifications`, `profile_visits`
- `posts`, `user_posts`, `feeds`
- `dm_chats` (with `messages` subcollection)
- `auditEvents` (locked: read/write = false)
- `rate_limits`, `storage_usage`

**No SQL database. No PostgreSQL. No Flyway/Liquibase/Exposed/JOOQ.** The plan's DB schema reference assumes PostgreSQL — **must be adapted entirely to Firestore**.

## 8. Auth Model

- Firebase Auth with email/password (via `SessionRepository` interface + `FirebaseSessionRepository` impl).
- App Check enforced on Android (Play Integrity for release, debug provider for debug builds).
- JWT-based auth on Cloud Functions (`context.auth.uid`).
- No custom claims / RBAC detected currently.

## 9. Existing Tests

### KMP (shared/commonTest) — 18 test files
- `ElementMasterCatalogTest`, `ElementSelectionStateHolderTest`
- `SocialGraphRulesTest`, `AuthDiagnosticsTest`, `SafeInputTest`
- `PublishingPipelineTest`, `ProfileInlineEditUtilsTest`, `ProfileStateHolderSaveTest`
- `PublisherDraftSecurityTest`, `ProfileVisualConfigTest`, `GlobalFeedInputValidationTest`
- `DMMediaAdapterTest`, `DMConversationIdsTest`, `DMMapperTest`, `SendDMMessageUseCaseTest`, `DMListViewModelTest`
- `MediaContentResolverTest`, `WaveRequestServiceTest`

### Cloud Functions (Jest) — 4 test suites, 86 tests
- `security.test.ts` — auth, app check, input validation, rate limiting, block cascade, idempotency
- `index.test.ts` — storage quotas (1 test failing: mock init order issue)

**All KMP tests pass. Functions tests: 85/86 pass (1 pre-existing failure).**

## 10. Secrets Audit

| Item | Status |
|---|---|
| `local.properties` | SDK path only. In `.gitignore`. Safe. |
| `androidApp/google-services.json` | Firebase config (public API key, project ID). Not a secret per Firebase docs. |
| `shared/src/iosMain/resources/GoogleService-Info.plist` | Firebase config for iOS. Not a secret per Firebase docs. |
| `.env` files | None found. |
| Hardcoded API keys / tokens | None found in source code. |
| `node_modules/` | Present but in `.gitignore`. |

**No secrets detected in version control.**

## 11. Adaptation Plan — Key Decisions Required

### Critical: No PostgreSQL
The plan assumes PostgreSQL with relational tables, migrations, SQL constraints, and transactional isolation. The actual project uses **Firestore** (NoSQL, document-based). All wallet/ledger schema must be adapted:

| Plan Concept | Firestore Adaptation |
|---|---|
| PostgreSQL tables | Firestore collections |
| SQL migrations (Flyway) | No migrations needed (schemaless), but schema versioning in documents |
| UNIQUE constraints | Firestore document IDs + Cloud Functions transaction checks |
| Foreign keys | Application-level enforcement in Cloud Functions |
| `SELECT FOR UPDATE` / row locking | Firestore transactions (optimistic locking with retry) |
| Append-only ledger | Cloud Functions: create-only, no update/delete via rules |
| Balance snapshots | Computed in transactions, stored as separate documents |

### Critical: No dedicated backend server
There's no Ktor / Spring Boot server. All backend logic lives in **Firebase Cloud Functions** (TypeScript). The wallet/ledger/purchase endpoints will be **callable Cloud Functions**, not REST endpoints.

### Architecture Mapping
| Plan Component | Where It Lives |
|---|---|
| Wallet service | New Cloud Function callables |
| Ledger (append-only) | Firestore collection `ledger_transactions` + `ledger_entries` |
| Balance snapshots | Firestore collection `balance_snapshots` |
| Purchases | Firestore collection `purchases` |
| Payment providers | TypeScript abstractions in `functions/src/lib/` |
| Idempotency keys | Firestore collection `idempotency_keys` |
| Fraud signals | Firestore collection `fraud_signals` |
| Admin actions | Firestore collection `admin_actions` |
| Audit logs | Firestore collection `audit_logs` |
| Wallet UI ("Moneda") | New KMP screen + new `AppRoute` |

## 12. Initial Risks

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| R1 | Plan assumes PostgreSQL; actual DB is Firestore | **High** | Adapt all schema to Firestore document model. Use Firestore transactions for atomicity. |
| R2 | Plan assumes REST API (Ktor/Spring); actual backend is Cloud Functions | **High** | Implement as callable Cloud Functions with same security envelope. |
| R3 | Firestore lacks native UNIQUE constraints on field combinations | **Medium** | Use composite document IDs (e.g., `{walletId}_{idempotencyKey}`) for uniqueness. |
| R4 | Firestore transactions are optimistic (can fail under contention) | **Medium** | Implement retry logic with exponential backoff for concurrent wallet operations. |
| R5 | No CI pipeline — no automated test gate before deploy | **Medium** | Add GitHub Actions in Phase 7 (DevSecOps). |
| R6 | 1 pre-existing test failure in functions (`enforceStorageQuotas`) | **Low** | Fix mock initialization order. Not related to wallet feature. |
| R7 | `app/` module is orphaned (not in `settings.gradle.kts`) | **Low** | Ignore. Does not affect build. |
| R8 | No DI framework — manual wiring in `App.kt` | **Low** | Follow existing pattern for wallet dependencies. |

## 13. Compatibility Notes

- The existing security envelope (App Check + Auth + Validation + Rate Limiting) should be reused for all new wallet endpoints.
- The existing `validators.ts` pattern should be extended with wallet/purchase validators.
- The existing rate limiter supports adding new actions trivially.
- Firestore Rules catch-all (`/{document=**}` → deny) means new collections are locked by default — explicit rules must be added.
- The `auditEvents` collection already exists (locked). Can be repurposed or extended for wallet audit logs.
