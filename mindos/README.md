# MindOS Command Router

Deterministic, typed-action-only command routing for Kotlin Multiplatform.

> **ZIP02 — Action Understanding Layer.** A deterministic intent/entity understanding layer
> (`mind-*` modules) sits upstream of this foundation. See
> [`docs/action-understanding.md`](docs/action-understanding.md).
>
> **ZIP03 — Action Execution Layer.** The execution half (`mind-action-model`, `mind-navigation`,
> `mind-reference-resolver`, `mind-action-validation`, `mind-action-execution`) turns a recognized
> intent into a validated, reference-resolved, typed navigation command handed to a coordinator
> that owns navigation. See [`docs/action-execution.md`](docs/action-execution.md).
>
> Full MindOS flow: `text → understanding → MindIntent + entities → IntentToActionMapper →
> MindAction → validate → resolve refs → MindNavigationCommand → NavigationCoordinator`. Invariants:
> the matcher never executes, the executor never parses text or navigates directly, the
> **coordinator owns navigation**, everything typed and deterministic.
>
> **ZIP04 — Testing & Reliability.** A deterministic, seeded testing platform (`mind-testkit`) with
> property-based, fuzz, 10k+ corpus, integration, E2E and benchmark/regression suites. See
> [`docs/testing-reliability.md`](docs/testing-reliability.md). 222 tests, P99 ≈ 26µs.

> **ZIP05 - Security Hardening.** A `mind-security` layer validates input before matching, detects
> Unicode/homoglyph spoofing, applies injectable abuse/rate-limit controls, and emits PII-free
> telemetry/audit events. See [`docs/security-hardening.md`](docs/security-hardening.md).

> **ZIP06 - Observability & Operations.** `mind-observability` now defines metric catalogs,
> structured logs, dashboards, SLO/SLA evaluation, alert policy, and production runbooks. Matcher
> and executor emit PII-free operation metrics/logs with request/session correlation. See
> [`docs/observability-operations.md`](docs/observability-operations.md).

> **ZIP07 - Production Repository.** Repository governance, ADR/RFC templates, release process,
> implementation roadmap, launch checklist, CODEOWNERS, contributor/security docs, and the
> `verifyProductionRepository` gate make the package operable as a production repository. See
> [`docs/production/repository-structure.md`](docs/production/repository-structure.md).

> **ZIP08 - KMP Contracts.** `mind-kmp-contracts` exposes serializable, versioned, KMP-pure action,
> entity, router result, validation, and API DTO contracts for app/backend boundaries. See
> [`docs/kmp-contracts.md`](docs/kmp-contracts.md).

> **ZIP09 - KMP Core Implementation.** `mind-kmp-core` implements the public KMP router over ZIP08
> contracts, reusing the deterministic matcher, normalization, extraction, validation, typed error
> model, coroutine API, and Flow helper. See [`docs/kmp-core-implementation.md`](docs/kmp-core-implementation.md).

> **ZIP10 - Decompose Integration.** `mind-decompose` wires the engine to real Decompose navigation:
> a `RootComponent` over `StackNavigation<Config>`, a `DecomposeNavigationCoordinator` (the only
> navigation entry point), per-screen component contracts, and a `DeepLinkBridge` (link/notification
> → `MindAction`). Decompose lives in this single module; everything upstream stays framework-free.
> Headless integration tests cover the full `text → … → child stack` chain. See
> [`docs/decompose-integration.md`](docs/decompose-integration.md).

> **ZIP11 - Datasets, Benchmarks & Regression.** `mind-testkit` gains a category-aware
> `DatasetGenerator`, separate alias/ambiguity datasets, a `DatasetExporter` (machine-readable JSON:
> committed sample + on-demand 10k/50k full sets), per-stage benchmarks with throughput/scalability,
> and a permanent regression suite. Matching p99 ≈ 16µs, ~275k ops/s, flat scaling. See
> [`docs/datasets-benchmarks.md`](docs/datasets-benchmarks.md).

## Dataflow

```
User input
    │
    ▼
CommandRouter.route(CommandInput)
    │  normalize → match patterns → resolve to action
    ▼
CommandRouterResult
  Success(action: CommandAction, trace)
  Failure(failure: CommandRouterFailure, trace)
    │
    ▼  (caller decides whether to execute)
ActionExecutor.execute(action, context)
    │  dispatch to handler
    ▼
ActionExecutionResult
  Success(actionId)
  Failure(failure: ActionExecutionFailure)
    │
    ▼  (NavigationActionHandler only)
Navigator.navigate(NavigationCommand)
    ▼
NavigationResult
```

**The router never executes. The executor owns all side effects, including navigation.**

## Modules

| Module | Responsibility |
|--------|---------------|
| `command-router-actions` | Typed action DSL — sealed `CommandAction`, value types, metadata |
| `navigation-contracts` | `Navigator`, `NavigationCommand`, `NavigationResult` — contracts only |
| `command-router-patterns` | Pattern matching engine — `ExactCommandPattern`, `ParameterizedCommandPattern`, `PatternRegistry` |
| `command-router-resolver` | Resolves `PatternMatch` → `CommandAction` — `NavigationIntentResolver`, `ResolverRegistry` |
| `command-router-core` | Public API — `CommandRouter`, `DefaultCommandRouter`, `CommandRouterBuilder`, trace |
| `action-executor` | Executes actions — `ActionExecutor`, `ActionHandlerRegistry`, `NavigationActionHandler` |

## Dependency graph (no cycles)

```
command-router-actions      ◄──────────────────────────────┐
navigation-contracts                                        │
command-router-patterns ──► command-router-actions          │
command-router-resolver ──► command-router-actions          │
                        ──► command-router-patterns         │
command-router-core     ──► command-router-actions          │
                        ──► command-router-patterns         │
                        ──► command-router-resolver         │
action-executor         ──► command-router-actions ─────────┘
                        ──► navigation-contracts
```

## Minimal usage

```kotlin
// 1. Build the router
val factory = ActionFactory(
    ActionIdFactory { ActionId(uuid()) },
    TimestampProvider { System.currentTimeMillis() }
)
val router = CommandRouterBuilder()
    .addPattern(
        ParameterizedCommandPattern(PatternId("open-target"), "open {target}"),
        NavigationIntentResolver(factory)
    )
    .addPattern(
        ParameterizedCommandPattern(PatternId("go-to-target"), "go to {target}"),
        NavigationIntentResolver(factory)
    )
    .build()

// 2. Build the executor (owns navigation)
val executor = DefaultActionExecutor(
    ActionHandlerRegistry(listOf(NavigationActionHandler(myNavigator)))
)

// 3. Route and execute
when (val result = router.route(CommandInput("open settings"))) {
    is CommandRouterResult.Success -> executor.execute(result.action, ActionExecutionContext.Empty)
    is CommandRouterResult.Failure -> handleFailure(result.failure, result.trace)
}
```

## Commands

```bash
cd mindos/

# Build all modules
./gradlew clean build

# Run all tests
./gradlew allTests

# Single module
./gradlew :command-router-core:jvmTest

# Production repository gate
./gradlew verifyProductionRepository
./gradlew productionReady
```

## Design invariants

- **Router does not execute.** `CommandRouter.route()` returns a typed `CommandAction` — it never calls `Navigator` or any side-effecting API.
- **Executor owns navigation.** `ActionExecutor` dispatches to `ActionHandler` implementations. Only `NavigationActionHandler` calls `Navigator`.
- **Typed actions only.** All outputs are sealed types. No stringly-typed results escape the pattern layer.
- **Typed failures.** Expected failure modes (`NoPatternMatched`, `AmbiguousCommand`, `InvalidParameters`, etc.) are sealed variants — never exceptions used as control flow.
- **Determinism.** Same input + same config → same `ActionType` + same `targetId` + same trace structure (IDs excluded — those depend on `ActionIdFactory`).
