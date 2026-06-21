# Agent Invocation Matrix

This matrix documents the operational status, configuration verification, and votes for all agents defined or invoked during the validation runs. 

Agents not listed as part of the active squads were marked as `NOT_INVOKED` and did not execute, in compliance with the **Anti-Fake-Delegation** rules.

---

## 1. Roster & Invocation Registry

| Agent Name | Subagent Type | Active Run | Conversation ID | Registered | Invoked | Identity Verified | Disclosed Session-Real | Disclosed Shared Parent Scope | Vote | Confidence | Evidence Grade |
| :--- | :--- | :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Product Strategy** | `product_strategy_agent` | Run 1 | `dd442d85-b1e6-4b45-95f0-fdaa786e87cd` | Yes | Yes | Yes | Yes | Yes | **NO-GO** | High | A / C |
| **UX/UI Experience** | `ux_ui_experience_agent` | Run 1 | `319e0817-cc0b-4583-ad2c-16aa375967b2` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |
| **Tech Architecture** | `technical_architecture_agent` | Run 1 | `79bbef71-cb31-4e65-9583-12d8550710a4` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | B |
| **QA/Evidence** | `qa_evidence_agent` | Run 1 | `99479896-c19f-44f6-a78a-e9ed8b8e7a96` | Yes | Yes | Yes | Yes | Yes | **NO-GO** | High | B / C |
| **Red Team** | `red_team_devil_advocate_agent` | Run 1 | `d3c94ca2-ee91-4a41-b9a9-769d9651c4ff` | Yes | Yes | Yes | Yes | Yes | **NO-GO** | High | B |
| **Psychometrics** | `psychometrics_measurement_agent` | Run 2 | `e8a5d67d-777c-4bda-be26-31b5a448b6fc` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |
| **Scoring Engine** | `scoring_engine_agent` | Run 2 | `67c97987-84cc-4e4d-bb45-1fa0e018e870` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |
| **Cognitive Science** | `cognitive_science_agent` | Run 2 | `e0ae0c8f-41d3-4d87-ba3c-e54c1af8efbc` | Yes | Yes | Yes | Yes | Yes | **GO (Cond.)** | High | A |
| **Metacognition** | `metacognition_agent` | Run 2 | `a74110aa-b438-4184-9921-4b53f4006ae8` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | B |
| **QA/Evidence** | `qa_evidence_agent` | Run 2 | `80eab452-cb70-4b21-8477-57079c480ded` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | C |
| **Red Team** | `red_team_devil_advocate_agent` | Run 2 | `fde85ace-edd2-437f-a7b0-cf04e42aba97` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | B |
| **Android Performance** | `android_performance_agent` | Run 3 | `ae2a874b-466e-40fa-a439-2cb261acb698` | Yes | Yes | Yes | Yes | Yes | **GO** | High | A |
| **Tech Architecture** | `technical_architecture_agent` | Run 3 | `0fed9411-82a7-4d72-8c08-35bd6266ff68` | Yes | Yes | Yes | Yes | Yes | **COND. GO** | High | B |
| **Exercise Engine** | `exercise_engine_agent` | Run 3 | `8f2e5e0c-e04b-4cd0-abc4-3bba14040027` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |
| **Scoring Engine** | `scoring_engine_agent` | Run 3 | `998a77fe-836e-47ae-b489-4fe060b00744` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |
| **QA/Evidence** | `qa_evidence_agent` | Run 3 | `ba6695d7-4019-4519-9441-cb55e304d7a3` | Yes | Yes (Crashed) | No | No | No | **CRASHED** | - | - |
| **Red Team** | `red_team_devil_advocate_agent` | Run 3 | `18c0e9a8-9262-4b51-b39a-67def7524f6e` | Yes | Yes | Yes | Yes | Yes | **REVISE** | High | A |

---

## 2. Inactive Council Roster (`NOT_INVOKED`)

The remaining 24 subagents from the 36-agent registry were not registered or invoked during these runs. Their simulated definitions remain quarantined in `.antigravity-only/agents/quarantine/` and are marked:

-   **Status:** `NOT_INVOKED`
-   **Trace:** No conversation ID spawned; no token usage.

*Roster of Inactive Agents:*
1. `chief_orchestrator_agent` (manual override by parent during validation runs)
2. `founder_strategy_agent`
3. `attention_control_agent`
4. `working_memory_agent`
5. `executive_function_agent`
6. `stress_performance_agent`
7. `fatigue_recovery_agent`
8. `human_learning_transfer_agent`
9. `user_psychology_agent`
10. `behavior_design_agent`
11. `premium_design_agent`
12. `creative_narrative_agent`
13. `gamification_integrity_agent`
14. `local_storage_agent`
15. `data_analytics_agent`
16. `security_privacy_agent`
17. `business_monetization_agent`
18. `financial_analyst_agent`
19. `growth_market_agent`
20. `competitive_intelligence_agent`
21. `positioning_category_agent`
22. `customer_segment_agent`
23. `legal_claims_ethics_agent`
24. `privacy_ethics_agent`
25. `stress_safety_agent`
