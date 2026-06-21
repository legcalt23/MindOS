# Round 10: 10 Serious Improvements

This document lists the 10 core improvements introduced to the Founder Council Extended during this deep review to optimize decision quality.

---

## The 10 Improvements

### 1. Unified Weighted Decision Index (WDI)
- **Why the draft missed it:** The draft relied on simple vote counting, treating low-confidence opinions and high-confidence domain assessments with equal weight.
- **Implementation Detail:** Formula weightings scale by agent self-reported confidence. Votes are aggregated mathematically.
- **Expected Impact:** Minimizes the risk of marginal opinions distorting key strategic directions.

### 2. Log-Normal Timing Calibration rules
- **Why the draft missed it:** Generic performance reviews did not address how millisecond fluctuations contaminate cognitive scoring.
- **Implementation Detail:** Android timing and scoring agents now mandate log-normal touch reaction time (RT) filters to discard accidental double-taps.
- **Expected Impact:** Ensures baseline measurement integrity is protected from physical noise.

### 3. Verification Trace IDs for Anti-Fake-Delegation
- **Why the draft missed it:** The draft had no audit trace, allowing the orchestrator to synthesize agent opinions without executing them.
- **Implementation Detail:** The Orchestrator must output the unique Conversation ID of every subagent in the report header.
- **Expected Impact:** Founder can verify that the subagents were actually run by inspecting logs in the system's brain folder.

### 4. Non-Clinical Naming Veto Rules
- **Why the draft missed it:** The marketing and creative agents did not have rules prohibiting clinical vocabulary.
- **Implementation Detail:** The legal/claims agent gets a mandatory veto over names using medical prefixes (e.g. "Neuro-", "Therapy-").
- **Expected Impact:** Protects the project from immediate regulatory actions for selling an unapproved medical device.

### 5. Automated Fatigue Recovery cool-down
- **Why the draft missed it:** Did not address the risk of users training past cognitive fatigue limits.
- **Implementation Detail:** The fatigue recovery agent enforces a maximum 15-minute training block followed by a mandatory cool-down.
- **Expected Impact:** Prevents task-specific fatigue from distorting user telemetry.

### 6. Opt-In Stress Trigger Warning Gates
- **Why the draft missed it:** The draft allowed sensory interference elements to be planned without safety parameters.
- **Implementation Detail:** Strobe-safety warnings and rapid opt-out gestures are made mandatory for time-pressure features.
- **Expected Impact:** Protects photosensitive users from safety incidents.

### 7. Baseline Recalibration cycles
- **Why the draft missed it:** The draft treated baselines as static metrics.
- **Implementation Detail:** The psychometrics agent enforces a recalibration of within-user baselines every 10 sessions to account for practice effects.
- **Expected Impact:** Prevents false reports of cognitive transfer when improvements are merely task-specific habituation.

### 8. Pay-to-Win Telemetry Blockers
- **Why the draft missed it:** Gamification design did not have rules separating visual rewards from metrics integrity.
- **Implementation Detail:** Vetoes any streak or cosmetic multiplier that impacts objective performance ratings.
- **Expected Impact:** Maintains competitive fairness and measurement reliability.

### 9. Hourly Normalization for Telemetry logs
- **Why the draft missed it:** Precise timestamp logging leaks PII, violating the local-first trust principle.
- **Implementation Detail:** Telemetry timestamps must be normalized to hourly buckets.
- **Expected Impact:** Prevents device tracking through timing correlations.

### 10. Manual QA Test Script Gate
- **Why the draft missed it:** Plans could be approved without a clear path to execution verification.
- **Implementation Detail:** The QA agent vetoes any proposal that lacks explicit step-by-step instructions for manual testing.
- **Expected Impact:** Prevents technical debt from entering the main codebase.
