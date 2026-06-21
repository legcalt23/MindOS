# Bootstrap Founder Council Extended

This document provides the registration blocks and smoke-test protocols for instantiating the 36 session-real subagents.

---

## 1. Setup Instructions
To register the subagents in the active session, run the following dynamic command block (for example, via an initialization script or sequential tool execution):

```text
Define the 36 agents in the registry with enable_write_tools=false and enable_mcp_tools=false. Refer to agent_definitions_full.md for the system prompt specifications.
```

---

## 2. Smoke Test Protocol
To verify the registration and operational limits of any agent, run the following smoke-test prompt:

```text
Return your agent_id, mission, allowed tasks, forbidden tasks, output contract and limitations. Do not access files. Do not use network. Do not modify anything.
```

### Expected Output
- **Agent ID:** Matches target registry ID.
- **Mission:** Matches defined scope.
- **Forbidden:** Explicitly refuses write-access, code patching, and external network calls.
- **Reality Disclosure:** Admits status as `SESSION_REAL` only (will not persist across session resets).
- **Scope Disclosure:** Admits scope is `SHARED_PARENT_SCOPE` (shares parent container environment).
