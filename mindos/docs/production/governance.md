# Governance Model

## Change Classes

| Change | Required Review |
|--------|-----------------|
| Action DSL or `MindAction` change | RFC, compatibility review, test review, migration notes. |
| Module boundary change | ADR, dependency graph review, production repository verification. |
| Security policy change | Security review, abuse case tests, audit/telemetry review. |
| Observability contract change | Operations review, dashboard/alert impact review. |
| Release process change | ADR or RFC depending on impact, owner approval. |

## Action DSL Rules

- No breaking action contract change without an RFC and migration plan.
- New action types must update validation, mapping, execution, docs, and tests in the same change.
- Ambiguous or fuzzy behavior requires explicit design approval. Determinism remains the default.
- Compatibility review must cover old callers, serialized actions, audit events, metrics, and dashboards.

## Ownership

- Core routing and module boundaries: platform owner.
- Security and abuse policy: security owner.
- Observability and runbooks: operations owner.
- Release approval: release owner plus affected module owner.

## Required Evidence Before Merge

- Relevant tests pass locally or in CI.
- `./gradlew verifyProductionRepository` passes.
- Public docs are updated for behavior or process changes.
- ADR/RFC exists for changes that affect compatibility, ownership, release, or architecture.
