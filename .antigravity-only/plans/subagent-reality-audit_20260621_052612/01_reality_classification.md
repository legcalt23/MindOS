# Phase 1: Reality Classification

This document classifies each custom subagent defined in `.antigravity-only/agents/` into one of the following categories:
- **SIMULATED:** Prompt-only persona or documentation with no runtime registration.
- **SESSION_REAL:** Registered with `define_subagent` and invocable only during the current runtime session.
- **PERSISTENT_REAL:** Registered, versioned, traceable, and reproducible across sessions.

## Reality Classification Table

| Agent | Location | Claimed Role | Runtime Registered | Invocable | Tool Scope Known | Output Traceable | Classification | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `01_chief_strategy_director` | `.antigravity-only/agents/` | Chief MindBoost Strategy Director | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `02_agent_orchestrator` | `.antigravity-only/agents/` | Agent Orchestrator | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `03_implementation_plan_architect` | `.antigravity-only/agents/` | Implementation Plan Architect | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `04_cognitive_science_lead` | `.antigravity-only/agents/` | Cognitive Science Lead | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `05_neuropsychology_reviewer` | `.antigravity-only/agents/` | Neuropsychology Reviewer | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `06_psychology_behavior_change_expert` | `.antigravity-only/agents/` | Psychology & Behavior Change Expert | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `07_psychiatry_safety_reviewer` | `.antigravity-only/agents/` | Psychiatry Safety Reviewer | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `08_psychometrics_measurement_scientist` | `.antigravity-only/agents/` | Psychometrics & Measurement Scientist | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `09_cognitive_training_researcher` | `.antigravity-only/agents/` | Cognitive Training Researcher | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `10_exercise_optimization_lead` | `.antigravity-only/agents/` | Exercise Optimization Lead | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `11_adaptive_difficulty_designer` | `.antigravity-only/agents/` | Adaptive Difficulty Designer | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `12_data_science_experimentation_lead` | `.antigravity-only/agents/` | Data Science & Experimentation Lead | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `13_fairness_bias_comparability_reviewer` | `.antigravity-only/agents/` | Fairness / Bias / Comparability Reviewer | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `14_privacy_safety_guard` | `.antigravity-only/agents/` | Privacy & Safety Guard | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `15_product_strategy_lead` | `.antigravity-only/agents/` | Product Strategy Lead | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `16_financial_strategy_partner` | `.antigravity-only/agents/` | Financial Strategy Partner | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `17_marketing_growth_strategist` | `.antigravity-only/agents/` | Marketing & Growth Strategist | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `18_competitive_intelligence_analyst` | `.antigravity-only/agents/` | Competitive Intelligence Analyst | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `19_creative_ideation_director` | `.antigravity-only/agents/` | Creative Ideation Director | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |
| `20_claude_code_handoff_packager` | `.antigravity-only/agents/` | Claude Code Handoff Packager | No | No | No | No | SIMULATED | File contains markdown definition only. Not registered in available subagent list. |

## Audit Summary

- Total Discovered Agents: 20
- SIMULATED: 20
- SESSION_REAL: 0
- PERSISTENT_REAL: 0
- Verification Status: **PENDING_HARDENING**
