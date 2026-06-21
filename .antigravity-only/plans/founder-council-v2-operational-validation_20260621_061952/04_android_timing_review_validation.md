# Android Timing & Performance Validation Report

**Review Mode:** `ANDROID_TIMING_REVIEW`  
**Proposal:** "Is the Sprint 1 timing architecture using TimestampProvider, MotionEvent event timestamps, monotonic nanos, Choreographer validation, 60Hz/120Hz testing, and invalid trial rules sufficient for MVP alpha?"  
**Consolidated Verdict:** **REVISE**  
**Orchestration ID:** Run 3

---

## 1. Squad Voting & Attributions

| Subagent | Conversation ID | Vote | Confidence | Evidence Grade | Key Focus |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Android Performance** | `ae2a874b-466e-40fa-a439-2cb261acb698` | **GO** | High | A | Frame pacing, input loops, VRR. |
| **Tech Architecture** | `0fed9411-82a7-4d72-8c08-35bd6266ff68` | **CONDITIONAL GO** | High | B | Modularity boundaries, mock clocks. |
| **Exercise Engine** | `8f2e5e0c-e04b-4cd0-abc4-3bba14040027` | **REVISE** | High | A | State transitions, range mismatches. |
| **Scoring Engine** | `998a77fe-836e-47ae-b489-4fe060b00744` | **REVISE** | High | A | MAD filtering, lapse blindness. |
| **QA/Evidence** | `ba6695d7-4019-4519-9441-cb55e304d7a3` | **CRASHED** | - | - | Failed in step 114 (RESOURCE_EXHAUSTED). |
| **Red Team** | `18c0e9a8-9262-4b51-b39a-67def7524f6e` | **REVISE** | High | A | Code-level crash vectors, edge cases. |

---

## 2. Key Findings & Risks

### A. Code-Level Crash Vectors (Red Team Audit)
Direct inspection of `TrialEngine.kt` and `FixedTrialEngine.kt` revealed two severe stability bugs:
1.  **Double-Tap Crash:** `FixedTrialEngine.onResponse` assumes linear trial progression. When a user double-taps quickly, the first tap clears the `pending` trial object. The second tap event executes immediately before recomposition completes, invoking `requireNotNull(pending)` which throws an `IllegalArgumentException` and crashes the app.
2.  **Negative RT Crash:** If a user taps extremely quickly, or if input latency is low and device timestamps register the touch event slightly before display VSYNC frame times, `rtNanos` is calculated as negative. `FixedTrialEngine.onResponse` executes `require(TimebasePolicy.isValidRt)`, which throws an exception and crashes the app instead of marking the trial as an invalid/premature start.

### B. Cognitive Lapse Blindness in SRI (Scoring Audit)
Lapses represent correct trials with reaction times exceeding a multiplier of the median ($RT > lapseMultiplier \times RT_{med}$). 
1.  In `MetricCalculator.kt`, all central tendency metrics ($RT_{med}$, $CV_{RT}$) are calculated using `cleanHitRts` (which has outliers rejected via MAD filtering).
2.  Since cognitive lapses are significantly slower than normal trials, MAD identifies them as statistical outliers and discards them.
3.  As a result, a highly fatigued user experiencing frequent lapses will still maintain a low median RT and low CV, resulting in a perfect SRI of 100%—rendering the readiness index blind to attention drift and fatigue.

### C. Impulsivity & Race Condition Loops
- **Impulsivity Loophole:** Touches registered during the foreperiod phase reset the timer but are **not** written to the database as trials. This allows users to spam-tap the screen without penalizing their accuracy, false alarm rate (`faRate`), or impulsivity index (`impIdx`).
- **VSYNC Race Condition:** Touches registered in the sub-frame window between phase transition and VSYNC trigger throw exceptions that are caught and ignored, granting "free retries" instead of logging an anticipation.
- **Range Mismatch:** The hardcoded 800ms–2200ms foreperiod range in `SpeedSessionStateHolder.kt` does not align with the E1 (1500–4000ms) and E6 (1000–3000ms) specifications.

---

## 3. Required Revisions

1.  **Gate Input State:** Modify `TrialEngine.onResponse` to gracefully ignore pointer inputs when `pending` is null (already resolved/not started) instead of throwing an exception.
2.  **Soft Invalidations:** Handle negative RTs as invalid trials (false starts) instead of throwing/crashing.
3.  **Lapse Re-integration:** Compute the Coefficient of Variation ($CV_{RT}$) on raw hit RTs (before MAD filtering) so that lapses correctly inflate the standard deviation, lowering the consistency score and penalizing the final SRI.
4.  **Write Premature Taps:** Record foreperiod touches to the database as false starts to prevent spam-tapping exploits.
5.  **Synchronize Bounds:** Align the hardcoded foreperiod ranges in `SpeedSessionStateHolder.kt` with the official E1/E6 specifications.
