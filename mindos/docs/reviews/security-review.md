# Security Review (ZIP13)

*Principal Security Engineer lens: input handling · unicode · abuse prevention · auditability ·
validation.* Grounded in `mind-security`, `mind-action-validation`, `mind-action-execution` and
`mind-action-matching`. Each finding has actionable remediation.

## Summary

Security posture is strong for a deterministic command engine. `InputSecurityInspector` screens input
before matching; `AbuseGuard` provides rate-limit/failure controls; `ActionValidator` blocks invalid
actions before any side-effect; the audit trail is immutable and PII-free; entity validators use
linear regexes (no ReDoS). The main risks are **configuration-dependent protections** (controls that
must be wired in production) and **deep-link inputs that skip input inspection**.

## Strengths (verified in code)

- `InputSecurityInspector` (`mind-security`) detects and can REJECT: `INPUT_TOO_LONG`,
  `INVALID_CONTROL_CHARACTER`, `MALFORMED_UNICODE`, `MIXED_SCRIPT`, `HOMOGLYPH_CONFUSABLE`. Confirmed
  by regression REG-003 (zero-width injection → rejected, never a spurious match).
- `AbuseGuard` models rate-limit + repeated-failure policies across `USER/DEVICE/SESSION` scopes with
  `THROTTLE/CHALLENGE/BLOCK` actions.
- `ActionValidator` enforces required fields, format/length bounds, **authorization** (`Authorizer`)
  and resolver readiness; invalid actions never reach the resolver or coordinator.
- Audit: immutable `AuditEvent` (requestId, actionType, timestamp, outcome, errorCode), append-only,
  PII-free by contract.
- Entity shapes are bounded single character-classes — no catastrophic backtracking; `maxInputLength`
  (default 512) caps DoS surface.

## Findings

### S1 — Protections are configuration-dependent (Medium)
`Authorizer` defaults to `AllowAllAuthorizer`; `AuditSink`/`MetricSink`/`AbuseGuard` default to
no-op/permissive. A deployment that forgets to wire them runs without authz/audit/rate-limiting.
- **Fix:** in production wiring, require a real `Authorizer`, `AuditSink` with a real
  `ExecutionClock`, and an `AbuseGuard`. Add a startup assertion / "secure mode" builder that fails
  if security-critical sinks are left at their no-op defaults.

### S2 — Deep links bypass `InputSecurityInspector` (Medium)
`DeepLinkBridge` (`mind-decompose`) parses a link into a `MindAction` whose ref flows into
validation→resolution. Entity-shape screening happens in the *matcher*, not on bridge-derived refs,
so a crafted deep link carries its ref straight to the resolver (which fails it as `NotFound`).
- **Impact:** safe-by-failure, but suspicious-unicode refs are not flagged/rejected as spoofing.
- **Fix:** run `InputSecurityInspector.inspect()` on the raw deep-link (and/or validate the ref shape
  in `ActionValidator`) before executing; reject on `HOMOGLYPH_CONFUSABLE`/`MALFORMED_UNICODE`.

### S3 — Audit clock defaults to zero (Low)
`ExecutionClock` defaults to `ExecutionClocks.Zero`; audit timestamps are `0` unless a real clock is
injected, undermining forensic value.
- **Fix:** require a real clock in production wiring (no default), or emit a warning when `Zero` is in
  use outside tests.

### S4 — Abuse/cache state is per-instance, in-memory (Low)
`AbuseGuard` counters and `CachingReferenceResolver` entries are per-process. In a multi-instance
deployment, rate limits and resolution caches are not shared.
- **Fix:** document the single-instance assumption; provide a distributed `AbuseGuard`/cache adapter
  (shared store) for horizontal scaling.

### S5 — Homoglyph table coverage unproven (Low)
Confusable detection exists; the breadth of the confusables table (Cyrillic/Greek/Latin lookalikes)
is not asserted.
- **Fix:** add tests for known spoof pairs (e.g. Latin `a` vs Cyrillic `а`, `o` vs `о`) to pin
  coverage and prevent regressions.

## Actionable fix priority

1. S1 — secure-mode wiring assertion (prevents silent disablement).
2. S2 — apply input inspection to deep-link-derived inputs.
3. S3 — require a real audit clock in production.
4. S5 — homoglyph coverage tests.
5. S4 — distributed abuse/cache adapter (only when scaling horizontally).
