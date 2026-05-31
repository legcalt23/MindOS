# Security Policy

## Supported Scope

This package covers the command-router core, action understanding, action execution, security,
testing, and observability modules under `mindos/`.

## Reporting

Report suspected vulnerabilities through the private project security channel, not public issues.
Include affected module, reproduction steps, expected impact, and whether user input or production
data is involved.

## Requirements

- Private content and sensitive identifiers must not be logged as raw text.
- New list/read controls must consider authorization and volume limits separately.
- Security-sensitive behavior must include negative tests.
- All storage/backend integrations must enforce the relevant app and user authorization controls in
the application layer.

## Review Triggers

- Input parsing, normalization, Unicode handling, matching, or routing changes.
- New action types that can trigger side effects.
- New telemetry, audit, metrics, or structured logging fields.
- Abuse prevention, rate limiting, authentication, authorization, or storage policy changes.
