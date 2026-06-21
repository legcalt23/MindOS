# Test Plan

This document outlines the testing strategies and validation suites for the MindBoost MVP Local Offline build.

---

## 1. Unit Tests (Pure Kotlin CommonMain)

### A. SRI Score Calculations (`SriScoringEngineTest`)
*   **Test Case 1 (Perfect Match):** If current reaction times and accuracy match the baseline exactly, SRI must output $100\%$.
*   **Test Case 2 (Fatigue Signature):** If median RT increases by $30\%$ and consistency ($CV_{RT}$) drops, verify that the SRI score drops below $80\%$, triggering the warning flag.
*   **Test Case 3 (High Accuracy / Slow Speed):** Verify that high accuracy cannot hide very slow reaction times (Speed weight operates independently).

### B. AHS Rule Mutator (`AhsEngineTest`)
*   **Test Case 4 (Streak Switch):** Verify that after $N$ consecutive correct inputs, the engine swaps target coordinates.
*   **Test Case 5 (Cooldown Gate):** Verify that the engine prevents a second swap from occurring within 6 trials of the first swap.

---

## 2. Timing and Input Latency Tests

### A. Precision Verification (`PrecisionTimingTest`)
*   **Test Case 6 (Mock Latency):** Inject a simulated digitizer lag. Verify that the [TimestampProvider](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L90) calculates relative differences correctly without accumulating error.
*   **Test Case 7 (Choreographer Frame Sync):** Verify that visual stimulus onset timestamps match monitor refresh frame boundaries (VSYNC events).

---

## 3. Storage & Integrity Tests

### A. Database Verification (`DatabaseIntegrityTest`)
*   **Test Case 8 (Row Checksum Validation):** Insert a mock row with a manual checksum. Modify the row data directly using raw SQL commands. Verify that the app's startup integrity check flags the row as corrupted.
*   **Test Case 9 (Purge Command):** Run the purge command. Verify that the local database file size returns to zero or is deleted from storage.

---

## 4. No-Network & Privacy Tests

### A. Offline Validation (`OfflineIsolationTest`)
*   **Test Case 10 (Airplane Mode Execution):** Run all baseline tests, daily drills, and score calculations under network isolation. Ensure no sockets are opened and no web calls are made.
*   **Test Case 11 (Telemetry Minimization):** Verify that Crashlytics exports do not include data payloads from `SessionRecord` or `TrialRecord` schemas.

---

## 5. Invalid Trial Tests

### A. Threshold Safeguards (`InvalidTrialTest`)
*   **Test Case 12 (False Start):** Tapping within 100ms of stimulus onset must mark the trial as `INVALID` and exclude it from the speed metrics.
*   **Test Case 13 (Timeout):** Failing to respond within 1500ms (E1) or 2000ms (E6) must mark the trial as a `TIMEOUT` and exclude it from the speed calculations.
