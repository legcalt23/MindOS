# CLI Status & Reconciliation M8 Audit

This document details the status CLI command and how it handles agent reconciliation.

## 1. CLI status Command
The `status` command remains strictly manifest-bound:
* It reads the manifest path provided with `--manifest`.
* It prints run statistics, final status, required/invoked/failed agents, and token usage metrics.
* It performs agent roster reconciliation using `reconcile_agents_against_manifest()`.

## 2. Reconciliation Modes
Reconciliation implements two distinct severity modes:
* **LEGACY_COMPAT_MODE:** Matches M3-M7 runs. Unknown agents (e.g. `agent_orchestration_architect`) are reported as warnings instead of causing errors.
* **STRICT_CANONICAL_36_MODE:** Matches new M8 manifests. Any mismatch, unknown agent, duplicate, or missing registration triggers validation failure.
