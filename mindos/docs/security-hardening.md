# ZIP05 Security Hardening

ZIP05 hardens the command pipeline before production use. The goal is to keep command integrity,
action integrity and navigation integrity deterministic, auditable and resistant to malformed or
abusive input.

## Controls

### Input validation before matching

`mind-security` exposes `InputSecurityInspector`, used by `DefaultActionMatcher` before
normalization and matching.

It rejects:

- oversized inputs (`InputSecurityPolicy.maxInputLength`)
- unsafe control/format characters, including zero-width characters
- malformed Unicode surrogate pairs

It detects and reports:

- mixed Latin/Cyrillic/Greek scripts
- common Cyrillic/Greek homoglyph characters that can spoof Latin names

Detection-only findings are emitted as telemetry and do not change deterministic matching. Reject
findings stop the pipeline before routing.

### Abuse prevention and rate limiting

`AbuseGuard` is the command-abuse boundary. The default no-op keeps pure tests simple; production
can inject `InMemoryAbuseGuard` or a durable implementation.

Supported scopes:

- user
- device
- session

Supported actions:

- throttle
- challenge
- temporary block

`DefaultActionMatcher` calls `beforeCommand()` before normalization and records failures for empty,
invalid, ambiguous and no-match requests. A successful understood intent clears the repeated-failure
history for that subject.

### Telemetry

ZIP05 adds PII-free telemetry events:

- `suspicious_input`
- `input_rejected`
- `abuse_guard_denied`

Events carry codes, actions, scopes and policy names only. They do not carry raw input.

### Audit and forensics

`AuditEvent` now supports:

- `INPUT_REJECTED`
- `RATE_LIMITED`

The matcher records immutable audit events for security stops with:

- `requestId`
- action type (`understanding`)
- timestamp from an injected `SecurityClock`
- outcome
- error code

Execution audit remains in `DefaultActionExecutor`.

## Pipeline position

```
raw input
  -> InputSecurityInspector
  -> AbuseGuard
  -> NormalizationPipeline
  -> deterministic matching
  -> intent + entities
  -> action mapper
  -> validator
  -> resolver
  -> NavigationCoordinator
```

The parser/matcher still never executes and never navigates directly.

## Testing

Coverage added:

- control/format character rejection
- malformed Unicode rejection
- Cyrillic homoglyph detection (`m\u0430rta`)
- session flood throttling
- repeated-failure challenge behavior
- matcher telemetry/audit on input rejection
- matcher audit/telemetry on abuse guard denial

The reliability fuzz model was updated so zero-width insertion is no longer treated as
semantics-preserving.
