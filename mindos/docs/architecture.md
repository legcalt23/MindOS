# Architecture

## Layers

```
┌─────────────────────────────────────────────────────┐
│                   Application layer                  │
│  Wires router + executor; provides Navigator impl    │
└──────────────────────┬──────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          │                         │
┌─────────▼──────────┐   ┌─────────▼──────────┐
│  command-router-*  │   │   action-executor   │
│  (pure resolution) │   │  (side effects)     │
└─────────┬──────────┘   └─────────┬──────────┘
          │                         │
          │              ┌──────────▼──────────┐
          │              │  navigation-contracts│
          │              │  (Navigator iface)   │
          │              └─────────────────────┘
          │
┌─────────▼──────────┐
│ command-router-    │
│    actions         │
│ (action model)     │
└────────────────────┘
```

## Module responsibilities

### command-router-actions
The type vocabulary. Defines `CommandAction` (sealed), all concrete actions, value types (`ActionId`, `ActionType`, `CorrelationId`, `CommandId`), `CommandSource`, `ActionMetadata`, and the `ActionIdFactory`/`TimestampProvider` interfaces. No logic, no execution, no platform deps.

### navigation-contracts
Pure contracts for navigation side effects. `Navigator` is the interface the application layer implements. `NavigationCommand`, `NavigationTarget`, `NavigationResult`, `RouteParameters` — all sealed or data classes with no logic. No dependency on any other module.

### command-router-patterns
The matching engine. Takes normalized text, evaluates patterns, ranks by (priority DESC, specificity DESC), returns `PatternMatchResult` which is exactly one of: `Matched`, `NoMatch`, `Ambiguous`. The ranking is deterministic. No resolver knowledge.

### command-router-resolver
Bridges pattern matches to typed actions. `CommandResolver` implementations know how to build a `CommandAction` given a `PatternMatch`. The foundational `NavigationIntentResolver` handles `open {target}` / `go to {target}` semantics. No execution, no navigator calls.

### command-router-core
Public orchestration API. `CommandRouter.route()` is the single entry point: validate → normalize → match → resolve → return typed result. Produces a `CommandRoutingTrace` for every call (full audit trail regardless of success/failure). `CommandRouterBuilder` provides the assembly DSL.

### action-executor
Side effects layer. `ActionExecutor.execute()` dispatches to registered `ActionHandler`s. `NavigationActionHandler` holds a `Navigator` reference and invokes it. `DefaultActionExecutor` wraps all handler calls in structured exception handling (re-throws `CancellationException`, maps others to `HandlerFailed`).

## Key boundaries enforced by the dependency graph

| Prohibited dependency | Why |
|-----------------------|-----|
| `command-router-core` → `action-executor` | Core must not know about execution |
| `command-router-core` → `navigation-contracts` | Core must not know about navigation |
| `command-router-actions` → anything else | Actions are the base vocabulary — no upward deps |
| `command-router-resolver` → `action-executor` | Resolution is pure; execution is separate |
| `navigation-contracts` → `action-executor` | Contracts cannot depend on implementations |
