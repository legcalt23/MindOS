# Production Repository Structure

MindOS command routing is organized as a Kotlin Multiplatform monorepo with strict module
boundaries. The repository is production-ready only when module ownership, governance docs, release
process, and verification gates are present.

## Modules

| ZIP | Modules | Responsibility |
|-----|---------|----------------|
| ZIP01 foundation | `command-router-actions`, `command-router-patterns`, `command-router-resolver`, `command-router-core`, `action-executor`, `navigation-contracts` | Legacy typed command routing foundation. |
| ZIP02 understanding | `mind-normalization`, `mind-entity-extraction`, `mind-action-core`, `mind-action-patterns`, `mind-action-matching` | Deterministic text to intent/entity understanding. |
| ZIP03 execution | `mind-action-model`, `mind-navigation`, `mind-reference-resolver`, `mind-action-validation`, `mind-action-execution` | Typed action validation, reference resolution, and navigation command execution. |
| ZIP04 testing | `mind-testkit` | Deterministic corpus, fuzz, property, and benchmark utilities. |
| ZIP05 security | `mind-security` | Input security, abuse prevention, and rate limiting contracts. |
| ZIP06 observability | `mind-observability` | Telemetry, audit, metrics, logs, SLOs, alerts, dashboards, and runbooks. |
| ZIP08 contracts | `mind-kmp-contracts` | Public KMP DTOs, result types, validation reports, serialization policy, and router API contracts. |
| ZIP09 KMP core | `mind-kmp-core` | Public KMP router implementation over contracts, matching, normalization, extraction, validation, and error models. |
| ZIP10 Decompose integration | `mind-decompose` | Framework boundary for Decompose navigation, deep-links, and bounded navigation coordinator wiring. |
| App runtime facade | `mind-app-runtime` | Public disabled-by-default application composition API over text and optional headless voice runtime. |
| Android voice adapter | `mind-android-voice` | Feature-flagged Android push-to-talk UI and platform audio boundary. |
| Command router hardening | `mind-security`, `mind-reference-resolver`, `mind-action-execution`, `mind-navigation`, `mind-kmp-core`, `mind-decompose` | Secure wiring, unified gate, concurrency-safe state, replay protection, bounded navigation, chained resolution, and PII-safe traces. |

The GitHub check `Command Router Voice / canonical-runtime` is the reproducibility gate for these modules. Branch protection must keep this check required for changes matching the workflow paths; the repository cannot configure that external GitHub setting itself.

## Repository Rules

- Keep ownership explicit: every module must have a documented responsibility and extension points.
- Keep dependencies acyclic. Main source sets must never introduce cycles.
- Keep side effects out of routers/matchers/resolvers; only executor/coordinator layers may trigger side effects.
- Keep production SDKs out of core modules. Apps inject concrete sinks, clocks, resolvers, navigators, and storage.
- Add every production process change to docs, not just to tribal knowledge.

## Verification

Run:

```bash
./gradlew verifyProductionRepository
```

The task checks required modules, mandatory repository docs, architecture layer classification,
prohibited production dependencies, and dependency cycles across main source sets.
