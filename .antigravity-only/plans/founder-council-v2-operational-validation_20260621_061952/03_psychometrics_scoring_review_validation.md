# Psychometrics & Scoring Validation Report

**Review Mode:** `PSYCHOMETRICS_SCORING_REVIEW`  
**Proposal:** "Should SRI use 40% accuracy, 30% latency, 30% consistency with confidence output and within-user 7-day baseline?"  
**Consolidated Verdict:** **REVISE**  
**Orchestration ID:** Run 2

---

## 1. Squad Voting & Attributions

| Subagent | Conversation ID | Vote | Confidence | Evidence Grade | Key Focus |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Psychometrics/Measurement** | `e8a5d67d-777c-4bda-be26-31b5a448b6fc` | **REVISE** | High | A | Formulas, consistency ratio paradox. |
| **Scoring Engine** | `67c97987-84cc-4e4d-bb45-1fa0e018e870` | **REVISE** | High | A | Edge cases, division-by-zero, timeouts. |
| **Cognitive Science** | `e0ae0c8f-41d3-4d87-ba3c-e54c1af8efbc` | **GO (Cond.)** | High | A | Construct validity, SATO tradeoffs. |
| **Metacognition** | `a74110aa-b438-4184-9921-4b53f4006ae8` | **REVISE** | High | B | Label clarity, self-efficacy loops. |
| **QA/Evidence** | `80eab452-cb70-4b21-8477-57079c480ded` | **REVISE** | High | C | Log-transforms compliance, baseline drift. |
| **Red Team** | `fde85ace-edd2-437f-a7b0-cf04e42aba97` | **REVISE** | High | B | Gaming vulnerability, FDA/FTC risk. |

---

## 2. Key Findings & Risks

### A. The Accuracy Compression & Gaming Loophole
Accuracy on simple attentional reaction tasks (like E1/E6) is naturally high (96%–100%). Weighting accuracy at 40% creates a "dead-weight" buffer:
1.  **SATO Gaming:** Fatigued users can artificially maintain a high SRI score by slowing down drastically. Since accuracy is weighted heavily and latency ratios are capped at 1.0, the speed penalty is offset by the perfect accuracy score.
2.  **Sensitivity Loss:** A 40% accuracy weight hides early prefrontal fatigue, which primarily manifests as increased reaction time latency ($RT_{med}$) and latency variability ($CV_{RT}$) rather than dropped trials.

### B. The Consistency Ratio Paradox
The formula uses the ratio of baseline coefficient of variation to current coefficient of variation:
$$\text{ConsistencyRatio} = \frac{CV_{baseline}}{CV_{current}}$$
1.  **Stability Penalty:** A highly consistent user (very low $CV_{baseline}$) will experience a major drop in their ConsistencyRatio if their reaction time deviates by even a few milliseconds, resulting in a false-alarm SRI drop.
2.  **Volatility Reward:** A user with a highly noisy baseline ($CV_{baseline}$ is high due to poor practice habits) is rewarded since a slightly noisy current session will still easily yield a ratio $\ge 1.0$.
3.  **Division by Zero:** If a user achieves perfect timing consistency or is simulated (making $CV_{current} = 0$), the formula crashes.

### C. Rolling Calendar-Day Baseline Pollution
- **Learning Curves:** During the first 7–14 days, a user's performance naturally improves due to task learning (habituation). A rolling 7-day average baseline will contain these slower onboarding times, artificially inflating current performance ratios.
- **Calendar Gaps:** If a user takes a weekend break, a calendar-day baseline expires, requiring a cold-start reset.

---

## 3. Required Revisions

1.  **Recalibrate Weights:** Reduce the accuracy weight to 10%–15% (or use it purely as a validation gate). Increase latency weight to 45% and consistency weight to 40%–45% to maximize sensitivity to fatigue.
2.  **Define Session-Based Baselines:** Redefine the 7-day baseline to use the **last 7 completed active training sessions** rather than 7 calendar days to handle weekend gaps.
3.  **Incorporate Log-Normalization:** Comply with the SRI/Scoring quality gate by utilizing log-transformed reaction times for latency and CV calculations, mitigating the skewness of raw RT distributions.
4.  **Lapse Accounting:** Account for timeouts and lapses (RT > 3x median) inside the scoring model rather than silently discarding them, as lapses are direct markers of attentional drift.
5.  **Soften Copy Terminology:** Replace scary labels like `CRITICAL LATENCY DEGRADATION` with non-clinical terms (`ATTENTIONAL VARIANCE DETECTED`) to avoid FDA/FTC classification as a medical diagnostic device.
