# Round 2: Agent Count & Squad Strategy

This document re-evaluates the optimal agent count and orchestration strategy for the MindOS Founder Council Extended.

## Comparison of Architectures

To identify the best architecture, we compare six different approaches across six critical dimensions:
1. **Decision Quality:** Precision and specificity of recommendations.
2. **Noise Risk:** Risk of duplicate or conflicting generic advice.
3. **Runtime Cost:** Token usage, API calls, and context footprint.
4. **Founder Usefulness:** Ease of consumption and actionable clarity.
5. **Scientific Depth:** Ability to audit specialized cognitive domains.
6. **Operational Clarity:** Clear boundaries of responsibility.

---

## Comparison Matrix

| Architecture | Decision Quality | Noise Risk | Runtime Cost | Founder Usefulness | Scientific Depth | Operational Clarity | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **18-Agent Council** | Moderate | Low | Low | High | Low | Moderate | REJECTED |
| **24-Agent Council** | Moderate | Medium | Moderate | Moderate | Moderate | Moderate | REJECTED |
| **36-Agent Extended** | High | Critical | Critical | Low | High | High | REJECTED (Too bloated) |
| **36-Agent Registry + 5-9 Active Squads** | **High** | **Low** | **Low** | **High** | **High** | **High** | **SELECTED (Optimal)** |
| **36-Agent Registry + Expert Lenses** | High | High | Low | Low | High | Low | REJECTED |
| **36-Agent Registry + Meta-Review Loop** | High | Low | High | Moderate | High | Moderate | REJECTED (High latency) |

---

## Rationale for Selection: 36-Agent Registry with Hybrid Dynamic Squads

- **Why not 18 or 24 agents?** An 18-agent or 24-agent council forced specialists to wear too many hats. A single "Cognitive Science" agent had to audit both working memory Dual N-Back timing and sustained attention distractors, leading to shallow analysis. Separating them is necessary for a world-class cognitive trainer.
- **Why not a flat 36-agent activation?** Activating all 36 agents at once is a disaster for usability, context window boundaries, and latency. It generates massive text walls and contradicts the anti-noise principle.
- **Why the Hybrid Dynamic Squads model?**
  - **Precision:** The registry defines 36 highly specialized agents with clear allowed/forbidden tasks and quality gates.
  - **Efficiency:** The parent Orchestrator activates only a focused subset (squad) of 5 to 9 agents based on the request type (e.g. `ANDROID_TIMING_REVIEW` uses only 5 performance, storage, and QA agents plus the Red Team).
  - **Consensus:** Keeps the review loop fast, deterministic, and highly actionable.
