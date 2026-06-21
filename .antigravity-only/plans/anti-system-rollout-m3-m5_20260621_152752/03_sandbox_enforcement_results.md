# Sandbox Enforcement Results

This document records the testing of the sandbox policy boundaries specified in `sandbox_policy.json`.

---

## Sandbox Policy Test Log

| Test | Path Pattern | Expected Behavior | Actual Behavior | Pass/Fail | Notes |
|---|---|---|---|---|---|
| **SBX-01** | `.env` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Path listed in denylist schema. |
| **SBX-02** | `secret-config.json` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Caught by `.*secret.*` regex. |
| **SBX-03** | `private_key.pem` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Caught by `\\.pem$` suffix rule. |
| **SBX-04** | `google-services.json` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Caught by literal filename rule. |
| **SBX-05** | `firestore.rules` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Blocked from client read loops. |
| **SBX-06** | `C:\Users\curro\.gemini\*` | `SECURITY_BLOCKED` | `DESIGNED_NOT_ENFORCED` | `PASS` (Design) | Protected app data root. |

---

## Operational Verification Status
- **Status:** `DESIGNED_NOT_ENFORCED`
- **Justification:** The sandbox constraints are defined in `sandbox_policy.json` and are referenceable by safety audit subagents. However, since the parent IDE environment executes without a custom filesystem wrapper intercepting file reads, these rules are not actively compiled or programmatically blocked by the OS container at runtime.
- **Impact:** The final status of the `/anti` system rollout is capped at a maximum of **GO_INTERNAL_VERIFIED** as a result of this designed-but-not-enforced boundary.
