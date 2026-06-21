# Phase 7: Conversion Plan

This document outlines the conversion steps to transform the simulated personas into a real, runtime-definable subagent architecture.

## Conversion Steps

1. **Schema Definition:**
   - Define a JSON schema for agent manifests in `.antigravity-only/agents/schemas/agent_manifest.schema.json`.
2. **Hardened Configuration Files:**
   - Create `.antigravity-only/agents/REALITY_STATUS.md` detailing the operational state.
   - Create `.antigravity-only/agents/ROUTING_MAP.md` mapping tasks to the 7 core subagents.
   - Create `.antigravity-only/agents/PERSISTENCE_TEST.md` explaining runtime persistence properties.
   - Create `.antigravity-only/agents/ROLLBACK.md` providing exact rollback commands.
3. **Quarantine Execution:**
   - Move the 20 legacy agent markdown files from `.antigravity-only/agents/` to `.antigravity-only/agents/quarantine/`.
   - Update `.antigravity-only/agents/README.md` to reference the new 7-agent tiered model.
4. **Runtime Registration:**
   - Call the `define_subagent` tool to register all 7 core subagents in the current session. (Completed in Phase 2).
5. **Validation Testing:**
   - Create `.antigravity-only/agents/tests/subagent_reality_tests.md` containing test cases.
   - Record results in `09_test_results.md` (Completed in Phase 2).

---

## Active & Quarantined Directory Layout

```text
.antigravity-only/agents/
├── README.md                 <-- Updated main description
├── REALITY_STATUS.md         <-- Reality classifications
├── ROUTING_MAP.md            <-- Task routing mapping
├── PERSISTENCE_TEST.md        <-- Persistence behaviors across reloads
├── ROLLBACK.md                <-- Backout/restore instructions
├── schemas/
│   └── agent_manifest.schema.json
├── tests/
│   └── subagent_reality_tests.md
└── quarantine/               <-- Legacy 20-agent files placed here
    ├── 01_chief_strategy_director.md
    ├── 02_agent_orchestrator.md
    ...
```
