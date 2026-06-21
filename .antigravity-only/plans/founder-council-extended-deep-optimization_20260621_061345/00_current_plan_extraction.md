# Round 0: Current Plan Extraction

This document extracts and evaluates the state of the initial 36-agent Founder Council Extended draft plan to establish where it succeeded and where it requires deep optimization.

## What It Does Well

1. **Strategic Intent:** Correctly aligns the agent system's scope with the core human intelligence training positioning of MindBoost (local-offline MVP, deterministic timing/scoring, non-diagnostic boundaries).
2. **Hardened Ground Rules:** Explicitly rejects autonomous write-access, secrets access, and network execution.
3. **Registry vs. Squad Distinction:** Proposes the concept of registering 36 agents but invoking only small squads to prevent token bloat and context saturation.
4. **Basic Routing & Operating Protocols:** Establishes initial rules for handling agent disagreements and enforcing Red Team presence.

---

## Technical and Operational Failures of the Draft

1. **Vague Agent Parameters:** The definitions of the 36 agents are too brief and overlap significantly in cognitive science domains (e.g. attention, memory, and executive function could cross-talk without distinct decision boundaries).
2. **Lack of Evidence Grading:** It lacks a structured evidence grading system to evaluate agent claims (separating peer-reviewed data, internal tests, and speculative assumptions).
3. **No Request Schema:** The draft fails to provide a standardized, founder-friendly input schema to ensure requests are submitted with necessary technical data.
4. **Veto Ambiguity:** The draft states that certain agents can veto, but does not define the exact conditions or technical consequences of a veto.
5. **State Machine Confusion:** The registration status of `SESSION_REAL` vs `DESIGNED_NOT_EXECUTED` is ambiguous. There is no clear lifecycle model tracking when agents are registered, smoke-tested, or operational.
6. **No Anti-Hype Gate:** The draft does not establish a clear trust gate to prevent agents from falling into generic "AI brain training" buzzword loops.
7. **Undefined Failure Handling:** It fails to explain how the parent orchestrator should handle situations where subagent invocation fails due to runtime limits.
