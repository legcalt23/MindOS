# Action Executor

## Purpose

The executor is the only layer that causes side effects. It:
1. Dispatches `CommandAction` to the registered `ActionHandler`
2. Owns the `Navigator` reference — navigation is a side effect of the executor, not the router
3. Wraps all handler failures in typed `ActionExecutionResult.Failure` variants

## Contracts

```kotlin
interface ActionExecutor {
    suspend fun execute(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult
}

interface ActionHandler {
    val actionType: ActionType
    suspend fun handle(action: CommandAction, context: ActionExecutionContext): ActionExecutionResult
}
```

## Failure types

```kotlin
sealed interface ActionExecutionFailure {
    class NoHandler(val actionType: ActionType)
    class HandlerFailed(val actionType: ActionType, val message: String)
    class NavigationFailed(val reason: String)
    class Unauthorized(val actionId: ActionId)
    class InvalidAction(val actionId: ActionId)
    object Cancelled
}
```

## DefaultActionExecutor

- Looks up the handler for `action.metadata.actionType`
- If no handler: returns `Failure(NoHandler)`
- Calls `handler.handle(action, context)`
- On `CancellationException`: re-throws (preserves coroutine structured cancellation)
- On any other exception: returns `Failure(HandlerFailed)` — never lets exceptions escape

## NavigationActionHandler

The only handler that calls `Navigator`. It:
1. Casts `CommandAction` to `NavigateAction` — returns `Failure(InvalidAction)` if wrong type
2. Builds a `NavigationCommand(NavigationTarget.Screen(action.targetId, action.parameters), ...)`
3. Calls `navigator.navigate(command)`
4. Maps `NavigationResult` to `ActionExecutionResult`

**The Navigator implementation lives in the application layer.** The `NavigationActionHandler` only holds the interface reference.

## Assembly

```kotlin
val executor = DefaultActionExecutor(
    ActionHandlerRegistry(listOf(
        NavigationActionHandler(myNavigator),
        NoOpActionHandler(),
        // custom handlers...
    ))
)
```

## Separation of router and executor

The router and executor are intentionally separate objects. The caller decides whether to execute a routed action:

```kotlin
val routeResult = router.route(input)
if (routeResult is CommandRouterResult.Success) {
    // Optionally validate, log, or gate before executing
    val execResult = executor.execute(routeResult.action, context)
}
```

This enables:
- Dry-run mode (route without executing)
- Auditing (inspect the action before execution)
- Conditional execution (authorization gate between router and executor)
- Testing the router without a real navigator
