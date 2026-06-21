# Next Phase Recommendation

This document outlines the recommendations and roadmap for the next phase of `/anti` system development (Phase M3-M4).

---

## 1. Scope of Next Phase
The M0–M2 hardening successfully implemented static checks, registries, verification scripts, and sandbox policies. To move toward high-stakes readiness, the following runtime integrations are recommended:
1.  **Platform Shell Runner:** Integrate `verify_subagents.py` as a pre-commit hook or CI/CD gate.
2.  **Runtime Path Guards:** Implement the sandbox path constraints inside the platform's tool execution adapters (wrapping read/write/MCP actions).
3.  **Encrypted Telemetry Ledger:** Write strategic verdicts to an append-only, local encrypted ledger.

## 2. Request for Founder Action
We request authorization from the founder to execute the following M3 milestones:
- Implement runtime path interception in the active platform orchestrator code.
- Auto-generate review manifests on every `/anti` run.
