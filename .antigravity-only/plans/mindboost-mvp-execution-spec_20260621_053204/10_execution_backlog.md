# Execution Backlog

This backlog outlines the sprints and tasks required to build the MindBoost MVP Local Offline.

---

## Epic 1: Timing Core & Calibration (Priority: Critical)

### User Story 1: Precision Timing Pipeline
*   **As a** system developer,
*   **I want** stimulus and input events to be captured with sub-millisecond nanosecond accuracy,
*   **So that** hardware latency variations do not corrupt my daily baseline metrics.
*   **Tasks:**
    *   Implement the `TimestampProvider` using low-level Android MotionEvent nanos.
    *   Implement Choreographer rendering sync to capture exact VSYNC visual presentation frames.
*   **Acceptance Criteria:** Standard deviation of synthetic timing sweeps on test devices remains $<3\text{ms}$.
*   **Dependencies:** None.

### User Story 2: Baseline Calibration
*   **As a** new operator,
*   **I want** to perform a 3-minute onboarding test,
*   **So that** my rolling baseline parameters are established.
*   **Tasks:**
    *   Implement the onboarding calibration flow (60 trials E1, 60 trials E6).
    *   Store baseline values in local SharedPreferences.
*   **Acceptance Criteria:** Rolling baseline generates stable average numbers after one run.

---

## Epic 2: Core Exercises & AHS-lite (Priority: High)

### User Story 3: E1 & E6 Exercise Core
*   **As a** training user,
*   **I want** to execute E1 (Destello) and E6 (Multivía) tasks with jittered start times,
*   **So that** I cannot predict stimulus onset.
*   **Tasks:**
    *   Build Jetpack Compose E1 game board.
    *   Build Jetpack Compose E6 game board.
    *   Implement randomized jitter timers (1000ms - 4000ms).
*   **Acceptance Criteria:** Tapping before stimulus triggers an "Anticipation/Invalid" state and restarts the jitter timer.

### User Story 4: AHS-lite Box Swapping
*   **As a** training user,
*   **I want** target box positions to swap dynamically during E6 drills,
*   **So that** I cannot cheat using muscle memory.
*   **Tasks:**
    *   Build AHS state monitor in `TrialEngine`.
    *   Trigger swap after 8 consecutive correct trials.
    *   Render Blue border transition highlight on swap.
*   **Acceptance Criteria:** Target boxes visually swap positions on the horizontal axis.

---

## Epic 3: Scoring & Local Storage (Priority: High)

### User Story 5: SRI Calculation
*   **As a** daily user,
*   **I want** my score to be calculated relative to my own baseline,
*   **So that** I see daily fluctuations.
*   **Tasks:**
    *   Implement standard deviation and coefficient of variation math in pure Kotlin.
    *   Write the weighted SRI calculator ($40\%$ Accuracy, $30\%$ Speed, $30\%$ Consistency).
*   **Acceptance Criteria:** Output SRI ranges from $0\%$ to $100\%$ and does not leak outside local database.

---

## Sprint 1 Core Scope (Attentional Performance Telemetry Foundation)
The following deliverables are scheduled for Sprint 1:
1.  **Timing Foundation:** `TimestampProvider.kt` and `TimeSource` abstraction (monotonic timing and input events).
2.  **Domain Models:** `TrialEvent` domain model and invalid trial classification logic.
3.  **Local Storage:** SQLite schema via Room: `SessionRecord`, `BlockRecord`, and `TrialRecord`.
4.  **Integrity:** Trial checksum/hash engine for basic integrity checks.
5.  **Scoring Engine:** `SriScoringEngine.kt` (relative scoring, coefficient of variation, baseline days, and confidence output calculations).
6.  **Core Exercises:** E1 (Destello) and E6 (Multivía) basic exercises engines.
7.  **Data Export:** Local evidence log export system (CSV format).
8.  **Verification:** Comprehensive unit/integration tests and no-network validation suite.

## Deferred / Post-Sprint 1 Modules:
*   **AHS-lite Engine:** Dynamic layout-swapping logic (Epic 2, User Story 4).
*   **Stress Tolerance Audit (Safe Crisis Mode):** Controlled stress-challenge sandbox (Epic 4 - Deferred).
*   **Biometric Wearable Integrations / SCLE Telemetry:** (Epic 5 - Deferred).

