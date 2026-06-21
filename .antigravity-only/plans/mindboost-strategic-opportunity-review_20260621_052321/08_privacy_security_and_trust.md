# Privacy, Security, and Trust

This document establishes the guidelines, privacy boundaries, data sensitivity ratings, and anti-surveillance rules for MindBoost.

---

## 1. Sensitive Data Inventory

MindBoost collects granular biological and behavioral performance metrics. We classify this data to implement strict access controls.

| Data Type | Description | Sensitivity | Location | Protection Mechanism |
| :--- | :--- | :---: | :--- | :--- |
| **Raw Trial Latency** | Reaction speed values measured in nanoseconds. | **Medium** | Local Room DB | SQLCipher / Encryption |
| **Error Patterns** | Incorrect trial responses and target confusion logs. | **Medium** | Local Room DB | SQLCipher / Encryption |
| **System Readiness Index (SRI)** | Daily cognitive readiness score percentages. | **High** | Local Room DB | Signed hash chain + hardware key |
| **Biometric Proxies** | Reaction consistency, attention drift rates. | **High** | Local Room DB | Local secure key encryption |
| **On-screen Motor Telemetry** | Touch velocity and touch coordinate offsets. | **High** | Local Memory | Ephemeral (Discard after block) |
| **User Activity Logs** | App opening timestamps, session durations. | **Low** | Local Room DB | Standard DB protection |

---

## 2. Local-First Design & Zero-Cloud Default

To secure the trust of enterprise operators, software engineers, and security-minded professionals, MindBoost uses a **zero-cloud default**:

*   **No Remote Databases:** All trial records, baselines, and scoring metrics are persisted locally in SQLCipher-encrypted databases.
*   **Opt-in Data Sharing:** Users can export their logs as plain text (.csv/.json) manually. MindBoost does not upload metrics in the background.
*   **Anonymized Analytics:** App performance tracking (crash logs, layout errors) is strictly separated from cognitive telemetry. No cognitive values or baseline metrics can be sent to external observability systems like Crashlytics (complying with [telemetry minimisation rules](file:///c:/Users/curro/AndroidStudioProjects/MindOS/AGENTS.md#L62)).

---

## 3. Anti-Surveillance Boundaries

MindBoost rejects any feature that turns the app into surveillanceware:

*   **No Background Keylogging:** We do not track keystrokes outside the application to calculate cognitive fatigue. Tracking keyboard cadence is limited strictly to active trials inside the app.
*   **No Background Audio/Video:** The camera and microphone are never activated in the background to detect sleepiness or cognitive load.
*   **No Screen Recording:** The app does not record or analyze screenshots of other applications.

---

## 4. Trust Principles for MindBoost

1.  **Zero Information Asymmetry:** The user owns 100% of their raw performance data. They can delete it, inspect it, or export it at any time.
2.  **Verified Scientific Openness:** All algorithms (including the [StaircaseEngine](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L47) and SRI math) are documented in open files within the app, allowing professional inspection.
3.  **No Manipulation Loops:** The app does not use dark patterns or gamified streaks to force engagement. It relies on the raw utility of the morning readiness check.
4.  **No Diagnostic Hype:** If a user registers highly degraded scores, we state: *"High attentional variance detected. This score represents variance relative to your baseline, not a clinical diagnosis. Consider reviewing your sleep, caffeine, or stress levels."*
