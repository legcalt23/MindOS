# Round 8: MindBoost Quality Gates V2

This document defines the 13 quality gates that proposals must pass to be approved.

---

## The 13 Quality Gates

### 1. Science Claims Gate
- **Gate Owner:** `cognitive_science_agent`
- **Trigger:** Any proposal mentioning cognitive performance, focus, memory, or brain improvement.
- **Required Evidence:** Peer-reviewed citations (Grade A) mapping task design to validated attentional constructs.
- **Pass Condition:** Copy is restricted to near-transfer attentional telemetry metrics.
- **Fail Condition:** Uses clinical, diagnostic, or general intelligence ("IQ") terms.
- **Consequence:** Immediate **NO-GO**; copy must be rewritten.

### 2. Psychometrics Gate
- **Gate Owner:** `psychometrics_measurement_agent`
- **Trigger:** Scoring features, training baseline designs.
- **Required Evidence:** Statistical baseline equations (Grade C).
- **Pass Condition:** Baselines are built on a minimum of 3 sessions with variance filtering.
- **Fail Condition:** Baselines calculated on <3 sessions or lacking log-normal outlier filtering.
- **Consequence:** **CONDITIONAL GO**; algorithm block must be corrected in specification.

### 3. SRI / Scoring Gate
- **Gate Owner:** `scoring_engine_agent`
- **Trigger:** Performance metrics presentation, System Readiness Index (SRI) calculations.
- **Required Evidence:** Mathematical equations showing time-accuracy scaling (Grade C).
- **Pass Condition:** Raw touch timings normalized with log-transforms before score aggregation.
- **Fail Condition:** Unweighted averaging of speed and accuracy metrics.
- **Consequence:** **NO-GO**; formula must be revised.

### 4. Android Timing Gate
- **Gate Owner:** `android_performance_agent`
- **Trigger:** Trial timing engines, touch event loops.
- **Required Evidence:** Choreographer frame callback timings (Grade B).
- **Pass Condition:** Presentation timing loop guarantees jitter <2ms.
- **Fail Condition:** Timing loops running on the main UI thread.
- **Consequence:** **NO-GO**; architecture must be decoupled.

### 5. Exercise Engine Gate
- **Gate Owner:** `exercise_engine_agent`
- **Trigger:** Trial lifecycle loops (E1/E6).
- **Required Evidence:** State machine diagram logs (Grade C).
- **Pass Condition:** Strict separation of state transitions from rendering threads.
- **Fail Condition:** Memory leak vectors in loop lifecycles.
- **Consequence:** **CONDITIONAL GO**; release blocked until leak is resolved in tests.

### 6. AHS / Anti-Practice-Effect Gate
- **Gate Owner:** `human_learning_transfer_agent`
- **Trigger:** Progression schemas, task level upgrades.
- **Required Evidence:** Stimulus randomization parameters (Grade C).
- **Pass Condition:** Random sequence generator ensures users cannot improve via muscle-memory habituation.
- **Fail Condition:** Predictable grid/target patterns.
- **Consequence:** **NO-GO**; target randomization algorithm required.

### 7. Stress / Pressure Safety Gate
- **Gate Owner:** `stress_safety_agent`
- **Trigger:** Time-limit pressure, strobe distractors, sensory interference.
- **Required Evidence:** Safety disclaimers and opt-out screen maps (Grade C).
- **Pass Condition:** Strobe-safety warnings present; immediate exit gesture active.
- **Fail Condition:** Lack of photo-sensitive seizure warnings.
- **Consequence:** **NO-GO**; safety elements must be added.

### 8. Privacy / Local-First Gate
- **Gate Owner:** `security_privacy_agent`
- **Trigger:** Telemetry database logging.
- **Required Evidence:** Local SQLite/Room schema audits (Grade B).
- **Pass Condition:** Device identifiers normalized; zero PII parameters logged.
- **Fail Condition:** Capture of system accounts, precise location, or device hardware IDs.
- **Consequence:** **NO-GO**; telemetry fields deleted.

### 9. Legal / Claims Gate
- **Gate Owner:** `legal_claims_ethics_agent`
- **Trigger:** App landing page copy, Store descriptions, disclaimers.
- **Required Evidence:** FTC/AEMPS compliance checklist logs (Grade C).
- **Pass Condition:** Standard non-medical disclaimer visible on onboarding.
- **Fail Condition:** Asserting memory improvement for ADHD, dementia, or age-related decline.
- **Consequence:** **NO-GO**; claims must be removed.

### 10. UX / Retention Gate
- **Gate Owner:** `ux_ui_experience_agent`
- **Trigger:** Onboarding flows, streak dashboards.
- **Required Evidence:** Wireframe step maps (Grade C).
- **Pass Condition:** User completes first exercise in <3 steps from launch.
- **Fail Condition:** Mandatory tutorial exceeding 3 screens.
- **Consequence:** **CONDITIONAL GO**; flow must be streamlined.

### 11. Monetization Integrity Gate
- **Gate Owner:** `business_monetization_agent`
- **Trigger:** Paywall prompts.
- **Required Evidence:** Monetization trigger map (Grade C).
- **Pass Condition:** Paywall placement restricted to dashboard/statistics areas.
- **Fail Condition:** Triggering paywall popups mid-exercise trial.
- **Consequence:** **NO-GO**; trigger repositioned.

### 12. Evidence Readiness Gate
- **Gate Owner:** `qa_evidence_agent`
- **Trigger:** Build releases.
- **Required Evidence:** Manual QA execution logs (Grade B).
- **Pass Condition:** Manual verify scripts match 100% of acceptance criteria.
- **Fail Condition:** Merging code changes without manual verification logs.
- **Consequence:** **NO-GO**; release blocked.

### 13. No-Hype / Founder Trust Gate
- **Gate Owner:** `founder_strategy_agent`
- **Trigger:** Roadmap milestone reviews.
- **Required Evidence:** Resource allocation forecast (Grade C).
- **Pass Condition:** Focus stays 100% on attentional readiness MVP.
- **Fail Condition:** Adding multi-player, cloud sync, or external LLM integrations to the MVP.
- **Consequence:** **NO-GO**; scope stripped.
