# Round 11: Final Upgraded Plan

This document presents the complete consolidated plan, architecture, agent roster, review modes, and operating rules for the MindOS Founder Council Extended.

---

## 1. Governance & Operating Rules
1. **Scope Limit:** Exclusive focus on MindBoost planning, feature evaluation, and strategic reviews. Code execution, network access, and secrets retrieval are strictly prohibited.
2. **Anti-Noise:** Every agent response must reference specific plan metrics or files. Generic summaries will trigger an Orchestrator warning.
3. **Anti-Fake-Delegation:** The Orchestrator must display Conversation IDs, timestamps, and prompts for every subagent invoked. Unused agents must be marked `NOT_INVOKED`.
4. **Veto Rules:** Vetoes from Legal/Claims, Psychometrics, Android Performance, Security/Privacy, Stress Safety, and QA/Evidence are absolute and trigger a **NO-GO** verdict.
5. **Disagreement Escalation:** Conflicting votes escalate to a Trade-off Matrix showing User Value vs. Technical Risk for the founder's review.

---

## 2. Roster of the 36 Specialist Agents

### Tier 0: Command & Governance
1. `chief_orchestrator_agent`: Coordinates reviews, resolves disagreements, compiles WDI.
2. `founder_strategy_agent`: Reviews focus, wedge strategy, and opportunity cost.
3. `red_team_devil_advocate_agent`: Plays antagonist, identifies worst-case risks.

### Tier 1: Science of the Mind
4. `cognitive_science_agent`: Validates cognitive constructs and near-transfer pathways.
5. `attention_control_agent`: Reviews Selective, sustained, and divided attention stimulus parameters.
6. `working_memory_agent`: Reviews n-back memory decay and dual-task memory capacity.
7. `executive_function_agent`: Reviews response inhibition and task set-shifting.
8. `stress_performance_agent`: Reviews cognitive pressure and time stress calibration.
9. `fatigue_recovery_agent`: Reviews session length limits and training burnout parameters.
10. `metacognition_agent`: Reviews confidence calibration and subjective readiness accuracy.
11. `psychometrics_measurement_agent`: Reviews baseline mathematics and outlier filters.
12. `human_learning_transfer_agent`: Reviews practice effects and stimulus randomization.

### Tier 2: Product, Behavior & Experience
13. `product_strategy_agent`: Evaluates value proposition and opportunity cost.
14. `user_psychology_agent`: Reviews motivation, self-efficacy, and user emotional safety.
15. `behavior_design_agent`: Reviews trigger-action-reward habit loops.
16. `ux_ui_experience_agent`: Audits screen flows, onboarding steps, and friction layout.
17. `premium_design_agent`: Reviews visual design, color tokens, and taste.
18. `creative_narrative_agent`: Audits copywriting, taglines, and category terms.
19. `gamification_integrity_agent`: Ensures streaks and visual rewards do not pollute metrics.

### Tier 3: Technology & Implementation
20. `technical_architecture_agent`: Audits KMP structure and local-first architecture.
21. `android_performance_agent`: Audits input timing, VSYNC, and Choreographer callbacks.
22. `exercise_engine_agent`: Audits E1/E6 stimulus timing loops and lifecycles.
23. `scoring_engine_agent`: Audits SRI math and reaction time normalizations.
24. `local_storage_agent`: Audits Room schemas, migrations, and SQLite queries.
25. `data_analytics_agent`: Audits telemetry schemas and data minimization rules.
26. `qa_evidence_agent`: Audits test plans and manual QA test script completeness.
27. `security_privacy_agent`: Audits Android permissions allowlist and storage encryption.

### Tier 4: Business, Market & Growth
28. `business_monetization_agent`: Reviews subscription models and paywall triggers.
29. `financial_analyst_agent`: Reviews runway impact, development burn, and opportunity cost.
30. `growth_market_agent`: Reviews GTM, referral loops, and user acquisition.
31. `competitive_intelligence_agent`: Compares features against Peak, Lumosity, and BrainHQ.
32. `positioning_category_agent`: Establishes "cognitive readiness" category copywriting.
33. `customer_segment_agent`: Reviews developer/high-performer segment requirements.

### Tier 5: Legal, Ethics & Trust
34. `legal_claims_ethics_agent`: Audits FTC/FDA compliance and disclaimers.
35. `privacy_ethics_agent`: Audits consent flows and data minimized tracking.
36. `stress_safety_agent`: Audits photo-sensitivity warnings and time-pressure opt-outs.

---

## 3. The 11 Review Modes

1. **FAST_FEATURE_REVIEW** (6 Agents): For minor UI/UX changes.
2. **SCIENCE_REVIEW** (9 Agents): For exercise stimulus design.
3. **PSYCHOMETRICS_SCORING_REVIEW** (7 Agents): For metrics and SRI math.
4. **TECHNICAL_REVIEW** (9 Agents): For database models and KMP refactoring.
5. **ANDROID_TIMING_REVIEW** (7 Agents): For millisecond precision input audits.
6. **LEGAL_CLAIMS_REVIEW** (7 Agents): For onboarding text and marketing copy compliance.
7. **PRIVACY_TRUST_REVIEW** (7 Agents): For telemetry logging schemas.
8. **BUSINESS_REVIEW** (8 Agents): For subscriptions, paywalls, and GTM plans.
9. **UX_RETENTION_REVIEW** (8 Agents): For streak mechanics and task friction loops.
10. **FULL_FEATURE_REVIEW** (18 Agents): For major exercise milestones.
11. **WAR_ROOM_REVIEW** (36 Agents): For major strategic pivots. Must be requested by the founder.

---

## 4. Final Signoff Verdict
- **Verdict:** `CONDITIONAL GO`
- **Reason:** The architecture provides elite, specialized decision analysis. However, since all subagents are session-scoped (`SESSION_REAL`) and do not persist, this system is a decision and planning council, not a persistent multiagent worker platform.
