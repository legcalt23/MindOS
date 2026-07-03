# Test Execution M8 Report

This document reports the local test execution results.

## 1. Test Verification
All test suites were executed locally in a clean Python process environment.

* **Sandbox Tests:** `test_sandbox_enforcement.py`
  * Total cases: 14
  * Result: PASS
* **Registry Tests:** `test_anti_agent_registry.py`
  * Total cases: 11
  * Result: PASS
* **CLI Tests:** `test_anti_cli.py`
  * Total cases: 22
  * Result: PASS

## 2. Test Coverage Metrics
* Exact 36 registry load: Covered
* Duplicate ID rejection: Covered
* Placeholder rejection: Covered
* Empty field rejection: Covered
* Missing source reference rejection: Covered
* Canonical count validation: Covered
* Legacy manifest compatibility warning: Covered
* M8 strict manifest failure on unknown agents: Covered
* Invoked agent not in registry: Covered
* Failed agent not invoked: Covered
* Uninvoked agent with verdict: Covered
* Fake GO verdict with failed agents: Covered
* Replay inconclusive on missing evidence: Covered
* Replay fail on hash tamper: Covered
* Status remains manifest-bound: Covered
* Agents command prints registry without fabricating runtime state: Covered
