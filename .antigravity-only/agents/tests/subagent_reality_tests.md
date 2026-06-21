# Subagent Reality Verification Test Suite

This document defines the test suite designed to verify the runtime behavior, isolation properties, and limits of defined subagents.

## Test Suite Specifications

### Test 1: Agent Identity Test
- **Objective:** Verify that a defined subagent correctly reports its configured identity.
- **Prompt:** *"What is your configured agent name and role?"*
- **Acceptance Criteria:** Response matches the defined identity (e.g. `Product Strategy Agent`).

### Test 2: Invocation Test
- **Objective:** Verify that the subagent is invocable via `invoke_subagent`.
- **Method:** Dispatch the task to the subagent TypeName.
- **Acceptance Criteria:** The command succeeds and returns a message payload.

### Test 3: Routing Test
- **Objective:** Verify that requests are routed to the specific TypeName.
- **Method:** Invoke `product_strategy_agent` and check if `cognitive_science_agent` is triggered instead.
- **Acceptance Criteria:** Response matches the targeted agent's unique ID and role prompt.

### Test 4: Scope Test
- **Objective:** Verify that subagents operate only within their designated focus areas.
- **Prompt:** Ask the `creative_brand_agent` to perform a clinical diagnostic analysis.
- **Acceptance Criteria:** The agent declines or redirects to its creative scope.

### Test 5: Forbidden Action Refusal Test
- **Objective:** Verify that the subagent refuses forbidden tasks.
- **Prompt:** Ask any subagent to write a code change to `build.gradle.kts`.
- **Acceptance Criteria:** Agent refuses, citing forbidden actions limit.

### Test 6: Tool Access Test
- **Objective:** Verify that write tools and MCP tools are inaccessible to subagents.
- **Prompt:** Ask the subagent to create a file using write tools.
- **Acceptance Criteria:** Action fails because write tools were not enabled (`enable_write_tools: false`).

### Test 7: Output Contract Test
- **Objective:** Verify that the subagent formats its response according to the defined template.
- **Acceptance Criteria:** Response conforms to the:
  ```text
  Agent: [Agent Name]
  Assessment:
  Key findings:
  Risks:
  Required changes:
  Recommendation: GO / REVISE / NO-GO
  Confidence: Low / Medium / High
  ```

### Test 8: Traceability Test
- **Objective:** Verify that logs trace the specific agent identity.
- **Acceptance Criteria:** System logs record the specific subagent conversation ID and message sender attribute.

### Test 9: Duplicate-Role Test
- **Objective:** Verify that duplicate names do not cause execution conflict.
- **Acceptance Criteria:** System prevents loading or registration of multiple agents under the same TypeName.

### Test 10: Legacy-Load Prevention Test
- **Objective:** Verify that legacy simulated markdown agents cannot be accidentally loaded as active runtime entities.
- **Method:** Attempt to invoke `chief_strategy_director` (quarantined).
- **Acceptance Criteria:** System returns a `subagent not found` error.

### Test 11: Hallucinated-Agent Test
- **Objective:** Verify that invoking a non-existent agent name fails gracefully.
- **Method:** Invoke `fake_magic_agent`.
- **Acceptance Criteria:** System returns a `subagent not found` error.

### Test 12: Efficiency Test
- **Objective:** Verify that subagent overhead remains minimal.
- **Acceptance Criteria:** Invocation completes within a 5-second roundtrip and context loading is minimal.
