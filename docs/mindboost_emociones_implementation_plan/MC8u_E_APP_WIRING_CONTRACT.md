# MC8u-E — App-Wide Integration Contracts & Debt (Emociones)

What MC8u-E wired in-module, and the enforceable contracts for the pieces that live in **foreign-WIP** or
out-of-module surfaces (another agent is actively editing them; per repo rule #10 they must not be touched).

## 1. Analytics — real sink (contract for `MindOSApplication`)
**Wired (this phase):** `core/analytics/MindBoostAnalyticsSink` (app-wide, ObservabilityHub-style registrable
singleton) + `EmotionAnalyticsBridge` mapping every `emotion_*` event to a PII-free `logEvent`; `rememberEmotionGraph`
now injects `EmotionAnalyticsBridge(MindBoostAnalyticsHub)` so production emotions events flow to the hub.
**Deferred (foreign-WIP `MindOSApplication.kt`):** at `onCreate()`, register a Firebase-Analytics-backed
`MindBoostAnalyticsSink` via `MindBoostAnalyticsHub.sink = …` and then call
`MindBoostAnalyticsPolicy.requireRealSinkInRelease(MindBoostAnalyticsHub.sink, isReleaseBuild = BuildConfig… )`.
**Release-blocking:** `MindBoostAnalyticsPolicy.requireRealSinkInRelease` throws for a release build still on the
no-op (test: `MindBoostAnalyticsTest.release_build_must_register_a_real_sink`). Firebase Analytics is an Android-only
SDK → the `:shared` module cannot register it directly (2026-05-28 Crashlytics learning); registration is app-layer.

## 2. Cloud sync — real backend (contract; converge on `core/sync`)
**Wired (this phase):** `EmotionGraph.cloudSync: EmotionCloudSyncSink = NoOpEmotionCloudSync`; the guard
`EmotionSyncPolicy.requireCloudSyncWiredWhenRequired` is invoked at graph construction; a durable summary is pushed
best-effort and its `syncStatus` flips from the result; a failed push emits `emotion_sync_failed` and never deletes
local data (tests in `EmotionSyncWiringTest`).
**Deferred (foreign-WIP `core/sync/**` + undeployed rules):** a real Firestore `EmotionCloudSyncSink` should converge
on the motor-agnostic `core/sync` foundation (currently a Focus pilot, untracked WIP) once it lands and the
`users/{userId}/sessions` rules (`MINDBOOST_FIRESTORE_RULES_DRAFT.md`) are deployed + emulator-verified (JDK-21,
not runnable here). When cloud sync is enabled: wire the real sink into `EmotionGraph` and flip
`cloudSyncRequired = true` — the guard then fails any build shipping the no-op.
**Charter:** local-first is the honest default; a completed session is durable on-device before any network.

## 3. Dashboard / hub — visible progress (wired in-module; contract for the global dashboard)
**Wired (this phase):** `EmotionCalibrationProgressCard` (previously tested-but-dead) is rendered in
`EmotionRootScreen.EmotionMenu` — the main Emociones experience now shows provisional/official, 0/5→5/5, the 0–100
block score, last session, next suggestion, and the non-clinical disclaimer. → progress is visible for the
**Android** track (not a blocker).
**Deferred (foreign-WIP `MindBoostBrainProfileScreen`/`BrainProfileUiModelBuilder`/`ProductMotorUiMapper`):** the
global brain-profile dashboard should render `EmotionDashboardContract.REQUIRED_SURFACE_ELEMENTS` by consuming the
reusable `EmotionCalibrationProgressUiModel`. Contract pinned by `EmotionCalibrationProgressBuilderTest`
(`dashboard_contract_elements_are_all_renderable_from_the_model`). Not edited — foreign-WIP.

## 4. Global cognitive profile — EMOTIONAL_REGULATION (debt, charter-gated)
**State:** Emotions IS wired into `MindBoostProfileSources` via `EmotionProfileSource`, but contributes
`normalizedScore = null` **by charter** (never fabricate a regulation-quality score from a data-maturity tier;
`null ≠ 0`). The dimension renders CALIBRATING and does not move the global composite index.
**Debt classification:** a real EMOTIONAL_REGULATION value requires a **validated** regulation score (Ola4
holdout/baseline). **P2 for the module / P1 for MVP público only if a visible per-motor emotions score is mandated**
— but the honest state until validation is CALIBRATING. Do NOT wire `EmotionBlockScore` into the profile (would
fabricate competence). No code change this phase.

## 5. AI Coach — app-wide coach (debt, P1 for MVP público)
**State:** there is **no app-wide AI Coach / coach aggregator** anywhere. `EmotionCoachSummary` is a ready,
privacy-safe per-block contribution (patterns + calibration state only; confidence/calibration; non-clinical
disclaimer; forbidden-term guard tested) but has **no production consumer**.
**Debt classification:** PRD §8.2 lists "IA Coach | Funcional para Elite" as MVP-mandatory → **P1 for MVP público**,
**P2 for module / Android internal beta** (Elite-gated, net-new infra). Building the app-wide coach (an aggregator +
per-module coach-contribution contract mirroring `CognitiveModuleProfileSource`, Elite-gated) is out of scope for
this phase; `EmotionCoachSummary` is the ready adapter input when that lands.

## 6. iOS
`POSTPONED — P1 for RC(multiplatform)/MVP`. See `IOS_RELEASE_GATE.md`. Not executed (no Mac). Never marked green.
