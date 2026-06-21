# Round 4: Decision Engine Design

This document details the scoring, voting, veto, and escalation mechanics of the Founder Council Extended.

## 1. Agent Vote Protocol
Every activated agent in a squad must cast a vote:
- **GO:** The proposal meets all domain criteria and contains no safety/privacy blockers.
- **CONDITIONAL GO:** The plan is conceptually sound, but the final implementation must pass specific checks (e.g. latency checks, manual QA run) before merge.
- **NO-GO:** Critical flaws identified. Reversion to planning is mandatory.

## 2. Confidence Protocol
Agents must output their confidence score (`Low`, `Medium`, or `High`) alongside their vote. The Orchestrator calculates the **Weighted Decision Index (WDI)**:
- High Confidence: Weight = 1.0
- Medium Confidence: Weight = 0.5
- Low Confidence: Weight = 0.2

$$\text{WDI} = \frac{\sum (\text{Vote Value} \times \text{Weight})}{\sum \text{Weight}}$$

Where Vote Value: `GO` = 1.0, `CONDITIONAL GO` = 0.5, `NO-GO` = 0.0.
- A WDI below `0.75` blocks the publication of a GO recommendation.

---

## 3. Evidence Grading Protocol

Every scientific, performance, or database claim must be annotated with one of these grades:

| Grade | Classification | Definition | Example Evidence |
| --- | --- | --- | --- |
| **A** | Benchmark Evidence | Peer-reviewed academic literature or industry-standard benchmark | A published double-blind study on N-Back transfer |
| **B** | Test Evidence | Reproducible local test logs or execution logs from the workspace | A Room migration test log or Android Choreographer output |
| **C** | Engineering Logic | Strong technical or scientific reasoning based on verified code/schemas | Modularity maps, SQLite schema validation |
| **D** | Plausible Hypothesis | Unvalidated assumption with logical backing | Speculative latency improvement from thread restructuring |
| **E** | Pure Opinion | Opinion, taste, or speculative marketing narrative | A branding tagline proposal, UI theme color selection |

---

## 4. Conflict Escalation Protocol
If the squad output results in conflicting votes:
1. **Identify the Core Clash:** The Orchestrator extracts the specific domain clash (e.g. Performance Agent votes NO-GO on animation, but Creative Agent votes GO).
2. **Collect Evidence:** Both agents must justify their vote using Grade A or Grade B evidence.
3. **Trade-off Mapping:** The Orchestrator constructs a Trade-off Matrix comparing User Value vs. Technical Risk.
4. **Founder Review:** The decision packet is escalated to the founder.

## 5. Mandatory Veto Protocol
Specific agents have absolute veto power. If a veto is triggered, the plan is marked **NO-GO** immediately. Veto conditions include:
- `legal_claims_ethics_agent` blocks copy with clinical/IQ claims.
- `psychometrics_measurement_agent` blocks baselines built on <3 sessions.
- `android_performance_agent` blocks animation frame rates dropping below VSYNC limits.
- `security_privacy_agent` blocks non-allowlisted permission requests.
- `stress_safety_agent` blocks stress features lacking opt-out controls.
- `qa_evidence_agent` blocks any deployment plan lacking manual verification test scripts.

## 6. Founder Override Protocol
The founder has absolute authority to override any veto or conflict. However, the override must be logged in `.antigravity-only/agents/OVERRIDE_LOG.md` detailing:
- The overridden agent and veto reason.
- The founder's strategic rationale.
- Risk mitigation steps accepted by the founder.
