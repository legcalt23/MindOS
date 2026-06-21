# Required Code Fixes & Roadmap

This document outlines the precise code changes required to address the failures identified by the **MindOS Founder Council Extended V2** during the validation runs. 

No production code changes have been applied during this validation phase, in strict compliance with the **No-Implementation** guidelines.

---

## 1. Daily Streak Display (FAST_FEATURE_REVIEW Revisions)

To resolve the NO-GO verdict, the following code revisions must be implemented in the `shared` module:

```diff
// File: shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/FocusHomeStateHolder.kt

- fun estimateStreak(history: List<SessionRecord>): Int {
-     // GMT division bug: divides millis by 86400000 directly.
-     // Stale bug: returns active streak even if last session was months ago.
- }
+ fun estimateStreak(history: List<SessionRecord>, timeZone: TimeZone): Int {
+     if (history.isEmpty()) return 0
+     val localToday = Clock.System.now().toLocalDateTime(timeZone).date
+     // 1. Verify if the last session is stale (> 1 day old and not a recommended rest day)
+     // 2. Iterate back comparing local date boundaries
+     // 3. Freeze streak if OvertrainingGuard forced an active recovery day
+ }
```

-   **UX Update:** Replace cutesy fire/badge icons on `FocusHomeScreen.kt` with a low-contrast monospace text chip (e.g. `STREAK: 5`), matching the `StatChip` token aesthetics.
-   **Tests:** Add `FocusHomeStateHolderTest.kt` with unit tests covering timezone shifts and forced rest days.

---

## 2. System Readiness Index (PSYCHOMETRICS_SCORING_REVIEW Revisions)

Modify the SRI calculation engine to fix statistical distortion:

1.  **Lower Accuracy Weight:** Adjust formula weights in `MetricCalculator.kt`:
    -   Accuracy Weight: **15%** (used as a validation filter)
    -   Latency Weight: **45%** (log-transformed median RT)
    -   Consistency Weight: **40%** (log-transformed Coefficient of Variation)
2.  **Define Active-Session Window:** Update `MetricCalculator.kt` baseline calculation:
    -   Utilize the **last 7 completed training sessions** instead of calendar days.
    -   Add a staleness threshold that warns the user if the last session was $> 7$ calendar days ago, but do not clear the historical calibration.
3.  **Outlier & Swap Cleansing:**
    -   Exclude the first 2 transition trials immediately following a box swap or stimulus change from the CV ($CV_{RT}$) calculation, as these trials contain cognitive switching overhead rather than baseline speed.

---

## 3. Crash Prevention & Timing (ANDROID_TIMING_REVIEW Revisions)

Resolve the application-crashing bugs in the core engine before alpha tester deployment:

### A. Gate Input Pointer State in TrialEngine
Modify `FixedTrialEngine.kt` to handle double-taps safely:

```diff
// File: shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/FixedTrialEngine.kt

  override fun onResponse(responseUptimeNanos: Long, choice: SpeedChoice?): TrialRecord {
-     val pending = requireNotNull(pendingTrial) { "No pending trial found" }
+     val pending = pendingTrial ?: return dummyTrialRecord() // Ignore double taps gracefully
      pendingTrial = null
      ...
  }
```

### B. Soft-Invalidate Negative RTs
Instead of throwing exceptions on early taps, handle them as false starts:

```diff
// File: shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/FixedTrialEngine.kt

  val rtNanos = TimebasePolicy.computeRtNanos(pending.stimulusUptimeNanos, responseUptimeNanos)
- require(TimebasePolicy.isValidRt(rtNanos)) { "Negative reaction time detected" }
+ if (!TimebasePolicy.isValidRt(rtNanos)) {
+     return recordFalseStart(pending, responseUptimeNanos)
+ }
```

### C. Factor Lapses into SRI
Compute the Consistency Ratio using $CV_{RT}$ calculated on **raw hit RTs** (before MAD outlier filtering), ensuring that attentional lapses correctly lower the consistency index and penalize the SRI.
