# Module Boundaries

## Dependency graph

```
command-router-actions       (no deps)
navigation-contracts         (no deps)
command-router-patterns  ──► command-router-actions
command-router-resolver  ──► command-router-actions
                         ──► command-router-patterns
command-router-core      ──► command-router-actions
                         ──► command-router-patterns
                         ──► command-router-resolver
action-executor          ──► command-router-actions
                         ──► navigation-contracts
```

Test-scope dependencies (does not affect production module graph):
- `command-router-core` (test) → `action-executor`, `navigation-contracts`

## What is allowed / forbidden

### command-router-actions
✅ Value types, sealed action hierarchy, `ActionIdFactory`, `TimestampProvider`  
❌ Any reference to other modules, platform APIs, execution logic

### navigation-contracts
✅ `Navigator` interface, `NavigationCommand`, `NavigationTarget`, `NavigationResult`, `RouteParameters`  
❌ Any reference to `command-router-actions` or executor; no Compose/Android/iOS imports

### command-router-patterns
✅ Pattern interfaces, matching logic, normalizer, `PatternRegistry`  
❌ Resolver logic, action creation, platform code, navigation

### command-router-resolver
✅ `CommandResolver` interface, `ResolutionResult`, `ResolutionFailure`, `ActionFactory`, `ResolverRegistry`, concrete resolvers  
❌ Calling `Navigator`, calling `ActionExecutor`, platform code

### command-router-core
✅ `CommandRouter`, `DefaultCommandRouter`, `CommandRouterBuilder`, `CommandRoutingTrace`, `CommandRouterResult`, `CommandInput`, `CommandRouterConfig`  
❌ Importing `action-executor` or `navigation-contracts` in production code; calling side effects; knowing about platform

### action-executor
✅ `ActionExecutor`, `ActionHandler`, `ActionHandlerRegistry`, `NavigationActionHandler`, `NoOpActionHandler`, `ActionExecutionResult`  
❌ Pattern matching, command normalization, text interpretation; depending on `command-router-core`

## Extension points

| Capability | Extension point |
|-----------|----------------|
| New action type | Add subtype to `CommandAction` sealed hierarchy + `ActionTypes` constant |
| New pattern syntax | Implement `CommandPattern` interface |
| New command intent | Implement `CommandResolver`, register in `ResolverRegistry` |
| New side effect | Implement `ActionHandler`, register in `ActionHandlerRegistry` |
| Custom normalization | Implement `CommandNormalizer`, pass to `CommandRouterBuilder.withNormalizer()` |
| Production Navigator | Implement `Navigator` in app layer, pass to `NavigationActionHandler` |
