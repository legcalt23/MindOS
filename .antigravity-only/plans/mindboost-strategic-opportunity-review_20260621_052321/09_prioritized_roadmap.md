# Prioritized Roadmap

This document outlines the phased development plan for MindBoost, setting clear gates and milestones from early design to advanced integrations.

---

## 1. Phased Development Roadmap

```
  PHASE 0              PHASE 1              PHASE 2              PHASE 3              PHASE 4
  Design Freeze   -->  MVP Local Offline ->  Personalization ->   Pressure/Stress  -->  Workflow Overlay
  - Calibrate E1/E6    - SRI Core scoring   - Multi-signal       - Crisis Mode        - IDE extensions
  - Safe claims doc    - AHS MVP engine     - Historical trend   - Stress feedback    - Zero-cloud B2B
```

### Phase 0: Evidence and Design Freeze
*   **Tasks:**
    *   Verify [TimebasePolicy ANDROID_UI_UPTIME_NANOS](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L3) to ensure latency timing precision.
    *   Freeze claims policy and legal boundaries.
    *   Draft deterministic scoring formulas for the System Readiness Index (SRI).
*   **Gate to Progress:** Deterministic temporal measurement accuracy verified to be $<1\text{ms}$ on test devices.

### Phase 1: MVP Local Offline (Hito 0 & Hito 1)
*   **Tasks:**
    *   Build local database schema (Room / SecureKeyValueStore) to persist trial-level data.
    *   Implement [E1 (Destello)](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L37) and [E6 (Multivía)](file:///c:/Users/curro/AndroidStudioProjects/MindOS/MindBoost_Velocidad_Mental_Plan_v2_2_3.md#L37).
    *   Build the core **Heuristic Disruption Engine (AHS)** (target swapping).
    *   Build the System Readiness Index (SRI) scoring algorithm.
*   **Gate to Progress:** 14-day local testing completed by internal beta testers with zero database sync issues and stable staircase difficulty curves.

### Phase 2: Personalization and Telemetry Analytics
*   **Tasks:**
    *   Build weekly diagnostic reports and data export functions (.csv/.json).
    *   Introduce multi-signal adaptation (calibrating staircase limits based on reaction speed consistency and error patterns).
    *   Refine baseline algorithms using rolling 7-day averages.
*   **Gate to Progress:** Positive correlation ($R > 0.6$) demonstrated between lower SRI scores and self-reported fatigue/sleep deprivation logs in beta cohort.

### Phase 3: Pressure and Stress Systems
*   **Tasks:**
    *   Build **Crisis Mode (Stress Tolerance Audit)**: Introduce high-intensity audio-visual distraction algorithms.
    *   Implement **Adversarial feedback** (visual border flashes, target acceleration).
    *   Introduce user-controlled difficulty caps and stress bypass gates.
*   **Gate to Progress:** Safety and legal disclaimer sign-off on stress induction protocols.

### Phase 4: Workflow Integrations (B2B Sandbox)
*   **Tasks:**
    *   Develop local command-line interface (CLI) for developer workflows (e.g., block git commit if SRI is under 80% - developer opt-in only).
    *   Design team dashboards for B2B accounts, maintaining strict user privacy (sharing team average readiness index without individual score identification).
*   **Gate to Progress:** Zero security-clearance alerts or IT flags on client machines.

### Phase 5: AI Layer (Only after validation)
*   **Tasks:**
    *   Introduce local AI diagnostic agent to analyze cognitive anomalies (e.g., recognizing unique patterns of attention fatigue).
*   **Gate to Progress:** Full peer-reviewed or internal scientific validation of the AI model's accuracy compared to standard neuropsychological profiles.
