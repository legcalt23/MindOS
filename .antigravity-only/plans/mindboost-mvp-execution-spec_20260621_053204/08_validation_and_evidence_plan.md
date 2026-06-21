# Validation and Evidence Plan

This document establishes the protocols to scientifically validate MindBoost's metrics and ensure physical and technical reliability.

---

## 1. Natural Fatigue Observational Validation

Rather than inducing sleep deprivation (which raises safety and legal concerns), MindBoost relies on **Observational within-user validation**:

```
[User Day-to-Day Operation] ---> [Collect SRI Telemetry] ---> [Correlate with Logs]
                                                                     |
                                  Compare variance to           - Sleep hours
                                  rolling averages              - Caffeine intake
                                                                - Subjective fatigue (1-5)
```

### Protocol:
1.  During the daily morning drill, the user logs their subjective state via three simple sliders:
    *   *Sleep Quality:* (1 = Restless, 5 = Excellent)
    *   *Energy Level:* (1 = Exhausted, 5 = Peak Energy)
    *   *Caffeine/Stimulant intake:* (Yes / No)
2.  The local database logs these variables alongside the calculated SRI.
3.  After 14 days, the engine analyzes the correlation. If the system is valid, a lower energy rating should correlate with a higher Coefficient of Variation ($CV_{RT}$) and lower speed scores.

---

## 2. Hardware Stability Tests

We must verify that different touch digitizers and screen refresh rates (60Hz vs 120Hz) do not distort the relative metrics:
*   **Static Lag Test:** Run 100 automated visual stimulus-response loops using a hardware testing rig or synthetic input injectors. The standard deviation of the measured latency must remain under **3ms**.
*   **Warmup Validation:** Ensure the device's CPU scheduler does not throttle thread speeds during the first 3 trials. We resolve this by running 3 un-scored "Warmup Trials" before starting the daily baseline block.

---

## 3. Scoring Stability Tests

We must ensure the SRI score is stable and does not fluctuate erratically under normal conditions:
*   **Baseline Lock Test:** Feed the scoring algorithm 7 days of stable reaction time values (mean $250\text{ms}$, standard deviation $20\text{ms}$). The generated SRI must sit between $95\%$ and $100\%$.
*   **Fatigue Sensitivity Test:** Feed a simulated fatigue scenario (mean reaction speed drops to $320\text{ms}$, standard deviation spikes to $60\text{ms}$). The algorithm must output an SRI $< 80\%$ (triggering a `COGNITIVE DRIFT DETECTED` flag).

---

## 4. User Comprehension Tests

We must verify that users interpret their System Readiness Index (SRI) correctly:
*   **Test Criteria:** User onboarding interviews must confirm that testers understand that an SRI of $84\%$ means:
    *   *"My focus is unstable today compared to my average."*
    *   **NOT:** *"I am losing intelligence or my brain is damaged."*

---

## 5. Evidence Required Before Claims

To maintain scientific credibility, we must meet these evidence thresholds before publishing claims:

| Public Claim | Required Internal Validation Threshold | Verification Method |
| :--- | :--- | :--- |
| *“Tracks daily cognitive readiness.”* | Correlation of $R > 0.60$ between SRI drops and low self-reported energy logs. | 14-day study on a cohort of 20 alpha testers. |
| *“Bypasses the game-practice effect.”* | AHS-trained cohort must show flat or declining learning curves on baseline tests, compared to control groups. | 30-day active-controlled internal cohort trial. |
| *“Measures attentional stability.”* | Coefficient of Variation ($CV_{RT}$) spikes must align with PVT-standard micro-lapses. | Correlation validation against academic PVT references. |
