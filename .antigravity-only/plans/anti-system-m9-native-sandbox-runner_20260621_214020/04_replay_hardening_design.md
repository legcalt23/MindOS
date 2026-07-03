# Replay Hardening Design Specification

This document details the verification flow and security assertions enforced by the replay command.

## 1. Replay Validation Model
The replay command ensures that the execution evidence is consistent, repeatable, and has not been tampered with since creation.

## 2. Hardened Verification Checks

* **manifest hash:** Asserts that the hash of `run_manifest.json` matches the integrity logs.
* **package hashes:** Re-computes the LF-normalized SHA-256 hash of every package file on disk and verifies they match the values in `package_integrity.md`.
* **evidence ledger hash:** Verifies that the ledger checksum matches manifest records and contains valid verification states for all claims.
* **transcript hashes:** Ensures each transcript JSONL file has a valid checksum that matches the integrity manifests.
* **registry source hashes:** Verifies the underlying registry source hashes (`03_agent_specialization_surgery.md` and `11_final_upgraded_founder_council_plan.md`) match the hardcoded baseline values.
* **canonical 36 consistency:** Validates that the active subagent registry is intact, has exactly 36 members, and does not contain duplicate IDs, empty fields, or placeholder items.
* **runner config:** Audits the execution environment parameters (mode, allowed write routes) recorded during the run.
* **sandbox mode:** Verifies that the sandbox configuration is captured and corresponds to the reported isolation level.
* **no-network proof:** Checks docker logs or network namespace traces to prove that the execution did not open external sockets.
* **file delta proof:** Compares the actual filesystem diffs with the mapped `file_deltas` stored in the transcripts.
* **forbidden write absence:** Audits transcripts to confirm that no security violations or blocked write requests occurred during execution.
