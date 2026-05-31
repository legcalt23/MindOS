# Contributing

MindOS command router changes must preserve deterministic behavior, typed contracts, strict module
boundaries, and production operability.

## Local Verification

Run before handing off a change:

```bash
./gradlew verifyProductionRepository
./gradlew allTests
```

Use `./gradlew productionReady` when you want the combined repository gate and full test suite.

## Design Requirements

- Use ADRs for architecture or module-boundary decisions.
- Use RFCs for action DSL, compatibility, release, security, or rollout-impacting changes.
- Keep core modules KMP-pure and backend-agnostic.
- Do not introduce hidden retries, global mutable state, platform SDKs, or raw user input in metrics/logs.

## Tests

- Add focused tests for every behavior change.
- For matcher behavior, cover deterministic output and failure modes.
- For security/observability behavior, assert the exact event/log/metric contract being changed.
- For production repository changes, keep `verifyProductionRepository` passing.
