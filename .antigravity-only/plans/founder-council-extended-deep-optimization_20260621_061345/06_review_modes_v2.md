# Round 6: Review Modes V2

This document defines the 11 refined review modes for the Founder Council Extended. Each mode enforces a "minimum useful agents" list and includes the Red Team.

---

## The 11 Review Modes

### 1. FAST_FEATURE_REVIEW
- **Purpose:** Rapid check on small UI components or minor flows.
- **Max Agents:** 6
- **Required Agents:** `chief_orchestrator_agent`, `product_strategy_agent`, `ux_ui_experience_agent`, `technical_architecture_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Standard UI iterations, local-only tweaks.
- **When Not to Use:** Changes involving scoring math, performance latency, or public copy.
- **Mandatory Gates:** UX Gate, Evidence Readiness Gate.

### 2. SCIENCE_REVIEW
- **Purpose:** Audit cognitive task design, task validity, and near-transfer constructs.
- **Max Agents:** 9
- **Required Agents:** `chief_orchestrator_agent`, `cognitive_science_agent`, `attention_control_agent`, `executive_function_agent`, `psychometrics_measurement_agent`, `human_learning_transfer_agent`, `legal_claims_ethics_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** New exercise mechanics, stimulus parameters.
- **When Not to Use:** Pure database migrations or monetization pricing plans.
- **Mandatory Gates:** Science Claims Gate, AHS / Anti-Practice-Effect Gate.

### 3. PSYCHOMETRICS_SCORING_REVIEW
- **Purpose:** Validate scoring formulas, baseline logic, and SRI metrics.
- **Max Agents:** 7
- **Required Agents:** `chief_orchestrator_agent`, `psychometrics_measurement_agent`, `scoring_engine_agent`, `cognitive_science_agent`, `metacognition_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Modifying composite scores, baseline drift algorithms.
- **When Not to Use:** Visual aesthetic theme checks.
- **Mandatory Gates:** Psychometrics Gate, SRI / Scoring Gate.

### 4. TECHNICAL_REVIEW
- **Purpose:** Audit KMP modularity, database schemas, and offline-first persistence.
- **Max Agents:** 9
- **Required Agents:** `chief_orchestrator_agent`, `technical_architecture_agent`, `exercise_engine_agent`, `scoring_engine_agent`, `local_storage_agent`, `data_analytics_agent`, `security_privacy_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Core refactoring, SQLite migrations, data model updates.
- **When Not to Use:** Visual design reviews or marketing copy audits.
- **Mandatory Gates:** Local-First Gate, Exercise Engine Gate.

### 5. ANDROID_TIMING_REVIEW
- **Purpose:** Audit input latency, frame rates, Choreographer synchronization, and GC timing.
- **Max Agents:** 7
- **Required Agents:** `chief_orchestrator_agent`, `android_performance_agent`, `technical_architecture_agent`, `exercise_engine_agent`, `scoring_engine_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Touch event handling, Choreographer stimulus presentation changes.
- **When Not to Use:** Pricing plans or category design reviews.
- **Mandatory Gates:** Android Timing Gate.

### 6. LEGAL_CLAIMS_REVIEW
- **Purpose:** Audit copy compliance, onboarding instructions, landing page taglines.
- **Max Agents:** 7
- **Required Agents:** `chief_orchestrator_agent`, `legal_claims_ethics_agent`, `privacy_ethics_agent`, `stress_safety_agent`, `cognitive_science_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Any public-facing copy, landing pages, marketing plans.
- **When Not to Use:** Internal backend refactoring.
- **Mandatory Gates:** Legal / Claims Gate, Stress / Pressure Safety Gate.

### 7. PRIVACY_TRUST_REVIEW
- **Purpose:** Audit local encryption boundaries, database rules, and consent flows.
- **Max Agents:** 7
- **Required Agents:** `chief_orchestrator_agent`, `security_privacy_agent`, `privacy_ethics_agent`, `data_analytics_agent`, `local_storage_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Logging schema updates, Android permission requests.
- **When Not to Invoke:** Basic branding or gamification reviews.
- **Mandatory Gates:** Privacy / Local-First Gate.

### 8. BUSINESS_REVIEW
- **Purpose:** Audit paywalls, pricing structures, packaging, and unit economics.
- **Max Agents:** 8
- **Required Agents:** `chief_orchestrator_agent`, `business_monetization_agent`, `financial_analyst_agent`, `growth_market_agent`, `competitive_intelligence_agent`, `positioning_category_agent`, `customer_segment_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Pricing updates, paywall triggers, competitor positioning.
- **When Not to Invoke:** Technical timing or latency audits.
- **Mandatory Gates:** Monetization Integrity Gate, PMF Gate.

### 9. UX_RETENTION_REVIEW
- **Purpose:** Audit task flows, onboardings, streaks, and user emotional safety.
- **Max Agents:** 8
- **Required Agents:** `chief_orchestrator_agent`, `ux_ui_experience_agent`, `premium_design_agent`, `user_psychology_agent`, `behavior_design_agent`, `gamification_integrity_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Trigger Conditions:** Core flow updates, notifications, streak structures.
- **When Not to Invoke:** Backend SQLite schemas or encryption checks.
- **Mandatory Gates:** UX / Retention Gate.

### 10. FULL_FEATURE_REVIEW
- **Purpose:** In-depth review of significant new exercise releases.
- **Max Agents:** 18
- **Required Agents:** All 18 core agents from Tiers 0, 1, 2, and 3.
- **Trigger Conditions:** Introducing E1/E6 core exercises.
- **When Not to Invoke:** Simple bugs or landing page reviews.
- **Mandatory Gates:** All Tiers 0-3 Gates.

### 11. WAR_ROOM_REVIEW
- **Purpose:** Deep strategic pivot audits or investor pitch deck alignments.
- **Max Agents:** 36 (All registry agents)
- **Constraint:** **Must be explicitly requested by the founder.** Never run automatically.
- **Trigger Conditions:** Strategic pivots.
