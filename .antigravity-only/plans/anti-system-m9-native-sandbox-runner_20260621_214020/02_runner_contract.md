# Runner Contract Specification

This document defines the functional interface, arguments, and operating constraints of the future `/anti` runner.

## 1. Interface Command Line Contract
The runner script will be invoked via the command-line interface:
```bash
python .antigravity-only/scripts/anti_runner.py run --manifest <manifest_path> --mode isolated
```

## 2. Runner Parameters

* **input manifest:** Path to the `run_manifest.json` file detailing required/invoked agents and planned file changes.
* **canonical 36 registry validation:** Before execution, the runner loads `anti_agent_registry.py`, validates the registry PASS status, and reconciles manifest requests against the canonical roster.
* **allowed read roots:** Set to the workspace root. Specific sensitive files (e.g. `.env`, `.env.local`, `google-services.json`, private keys) are explicitly blocked.
* **allowed write roots:** Restrained strictly to `.antigravity-only/plans/` folders and workspace `scratch/` directories.
* **scratch workspace:** A localized temporary scratch space created on disk under the workspace root to support transient outputs.
* **read-only source snapshot:** In `isolated` mode, the product source files (e.g. Kotlin, Swift, Gradle scripts) are mounted read-only to prevent unauthorized modifications.
* **network policy:** Configured to block all outgoing sockets (`--network none` in container configurations).
* **environment variable allowlist:** Limits exported environment variables. Only standard non-sensitive path and locale variables are inherited (e.g. `PATH`, `LANG`).
* **transcript output path:** Target folder inside the evidence package where individual agent transcripts are persisted.
* **evidence ledger output path:** Target file path for the generation of `evidence_ledger.md`.
* **file hash delta summary:** Computes and logs the SHA-256 hash modifications for all files affected by the run.
* **exit code contract:**
  * `0`: Run successfully completed and verified.
  * `1`: Execution failed (syntax errors, validation failures, or agent vote contradictions).
  * `2`: Inconclusive (missing transcripts, unresolvable evidence dependencies).
* **cleanup policy:** Scratch workspaces, temp build dirs, and decrypted logs are automatically removed/re-encrypted upon exit.
* **replay integration:** Every run produces a complete sealed package that can be replayed by passing the manifest to `anti_cli.py replay`.
