# System Readiness Index (SRI) Scoring Specification

---

## 1. System Readiness Index (SRI) Definition

The **System Readiness Index (SRI)** is a deterministic percentage score ($0\% \text{ to } 100\%$) indicating how close the user's current cognitive capacity is to their established 7-day baseline. 

### Explicit Comparison Rules
*   **MindBoost SRI must compare:**
    *   The same user.
    *   The same device.
    *   The current session vs the user's personal historical baseline.
*   **MindBoost SRI must NOT compare:**
    *   Different users.
    *   Different devices.
    *   Absolute intelligence (no IQ claims).
    *   Clinical status or medical conditions.

---

## 2. All Input Metrics

The scoring engine reads raw metrics from the local database:
1.  **Median Reaction Time ($RT_{\text{med}}$):** The median latency in milliseconds of all valid trials during the E1 and E6 daily blocks.
2.  **Coefficient of Variation ($CV_{RT}$):** Calculated as:
    $$CV_{RT} = \frac{\sigma_{RT}}{\mu_{RT}}$$
    Where $\sigma$ is the standard deviation and $\mu$ is the mean of reaction times. $CV_{RT}$ measures attentional consistency; higher variation indicates micro-lapses and fatigue.
3.  **Accuracy Rate ($ACC$):** The ratio of correct trials to total valid trials.

---

## 3. Within-User Baseline Method & 7-Day Moving Average

MindBoost does not compare raw reaction times between different users, as hardware, age, and individual baselines vary.

*   **Establishing the Baseline:** The baseline comprises the rolling 7-day average of $RT_{\text{med}}$, $CV_{RT}$, and $ACC$.
*   **The Baseline Window:** A minimum of 3 training days is required to generate the initial baseline. Until day 3, the dashboard displays: *"Calibrating Baseline..."*

---

## 4. SRI Mathematical Formulas

The daily SRI is calculated by evaluating the current day's performance against the baseline values:

$$\text{SRI} = 0.40 \cdot \text{AccRatio} + 0.30 \cdot \text{SpeedRatio} + 0.30 \cdot \text{ConsistencyRatio}$$

Where:

1.  **AccRatio:**
    $$\text{AccRatio} = \frac{ACC_{\text{current}}}{ACC_{\text{baseline}}}$$
    *(capped at $1.0$)*
2.  **SpeedRatio:**
    $$\text{SpeedRatio} = \frac{RT_{\text{baseline}}}{RT_{\text{current}}}$$
    *(capped at $1.0$)*
3.  **ConsistencyRatio:**
    $$\text{ConsistencyRatio} = \frac{CV_{\text{baseline}}}{CV_{\text{current}}}$$
    *(capped at $1.0$)*

---

## 5. Confidence Bands and Variance Flags

To prevent daily score noise from causing user anxiety:
*   **Normal Variance Band:** If the final calculated SRI is $\ge 90\%$, it is flagged as `SYSTEM STABLE`.
*   **Mild Drift Band:** SRI between $80\%$ and $89\%$ is flagged as `COGNITIVE DRIFT DETECTED` (typical signature of mild fatigue or distraction).
*   **Critical Variance Band:** SRI $< 80\%$ is flagged as `CRITICAL LATENCY DEGRADATION` (indicates high likelihood of sleep-deprivation or severe cognitive fatigue).

---

## 6. Invalid Data Handling

If a session is interrupted, or if more than $30\%$ of the trials are flagged as `INVALID` (due to timeouts or false starts), the daily SRI calculation is aborted for that day. The system alerts the user: *"Drill aborted: High rates of attentional variance or input anomalies detected. Please run the drill again."*

---

## 7. Prohibition of Cross-User Ranking

The database schemas do not contain fields to export or rank users against other users. The app copy reinforces this concept:
*   **Prohibited Visuals:** No global leaderboards.
*   **Prohibited Comparisons:** No percentile rankings (e.g., "You are faster than 80% of developers").

---

## 8. Dashboard Explainability Copy

When the user taps their SRI score, the app displays:
> *"The System Readiness Index (SRI) compares your processing speed, accuracy, and attention stability today against your personal 7-day average. It represents daily cognitive variance, not clinical performance or general intelligence."*

---

## 9. SRI Confidence Output Structure

The SRI scoring engine does not return a simple raw number. It returns a structured data object containing the following parameters:

*   **score** (Double): The calculated weighted percentage ($0.0 \text{ to } 100.0$).
*   **confidence** (Enum):
    *   `HIGH`: Baseline established with $\ge 7$ days of data and low recent variance.
    *   `MEDIUM`: Baseline established with $4 \text{ to } 6$ days of data.
    *   `LOW`: Baseline established with $3$ days of data, or high baseline variance.
    *   `INSUFFICIENT_BASELINE`: Fewer than 3 days of valid data available.
*   **baseline_days_available** (Int): Number of historic days contributing to the current baseline ($0 \text{ to } 7$).
*   **valid_trials_count** (Int): The number of trials in the current session marked as valid.
*   **invalid_trial_rate** (Double): The percentage of total trials in this session that were classified as invalid (timeouts or false starts).
*   **explanation_text** (String): A user-friendly, non-clinical explanation of the current result based on the confidence level and score band.

