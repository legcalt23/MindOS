# Round 1: Brutal Critique

This document attacks the initial Founder Council Extended draft plan across 10 expert perspectives, identifying 20 critical weaknesses and their exact technical fixes.

---

## The 10 Perspectives

### 1. Founder
- *Critique:* "If I have to read 36 separate opinions every time I want to tweak a button or review a plan, I will stop using this system. I need clear trade-offs, not a wall of text."

### 2. Skeptical Investor
- *Critique:* "36 agents sounds like marketing hype to make a single LLM prompt look like a team. Show me how this actually improves decision quality and where the boundaries are."

### 3. Cognitive Scientist
- *Critique:* "The cognitive domains overlap too much. How does the system prevent the Attention Agent and the Executive Function Agent from arguing about the same Stroop task trial without clear constructs?"

### 4. Psychometrician
- *Critique:* "The draft mentions baselines but does not define outlier filtering or how to prevent learning effects from skewing the System Readiness Index (SRI)."

### 5. Android Performance Engineer
- *Critique:* "Evaluating latency in a high-level LLM prompt is useless unless you require precise instrumentation limits (e.g. Choreographer frame callbacks, GC allocation tracking)."

### 6. Legal / Regulatory Reviewer
- *Critique:* "The FTC aggressively prosecutes brain training apps that claim to improve IQ or stop cognitive decline. The plan must have a hard boundary preventing these words from leaking into any public copy."

### 7. Privacy Expert
- *Critique:* "Even local telemetry can leak PII if device models, timestamps, and precise locations are logged. The data analytics agent must have a strict do-not-collect specification."

### 8. Product Lead
- *Critique:* "We are building a local-offline MVP. If the agents recommend cloud features or complex sync protocols, they are actively hurting our roadmap focus."

### 9. UX Lead
- *Critique:* "MindBoost's target users are high performers. If the UX agent recommends childish gamification streaks, we will lose our audience."

### 10. Red Team Adversary
- *Critique:* "The Orchestrator can easily bypass subagents and generate fake reviews by pretending they ran. There must be an immutable proof-of-work link for every active subagent."

---

## 20 Concrete Weaknesses & Fixes

### 1. Lack of Evidence Grading Scale
- **Severity:** High
- **Why it matters:** The system treats speculative opinions and peer-reviewed studies with equal weight.
- **Failure:** Over-reliance on weak assumptions disguised as scientific facts.
- **Fix:** Implement a strict A-to-E Evidence Grading protocol.

### 2. No Request Input Schema
- **Severity:** High
- **Why it matters:** Users submit ambiguous prompts without technical parameters, leading to generic agent outputs.
- **Failure:** Prompt noise and useless, high-level strategic recommendations.
- **Fix:** Require a strict founder-request template mapping timing, claims, and data parameters.

### 3. Vague Veto Authorities
- **Severity:** High
- **Why it matters:** The draft allows vetoes but fails to detail how they block progress or how they are resolved.
- **Failure:** Circular arguments and deadlocks between agents.
- **Fix:** Establish explicit veto scopes for Legal, Psychometrics, Privacy, Performance, and QA.

### 4. Overlapping Cognitive Science Scopes
- **Severity:** Medium
- **Why it matters:** Attention and Executive Control roles review the same stimulus loops with different terminology.
- **Failure:** Duplicate reviews and conflicting design recommendations.
- **Fix:** Restrict `attention_control_agent` to stimulus density/timing, and `executive_function` to go/no-go ratios and shifting panels.

### 5. No Session Lifecycle State Machine
- **Severity:** Medium
- **Why it matters:** The relationship between designed agents and in-session registered agents is unclear.
- **Failure:** Trusting unverified subagent outputs because the system hides initialization failures.
- **Fix:** Design a 7-stage state machine tracking registration and smoke-testing.

### 6. No Anti-Hype Trust Gate
- **Severity:** High
- **Why it matters:** Agents can slip into generic, exaggerated marketing speak about cognitive enhancement.
- **Failure:** Legal exposure due to unscientific marketing copy recommendations.
- **Fix:** Add a strict "No-Hype" gate owned by the Legal/Claims agent.

### 7. No Offline-First Constraint Enforcement
- **Severity:** High
- **Why it matters:** Technical subagents might suggest cloud-based database syncs or external API calls.
- **Failure:** Architectural deviation from the local-offline MVP requirement.
- **Fix:** Make offline-first a hard-coded gate for the Technical Architecture agent.

### 8. Vague Android Latency Benchmarks
- **Severity:** Medium
- **Why it matters:** General performance reviews do not ensure millisecond-level precision.
- **Failure:** Frame drops (>16.6ms at 60Hz) during stimulus presentation, invalidating psychometric scores.
- **Fix:** Require the Android Performance agent to audit Choreographer and `System.nanoTime` timing boundaries.

### 9. Undefined Baseline Outlier Rules
- **Severity:** Medium
- **Why it matters:** Extreme values (e.g. accidental double-clicks) skew user baseline models.
- **Failure:** Flawed SRI scoring and baseline pollution.
- **Fix:** Mandate psychometrics agent to enforce a coefficient of variation check and log-normal outlier filters.

### 10. No PII Telemetry Boundary
- **Severity:** High
- **Why it matters:** The data analytics agent might allow passive capture of device serials or precise locations.
- **Failure:** Violation of local-first privacy rules and GDPR/CCPA exposure.
- **Fix:** Define a strict "Do-Not-Collect" list of parameters in the analytics agent config.

### 11. Overload of Active Agents in Simple Reviews
- **Severity:** Medium
- **Why it matters:** Running too many agents for a simple text fix wastes tokens and increases latency.
- **Failure:** Latency spikes and cost bloat.
- **Fix:** Establish a "minimum useful agents" count (e.g. max 6 for FAST_FEATURE_REVIEW).

### 12. No Structured Agent Disagreement Resolver
- **Severity:** Medium
- **Why it matters:** When agents disagree, the Orchestrator has no rule to resolve the issue.
- **Failure:** Synthetic consensus that hides critical warnings.
- **Fix:** Force a Trade-off Matrix showing risk vs reward for founder review.

### 13. Lack of Naming Restraints
- **Severity:** High
- **Why it matters:** App names or copy can easily fall into clinical-sounding claims.
- **Failure:** Immediate FTC/FDA action for marketing an unapproved medical device.
- **Fix:** Veto any naming proposal that uses medical or diagnostic prefixes (e.g. "Neuro-", "Therapy-").

### 14. Uncontrolled Overtraining Risk
- **Severity:** Medium
- **Why it matters:** Users might train for hours, leading to mental fatigue and practice-effect bias.
- **Failure:** Invalid psychometric results and user burnout.
- **Fix:** Add a fatigue recovery safety gate with a mandatory 15-minute training limit.

### 15. No Opt-In Stress Trigger Controls
- **Severity:** High
- **Why it matters:** Sensory interference features (e.g. strobe distractors) can induce anxiety or trigger epilepsy.
- **Failure:** User safety incidents and negative reviews.
- **Fix:** Stress performance agent must veto any time-pressure or distraction feature that lacks opt-in toggles.

### 16. Incomplete Test Coverage Gates
- **Severity:** Medium
- **Why it matters:** Technical plans can move forward without clear manual or automated test specs.
- **Failure:** Unreproducible bugs in production code.
- **Fix:** QA agent must veto any proposal that lacks explicit steps for manual verification.

### 17. No Validation for baseline drift
- **Severity:** Medium
- **Why it matters:** Learning/practice effects can cause baseline drift, making exercises look effective when they are not.
- **Failure:** Misleading user reports of cognitive improvement.
- **Fix:** Mandate psychometrics agent to require baseline recalibration every 10 sessions.

### 18. Pay-to-Win Telemetry Risk
- **Severity:** High
- **Why it matters:** Gamification features could offer boosters that affect rating or score metrics.
- **Failure:** Destruction of measurement integrity and competitive fairness.
- **Fix:** Veto any gamification feature that modifies baseline scores or ratings.

### 19. No Verification of Subagent Call Trace
- **Severity:** High
- **Why it matters:** Orchestration outputs are difficult to verify without a trace ID map.
- **Failure:** Unattributable outputs and difficulty debugging subagent hallucination.
- **Fix:** Force Orchestrator to output the unique Conversation ID of every subagent in the response header.

### 20. Lack of Opt-Out for Telemetry Logs
- **Severity:** High
- **Why it matters:** Logging user performance logs without user consent violates privacy ethics.
- **Failure:** Loss of user trust and compliance risk.
- **Fix:** Require the privacy ethics agent to veto any logging feature that lacks user control toggles.
