# Product MVP Recommendation

This document outlines the Minimum Viable Product (MVP) specification for MindBoost, prioritizing core systems, user loops, retention mechanics, and scoring architecture.

---

## 1. Exact MVP Scope (The "Core Readiness Test")

The MVP must focus exclusively on measuring daily cognitive baseline variance and preventing game-specific muscle-memory. It rejects complex integrations, AI features, and medical claims.

### Top 3 Systems to Build First:
1.  **Staircase-Driven Baseline Engine (E1 & E6):**
    *   *Implementation:* [E1 (Destello)](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L37) (Simple reaction time) and [E6 (Multivía)](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L37) (Choice reaction time, 2-alternative).
    *   *Role:* Collects raw, deterministic millisecond-level data to calculate the user's daily baseline variance.
2.  **Adversarial Heuristic Saboteur (AHS - MVP Version):**
    *   *Implementation:* Swapping the left/right tap areas in E6 when the user hits a precision threshold of $>95\%$ over 10 consecutive trials.
    *   *Role:* Disrupts motor automation, keeping the cognitive challenge active and preventing cheating via muscle memory.
3.  **Readiness Score Engine:**
    *   *Implementation:* Translates raw reaction times, precision, and consistency (coefficient of variation) into a relative percentage compared to the user's personal 7-day baseline.
    *   *Role:* Provides the core utility: a daily readiness dashboard.

### Systems to Delay:
*   **Sensor-less Cognitive Load Estimator (SCLE):** Delay. Do not build touch pressure or typing cadence analysis in early stages.
*   **Ecological Workflow Overlay (EWO):** Delay. Do not attempt integrations with IDEs or external tools.
*   **Stress/Crisis Systems (AAO/ETIE):** Delay to Phase 3.
*   **Spatial-Acoustic Memory (SA-WMG):** Delay.

---

## 2. The 7-Day User Loop

```
[Day 1: Initial Calibration] ---> [Day 2-3: Stabilizing Baseline] ---> [Day 4+: Daily Readiness Check]
           |                                     |                                   |
    - 3-min E1 test                       - Morning check-in                  - 90-sec quick check
    - Establish raw RT                    - Measure deviations                - Readiness % output
```

*   **Day 1 (Onboarding & Calibration):** The user performs a 3-minute baseline setup (60 trials of E1, 60 trials of E6 at deterministic, non-adaptive speeds). This establishes their initial, clean-state baseline (reaction speed, error rates, consistency).
*   **Days 2-3 (Stabilization):** The user does a morning session (90 seconds). The system refines the baseline using a [StaircaseEngine](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L47) to find the user's cognitive limits.
*   **Days 4-7 (Daily Readiness):** The daily loop is established. The user opens the app before starting work, runs a 90-second "Readiness Test," receives their score (e.g., *98% - Ready*, or *84% - Attentional Instability Detected*), and optionally performs 3 minutes of training with AHS active to "warm up."

---

## 3. 30-Day Retention Mechanism

Instead of gamified badges, retention is driven by **utility and insight**:
1.  **Weekly Performance Snapshots:** A PDF/Markdown export detailing cognitive stability trends, reaction time variance, and cognitive fatigue thresholds.
2.  **Crisis Mode Sandbox:** An opt-in, 3-minute high-stress test that users can trigger when they are sleep-deprived or highly caffeinated to map their cognitive degradation limits.
3.  **Local History Log:** A local dashboard comparing productivity logs (manually input, e.g., "Good day," "Hard day") with daily cognitive readiness, proving the correlation between the score and real-world efficiency.

---

## 4. Scoring Concept (Conceptual Level)

The score is **strictly relative** to the user's own baseline, avoiding demographic vanity metrics:

$$\text{Readiness Score} = w_1 \cdot \text{AccuracyScore} + w_2 \cdot \text{SpeedScore} + w_3 \cdot \text{ConsistencyScore}$$

Where:
*   **AccuracyScore:** $\frac{\text{Current Accuracy}}{\text{Baseline Accuracy}} \times 100$ (capped at 100%).
*   **SpeedScore:** $\frac{\text{Baseline Median RT}}{\text{Current Median RT}} \times 100$.
*   **ConsistencyScore:** $\frac{\text{Baseline Coefficient of Variation}}{\text{Current Coefficient of Variation}} \times 100$, where Coefficient of Variation ($CV$) = $\frac{\text{RT Standard Deviation}}{\text{RT Mean}}$. High $CV$ represents attentional instability and micro-lapses.
*   Weights: $w_1 = 0.4$, $w_2 = 0.3$, $w_3 = 0.3$.

---

## 5. Offline and Local Constraints

*   **100% Local-First:** All raw trial metrics (`TrialRecord`), block configurations, and raw database entries are stored in a secure local database ([SecureKeyValueStore](file:///c:/Users/curro/AndroidStudioProjects/MindOS/AGENTS.md#L141)).
*   **Zero Telemetry for MVP:** No remote logging of cognitive profiles, avoiding security clearance problems for developer users.
*   **Deterministic Calculations:** Scoring and adaptation logic must be fully deterministic (using simple, tested mathematical models) without relying on remote servers.
