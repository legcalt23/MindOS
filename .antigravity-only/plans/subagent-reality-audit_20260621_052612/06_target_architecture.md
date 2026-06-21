# Phase 6: Target Architecture

This document defines the hardened, minimal tiered subagent architecture designed for the Antigravity workspace.

## Tiered Architecture Diagram

```mermaid
graph TD
    User([User Request]) --> Tier0[Tier 0: Parent Orchestrator / Antigravity]
    Tier0 -->|Manual Invocations| Tier1[Tier 1: Core Subagents]
    
    subgraph Tier 1: Core Subagents (SESSION_REAL)
        A[Product Strategy Agent]
        B[Cognitive Science Agent]
        C[Financial/Business Agent]
        D[Creative/Brand Agent]
        E[Technical Architecture Agent]
        F[Security/Privacy Agent]
        G[QA/Evidence Agent]
    end
    
    Tier0 -->|Markdown Templates| Tier3[Tier 3: Tools, Templates & Checklists]
    
    style Tier0 fill:#f9f,stroke:#333,stroke-width:2px
    style Tier1 fill:#ccf,stroke:#333,stroke-width:1px
    style Tier3 fill:#ffc,stroke:#333,stroke-width:1px
```

---

## Tier Definitions

### Tier 0 — Orchestrator (Parent Agent)
- **Role:** Coordinating parent agent (Antigravity).
- **Routing Mode:** Manual / Prompt-Mediated. The orchestrator maps the incoming user request, defines/activates the necessary subagents, feeds them their inputs, and synthesizes their output blocks. No automatic runtime routing is supported.

### Tier 1 — Core Strategic Agents (SESSION_REAL)

1. **Product Strategy Agent (`product_strategy_agent`):**
   - **Purpose:** Audits long-term value, moat, feature alignment, and product coherence.
   - **Allowed Tasks:** Strategic fit analysis, competitive comparisons, moat evaluations.
   - **Forbidden Tasks:** Writing code, modifying production files.

2. **Cognitive Science Agent (`cognitive_science_agent`):**
   - **Purpose:** Validates cognitive science rigor, transfer-of-training mechanics, and validity of claims.
   - **Allowed Tasks:** Evidence-level scoring, cognitive task analysis, mapping transfer effects.
   - **Forbidden Tasks:** Medical or IQ diagnostic claims, code implementation.

3. **Financial/Business Agent (`financial_business_agent`):**
   - **Purpose:** Reviews monetization modeling, unit economics, conversion impact, and risk-adjusted ROI.
   - **Allowed Tasks:** Cost/benefit estimation, CAC/LTV forecasting, pricing reviews.
   - **Forbidden Tasks:** Direct ledger modification, writing operational scripts.

4. **Creative/Brand Agent (`creative_brand_agent`):**
   - **Purpose:** Audits branding, premium design aesthetics, UI consistency, and emotional engagement loops.
   - **Allowed Tasks:** Reviewing mockup visual elements, copy emotional tone, UX flow logic.
   - **Forbidden Tasks:** Asset file exports, coding styling files directly.

5. **Technical Architecture Agent (`technical_architecture_agent`):**
   - **Purpose:** Verifies KMP architecture patterns, Compose performance constraints, and database scalability.
   - **Allowed Tasks:** Feasibility checks, inspecting package structures, detailing database changes.
   - **Forbidden Tasks:** Patching Kotlin/Swift source files.

6. **Security/Privacy Agent (`security_privacy_agent`):**
   - **Purpose:** Reviews Firebase Security Rules, encryption mechanisms, and telemetry scopes.
   - **Allowed Tasks:** Static review of rules, validating data minimization parameters.
   - **Forbidden Tasks:** Accessing secrets, enabling network access, modifying active rules.

7. **QA/Evidence Agent (`qa_evidence_agent`):**
   - **Purpose:** Reviews test scripts, coverage criteria, regression verification, and gate compliance.
   - **Allowed Tasks:** Checking test logs, reviewing test plans, validating acceptance criteria.
   - **Forbidden Tasks:** Modifying pipeline configs, bypassing approval gates.

### Tier 3 — Tools and Templates (Not Agents)
- File validators, static rule checking scripts (`check-storage-rules.js`), lint configurations, checklists (like `mvp_domain_checklist.md`), and markdown prompt generators (e.g. `claude_code_prompt_template.md`). These are handled as tools, templates, or scripts, not as independent active agents.
