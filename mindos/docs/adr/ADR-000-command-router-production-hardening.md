# ADR-000 - Command Router Production Hardening

## Status

Accepted.

## Context

The command router had production blockers around insecure defaults, external input surfaces,
process-local mutable state, replay/idempotency, unbounded navigation, chained resolution, and PII in
observability traces.

## Decision

MindOS keeps `mind-kmp-core` as the public KMP API and `mind-action-*` as the canonical engine.
Legacy `command-router-*` modules remain frozen compatibility/foundation modules. New production
behavior must be added through the canonical engine or the public KMP facade, not through a parallel
matcher or extractor.

Production wiring is explicit:

- `CommandRouterFactory.forProduction` rejects no-op abuse/audit wiring and zero clocks.
- `ActionExecutorFactory.forProduction` rejects missing resolvers, no-op audit, zero clocks, and
  missing idempotency store.
- `CommandInputSecurityGate` is the shared gate for text, deep-links, notification/API-style DTO
  fields, and action references.
- `DefaultActionExecutor` preserves `validate -> resolve all -> idempotency -> navigate`.
- `BoundedNavigationCoordinator` owns stack safety policy before Decompose receives commands.

## Consequences

Tests and dev wiring must use `forTests`/development config explicitly. In-memory abuse guard,
resolver cache, and idempotency store are process-local reference implementations; distributed
production deployments must inject durable equivalents.

## Verification

Run:

```powershell
.\gradlew.bat productionReady --no-daemon
```
