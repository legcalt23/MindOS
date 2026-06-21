# Product Requirement Document (PRD): MVP Local Offline

---

## 1. Product Goal
To build a deterministic, offline-first mobile operational self-assessment tool that measures daily cognitive baseline variance (reaction speed, accuracy, attentional stability) and provides a daily **System Readiness Index (SRI)** to prevent cognitive lapses in high-pressure professionals.

---

## 2. Target User
*   **Primary Profile:** Software developers, system operators, quantitative traders, and technical builders.
*   **Key Characteristics:** Data-driven, highly sensitive to data privacy, works in high-pressure/high-focus environments, appreciates utility over gamification.

---

## 3. Core Use Cases

### A. The Morning Readiness Audit
*   *Scenario:* Before starting their work shift or deploying code, the user opens MindBoost, runs a 90-second cognitive check, and receives a readiness percentage.
*   *Value:* Tells the user if their cognitive stability is degraded due to fatigue or stress, allowing them to adjust their task allocation for the day.

### B. Natural Fatigue Mapping (Crisis Mode Sandbox)
*   *Scenario:* A user is feeling fatigued after a long coding session or a late night. They opt-in to a 3-minute **Stress Tolerance Audit** to map their cognitive degradation floor.
*   *Value:* Verifies their cognitive limits and validates how their baseline changes under fatigue.

---

## 4. Daily Session Structure

The entire daily session is designed to take **under 2 minutes**:
1.  **Launch (10s):** App displays the daily dashboard and prompts: *"System ready for daily check."*
2.  **Daily Drill (90s):**
    *   30 trials of **E1 (Destello)** (Simple Reaction Time).
    *   30 trials of **E6 (Multivía)** (Choice Reaction Time with AHS-lite active).
3.  **Result & Analytics (20s):** The app calculates the System Readiness Index (SRI) and displays:
    *   SRI: `94% (Stable)` or `82% (Cognitive Drift Detected)`.
    *   Reaction time latency (median in milliseconds).
    *   Consistency score (coefficient of variation).
    *   Simple visual graph comparing the values to the rolling 7-day average.

---

## 5. Onboarding Flow

1.  **Privacy Lock:** The app starts with a screen declaring: *"All data is stored locally on this device. MindBoost is 100% offline. No cloud sync, no tracking."* The user taps **Agree**.
2.  **Operator Persona Selection:** User chooses their role (Developer, Trader, Operator, Student). This is used only for in-app micro-copy personalization.
3.  **Baseline Calibration Setup:** The screen explains: *"We need to establish your clean baseline. You will run a 3-minute, double-length session. Keep distractions to a minimum."*
4.  **Calibration Run:** User performs 60 trials of E1 and 60 trials of E6.
5.  **Dashboard Generation:** The calibration outputs the initial 7-day baseline values, and the user is guided to their daily dashboard.

---

## 6. Core Screens (Android UI)

1.  **Dashboard Screen:** Dark gray background (`#121212`). Displays the daily SRI %, raw latency metrics, daily training prompt, and history graph. Monospace fonts for numbers.
2.  **Exercise Workspace Screen:** Clean, high-contrast, black workspace. Minimal visual elements to prevent distraction. The stimulus (flashing target) uses highly visible colors.
3.  **Drill Summary Screen:** Displays daily SRI metrics compared to the rolling baseline, a brief explainability note, and a button to close or run the optional Stress Audit.
4.  **History & Settings Screen:** Shows historical daily logs, a button to purge all local databases, and a button to export data as CSV.

---

## 7. Non-Goals (MVP Out of Scope)
*   No cloud database (Firebase, AWS) synchronization.
*   No user account registration or email login.
*   No user-vs-user leaderboard or competitive ranking.
*   No background telemetry logging (monitoring other app activities).
*   No camera-based tracking or gaze tracking.

---

## 8. Acceptance Criteria

*   **Timing Accuracy:** All stimulus-response latencies must be calculated using monotonic system nanoseconds, filtering out UI rendering delays.
*   **Offline Operation:** The app must compile, install, run, calibrate, and score with airplane mode enabled.
*   **Data Deletion:** Tapping "Purge Data" in Settings must delete all SQLite files and local key-value stores from the device's storage.
*   **No Crashlytics Leakage:** Telemetry reports or error logging must never include raw reaction times or SRI scores.
