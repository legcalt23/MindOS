# Updated Runtime Contract

This document specifies the runtime execution parameters, exit codes, and sandbox policies for the hardened `/anti` subagent reviews.

---

## 1. Exit Codes and CLI Interface
All `/anti` CLI operations must return deterministic exit codes to integration pipelines:
- **`0`**: Run successfully completed and verified. Final status is `GO_INTERNAL` or `CONDITIONAL_GO_INTERNAL`.
- **`1`**: Critical error occurred. This includes agent timeouts, dynamic registration failures, validation script failures, sandbox violations, or active vetoes. Final status is `NO_GO_HIGH_STAKES` or `INCONCLUSIVE_BLOCKED`.

## 2. Dynamic Registry Verification
Before executing any review mode:
- The orchestrator validates `agent_registry.json` against `agent_manifest.schema.json`.
- The orchestrator validates the select squad mapping inside `review_modes.json`.
- Preflight checks fail if schema mismatches are found.

## 3. Sandbox Restrictions
- Subagents are blocked from reading files that match the regex patterns in `sandbox_policy.json`.
- The platform file-reading wrapper checks all file targets before returning handles to subagents.
- Any attempt to read restricted paths aborts the invocation, flags a `SECURITY_BLOCKED` event, and terminates the run with status `INCONCLUSIVE_BLOCKED`.
