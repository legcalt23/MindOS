# Core Exercise Specification: E1 (Destello) and E6 (Multivía)

---

## 1. Timing Policy and Temporal Baseline

To guarantee scientific credibility, MindBoost unifies all temporal calculations to:
*   **Timebase:** Monotonic system nanoseconds (`System.nanoTime()`).
*   **Android Policy:** `ANDROID_UI_UPTIME_NANOS`. Timestamps for user inputs must be read directly from `MotionEvent.getEventTimeNanos()` (or equivalent low-level input queues). This timing model does not claim to fully bypass all Android UI latency, but is used to reduce processing noise and enable within-user relative comparison on the same device.

---

## 2. E1 (Destello) — Simple Reaction Time

### Target Capacity:
Raw cognitive processing speed and basic motor execution.

### Behavior:
1.  **Preparation Phase:** The screen shows a clean black space with a central dim indicator (e.g., a faint gray ring).
2.  **Jittered Delay:** The system waits for a randomized delay interval between **1500ms and 4000ms** to prevent anticipatory tapping.
3.  **Stimulus Presentation:** A bright emerald circle appears in the center of the screen. The exact timestamp of this frame render is captured via choreographer callback (`StimulusTimestampNanos`).
4.  **User Response:** The user must tap the screen anywhere as fast as possible.
5.  **Response Capture:** The touch timestamp is captured (`ResponseTimestampNanos`).
6.  **Trial Interval:** The screen resets to black, waiting 1000ms before starting the next trial.

---

## 3. E6 (Multivía) — Choice Reaction Time

### Target Capacity:
Attentional selection, rule-matching, and cognitive gating.

### Behavior:
1.  **Preparation Phase:** The screen shows two distinct gray target areas (Left Box and Right Box).
2.  **Jittered Delay:** Random delay of **1000ms to 3000ms**.
3.  **Stimulus Presentation:** A target shape (Arrow pointing left $\leftarrow$ or Arrow pointing right $\rightarrow$) appears in the center of the screen.
4.  **User Response:**
    *   If Arrow points Left: User must tap the Left Box.
    *   If Arrow points Right: User must tap the Right Box.
5.  **AHS-lite Interaction:** (See [AHS-lite Spec](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/plans/mindboost-mvp-execution-spec_20260621_053204/03_ahs_lite_spec.md) for detail). The visual location of the response boxes can swap dynamically.

---

## 4. Trial Lifecycle

```
  [PREPARE] -> [JITTERED DELAY] -> [RENDER STIMULUS] -> [CAPTURE INPUT] -> [EVALUATE] -> [COOLDOWN]
      |              |                    |                    |              |             |
   Reset UI     1.5s - 4.0s        Choreographer Nanos    MotionEvent Nanos  Verify RT     1000ms
```

---

## 5. Scoring Inputs

For every trial, the engine records:
*   `trialIndex` (Int)
*   `stimulusTimestampNanos` (Long)
*   `responseTimestampNanos` (Long)
*   `reactionTimeMs` (Double): calculated as $\frac{\text{ResponseTimestampNanos} - \text{StimulusTimestampNanos}}{1,000,000}$
*   `isCorrect` (Boolean)
*   `difficultyState` (Double)

---

## 6. Invalid Trial Rules

Trials that violate physical thresholds are marked as `INVALID` and excluded from scoring:
*   **Anticipation / False Start:** Reaction times **under 100ms** are classified as anticipatory taps (cheating or predictive muscle twitch) and are invalidated.
*   **Timeout:** Reaction times **above 1500ms** in E1 or **above 2000ms** in E6 are flagged as lapses/distractions and invalidated.
*   **Double Tap:** If multiple touch events are registered within a single trial, only the first valid event is recorded; subsequent events are discarded.

---

## 7. Hardware-Latency Assumptions

Mobile screens suffer from tactile latency (input digitizer lag) and rendering latency (frame rate delays). MindBoost reduces processing noise and enables within-user relative comparison on the same device by:
1.  Using monotonic timing and input-event timestamps (such as `MotionEvent.getEventTimeNanos()`) rather than the software event processing timestamp. This timing model does not claim to fully bypass all Android UI latency, but it stabilizes measurements to reduce noise.
2.  Calibrating the System Readiness Index (SRI) as a relative change within the **same device** and for the **same user** compared to their personal baseline. If a device has a constant 40ms digitizer lag, it affects the baseline and daily logs equally, canceling out the hardware latency variance.

