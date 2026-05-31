package com.mindos.action.observability

import kotlinx.serialization.Serializable

@Serializable
enum class DashboardName {
    PRODUCT,
    ENGINEERING,
    OPERATIONS,
}

@Serializable
enum class DashboardView {
    COMMAND_HEALTH,
    LATENCY,
    FAILURES,
    ADOPTION,
}

@Serializable
data class DashboardSpec(
    val name: DashboardName,
    val views: List<DashboardView>,
    val metrics: List<OperationMetric>,
)

object DashboardCatalog {
    val Product = DashboardSpec(
        DashboardName.PRODUCT,
        views = listOf(DashboardView.ADOPTION, DashboardView.COMMAND_HEALTH, DashboardView.FAILURES),
        metrics = listOf(OperationMetric.MATCH_RATE, OperationMetric.UNKNOWN_COMMAND_RATE, OperationMetric.ACTION_EXECUTION_RATE),
    )

    val Engineering = DashboardSpec(
        DashboardName.ENGINEERING,
        views = listOf(DashboardView.COMMAND_HEALTH, DashboardView.LATENCY, DashboardView.FAILURES),
        metrics = listOf(
            OperationMetric.MATCH_RATE,
            OperationMetric.INTENT_RESOLUTION_RATE,
            OperationMetric.ENTITY_RESOLUTION_RATE,
            OperationMetric.P95_LATENCY,
            OperationMetric.P99_LATENCY,
            OperationMetric.RESOLVER_DEGRADATION_RATE,
        ),
    )

    val Operations = DashboardSpec(
        DashboardName.OPERATIONS,
        views = listOf(DashboardView.COMMAND_HEALTH, DashboardView.LATENCY, DashboardView.FAILURES),
        metrics = listOf(
            OperationMetric.ACTION_EXECUTION_RATE,
            OperationMetric.VALIDATION_FAILURE_RATE,
            OperationMetric.P50_LATENCY,
            OperationMetric.P95_LATENCY,
            OperationMetric.P99_LATENCY,
        ),
    )

    val All = listOf(Product, Engineering, Operations)
}

@Serializable
enum class IncidentSeverity {
    SEV1,
    SEV2,
    SEV3,
}

@Serializable
enum class IncidentPhase {
    DETECT,
    TRIAGE,
    MITIGATE,
    RESOLVE,
    POSTMORTEM,
}

@Serializable
data class RunbookChecklist(
    val name: String,
    val steps: List<String>,
)

object ProductionRunbook {
    val Startup = RunbookChecklist(
        "startup",
        listOf(
            "Verify metric sinks, structured logs, audit sink, and telemetry sink are wired.",
            "Verify command registry compilation succeeds before serving traffic.",
            "Verify dashboards receive samples with appVersion, platform, and commandType tags.",
        ),
    )

    val Deployment = RunbookChecklist(
        "deployment",
        listOf(
            "Deploy with a fresh appVersion tag.",
            "Watch MatchRate, ActionExecutionRate, P95Latency, and UnknownCommandRate for the first window.",
            "Stop rollout if critical alerts fire.",
        ),
    )

    val Rollback = RunbookChecklist(
        "rollback",
        listOf(
            "Freeze rollout.",
            "Return traffic to the previous appVersion.",
            "Keep requestId and sessionId correlation available for postmortem.",
        ),
    )

    val Monitoring = RunbookChecklist(
        "monitoring",
        listOf(
            "Check command health, latency, failures, and adoption dashboards.",
            "Compare p95 latency with the 20ms SLO.",
            "Inspect warning alerts for unknown command growth or resolver degradation.",
        ),
    )

    val Recovery = RunbookChecklist(
        "recovery",
        listOf(
            "Confirm router availability.",
            "Disable risky command registries if failures are isolated to a command type.",
            "Replay audit/requestId trails for impacted sessions.",
        ),
    )

    val IncidentResponse = RunbookChecklist(
        "incident_response",
        IncidentPhase.entries.map { it.name.lowercase() },
    )

    val All = listOf(Startup, Deployment, Rollback, Monitoring, Recovery, IncidentResponse)
}
