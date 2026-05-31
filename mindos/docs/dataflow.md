# Dataflow

## Step-by-step

```
CommandInput("open settings", source=UserInput)
         │
         ▼ DefaultCommandRouter.route()
         │
    [validate length]
         │ ✓
         ▼
    DefaultCommandNormalizer.normalize()
         → "open settings"
         │
         ▼ PatternRegistry.findMatch("open settings")
         │
         ├─ ExactCommandPattern("open settings", priority=10).match() → PatternMatch(id, {})
         ├─ ParameterizedCommandPattern("open {target}", priority=0).match() → PatternMatch(id, {target="settings"})
         │
         │  Ranking: ExactPattern(priority=10, specificity=(2,0))
         │         > ParamPattern(priority=0, specificity=(1,1))
         │
         ▼ PatternMatchResult.Matched(match, exactPattern)
         │
         ▼ ResolverRegistry.getResolver(exactPattern.id)
         │  → FixedNavResolver (application-registered)
         │
         ▼ FixedNavResolver.resolve(match, ResolutionContext)
         │  → ResolutionResult.Resolved(NavigateAction(targetId="settings"), resolverId)
         │
         ▼ CommandRouterResult.Success(
               action = NavigateAction("settings"),
               trace = CommandRoutingTrace(...)
           )
         │
         ▼  [caller decides to execute]
         │
ActionExecutor.execute(NavigateAction("settings"), context)
         │
         ▼ ActionHandlerRegistry.getHandler(ActionType("navigate"))
         │  → NavigationActionHandler
         │
         ▼ NavigationActionHandler.handle(NavigateAction, context)
         │  → NavigationCommand(Screen("settings"), ...)
         │
         ▼ Navigator.navigate(NavigationCommand)
         │  [platform implementation]
         │
         ▼ NavigationResult.Success(Screen("settings"))
         │
         ▼ ActionExecutionResult.Success(actionId)
```

## Failure paths

All failures are typed — no exceptions escape as control flow.

| Where | Failure type | Result |
|-------|-------------|--------|
| `CommandRouter.route()` | `EmptyInput` | `CommandRouterResult.Failure` |
| `CommandRouter.route()` | `InputTooLong` | `CommandRouterResult.Failure` |
| `PatternRegistry` | `NoMatch` | `CommandRouterResult.Failure(NoPatternMatched)` |
| `PatternRegistry` | `Ambiguous` | `CommandRouterResult.Failure(AmbiguousCommand)` |
| `ResolverRegistry` | no resolver for pattern | `CommandRouterResult.Failure(NoResolverForPattern)` |
| `CommandResolver.resolve()` | `InvalidParameters`, `Unauthorized`, etc. | `CommandRouterResult.Failure(ResolutionFailed)` |
| `ActionExecutor.execute()` | no handler registered | `ActionExecutionResult.Failure(NoHandler)` |
| `ActionHandler.handle()` | exception | `ActionExecutionResult.Failure(HandlerFailed)` |
| `Navigator.navigate()` | `TargetNotFound` | `ActionExecutionResult.Failure(NavigationFailed)` |

## Trace

Every `CommandRouterResult` (success or failure) includes a `CommandRoutingTrace`:

```kotlin
CommandRoutingTrace(
    rawInput        = "  OPEN  Settings  ",
    normalizedInput = "open settings",
    source          = "UserInput",
    patternId       = PatternId("open-settings-exact"),
    resolverId      = ResolverId("fixed-nav-settings"),
    actionId        = ActionId("..."),
    failureReason   = null,
    ambiguousCandidates = null
)
```
