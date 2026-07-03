# Claude Code Opus 4.8 Handoff — MindBoost Sprint 1

## 1. Mission
Your mission is to implement the Sprint 1 deterministic local/offline foundation for the MindBoost Speed (Velocidad Mental) module in Kotlin Multiplatform (KMP), native Android, and Compose Multiplatform. You will deliver a sub-millisecond precise physical timing interface, a relative within-user scoring engine (System Readiness Index), local-only persistence, basic row-level integrity checks, and basic E1/E6 interaction screens. All implementation must be strictly local, offline, free of AI runtimes or cloud dependencies, and strictly aligned with the project's non-clinical, non-IQ claims guardrails.

---

## 2. Repository Facts Discovered

Based on codebase inspection, here are the architectural facts of the project:
*   **Modules:**
    *   `:shared` — Kotlin Multiplatform module containing domain, data, and presentation logic for iOS, Android, and Desktop targets.
    *   `:androidApp` — Native Android application module.
    *   `:desktopApp` — JVM desktop application module.
*   **Package Names:**
    *   Shared Core: `com.mindos.app.core`
    *   MindBoost Speed: `com.mindos.app.features.mindboost.speed`
        *   Domain: `com.mindos.app.features.mindboost.speed.domain`
        *   Data: `com.mindos.app.features.mindboost.speed.data`
        *   Presentation: `com.mindos.app.features.mindboost.speed.presentation`
*   **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with version catalog in `gradle/libs.versions.toml`.
*   **UI Stack:** Compose Multiplatform (Jetbrains Compose) with Jetpack Compose on Android.
*   **Persistence Stack:**
    *   The project uses **SQLDelight** for structured SQL databases (`MindBoostDecisionDatabase` is declared).
    *   **CRITICAL DISCOVERY:** For MindBoost `speed` and `focus` features, the codebase **does not use Room**. Instead, it uses `@Serializable` data classes and serializes data models to encrypted JSON strings persisted locally in `SecureKeyValueStore` (AES-256-GCM + Android KeyStore).
    *   Existing files `SpeedRecords.kt` and `SpeedMeasurementStore.kt` specifically document: *"este repo NO usa Room... su patrón establecido... es @Serializable + SecureKeyValueStore"*.
*   **Test Setup:** KMP tests in `shared/src/commonTest` and `shared/src/desktopTest` using standard `kotlin.test` assertions.
*   **Detekt Gating:** Detekt static analysis is configured. Detekt scans will fail on code quality issues or forbidden terminology. A terminology scanner (`arenaTerminologyScanner`) enforces bans on forbidden terms in some modules.

---

## 3. Non-Negotiable Product/Claims Guardrails

To prevent regulatory penalties (FTC/FDA) and maintain professional-grade positioning, you must follow these rules:
1.  **Banned Terms:** Do not use `diagnóstico`, `diagnostic`, `diagnosis`, `curar`, `cure`, `tratamiento`, `treatment`, `ADHD`, `TDAH`, `dementia`, `Alzheimer`, or `IQ` in any user-facing UI, documentation, code comments, or commit messages.
2.  **Allowed Terms:** Use `autoevaluación operativa` (operational self-assessment) and `telemetría local de rendimiento atencional` (attentional performance telemetry) for all product descriptions.
3.  **No Absolute Comparisons:** SRI scores must compare only the **same user** on the **same device** against their own historical baseline. You must never compare different users, different devices, or absolute processing latency rankings.
4.  **No Health/IQ Claims:** Do not claim the app treats cognitive decline, burnout, or increases general fluid intelligence.
5.  **Timing Claims:** Do not claim the timing Provider fully bypasses Android UI or hardware digitizer latency. Describe the timing mechanics as *reducing processing noise* to enable within-user relative comparison.
6.  **Integrity Limits:** Do not claim row-level checksums make the database "tamper-proof." Describe it as a *tamper-evident integrity check* that detects accidental or trivial modification, offering no protection against root device access.
7.  **Deferred Modules:** **Do NOT** implement the AHS box-swapping mutation (Sprint 2) or the Stress Tolerance Audit (Sprint 3). Keep E6 Multivía completely deterministic and basic in Sprint 1.

---

## 4. Sprint 1 Implementation Order

You must execute the implementation in these 11 sequential, test-backed stages:

```
[1. Baseline] -> [2. Time Provider] -> [3. Domain Models] -> [4. KV/Room Schema]
      |
      v
[5. Checksum] -> [6. SRI Engine]    -> [7. E1 Screen]     -> [8. E6 Basic Screen]
      |
      v
[9. CSV Export] -> [10. Offline Gate] -> [11. Verification]
```

### Stage 1: Build Baseline
*   **Goal:** Verify that the project compiles and existing unit tests pass before editing.
*   **Files:** `shared/build.gradle.kts`, dependencies.
*   **Verification:** Run `./gradlew :shared:desktopTest` and `./gradlew :shared:detekt`.

### Stage 2: Time Abstraction
*   **Goal:** Complete the timebase policy and timestamp provider interface.
*   **Files to Edit/Create:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimestampProvider.kt`
    *   `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimestampProvider.android.kt` (Android-specific low-level `MotionEvent` provider).
*   **Acceptance Criteria:** `getCurrentMonotonicNanos()` returns nanoseconds linked to `System.nanoTime()` (or uptime nanos) and input timestamps are read directly from `MotionEvent.getEventTimeNanos()` where available.

### Stage 3: Domain Models & Invalid-Trial Rules
*   **Goal:** Implement the `TrialEvent` class and add validity rules in the domain engine.
*   **Files:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/model/SpeedRecords.kt`
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TrialEngine.kt`
*   **Rules:** Invalid trials: anticipations $< 100\text{ms}$; timeouts $> 1500\text{ms}$ (E1) or $> 2000\text{ms}$ (E6). Mark `isValid = false` on `TrialRecord`.
*   **Acceptance Criteria:** Domain models compile with Kerialization support and invalid trials are classified correctly.

### Stage 4: Local Storage Persistence Schema
*   **Goal:** Align storage architecture.
*   **Architectural Conflict Resolution:** Do not introduce Room dependencies to avoid complex KMP build debt. Instead, extend the existing `@Serializable` + `SecureKeyValueStore` persistence models declared in `SpeedRecords.kt` and implemented in `SecureSpeedMeasurementStore.kt`. Persist `SessionRecord`, `BlockRecord`, and `TrialRecord` as part of the `MeasurementRun` object.
*   **Acceptance Criteria:** Storage reads and writes runs successfully through `SecureSpeedMeasurementStore`.

### Stage 5: Checksum / Integrity Layer
*   **Goal:** Add tamper-evident SHA-256 checksums to stored trial records.
*   **Files:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/model/SpeedRecords.kt`
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/data/persistence/SecureSpeedMeasurementStore.kt`
*   **Implementation:** Compute SHA-256 hash using trial variables and a local secret key. Discard records failing validation on load.
*   **Acceptance Criteria:** Verification fails if a trial record's reaction time value is manually altered in the local data model.

### Stage 6: SRI Scoring Engine
*   **Goal:** Create `SriScoringEngine.kt` to calculate the relative System Readiness Index (SRI) score.
*   **Files to Create:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/score/SriScoringEngine.kt`
*   **Acceptance Criteria:** Score returns a structured object matching the SRI Contract (including `score`, `confidence`, `baseline_days_available`, `valid_trials_count`, `invalid_trial_rate`, `explanation_text`, and `CV_RT`).

### Stage 7: E1 "Destello" Screen
*   **Goal:** Create the basic simple reaction time screen.
*   **Files:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e1/E1DestelloScreen.kt`
*   **Acceptance Criteria:** Render black screen, random jitter delay (1500ms-4000ms), emerald circle stimulus, VSYNC choreographer timestamp capture, and touch capture.

### Stage 8: E6 "Multivía" Screen (Basic, No AHS swaps)
*   **Goal:** Create the basic choice reaction time screen.
*   **Files:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e6/E6MultiviaScreen.kt`
*   **Acceptance Criteria:** Left and Right response boxes rendered in fixed positions. Jitter delay (1000ms-3000ms), visual arrow stimulus ($\leftarrow$ or $\rightarrow$). **No AHS button coordinate mutation** in Sprint 1.

### Stage 9: CSV Export
*   **Goal:** Export logs manually.
*   **Files:**
    *   `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/data/persistence/SpeedMeasurementStore.kt`
*   **Acceptance Criteria:** Export outputs plain text CSV containing `SessionID, ExerciseType, TrialIndex, LatencyMs, IsCorrect, SRI` to the local storage directory.

### Stage 10: Offline / Network Gating
*   **Goal:** Verify network isolation.
*   **Acceptance Criteria:** Run all tests in network-isolated sandboxes. No sockets opened.

### Stage 11: Tests & final verification
*   **Goal:** Verify entire Sprint 1 package.
*   **Verification:** Run `./gradlew :shared:desktopTest` and `./gradlew :shared:detekt`.

---

## 5. Data Model Contract

All models are defined in `com.mindos.app.features.mindboost.speed.domain.model.SpeedRecords.kt`:

1.  **SessionRecord:**
    *   `id`: `String` (UUID)
    *   `userId`: `String`
    *   `timestampEpochMs`: `Long`
    *   `deviceProfileId`: `String`
    *   `durationMs`: `Long`
2.  **BlockRecord:**
    *   `id`: `String` (UUID)
    *   `sessionId`: `String`
    *   `blockIndex`: `Int`
    *   `exerciseId`: `String` (e.g. "E1", "E6")
    *   `startedAtEpochMs`: `Long`
    *   `durationMs`: `Long?`
    *   `fixedDifficulty`: `Double`
    *   `deadlineNanos`: `Long?`
3.  **TrialRecord:**
    *   `id`: `String` (UUID)
    *   `blockId`: `String`
    *   `trialIndex`: `Int`
    *   `exerciseId`: `String`
    *   `stimulusUptimeNanos`: `Long`
    *   `responseUptimeNanos`: `Long`
    *   `rtNanos`: `Long`
    *   `isCorrect`: `Boolean`
    *   `isAnticipation`: `Boolean`
    *   `isLapse`: `Boolean`
    *   `isValid`: `Boolean`
    *   `checksum`: `String`
4.  **TrialEvent:**
    *   `stimulusUptimeNanos`: `Long`
    *   `responseUptimeNanos`: `Long`
    *   `isCorrect`: `Boolean`

*Note: Records must be immutable (declared as `val`). Checksums are calculated on the data fields of the `TrialRecord` before serialization.*

---

## 6. Timing Contract

*   **Monotonic Timebase:** Use nanoseconds derived from `System.nanoTime()` or `SystemClock.uptimeMillis() * 1_000_000` (on Android) to avoid system wall clock mutations (e.g., timezone changes).
*   **Android Touch Capture:** Extract nanosecond timestamps from `MotionEvent.getEventTimeNanos()` where API level $\ge 34$, falling back to `eventTime * 1_000_000` on older APIs.
*   **Choreographer Frame Capture:** Onset of stimulus must be registered in the Choreographer callback (`Choreographer.getInstance().postFrameCallback`), recording the `frameTimeNanos` variable.
*   **Latency Statement:** Timestamps reduce input processing thread noise and allow precise comparative variance analysis, but do not bypass hardware touchscreen/digitizer physics latency.
*   **Invalid Thresholds:**
    *   `rtNanos < 100_000_000` (100ms) $\rightarrow$ anticipation (invalid).
    *   `rtNanos > 1_500_000_000` (1.5s in E1) or `rtNanos > 2_000_000_000` (2.0s in E6) $\rightarrow$ timeout (invalid).

---

## 7. SRI Contract

`SriScoringEngine.kt` must return the following data structure:

```kotlin
data class SriResult(
    val score: Double, // Weighted percentage (0.0 to 100.0)
    val confidence: SriConfidence,
    val baselineDaysAvailable: Int,
    val validTrialsCount: Int,
    val invalidTrialRate: Double, // Ratio of invalid/total trials
    val cvRt: Double, // Coefficient of variation of valid trial reaction times
    val explanationText: String
)

enum class SriConfidence {
    HIGH,                 // Baseline >= 7 days, low variance
    MEDIUM,               // Baseline 4-6 days
    LOW,                  // Baseline 3 days, or high baseline variance
    INSUFFICIENT_BASELINE // Baseline < 3 days
}
```

### Confidence Thresholds Rule (Conservative Default):
*   `INSUFFICIENT_BASELINE` if `baselineDaysAvailable < 3`.
*   `LOW` if `baselineDaysAvailable == 3` OR (baseline `CV` $> 0.25$).
*   `MEDIUM` if `baselineDaysAvailable` is $4$, $5$, or $6$ AND (baseline `CV` $\le 0.25$).
*   `HIGH` if `baselineDaysAvailable` $\ge 7$ AND (baseline `CV` $\le 0.15$).

---

## 8. Exercise Contracts (Sprint 1 Only)

*   **E1 (Destello):** Simple Reaction Time. A single touch workspace. Emerald green circle stimulus triggers the response. Delay before stimulus must be random between 1500ms and 4000ms.
*   **E6 (Multivía):** Choice Reaction Time. Two fixed target boxes (Left Box, Right Box). Arrow stimulus ($\leftarrow$ or $\rightarrow$) is shown. Delay before stimulus is random between 1000ms and 3000ms.
*   **AHS Prohibited:** No swapped button layouts, color mutations, or dynamic coordinate changes. Buttons must remain fixed at their standard coordinates in Sprint 1.

---

## 9. Storage and Integrity Contract

*   **Zero-Cloud Encryption:** All files are serialized to JSON DTOs and stored in the encrypted `SecureKeyValueStore`. No Room database dependencies should be added.
*   **Tamper-Evident Integrity Check:** When storing `TrialRecord`, compute a SHA-256 hash using the record fields combined with a locally generated secret key. If a record loaded from `SecureKeyValueStore` has an invalid checksum, mark the session as contaminated and discard the values from the SRI calculation.
*   **Ethics Statement:** This checksum is tamper-evident only, designed to detect accidental file damage or trivial string manipulation, and does not protect against a root attacker with complete control of the device memory.

---

## 10. CSV Export Contract

*   **Manual Trigger Only:** The user must manually click "Export Logs" in settings. No automated exports or cloud uploads.
*   **Format:**
    `SessionID, ExerciseType, TrialIndex, LatencyMs, IsCorrect, SRI`
*   **Storage Location:** Save to the local application document directory (e.g. Android `context.filesDir` or Desktop local work directory).
*   **Privacy:** No user IDs or device hardware identifiers (IMEI, MAC address) may be included in the CSV file.

---

## 11. Test Plan

You must write and run the following tests:
1.  **Unit Tests (`shared/src/commonTest`):**
    *   `TimebasePolicyTest`: Verify that `computeRtNanos` computes correct nanosecond values and detects anticipations.
    *   `SriScoringEngineTest`: Feed mock sessions to verify score percentages, coefficient of variation, and correct `SriConfidence` enum state returns.
    *   `TrialIntegrityTest`: Verify SHA-256 checksum generation, and ensure that manually altering a reaction time value throws a verification failure.
2.  **Integration Tests (`shared/src/desktopTest`):**
    *   `SpeedStoreTest`: Verify writing and reading a complete `MeasurementRun` to/from `InMemorySpeedMeasurementStore` and check transaction data deletion.
    *   `NoNetworkCheck`: Run mock tests and assert that no network operations or web sockets are requested during execution.

---

## 12. Build/Verification Commands

You must verify your changes using these commands:
*   **JVM Unit Tests:** `./gradlew :shared:desktopTest`
*   **Detekt Linter:** `./gradlew :shared:detekt`
*   **Forbidden Terminology Check:** `./gradlew :shared:check` (ensuring no banned words such as 'clinical' or 'diagnóstico' are introduced in files).

---

## 13. Claude Code Execution Prompt

Copy and paste this exact prompt to initiate the coding execution phase:

```markdown
You are Claude Code Opus 4.8. Your task is to implement the Sprint 1 deterministic local/offline foundation for the MindBoost Speed (Velocidad Mental) module in Kotlin Multiplatform (KMP), native Android, and Compose Multiplatform.

Read all instructions below carefully before making any edits.

### 1. Hard Boundaries & Restrictions
* Do NOT use Room database. The project specifically rejects Room (as documented in `SpeedRecords.kt`) to avoid KMP build issues. Use the established `@Serializable` DTO + `SecureKeyValueStore` persistence pattern implemented in `SecureSpeedMeasurementStore.kt`.
* Do NOT implement AHS box-swapping button mutations (swaps) or the Stress Tolerance Audit. Defer them to Sprints 2 and 3 respectively. Keep E6 Multivía completely basic and deterministic.
* Do NOT call external web APIs, cloud databases, or AI runtimes. The module must operate 100% offline.
* Do NOT make or reference medical, clinical, ADHD, dementia, treatment, or IQ-improvement claims in code comments, UI text, or logs. 
* Use safe terminology: "autoevaluación operativa" (operational self-assessment) and "telemetría local de rendimiento atencional" (attentional performance telemetry).
* The System Readiness Index (SRI) must compare ONLY the same user on the same device. Never perform cross-user or cross-device rankings.

### 2. Implementation Scope (Sprint 1 Only)
1. Timing Abstraction: Implement `TimestampProvider` and `TimeSource` using monotonic system nanoseconds and MotionEvent inputs. Frame the timing provider as a noise reduction layer, not a full digitizer latency bypass.
2. Domain Models: Integrate `TrialEvent` and invalid trial classification (<100ms anticipation, timeouts >1.5s/2s) into the `TrialEngine` loop.
3. Storage: Extend the serialization schema to save sessions, blocks, and trials via the `SecureSpeedMeasurementStore` wrapper.
4. Integrity Check: Implement a SHA-256 row checksum on each TrialRecord to detect accidental data modification.
5. SRI scoring: Create `SriScoringEngine.kt` to calculate weighted readiness percentages, coefficient of variation (CV_RT), baseline days, and the `SriConfidence` enum output (HIGH/MEDIUM/LOW/INSUFFICIENT_BASELINE).
6. UI Screens: Create basic, Compose Multiplatform E1 (Destello) and E6 (Multivía - no mutations) screens wrapping the existing `SpeedExerciseScreen` component.
7. Local Export: Implement manual local CSV export of logs.
8. Verification: Add KMP unit and integration tests and ensure Detekt passes.

### 3. Execution Protocol
* Read files before editing. Do not rewrite files unless necessary; use precise patches.
* Preserve existing architecture, package structures, and naming conventions.
* Implement in small, verifiable steps. Run `./gradlew :shared:desktopTest` and `./gradlew :shared:detekt` after each milestone to ensure correctness.
* If you encounter minor ambiguities, select conservative deterministic defaults and report them in your summaries. Stop only for severe build-blocking conflicts.

Sprint 1 GO: “Sprint 1 GO for deterministic local timing/scoring/storage foundation. Stress Tolerance Audit deferred. AI runtime blocked. Clinical/IQ claims prohibited.”
```
