# Remaining Risks

This document details the residual security risks and structural limitations of the `/anti` subagent system.

## 1. Sandbox In-Process Escape Risks
- **Subprocess Escape:** Since the sandbox enforcer operates as an in-process path-checking utility, it cannot prevent subprocesses spawned by the subagent (e.g. running `cat .env` or `powershell Get-Content .env`) from accessing denied files.
- **Low-Level Native Access:** Python libraries using native C extensions or direct syscalls that bypass the Python `open` and `os` namespaces are not monitored by our Python assertions.
- **IDE/Tool Integration Boundary:** We cannot programmatically intercept or block files accessed directly by the MCP tool server or the host IDE editor itself.

---

## 2. Replay and Verification Drift
- **Dynamic Context Drift:** A passing replay verifies that output files match manifest hashes, but does not guarantee that the LLM will generate identical text if run again live, due to model temperature and context window volatility.
- **Transcript Archival Limits:** Since subagent transcripts are not stored by default in the review package directory, replay returns `REPLAY_INCONCLUSIVE` unless logs are manually gathered and archived.
