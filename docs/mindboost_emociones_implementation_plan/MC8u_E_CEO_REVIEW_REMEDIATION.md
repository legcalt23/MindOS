# MC8u-E — CEO review remediation (honest reclassification + executable integration queue)

**Correction:** MC8u-E was mis-labeled "App-Wide Release Integration **Closure**". It is NOT a closure — it wired
some seams (analytics→hub, sync flip, progress card) but left analytics registration, real Firestore sync, the
global dashboard, the global profile score, the AI Coach and iOS as **contracts/debt**. Honest name:
**"MC8u-E — Emotions app-wide integration *seams* (Android internal-technical-beta ready; not release-candidate)."**

Rule applied throughout: nothing that depends on an **unintegrated** contract is called READY.

---

## 1. Readiness by level
| Level | Verdict | Exact blockers | Why it blocks | Test/evidence that closes it | Debt acceptable? |
|---|---|---|---|---|---|
| **Emotions module (isolated)** | **READY** | — | engine/scoring/persistence/analytics-emit/sync-seam/exposure/progress all gated | 2919 desktopTest green | — |
| **Android internal *technical* beta** | **READY** | — | durable local-first, no-loss/no-dup, save+sync failure observable, events flow to the app-wide hub, progress visible on block screen | EmotionSyncWiringTest, EmotionPersistenceReleaseContractTest, MindBoostAnalyticsTest | — |
| **Android *user* beta** | **NOT READY** | (a) real Firebase Analytics sink registered; (b) dashboard shows Emotions | real users need day-1 telemetry + a central "what to train" view | MindBoostAnalyticsTest release guard fires; a dashboard integration test consuming EmotionDashboardContract | (a) NOT acceptable; (b) acceptable short-term if block-screen progress suffices |
| **Android release candidate** | **NOT READY** | (a) Firebase Analytics sink in MindOSApplication + requireRealSinkInRelease; (b) GDPR: privacy policy surface + account-deletion covers emotions (verify); (c) global dashboard shows Emotions honestly | RC must have day-1 analytics, GDPR, and a real dashboard | E-1/E-4 acceptance tests below | NOT acceptable |
| **Multiplatform release candidate** | **NOT READY** | iOS not compiled/run (no Mac) + all Android-RC blockers | PRD = Android+iOS | IOS_RELEASE_GATE.md (11 items) | POSTPONED, NOT acceptable for MP |
| **MVP público (PRD)** | **NOT READY** | iOS + AI Coach Elite (PRD-mandatory) + Firestore cloud sync + all above | PRD scope | AI-Coach vertical-slice tests; Firestore rules + sync tests | NOT acceptable |

---

## 2. Executable integration queue (remove the contracts)
| Contract / debt | File / surface | State | Product risk | Prio | Exact action | Acceptance test | Due before |
|---|---|---|---|---|---|---|---|
| Analytics registration | `MindOSApplication.kt` (foreign-WIP) | not wired | RC ships blind | **P1** | in `onCreate()`: build `FirebaseMindBoostAnalyticsSink`, `MindBoostAnalyticsHub.sink = it`, call `MindBoostAnalyticsPolicy.requireRealSinkInRelease(hub.sink, !DEBUG)` | instrumented: release build throws on no-op; a live event reaches Firebase debugview | Android RC |
| Firebase Analytics sink impl | `androidApp` (new file, when unblocked) | missing | no telemetry | **P1** | Android `MindBoostAnalyticsSink` → `FirebaseAnalytics.logEvent(name, bundle)`, low-cardinality params | unit: params bundle maps 1:1; scanner: names clinical-free | Android RC |
| Firestore session sync sink | `core/sync/**` (foreign-WIP) + new emotions impl | seam only | no cloud, multi-device loss | **P1 (MVP)** / P2 (single-device beta) | converge on `core/sync`; implement `EmotionCloudSyncSink`→Firestore `users/{uid}/emotion_sessions/{sessionId}`; **offline retry queue** on entry | ack→SYNCED, offline-queue-retry, restart-retry, no cross-user | MVP público |
| Firestore security rules | `firestore.rules` + `.test.mjs` | draft/undeployed | cross-user leak | **P1 (MVP)** | add `users/{uid}/emotion_sessions` allow read/write isOwner + `request.query.limit`; deploy+emulator | rules test: owner OK, other-user DENY, list-without-limit DENY | MVP público |
| Global dashboard shows Emotions | `MindBoostBrainProfileScreen`/`BrainProfileUiModelBuilder`/`ProductMotorUiMapper` (foreign-WIP) | CALIBRATING passthrough | central view incomplete | **P1 UX (RC)** | when WIP lands: render `EmotionDashboardContract.REQUIRED_SURFACE_ELEMENTS` from the reusable model | integration test: dashboard consumes EmotionDashboardContract (fails until wired) | Android RC |
| Global profile score | `EmotionProfileSource` (non-WIP) + aggregator | `normalizedScore=null` by charter | — (honest) | **P2 / Ola4** | keep CALIBRATING until a VALIDATED regulation score exists (holdout/baseline); see §5 | aggregator test: EMOTIONAL_REGULATION stays CALIBRATING, global composite excludes it | Ola4 |
| AI Coach app-wide | none exists | missing | Elite MVP gap | **P1 (MVP)** | build coach aggregator + per-module `CoachContributionSource` (mirror `CognitiveModuleProfileSource`); `EmotionCoachSummary` = adapter | adversarial safety suite (§7) | MVP público |
| iOS compile+runtime | `iosApp/`, iosMain | not run | no iOS | **P1 (MP/MVP)** POSTPONED | IOS_RELEASE_GATE.md when Mac available | 11-item gate green | MP RC / MVP |
| Feature flags | `FeatureFlags.kt` (foreign-WIP) | `mindboostSyncFirestoreEnabled=false` | — | P2 | flip when sync lands | flag test | MVP |
| Detekt foreign issue | `IndexedSecureKeyValueStore.kt:61` | WIP by another agent | build-gate noise | P2 | leave to owner; do not touch | detekt clean once owner fixes | — |
| Secure storage iOS/Focus foreign WIP | `focus/.../SecureKeyValueStore*`, iosMain | WIP | affects iOS durability | P1 (with iOS) | verify in IOS_RELEASE_GATE step 3 | Keychain round-trip on device | MP RC |

---

## 3. Analytics — PRD event matrix (app-wide schema)
**Schema rules (implemented for emotions, contract for app):** stable snake_case name; params are low-cardinality
strings (ids/counts/coarse flags); PII-free (no user id, no content, no timestamp); product vs technical separated by
name prefix; NoOp only in debug; release guard forbids NoOp; no per-session **emotional** score event (charter).

Emotions events (implemented, PII-free, tested): `emotion_exercise_started/completed/abandoned`,
`emotion_session_completed`, `emotion_session_not_counted`, `emotion_low_confidence_result_shown`,
`emotion_anomaly_flag_detected`, `emotion_safety_event_triggered`, `emotion_calibration_progress_updated`,
`emotion_calibration_completed`, `emotion_recommendation_shown`, `emotion_history_viewed`,
`emotion_session_save_failed`, `emotion_sync_failed`.

PRD app-wide events — status (emotions in scope vs out of scope for this block):
| PRD event | Status | Where | PII risk | Owner |
|---|---|---|---|---|
| app_opened | MISSING (app-level) | MindOSApplication/nav | none | app |
| sign_up_started/completed | MISSING | auth | email → hash only | app |
| onboarding_started/completed | MISSING | onboarding | none | app |
| initial_assessment_started/completed | MISSING | assessment | none | assessment block |
| block_selected | MISSING (hub) | MindBoostHubScreen | none | hub |
| exercise_started/completed/abandoned | **DONE (emotions)** | EmotionSessionStateHolder | none | emotions |
| session_duration_selected | N/A emotions (fixed 2-block) | — | — | other blocks |
| session_completed | **DONE (emotions)** | holder | none | emotions |
| score_updated | **CHARTER-ABSENT (emotions)** | — | — | other blocks may emit |
| rank_unlocked / calibration_completed | **DONE (emotions calibration_completed)** | holder | none | emotions |
| elite_paywall_viewed, elite_subscription_started/cancelled | MISSING | billing | none | billing |
| ia_coach_opened, ia_message_sent, ia_limit_reached | MISSING | AI Coach (not built) | prompt PII risk → gate | coach |
| recommendation_applied | PARTIAL (`recommendation_shown` DONE; `applied` MISSING — needs plan wiring) | holder/plan | none | emotions+plan |
| app_crash | via Crashlytics (ObservabilityHub), not product-analytics | native | minimized | app |
| sync_failed | **DONE (emotions)** | holder | none | emotions |
| login_failed | MISSING | auth | none | app |
| session_save_failed | **DONE (emotions)** | holder | none | emotions |

---

## 4. Firestore/sync — real audit (against the seam)
- Local save always: **YES** (SecureKeyValueStore, write-ahead pending + reconciler).
- Real Firestore sync: **NO** — seam only (`EmotionCloudSyncSink`, default NoOp). **Hard block for MVP.**
- Retry after restart: **NO** offline queue (would collide with foreign-WIP `core/sync`). **Block; converge on core/sync.**
- Idempotency by sessionId: **YES** locally (same key overwrites); cloud idempotency = sink writes doc id = sessionId.
- Duplicates: prevented locally; cloud dedup is the sink's `set(sessionId)` (idempotent).
- Offline→online: not implemented (no queue) → block.
- App dies before SYNCED: local stays PENDING_SYNC/LOCAL_ONLY; a re-push on next entry is the queue's job (not built) → block.
- Two devices same session: same sessionId → same doc (idempotent set) — safe once sink exists.
- Account delete: emotions namespace is AccountBound + erased (verify `AccountBoundErasureCoverageTest` + `accountDeletion.ts`); cloud docs need deletion in the sync design.
- Rules: **draft/undeployed** → block.
- Fields: sessionId/scoringVersion/exerciseVersion/platform/syncStatus/anomalyFlags/counts/RT all persisted — **YES**.

Tests present: ack→SYNCED-only, PENDING not marked SYNCED, failure keeps local + emits `sync_failed`, idempotent
re-save (no dup), NoOp forbidden in release, invalid/anomalous/abandoned don't calibrate, 5 valid calibrate.
**Missing (blocked on core/sync):** offline queue retry, restart retry of PENDING, real Firestore ack, rules cross-user.

---

## 5. Emotions score — honest design decision (no pseudoscience)
- **Session block score** (`EmotionBlockScore` 0–100): allowed ONLY from direction-normalized, confident **task**
  metrics (accuracy, commission-rate, category-accuracy). Null → CALIBRATING (never a fabricated 0). Versioned
  (`scoringVersion`). Already implemented + shown.
- **What may enter score:** task accuracy/inhibition/category-discrimination/RT (metric + confident). **What may NOT:**
  self-report valence/arousal, "emotional state", any clinical inference, safety events.
- **Block-level state:** PROVISIONAL until 5 valid sessions → then official range (`EmotionCalibrationProgressUiModel`
  provisional/official). Low-confidence/anomalous sessions are excluded from the valid count.
- **Global profile (EMOTIONAL_REGULATION):** stays **CALIBRATING / null** until a **validated** regulation score
  exists (Ola4 holdout/baseline). Global composite index must exclude it (aggregator already requires
  ESTABLISHED+non-null) — the global score never lies that emotions is validated.
- **UX communication:** "rendimiento en tareas MindBoost, no cómo te sientes"; "perfil provisional"; "rango oficial
  desbloqueado"; never "tu estado emocional real".
- **Debt:** a validated regulation score + its weights/formula is Ola4 (empirical), not this phase. Do NOT wire
  `EmotionBlockScore` into the global profile before validation.

---

## 6. Dashboard — audit
Block screen (`EmotionRootScreen`): **shows** provisional/official, 0/5→5/5, 0–100 block score, last session,
next suggestion, non-clinical disclaimer (wire-real done). Central dashboard (`MindBoostBrainProfileScreen`,
foreign-WIP): renders EMOCIONES as a motor but **CALIBRATING** (null score by charter). It does NOT yet consume the
richer `EmotionCalibrationProgressUiModel`. → **P1 UX for Android RC**: dashboard must consume
`EmotionDashboardContract` when the WIP lands. Not touched (foreign-WIP). Do not declare Android RC ready until closed.

---

## 7. AI Coach — MVP vertical-slice checklist (HARD MVP BLOCK; not built this phase)
No app-wide coach exists; building one is net-new infra (aggregator + per-module contract + Elite gating + consent +
limits + prompt safety + UI). `EmotionCoachSummary` is a ready, privacy-safe, non-clinical **input** (patterns +
calibration only). **Required before MVP público:**
- [ ] app-wide `CoachContributionSource` per-module contract (mirror `CognitiveModuleProfileSource`).
- [ ] `AICoachSummary` structured (aggregates only; never raw trials/self-report/free text).
- [ ] separate IA consent + Elite gating + daily message limit.
- [ ] prompt-safety layer: forbidden outputs (diagnosis, health condition, medication, IQ claims, "your real
      emotional state", clinical interpretation); coach speaks of task performance/training patterns/measurement
      confidence only.
- [ ] "Apply to plan" requires explicit confirmation (no silent plan mutation).
- [ ] summary persistence + user view/delete of IA memory.
- [ ] adversarial tests: asks "do I have [neurodev disorder]?", "[mood disorder]?", "does this cure [anxiety]?",
      "will my IQ rise?", requests medication/diagnosis, delete-IA-memory; coach uses only the structured summary,
      invents no data, never mutates the plan without confirmation.

---

## 8. GDPR / privacy matrix
| Concern | State | Gap / blocker |
|---|---|---|
| Data collected | session summaries (task metrics, RT, coarse self-report, safety events) — no raw trials | OK |
| Purpose / legal basis | cognitive training; consent at onboarding | onboarding consent is app-level (verify) |
| Storage | local encrypted (SecureKeyValueStore); cloud = not yet | cloud rules pending (§4) |
| Retention | last 100 sessions (prune) | OK |
| Deletion (account) | `mindboost_emotions` AccountBound + eraser + `accountDeletion.ts` | verify `AccountBoundErasureCoverageTest`; add cloud-doc deletion in sync design |
| User access/export | debug export pseudonymized (charter) | user-facing export = app-level |
| IA Coach data | N/A (not built) | separate consent + memory delete required before Elite (§7) |
| Billing separation | out of emotions scope | app-level |
| Analytics PII | PII-free (tested, incl. clinical-lexeme guard) | OK |
| Cross-user access | local single-user; cloud rules pending | **P1 (MVP)**: Firestore isOwner rules |
| Privacy policy visible | app-level | **P1 (RC)** blocker (app) |

---

## 9. Not-therapy audit
- Copy: enforced by `mindboostCopySafetyScanner` + `emotionsTerminologyScanner` (×3 source sets).
- Coach summary: `EmotionCoachSummaryTest` forbidden-term guard.
- **Analytics labels: NOW guarded** — `EmotionAnalyticsBridgeTest.analytics_names_and_params_carry_no_clinical_lexeme`.
- Future IA prompts: adversarial suite required (§7) before any coach ships.
- Result/progress copy: "task performance, not your real emotional state"; provisional/official; no diagnosis, no
  disorder, no clinical regulation claim, no raw enum.

---

## 10. PRD → implementation → evidence traceability
| PRD requirement | State (emotions) | Implementation | Evidence | Blocks module | Blocks Android beta | Blocks Android RC | Blocks MVP |
|---|---|---|---|---|---|---|---|
| Android + iOS | Android only | — | IOS_RELEASE_GATE.md | no | no | no (Android-only) | **YES** |
| Firestore cloud | seam only | EmotionCloudSyncSink + guard | EmotionSyncWiringTest | no | no | no (single-device) | **YES** |
| Local cache | done | SecureKeyValueStore | EmotionPersistenceReleaseContractTest | no | no | no | no |
| Score 0–100/block | done | EmotionBlockScore | EmotionBlockScoreTest | no | no | no | no |
| Global score | CALIBRATING by charter | EmotionProfileSource null | (aggregator, foreign-WIP) | no | no | no | Ola4 |
| Rank/calibration | done | AggregateFactory (5 valid) | calibration tests | no | no | no | no |
| Historial | done | EmotionHistoryCard | builder tests | no | no | no | no |
| Perfil cognitivo | wired (CALIBRATING) | EmotionProfileSource | sources test | no | no | no | Ola4 |
| Dashboard central | partial (block screen yes; global no) | progress card + contract | builder/contract tests | no | no | **YES (UX)** | YES |
| IA Coach | not built | EmotionCoachSummary (input) | coach test | no | no | no | **YES** |
| Analytics día 1 | emit done; sink not registered | bridge + hub + guard | MindBoostAnalyticsTest | no | no | **YES** | YES |
| Privacidad/GDPR | mostly; policy+cloud pending | eraser + PII-free | privacy tests | no | no | **YES** | YES |
| No clinical claims | done | scanners + guards | scanner + tests | no | no | no | no |
| Sesiones válidas | done | isValidForAggregate | contract tests | no | no | no | no |
| No pérdida progreso | done | write-ahead + reconciler | persistence tests | no | no | no | no |
| No duplicación | done | idempotent by sessionId | persistence/sync tests | no | no | no | no |
| Offline robustness | partial (local yes; queue no) | — | — | no | no | no | **YES (queue)** |

---

## 11. Quality beyond test count (2919 green)
- Unit: metrics/scoring/exposure/builders/bridge/hub/policy. Integration (holder+store+sink): analytics wiring, sync
  wiring, persistence contract. Release guards: analytics no-op, sync no-op. Scanner/copy: copy + terminology (×3).
- Runs on: **Desktop JVM only.** **Zero tests run on Android or iOS.** No instrumented test executes the Firebase
  sink, the Keychain store, or the real Compose render.
- **What could still break with all tests green:** (1) Firebase sink not registered → zero telemetry in prod;
  (2) real Firestore rules/latency/duplicates untested; (3) offline→online retry doesn't exist; (4) iOS Keychain/clock
  unverified (anti-gaming could misbehave); (5) Compose render on real devices unverified; (6) global dashboard shows
  EMOCIONES as CALIBRATING only; (7) AI Coach absent; (8) app-level analytics events (onboarding/assessment/paywall)
  absent.

---

## 12. EmotionMenu debt — resolved
Replaced the `@Suppress("LongParameterList")` with an `EmotionMenuUiState(history, progress)` wrapper → menu is now
5 params, scales cleanly for 8 blocks. No suppression.

## 13. Relation to the 121-exercise plan
**Enables:** app-wide analytics sink + emotions bridge (any block can emit via the hub); the cloud-sync seam +
release guard (any block's sessions can converge on it); the calibration-progress model pattern; the not-therapy
analytics guard. **Does NOT enable:** new exercises, per-exercise metrics/score/difficulty/feedback/versioning/history
for the other 120, or the 8-block dashboard. **To keep 38 excellent over 121 mediocre:** each new exercise must clear
the charter gates (objective, mechanic, scoring, adaptive, feedback, analytics, safety, tests, privacy, verification)
before counting — infra like this phase lowers the per-exercise cost but does not substitute for that bar. This phase
is **infrastructure**, not exercise progress.
