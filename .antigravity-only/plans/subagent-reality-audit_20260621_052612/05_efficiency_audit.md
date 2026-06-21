# Phase 5: Efficiency Audit

This document evaluates the efficiency of the previous 20-agent system and details how they overlap and consolidate.

## Bloat and Duplication Analysis

The original 20-agent team has massive role duplication and cognitive science specialization overlap:
1. **Science/Psychology Overlap:** 7 separate roles (`Cognitive Science Lead`, `Neuropsychology Reviewer`, `Psychology & Behavior Change Expert`, `Psychiatry Safety Reviewer`, `Psychometrics & Measurement Scientist`, `Cognitive Training Researcher`, and `Exercise Optimization Lead`) all analyze the same cognitive scientific validity parameters.
2. **Strategy Overlap:** `Chief Strategy Director`, `Product Strategy Lead`, and `Competitive Intelligence Analyst` all evaluate business fit and competitive positioning.
3. **Execution Overlap:** `Implementation Plan Architect` and `Claude Code Handoff Packager` both construct developer instructions.

By consolidating these 20 roles into **7 core subagents**, we eliminate massive prompt loading, minimize token consumption, prevent handoff noise, and establish clear operational boundaries.

---

## Agent Consolidation & Scoring Table

| Agent | Keep | Merge | Quarantine | Convert to Tool | Delete Later | Reason for Change |
| --- | :---: | :---: | :---: | :---: | :---: | --- |
| `01_chief_strategy_director` | | Yes | Yes | | | Consolidated into **Product Strategy Agent**. |
| `02_agent_orchestrator` | | Yes | Yes | | | Replaced by the **Parent Orchestrator** layer. |
| `03_implementation_plan_architect` | | Yes | Yes | | | Consolidated into **Technical Architecture Agent**. |
| `04_cognitive_science_lead` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `05_neuropsychology_reviewer` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `06_psychology_behavior_change_expert` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `07_psychiatry_safety_reviewer` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `08_psychometrics_measurement_scientist` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `09_cognitive_training_researcher` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `10_exercise_optimization_lead` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `11_adaptive_difficulty_designer` | | Yes | Yes | | | Consolidated into **Cognitive Science Agent**. |
| `12_data_science_experimentation_lead` | | Yes | Yes | | | Consolidated into **QA/Evidence Agent**. |
| `13_fairness_bias_comparability_reviewer` | | Yes | Yes | | | Consolidated into **QA/Evidence Agent**. |
| `14_privacy_safety_guard` | | Yes | Yes | | | Consolidated into **Security/Privacy Agent**. |
| `15_product_strategy_lead` | | Yes | Yes | | | Consolidated into **Product Strategy Agent**. |
| `16_financial_strategy_partner` | | Yes | Yes | | | Consolidated into **Financial/Business Agent**. |
| `17_marketing_growth_strategist` | | Yes | Yes | | | Consolidated into **Product Strategy Agent**. |
| `18_competitive_intelligence_analyst` | | Yes | Yes | | | Consolidated into **Product Strategy Agent**. |
| `19_creative_ideation_director` | | Yes | Yes | | | Consolidated into **Creative/Brand Agent**. |
| `20_claude_code_handoff_packager` | | | Yes | Yes | | Replaced by markdown prompt templates (Tools). |

---

## Recommendations

- **Target Agent Count:** 7 core subagents + 1 Parent Orchestrator.
- **Action:** Move all 20 legacy agent markdown files to `.antigravity-only/agents/quarantine/` to clean the active workspace while preserving their contents for potential rollback.
