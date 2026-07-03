# Mind Command Runtime

`mind-command-runtime` is the textual command orchestration boundary for future voice, command bar,
shortcut, deep-link, notification, and automation adapters.

## Responsibilities

`TextCommandGateway.routeAndExecute()`:

1. Rejects requests while `CommandRuntimeConfig.enabled` is false.
2. Calls the public `KmpCommandRouter`.
3. Rejects routing failures and correlation/schema mismatches before execution.
4. Maps `RouterActionDto` exhaustively to `MindAction`.
5. Applies `CommandSafetyPolicy`.
6. Returns a dry-run result, issues confirmation, or delegates to `ActionExecutor`.
7. Returns a typed `CommandDispatchResult`.

`TextCommandGateway.confirmAndExecute()` consumes a single-use confirmation bound to token,
`requestId`, session, action, and TTL. It re-evaluates safety before delegating to the executor.
`TextCommandGateway.cancelConfirmation()` consumes the same bound token without delegating to the
executor, so an explicit cancellation cannot later be replayed as a confirmation.

The gateway never parses text itself and never navigates. The router never executes.
`ActionExecutor` owns effects and `NavigationCoordinator` owns navigation.

## Security defaults

- Runtime disabled by default.
- Current navigation actions explicitly classified low risk.
- Unclassified actions denied.
- Sensitive actions require confirmation.
- Confirmation requires a non-blank session, expires, and rejects replay.
- Unexpected component failures become safe typed rejections; coroutine cancellation propagates.
- `ResponseTextResolver` emits generic localized text without request IDs, raw input, or entity
  values.

Production wiring must inject a cryptographically strong, unpredictable `ConfirmationTokenFactory`
and a real monotonic or wall clock appropriate for the deployment. The in-memory confirmation
store is bounded and deterministic, but process-local.

## Extension rules

When adding a new public action:

1. Add an explicit DTO-to-`MindAction` mapping.
2. Classify it as low risk or sensitive. Never leave production behavior implicit.
3. Add adapter, safety, gateway, and response regression tests.
4. Keep provider-specific speech code outside this module.

## Rollback

Set `CommandRuntimeConfig.enabled=false` or remove its application wiring. No public router or
executor contract must be reverted.
