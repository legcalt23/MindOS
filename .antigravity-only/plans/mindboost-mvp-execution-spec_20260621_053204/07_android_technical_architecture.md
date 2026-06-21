# Android Technical Architecture

---

## 1. Architectural Modules (KMP Workspace)

MindBoost uses a clean, domain-driven multiplatform module structure:

```
shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/
├── core/
│   ├── time/             // Timing policies and Monotonic nanosecond interfaces
│   └── di/               // Local dependency graph (AppGraph)
├── speed/
│   ├── domain/           // Pure Kotlin logic (StaircaseEngine, SRI formulas)
│   │   ├── StaircaseEngine.kt
│   │   └── SriScoringEngine.kt
│   ├── data/             // Local repositories & Room DB models
│   │   ├── database/
│   │   └── repositories/
│   └── presentation/     // ViewModels and UI state representations
```

---

## 2. Timing Module (`TimestampProvider`)

The sub-millisecond physical measurement loop is defined by the [TimestampProvider](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L90) interface.

*   **Android Implementation:** Use `android.view.MotionEvent.getEventTimeNanos()` to capture the physical screen touch event time.
*   **Choreographer Hook:** Synchronize the stimulus display with the monitor's vertical sync (VSYNC) via `Choreographer.getInstance().postFrameCallback` to capture the precise frame render time.
*   **Latency Policy:** This timing pipeline does not claim to fully bypass Android UI latency; it is designed to reduce processing noise and enable within-user relative comparison on the same device.

---

## 3. Exercise Engine (`TrialEngine`)

The engine manages state transitions in a thread-safe manner, executing asynchronously on a dedicated high-priority CPU thread:

```kotlin
interface TrialEngine {
    fun startTrial(difficulty: Double, deadlineNanos: Long?): Long
    fun onResponse(responseUptimeNanos: Long, isCorrect: Boolean): TrialRecord
    fun getTrialHistory(): List<TrialRecord>
}
```

*   **Thread Safety:** Inputs are dispatched to a queue and processed sequentially. No parallel state mutations are allowed during a block.

---

## 4. Scoring Engine

The scoring engine resides entirely within the domain layer, making it independent of Android dependencies:

```kotlin
class SriScoringEngine {
    fun calculateSri(
        currentMetrics: BlockMetrics,
        baselineMetrics: BaselineMetrics
    ): Double {
        // Deterministic formula execution (See SRI Scoring Spec)
    }
}
```

---

## 5. Local Persistence & Telemetry

*   **Database:** Standard Android Room database configured with a local SQLite engine.
*   **Data Minimization:** No third-party analytics libraries (Firebase, Amplitude) are initialized in the `speed` module. All metrics remain locked inside the local DB.

---

## 6. UI State Flow

MindBoost uses Jetpack Compose for UI state management. The state flow uses unidirectional data streams:

```
[User Action: Tap] ---> [ViewModel: Process Input] ---> [TrialEngine: Mutate State]
         ^                                                        |
         |                                                        v
[UI Render Screen] <-------------------------------------- [UI State Update]
```

*   **UI Thread Isolation:** Exercises run in full-screen setups. Composable recompositions are minimized during active trials to prevent frame drops that could impact reaction times.

---

## 7. Testability Requirements

*   **Platform Abstraction:** All domain logic (Staircase, SRI formulas, AHS rules) must compile on Desktop and iOS targets, enabling local unit tests without launching an Android emulator.
*   **Deterministic Mock Clocks:** The timing provider must allow an injected mock clock (`MockTimestampProvider`) to run unit tests with deterministic millisecond sequences.
