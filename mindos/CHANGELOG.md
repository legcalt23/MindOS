# Changelog

## Unreleased

- Command router hardening: closed B1-B7 with production-safe builders, unified input gate,
  thread-safe bounded in-memory state, idempotency/replay protection, bounded navigation policy,
  story-of-user chained resolution, relative dates, PII-safe traces, and hardening gates.
- ZIP09: added the KMP core command router implementation with normalization, alias matching,
  pattern matching, entity extraction, validation, typed router errors, Flow routing, and tests.
- ZIP07: added production repository governance, ADR/RFC templates, release process, roadmap,
  production launch checklist, contributor/security docs, and automated repository verification.
- ZIP06: added observability and operations contracts for metrics, logs, SLOs, alerts, dashboards,
  and runbooks.
- ZIP05: added input security, Unicode/homoglyph defense, abuse prevention, and rate limiting.
