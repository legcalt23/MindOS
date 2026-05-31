# Production Launch Checklist

Use this checklist before promoting a command-router release to production.

## Architecture

- [ ] Architecture and module boundaries approved.
- [ ] Any compatibility-affecting change has an RFC or ADR.
- [ ] No direct navigation or side effects from matcher/router/resolver layers.
- [ ] `./gradlew verifyProductionRepository` passes.

## Tests

- [ ] `./gradlew allTests` passes.
- [ ] Unit, integration, E2E, property/fuzz, and benchmark gates are green.
- [ ] New action DSL behavior has mapping, validation, execution, and negative tests.

## Security

- [ ] Input validation and abuse/rate-limit behavior reviewed.
- [ ] No raw user input in metrics or structured logs.
- [ ] Audit events include requestId and outcome.
- [ ] Security-sensitive changes have reviewer sign-off.

## Observability

- [ ] Metrics include `appVersion`, `platform`, and `commandType`.
- [ ] Structured logs include requestId, sessionId, timestamp, action, outcome, and latencyMs.
- [ ] SLO dashboards are active.
- [ ] Alert policy is active for availability, execution failures, latency, unknown command growth, and resolver degradation.

## Operations

- [ ] Rollback plan prepared.
- [ ] Runbooks available.
- [ ] Owner assigned for rollout monitoring.
- [ ] Incident response path confirmed.
