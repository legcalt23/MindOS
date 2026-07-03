# ADR-voice-001 - Canonical Command Stack

## Status

Accepted.

## Context

MindOS contains a legacy `command-router-*` foundation and the current public KMP router and
execution modules. Adding voice-specific parsing or a second route-to-execution pipeline would
create divergent behavior, weaken safety controls, and make ambiguous commands capable of reaching
effects through an alternate path.

## Decision

The canonical command stack is:

```text
input adapter
  -> TextCommandGateway
  -> KmpCommandRouter
  -> RouterActionDto
  -> RouterActionExecutionAdapter
  -> MindAction
  -> CommandSafetyPolicy / ConfirmationStore
  -> ActionExecutor
  -> NavigationCoordinator
```

`mind-kmp-core` remains the only public text router. `mind-action-*` remains the only execution
stack. `mind-command-runtime` only orchestrates these existing boundaries. It does not parse text,
resolve navigation, or execute effects itself.

The legacy `command-router-*` modules remain compatibility/foundation modules and receive no new
production behavior.

Speech, shortcuts, deep links, notification actions, and automation may call
`TextCommandGateway`; none may call `ActionExecutor` directly. Provider-specific speech backends
remain external adapters. The runtime is disabled by default until application wiring explicitly
enables it.

## Consequences

- Unknown, ambiguous, unsafe, unclassified, expired, replayed, and correlation-mismatched commands
  fail closed before execution.
- Sensitive actions require a session-bound, request-bound, action-bound, single-use confirmation.
- The router still never executes, the executor still owns effects, and `NavigationCoordinator`
  remains the only navigation owner.
- New public router action DTOs must add an explicit adapter mapping and safety classification.

## Verification

Run:

```powershell
.\gradlew.bat :mind-kmp-core:jvmTest :mind-command-runtime:jvmTest --console=plain
.\gradlew.bat productionReady --console=plain
```
