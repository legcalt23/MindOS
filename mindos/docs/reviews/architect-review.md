# Architect Review (ZIP13)

*Distinguished Architect lens: coupling · cohesion · scalability · maintainability · evolution cost.*
Findings are grounded in the current code; each has a severity and remediation.

## Summary

The platform is interface-driven, deterministic and typed end-to-end, with a clean layered graph
(understanding → execution → integration) verified acyclic by `verifyProductionRepository`. The main
architectural debts are **two coexisting stacks** (legacy ZIP01 vs `mind-*`) and a **manually
maintained dependency-rule map**. No blocker-level concerns.

## Findings

### A1 — Dual command-router stacks (Medium)
`command-router-*` (ZIP01) and `mind-action-*` (ZIP02+) both implement matching/resolution/execution.
`command-router-patterns` is functionally superseded by `mind-action-matching`; `action-executor` by
`mind-action-execution`. Two mental models increase onboarding and maintenance cost.
- **Impact:** conceptual duplication; ambiguity about the canonical surface.
- **Remediation:** explicitly mark the foundation modules as *legacy* (done in docs) and schedule
  retirement, or fold their unique value (the trace model) into the `mind-*` stack. Add a deprecation
  note to their package docs.

### A2 — Manually maintained dependency-rule map (Medium)
`verifyProductionRepository` hardcodes `prohibitedDependencies` and the module list. A new module that
nobody adds to the map is silently unenforced.
- **Impact:** boundary enforcement can drift as the repo grows.
- **Remediation:** declare a layer index (e.g. `layer = base|understanding|execution|integration`)
  per module and derive allowed edges from it; fail when a module is unclassified.

### A3 — Parallel public surfaces: `mind-kmp-core` vs `mind-action-*` (Low)
`mind-kmp-core` (ZIP09) reimplements router wiring (`CommandRouterImpl`, `PatternMatcherImpl`…) over
`mind-kmp-contracts`, parallel to the `mind-action-*` engine.
- **Impact:** unclear which is "the" public API for app/backend consumers.
- **Remediation:** document `mind-kmp-core` as the stable external API and `mind-action-*` as the
  internal engine it composes; add an ADR.

### A4 — High module count (Low)
22 modules including micro-modules (`mind-action-model`, `mind-navigation`). Strong boundaries, but
navigation/build overhead.
- **Impact:** more `build.gradle.kts` and project graph nodes.
- **Mitigation already applied:** convention plugins (ZIP13) removed the per-module boilerplate.
  Keep the granularity — it is what enforces the dependency rules.

### A5 — `IntentToActionMapper` hardcodes v1 intents (Low)
Default builders cover the five v1 intents; new intents require app registration.
- **Impact:** the bridge is a known extension point, not a closed surface.
- **Remediation:** none required — `builders` map is the documented extension seam.

## Strengths (keep)

- Executor depends on the `NavigationCoordinator` **interface**; Decompose is isolated to one module.
- Compile-once/serve-fast registry; token-trie matching is **O(k)**, independent of registry size
  (benchmarked: flat p99 from 10→500 patterns).
- All outputs and failures are sealed/typed; no exceptions as control flow.
- Determinism verified at scale (10k corpus, 50k variations, seeded property/fuzz).

## Evolution cost

Low for the common cases: new entities/intents are registry-driven; new navigation targets add a
`Config`/`MindNavigationCommand` pair + a mapper case. The dual-stack debt (A1) is the main item to
pay down before the surface widens further.
