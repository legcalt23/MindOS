# Remaining Risks

This document tracks unresolved risks in the `/anti` system after the M0–M2 hardening corrections have been applied.

---

## 1. API Rate Limiting & Resource Exhaustion (High Risk)
- **Risk:** Sequential or concurrent execution of multiple subagents during a `DEEP_REVIEW` may exceed Gemini rate limits, resulting in `RESOURCE_EXHAUSTED` errors.
- **Mitigation:** Enforced JIT (Just-in-Time) Lazy Bootstrap to keep squads as small as possible. The orchestrator must implement linear back-off retry loops.

## 2. Platform Sandbox Limitations (Medium Risk)
- **Risk:** Subagents operate under `SHARED_PARENT_SCOPE`. Although `sandbox_policy.json` denylists sensitive paths, path enforcement must be actively supported by the platform's tool execution adapters.
- **Mitigation:** The preflight script checks that no sensitive file paths are passed as arguments to file-reading tools.

## 3. Clock Alteration (Low Risk)
- **Risk:** Alteration of local system clocks could spoof validation timestamps in offline development containers.
- **Mitigation:** Preflight validation requires internet-based NTP synchronization checks before running high-stakes reviews.

## 4. Script Tampering (Low Risk)
- **Risk:** A malicious developer could modify the verification script (`verify_subagents.py`) to bypass quality gates.
- **Mitigation:** Integrate the verification script directly into the CI/CD pipeline configuration, ensuring it is fetched from a protected read-only branch during deployment.
