# CLI Replay M8 Audit

This document details the replay CLI command verification steps.

## 1. CLI replay Command
The `replay` command verifies the evidence package for repeatability and tamper prevention:
* Validates package path completeness.
* Asserts hashes of all files match the manifest hashes.
* Confirms manifest hashes match records in `package_integrity.md`.

## 2. Replay Safeguards
Replay enforces the following logic gates:
* **Fake Verdict Prevention:** If final status is `GO_INTERNAL` / `GO_INTERNAL_VERIFIED` but there are `failed_agents`, replay exits with code 1.
* **Uninvoked Agent Verdict Prevention:** If an agent is marked as uninvoked in the vote matrix but has a non-empty verdict, replay exits with code 1.
* **Inconclusive Exit:** If transcripts or `evidence_ledger.md` are missing, replay exits with code 2 instead of claiming verification.
