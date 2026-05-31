# Implementation Roadmap

## Phase 1: Core Contracts

Typed command actions, navigation contracts, resolver contracts, and command router foundation.

Status: Implemented.

## Phase 2: Matching Engine

Deterministic action understanding, registry compiler, exact/alias/parametric matching, typed entity
extraction, and explain traces.

Status: Implemented.

## Phase 3: Execution Layer

Typed actions, validation, reference resolution, navigation coordinator boundary, audit, and
execution telemetry.

Status: Implemented.

## Phase 4: Testing

Seeded corpus, fuzz, property, integration, E2E, and benchmark/regression harness.

Status: Implemented.

## Phase 5: Security

Input validation, Unicode defense, abuse guard, rate limiting contracts, and security audit events.

Status: Implemented.

## Phase 6: Observability

Metric catalog, structured logs, SLOs, alerts, dashboards, and runbooks.

Status: Implemented.

## Phase 7: Production Rollout

Repository governance, ADR/RFC templates, release process, launch checklist, and automated
repository verification.

Status: Implemented.

## Phase 8: KMP Contracts

Serializable, versioned Kotlin Multiplatform API contracts for actions, router results, entities,
validation, and router API boundaries.

Status: Implemented.

## Phase 9: KMP Core Implementation

Production KMP router implementation over ZIP08 contracts, backed by deterministic matching,
normalization, entity extraction, validation, typed errors, coroutine APIs, and Flow helpers.

Status: Implemented.

## Next Hardening Items

- Add CI wiring for `productionReady` when this nested package is promoted to a tracked repository.
- Add real backend adapters for metrics/logs/audit in the application layer.
- Add persisted benchmark trend storage for release comparison.
