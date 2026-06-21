# Founder Council Review Modes

This document defines the 11 active squad configurations and their membership parameters.

---

## Operating Rules for Squads

1. **Anti-Noise squad limits:** The Orchestrator must activate only the agents listed for the selected mode. Do not trigger additional agents unless requested by the founder.
2. **Mandatory Red Team:** `red_team_devil_advocate_agent` is present in all modes.
3. **Triggered Inclusions:**
   - Add `qa_evidence_agent` for any technical or science reviews.
   - Add `legal_claims_ethics_agent` for public copy, health, or sensory pressure reviews.
   - Add `psychometrics_measurement_agent` if scoring formulas are evaluated.
   - Add `android_performance_agent` if input timing or latency is discussed.

---

## Roster of Review Modes

### 1. FAST_FEATURE_REVIEW (6 Agents)
- **Required:** `chief_orchestrator_agent`, `product_strategy_agent`, `ux_ui_experience_agent`, `technical_architecture_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Small UI components, local flows.

### 2. SCIENCE_REVIEW (9 Agents)
- **Required:** `chief_orchestrator_agent`, `cognitive_science_agent`, `attention_control_agent`, `executive_function_agent`, `psychometrics_measurement_agent`, `human_learning_transfer_agent`, `legal_claims_ethics_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Cognitive task design, task validity.

### 3. PSYCHOMETRICS_SCORING_REVIEW (7 Agents)
- **Required:** `chief_orchestrator_agent`, `psychometrics_measurement_agent`, `scoring_engine_agent`, `cognitive_science_agent`, `metacognition_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Formulas, SRI calculations, outlier filters.

### 4. TECHNICAL_REVIEW (9 Agents)
- **Required:** `chief_orchestrator_agent`, `technical_architecture_agent`, `exercise_engine_agent`, `scoring_engine_agent`, `local_storage_agent`, `data_analytics_agent`, `security_privacy_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Code refactoring, SQLite schemas.

### 5. ANDROID_TIMING_REVIEW (7 Agents)
- **Required:** `chief_orchestrator_agent`, `android_performance_agent`, `technical_architecture_agent`, `exercise_engine_agent`, `scoring_engine_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** VSYNC frame rates, touch event latency audits.

### 6. LEGAL_CLAIMS_REVIEW (7 Agents)
- **Required:** `chief_orchestrator_agent`, `legal_claims_ethics_agent`, `privacy_ethics_agent`, `stress_safety_agent`, `cognitive_science_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Store text, landing pages compliance.

### 7. PRIVACY_TRUST_REVIEW (7 Agents)
- **Required:** `chief_orchestrator_agent`, `security_privacy_agent`, `privacy_ethics_agent`, `data_analytics_agent`, `local_storage_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Consent flows, logging parameters.

### 8. BUSINESS_REVIEW (8 Agents)
- **Required:** `chief_orchestrator_agent`, `business_monetization_agent`, `financial_analyst_agent`, `growth_market_agent`, `competitive_intelligence_agent`, `positioning_category_agent`, `customer_segment_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Subscriptions, paywall triggers, GTM.

### 9. UX_RETENTION_REVIEW (8 Agents)
- **Required:** `chief_orchestrator_agent`, `ux_ui_experience_agent`, `premium_design_agent`, `user_psychology_agent`, `behavior_design_agent`, `gamification_integrity_agent`, `qa_evidence_agent`, `red_team_devil_advocate_agent`.
- **Use Case:** Onboardings, streaks, UX layout.

### 10. FULL_FEATURE_REVIEW (18 Agents)
- **Required:** Tiers 0, 1, 2, and 3 core agents.
- **Use Case:** E1/E6 core exercise releases.

### 11. WAR_ROOM_REVIEW (36 Agents)
- **Required:** Full 36-agent registry.
- **Use Case:** Major pivots. Must be explicitly requested by the founder.
