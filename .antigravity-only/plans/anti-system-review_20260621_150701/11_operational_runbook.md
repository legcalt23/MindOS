# Operational Runbook

This runbook outlines the operational protocols, troubleshooting guides, and registry maintenance steps for running and maintaining the `/anti` subagent review system.

---

## 1. Execution Guidelines

Follow these operational guidelines to run the subagent review engine:

### How to Run `/anti`
- Invoke the command from the repository root.
- Specify the input file or directory to review, and pick the appropriate mode flag.
- *Examples:*
  - `/anti fast plans/my_plan.md`
  - `/anti deep plans/my_plan.md`
  - `/anti implementation specs/timing_architecture.md`

### How to Choose the Review Mode
- Use `FAST_REVIEW` (`/anti fast`) for rapid syntax, sanity, and baseline risk checks.
- Use `DEEP_REVIEW` (`/anti deep`) for major strategic milestones, architectural updates, and release gates.
- Use `SCIENTIFIC_REVIEW` (`/anti prompt`) when a plan introduces cognitive science claims, user baselines, or clinical terms.
- Use `SECURITY_REVIEW` when updates touch authentication boundaries, environment variables, or read/write scopes.

---

## 2. Status Interpretation

The system returns one of four statuses:
- **`GO_INTERNAL`:** All validation checks passed, no critical blockers remain, and the plan is approved for internal testing.
- **`CONDITIONAL_GO_INTERNAL`:** Minor issues exist, but the plan can proceed to internal testing provided the owner addresses the specified items before release.
- **`NO_GO_HIGH_STAKES`:** Critical blockers, security vulnerabilities, or vetoes were triggered. The plan is rejected and must be revised.
- **`INCONCLUSIVE_BLOCKED`:** The review run crashed, timed out, failed verification checks, or lacked valid credentials.

---

## 3. Failure Troubleshooting

In the event of runtime failures, agent timeouts, or API quota exhaustions, use these troubleshooting steps:

### Handling Agent Crashes or Quota Failures
If an agent fails to respond or triggers `RESOURCE_EXHAUSTED` (Rate Limits):
1.  The orchestrator will retry the invocation once.
2.  If the retry fails, check if the agent is marked as `critical`.
3.  If the crashed agent is `critical`, the run is terminated with `INCONCLUSIVE_BLOCKED`.
4.  Do not attempt to bypass this by editing the local verdict markdown file; the verification script will detect the trace mismatch.

### Handling Verification Failure
If `/anti verify` fails and generates `13_verification_failures.md`:
1.  Open the failure report to identify the missing files, headers, or placeholder strings.
2.  Fix the specific format errors or add missing evidence ledgers.
3.  Re-run the verification command: `/anti verify [package_path]`.
4.  The final status cannot be upgraded until the verification script returns a clean `PASS`.

---

## 4. Roster and Registry Maintenance

Maintain the active subagent registry, modes, and definitions using these procedures:

### Adding a New Agent
1.  Draft the agent configuration in a JSON file following the schema in `07_updated_agent_registry_spec.md`.
2.  Save the file under `.antigravity-only/agents/schemas/[agent_id].json`.
3.  Add the agent's system prompt definition to `agent_definitions_full.md`.
4.  Submit a plan update to the founder for approval before activating the agent in the routing map.

### Retiring an Agent
1.  Remove the agent ID from all review modes in `FOUNDER_COUNCIL_REVIEW_MODES.md`.
2.  Move the agent's JSON manifest to `.antigravity-only/agents/quarantine/`.
3.  Archive the agent's prompts and update the registry document.

### Updating Review Modes
1.  Modify the mode mappings inside `FOUNDER_COUNCIL_REVIEW_MODES.md`.
2.  Update the command contracts in `06_updated_anti_command_contract.md`.
3.  Run a validation test suite using the Golden Fixtures to ensure routing works as expected.

---

## 5. Governance and Founder Approval
- **Runtime Modification Lock:** Any changes to active `/anti` runtime files (command router, bootstrap script, or core prompts) require explicit authorization from the founder.
- **Veto Overrides:** A founder may override a subagent veto by logging a signed block in `OVERRIDE_LOG.md` following the Human Override Policy.
