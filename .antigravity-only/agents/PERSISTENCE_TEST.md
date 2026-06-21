# Persistence Test Results

This document defines whether the dynamically registered subagents (defined via `define_subagent`) persist through various runtime lifecycle events.

## Persistence Lifecycle Matrix

| Event | Survives? | Explanation / Technical Evidence |
| --- | :---: | --- |
| **Current Session** | **YES** | Once registered in the session container, subagents can be invoked repeatedly within the same conversation without re-defining them. |
| **New Session** | **NO** | Subagents are sandboxed in memory for the active Conversation ID. A new session initializes a clean state where the custom subagent registers are empty. |
| **Runtime Restart** | **NO** | Restarts wipe the in-memory session registers. Since there is no persistent global registration daemon, subagents must be re-defined. |
| **Workspace Reload** | **NO** | Workspace reloads tear down the active sandboxed model context and initialize a fresh session context. |
| **Config Reload** | **NO** | Reloading configuration files re-reads rule matrices and customizations, but does not persist in-memory subagent registrations. |

---

## Technical Conclusion

All custom subagents defined via `define_subagent` are **SESSION_REAL only**. They do **not** qualify as `PERSISTENT_REAL`. To utilize these agents in any new session, the parent orchestrator must explicitly run the definition block first.
