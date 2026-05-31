# MindOS Observability & Operations (ZIP06)

ZIP06 turns the command router into an operable production component. The implementation lives in
`mind-observability` and stays backend-agnostic: apps can forward samples/logs to OpenTelemetry,
Firebase, Sentry, BigQuery, or any other sink without coupling core modules to those SDKs.

## Metrics

`MetricCatalog` defines the ZIP06 core metrics:

- `MatchRate`
- `UnknownCommandRate`
- `IntentResolutionRate`
- `EntityResolutionRate`
- `ActionExecutionRate`
- `ValidationFailureRate`
- `P50Latency`
- `P95Latency`
- `P99Latency`

Every `MetricSample` requires `MetricTags(appVersion, platform, commandType)`. Additional
attributes are allowed only when their keys are safe and PII-free. Runtime latency is recorded as
`LatencySample`; `InMemoryMetricAggregator` turns samples into `OperationHealthSnapshot` values
with p50/p95/p99 and success/failure rates.

## Structured Logs

`StructuredLogEvent` enforces the logging standard:

- `requestId`
- `sessionId`
- `timestampMillis`
- `action`
- `outcome`
- `latencyMs`

Sensitive keys such as raw input, normalized text, email, phone, token, password, or secret are
rejected at construction time. The matcher and executor emit operation logs after each request.

## Pipeline Wiring

`DefaultActionMatcher` now emits:

- match/unknown/intent/entity metrics
- latency samples
- structured logs with request/session correlation
- telemetry without raw unmatched text

`DefaultActionExecutor` now emits:

- action execution metrics
- validation failure metrics
- resolver degradation metrics
- latency samples
- structured logs with request/session correlation

Both default to no-op sinks, preserving the current dependency boundary.

## Dashboards

`DashboardCatalog` exposes three dashboard specs:

- Product: adoption, command health, failures
- Engineering: command health, latency, failures
- Operations: command health, latency, failures

## SLOs and Alerts

`SloCatalog` evaluates:

- availability >= 99.99%
- p95 latency < 20ms
- execution success > 99.5%

`AlertPolicy` produces:

- critical: router unavailable
- critical: execution failure spike
- critical: latency breach
- warning: unknown command growth
- warning: resolver degradation

## Runbooks

`ProductionRunbook` contains startup, deployment, rollback, monitoring, recovery, and incident
response checklists. The incident flow is `detect -> triage -> mitigate -> resolve -> postmortem`.
