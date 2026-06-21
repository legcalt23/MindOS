# Phase 4: Permissions and Isolation Audit

This document audits the permissions, tool access, and isolation properties of the subagents.

## Tool Isolation & Permissions Analysis

1. **Write Access:** We disabled write tools (`enable_write_tools: false`) when defining the subagents. They cannot modify files or run terminal commands.
2. **Subagent Invocation:** We disabled subagent tools (`enable_subagent_tools: false`). Subagents cannot recursively define or invoke other subagents.
3. **MCP Tools:** We disabled MCP tools (`enable_mcp_tools: false`). Subagents cannot access MCP tools.
4. **Network & Secrets:** Subagents have no network access or access to secrets/credentials.
5. **Permission Model:** Subagents execute within the same sandbox environment as the parent agent. They share the same workspace folder path. Because of this, isolation is classified as `SHARED_PARENT_SCOPE` rather than process-level isolation.

---

## Risk Matrix

| Agent | Read Scope | Write Scope | Network | Secrets Risk | Can Modify Config | Can Invoke Others | Risk | Required Fix |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `Product Strategy Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `Cognitive Science Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `Financial/Business Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `Creative/Brand Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `Technical Architecture Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `Security/Privacy Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |
| `QA/Evidence Agent` | Workspace root (read-only) | None | No | No | No | No | Low | Keep `enable_write_tools` and `enable_mcp_tools` false. |

---

## Permission Isolation Maturity

**Maturity level:** `SHARED_PARENT_SCOPE`

- **Description:** True operating-system or process-level container isolation is not proven. The subagents inherit the parent's read permissions and execute inside the shared sandbox. However, write boundaries and recursive delegation are successfully restricted by disabling write and subagent tools at definition time.
