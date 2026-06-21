# Internal Rollout Runbook

This runbook outlines the rollout guidelines, safe commands, troubleshooting protocols, and merge procedures for operating the hardened `/anti` subagent system.

---

## 1. Execution Guidelines

### How the Founder Runs `/anti`
- Ensure you are operating within the safety branch or target workspace repository.
- Invoke commands following the contract rules:
  - `/anti fast [fixture]`
  - `/anti deep [directory]`
  - `/anti verify [package_path]`

### Safe and Blocked Review Modes
- **Safe Modes (GO_INTERNAL_VERIFIED):**
  - `FAST_REVIEW` (`/anti fast`)
  - `DEEP_REVIEW` (`/anti deep` for internal audits)
  - `SUBAGENT_SYSTEM_REVIEW` (`/anti system` for verifying `/anti`)
- **Blocked Modes (Requires Platform Sandbox Wiring):**
  - `SECURITY_REVIEW` and any production release-gate reviews. The sandbox policy in `sandbox_policy.json` is not actively enforced by the platform's execution layer; high-stakes evaluations remain blocked.

---

## 2. Troubleshooting and Recovery

### Handling Failed Agents or Timeouts
1.  Check the orchestrator execution logs. If a critical agent crashed, the run status is forced to `INCONCLUSIVE_BLOCKED`.
2.  Do not attempt to write a simulated result; mock fallbacks are disabled.
3.  Re-run the command once rate limits reset.

### Handling Verification Failure
1.  Verify the package files by running `python verify_subagents.py --package [path] --json --strict --write-failures`.
2.  Inspect the generated `13_verification_failures.md`.
3.  Fix the specific header or placeholder errors and recompute file hashes inside `package_integrity.md`.

---

## 3. Deployment and Maintenance

### Reviewing Diffs before Merge
Before merging changes from the safety branch to `main`:
1.  Run `git diff main HEAD --stat` to verify that no product code has been touched.
2.  Run `python verify_subagents.py --package [your_package] --strict` to verify package completeness.

### Rollback Procedures
To undo the hardening changes and restore the legacy simulated agents system:
1.  Checkout `main`.
2.  Delete the created JSON registries and policy files.
3.  Run the restoration commands in `.antigravity-only/agents/ROLLBACK.md`.

### Adding/Retiring Agents
- Manifest configurations must validate against `.antigravity-only/agents/agent_manifest.schema.json`.
- Add agent system prompts to `agent_definitions_full.md` before registration.

### Updating Sandbox Policy
1.  Edit `.antigravity-only/sandbox_policy.json`.
2.  Add denied path regex patterns to `denied_path_patterns`.
3.  Update the runbook and get founder approval before deployment.
