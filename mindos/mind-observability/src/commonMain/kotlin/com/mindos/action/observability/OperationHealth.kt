package com.mindos.action.observability

import kotlinx.serialization.Serializable

@Serializable
data class OperationHealthSnapshot(
    val routerAvailable: Boolean = true,
    val matchAttempts: Long = 0,
    val matchSuccesses: Long = 0,
    val unknownCommands: Long = 0,
    val intentResolutionAttempts: Long = 0,
    val intentResolutionSuccesses: Long = 0,
    val entityResolutionAttempts: Long = 0,
    val entityResolutionSuccesses: Long = 0,
    val actionExecutionAttempts: Long = 0,
    val actionExecutionSuccesses: Long = 0,
    val validationFailures: Long = 0,
    val resolverFailures: Long = 0,
    val latencySamplesMs: List<Double> = emptyList(),
) {
    val availabilityPercent: Double get() = if (routerAvailable) 100.0 else 0.0
    val matchRate: Double? get() = ratio(matchSuccesses, matchAttempts)
    val unknownCommandRate: Double? get() = ratio(unknownCommands, matchAttempts)
    val intentResolutionRate: Double? get() = ratio(intentResolutionSuccesses, intentResolutionAttempts)
    val entityResolutionRate: Double? get() = ratio(entityResolutionSuccesses, entityResolutionAttempts)
    val actionExecutionRate: Double? get() = ratio(actionExecutionSuccesses, actionExecutionAttempts)
    val validationFailureRate: Double? get() = ratio(validationFailures, actionExecutionAttempts)
    val resolverFailureRate: Double? get() = ratio(resolverFailures, actionExecutionAttempts)

    val p50LatencyMs: Double? get() = percentile(50.0)
    val p95LatencyMs: Double? get() = percentile(95.0)
    val p99LatencyMs: Double? get() = percentile(99.0)

    private fun ratio(numerator: Long, denominator: Long): Double? =
        if (denominator == 0L) null else numerator.toDouble() / denominator.toDouble()

    private fun percentile(percentile: Double): Double? {
        if (latencySamplesMs.isEmpty()) return null
        val sorted = latencySamplesMs.sorted()
        val rank = ((percentile / 100.0) * (sorted.size - 1)).coerceIn(0.0, (sorted.size - 1).toDouble())
        val lower = rank.toInt()
        val upper = if (lower == sorted.lastIndex) lower else lower + 1
        val weight = rank - lower
        return sorted[lower] + ((sorted[upper] - sorted[lower]) * weight)
    }
}

@Serializable
data class SloTarget(
    val name: String,
    val description: String,
    val target: Double,
    val unit: MetricUnit,
)

@Serializable
data class SloEvaluation(
    val target: SloTarget,
    val actual: Double?,
    val met: Boolean,
)

object SloCatalog {
    val Availability = SloTarget("availability", "Router availability must be at least 99.99%.", 99.99, MetricUnit.RATIO)
    val P95Latency = SloTarget("p95_latency", "P95 command latency must stay below 20ms.", 20.0, MetricUnit.MILLISECONDS)
    val ExecutionSuccess = SloTarget("execution_success", "Action execution success must be above 99.5%.", 0.995, MetricUnit.RATIO)

    val DefaultTargets = listOf(Availability, P95Latency, ExecutionSuccess)

    fun evaluate(snapshot: OperationHealthSnapshot): List<SloEvaluation> =
        listOf(
            SloEvaluation(Availability, snapshot.availabilityPercent, snapshot.availabilityPercent >= Availability.target),
            SloEvaluation(P95Latency, snapshot.p95LatencyMs, snapshot.p95LatencyMs?.let { it < P95Latency.target } ?: true),
            SloEvaluation(
                ExecutionSuccess,
                snapshot.actionExecutionRate,
                snapshot.actionExecutionRate?.let { it > ExecutionSuccess.target } ?: true,
            ),
        )
}

@Serializable
enum class AlertSeverity {
    CRITICAL,
    WARNING,
}

@Serializable
enum class AlertCode {
    ROUTER_UNAVAILABLE,
    EXECUTION_FAILURE_SPIKE,
    LATENCY_BREACH,
    UNKNOWN_COMMAND_GROWTH,
    RESOLVER_DEGRADATION,
}

@Serializable
data class Alert(
    val code: AlertCode,
    val severity: AlertSeverity,
    val message: String,
    val runbook: String,
)

object AlertPolicy {
    const val UnknownCommandWarningThreshold = 0.10
    const val ResolverFailureWarningThreshold = 0.02

    fun evaluate(snapshot: OperationHealthSnapshot): List<Alert> {
        val alerts = ArrayList<Alert>()
        if (!snapshot.routerAvailable) {
            alerts += Alert(
                AlertCode.ROUTER_UNAVAILABLE,
                AlertSeverity.CRITICAL,
                "Command router is unavailable.",
                ProductionRunbook.Recovery.name,
            )
        }
        if (snapshot.actionExecutionRate?.let { it <= SloCatalog.ExecutionSuccess.target } == true) {
            alerts += Alert(
                AlertCode.EXECUTION_FAILURE_SPIKE,
                AlertSeverity.CRITICAL,
                "Action execution success breached the 99.5% SLO.",
                ProductionRunbook.IncidentResponse.name,
            )
        }
        if (snapshot.p95LatencyMs?.let { it >= SloCatalog.P95Latency.target } == true) {
            alerts += Alert(
                AlertCode.LATENCY_BREACH,
                AlertSeverity.CRITICAL,
                "P95 command latency breached 20ms.",
                ProductionRunbook.Monitoring.name,
            )
        }
        if (snapshot.unknownCommandRate?.let { it >= UnknownCommandWarningThreshold } == true) {
            alerts += Alert(
                AlertCode.UNKNOWN_COMMAND_GROWTH,
                AlertSeverity.WARNING,
                "Unknown command rate is growing.",
                ProductionRunbook.Monitoring.name,
            )
        }
        if (snapshot.resolverFailureRate?.let { it >= ResolverFailureWarningThreshold } == true) {
            alerts += Alert(
                AlertCode.RESOLVER_DEGRADATION,
                AlertSeverity.WARNING,
                "Reference resolver degradation detected.",
                ProductionRunbook.IncidentResponse.name,
            )
        }
        return alerts
    }
}

class InMemoryMetricAggregator : MetricSink {
    private val _samples = ArrayList<MetricSample>()
    val samples: List<MetricSample> get() = _samples.toList()

    override fun record(sample: MetricSample) {
        _samples += sample
    }

    fun snapshot(routerAvailable: Boolean = true): OperationHealthSnapshot {
        var matchAttempts = 0L
        var matchSuccesses = 0L
        var unknownCommands = 0L
        var intentAttempts = 0L
        var intentSuccesses = 0L
        var entityAttempts = 0L
        var entitySuccesses = 0L
        var executionAttempts = 0L
        var executionSuccesses = 0L
        var validationFailures = 0L
        var resolverFailures = 0L
        val latencies = ArrayList<Double>()

        for (sample in _samples) {
            val count = sample.value.toLong().coerceAtLeast(1L)
            val outcome = sample.tags.attributes["outcome"]
            when (sample.metric) {
                OperationMetric.MATCH_RATE -> {
                    matchAttempts += count
                    if (outcome == OutcomeSuccess) matchSuccesses += count
                }
                OperationMetric.UNKNOWN_COMMAND_RATE -> unknownCommands += count
                OperationMetric.INTENT_RESOLUTION_RATE -> {
                    intentAttempts += count
                    if (outcome == OutcomeSuccess) intentSuccesses += count
                }
                OperationMetric.ENTITY_RESOLUTION_RATE -> {
                    entityAttempts += count
                    if (outcome == OutcomeSuccess) entitySuccesses += count
                }
                OperationMetric.ACTION_EXECUTION_RATE -> {
                    executionAttempts += count
                    if (outcome == OutcomeSuccess) executionSuccesses += count
                }
                OperationMetric.VALIDATION_FAILURE_RATE -> validationFailures += count
                OperationMetric.RESOLVER_DEGRADATION_RATE -> resolverFailures += count
                OperationMetric.LATENCY_SAMPLE -> latencies += sample.value
                OperationMetric.P50_LATENCY,
                OperationMetric.P95_LATENCY,
                OperationMetric.P99_LATENCY,
                -> Unit
            }
        }

        return OperationHealthSnapshot(
            routerAvailable = routerAvailable,
            matchAttempts = matchAttempts,
            matchSuccesses = matchSuccesses,
            unknownCommands = unknownCommands,
            intentResolutionAttempts = intentAttempts,
            intentResolutionSuccesses = intentSuccesses,
            entityResolutionAttempts = entityAttempts,
            entityResolutionSuccesses = entitySuccesses,
            actionExecutionAttempts = executionAttempts,
            actionExecutionSuccesses = executionSuccesses,
            validationFailures = validationFailures,
            resolverFailures = resolverFailures,
            latencySamplesMs = latencies,
        )
    }

    fun clear() = _samples.clear()
}

const val OutcomeSuccess = "success"
const val OutcomeFailure = "failure"
const val OutcomeRejected = "rejected"
