package com.mindos.action.observability

import kotlinx.serialization.Serializable

@Serializable
enum class MetricUnit {
    COUNT,
    RATIO,
    MILLISECONDS,
}

@Serializable
enum class MetricAggregation {
    RATE,
    PERCENTILE,
    SAMPLE,
}

@Serializable
enum class OperationMetric(val wireName: String) {
    MATCH_RATE("MatchRate"),
    UNKNOWN_COMMAND_RATE("UnknownCommandRate"),
    INTENT_RESOLUTION_RATE("IntentResolutionRate"),
    ENTITY_RESOLUTION_RATE("EntityResolutionRate"),
    ACTION_EXECUTION_RATE("ActionExecutionRate"),
    VALIDATION_FAILURE_RATE("ValidationFailureRate"),
    P50_LATENCY("P50Latency"),
    P95_LATENCY("P95Latency"),
    P99_LATENCY("P99Latency"),
    LATENCY_SAMPLE("LatencySample"),
    RESOLVER_DEGRADATION_RATE("ResolverDegradationRate"),
}

@Serializable
data class MetricDefinition(
    val metric: OperationMetric,
    val unit: MetricUnit,
    val aggregation: MetricAggregation,
    val requiredTags: Set<String> = RequiredMetricTags,
)

object MetricCatalog {
    val CoreMetrics: List<MetricDefinition> = listOf(
        MetricDefinition(OperationMetric.MATCH_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.UNKNOWN_COMMAND_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.INTENT_RESOLUTION_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.ENTITY_RESOLUTION_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.ACTION_EXECUTION_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.VALIDATION_FAILURE_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
        MetricDefinition(OperationMetric.P50_LATENCY, MetricUnit.MILLISECONDS, MetricAggregation.PERCENTILE),
        MetricDefinition(OperationMetric.P95_LATENCY, MetricUnit.MILLISECONDS, MetricAggregation.PERCENTILE),
        MetricDefinition(OperationMetric.P99_LATENCY, MetricUnit.MILLISECONDS, MetricAggregation.PERCENTILE),
    )

    val RuntimeMetrics: List<MetricDefinition> = CoreMetrics + listOf(
        MetricDefinition(OperationMetric.LATENCY_SAMPLE, MetricUnit.MILLISECONDS, MetricAggregation.SAMPLE),
        MetricDefinition(OperationMetric.RESOLVER_DEGRADATION_RATE, MetricUnit.RATIO, MetricAggregation.RATE),
    )

    private val definitionsByMetric = RuntimeMetrics.associateBy { it.metric }

    fun definitionFor(metric: OperationMetric): MetricDefinition =
        definitionsByMetric.getValue(metric)
}

@Serializable
data class MetricTags(
    val appVersion: String,
    val platform: String,
    val commandType: String,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(appVersion.isNotBlank()) { "MetricTags.appVersion must not be blank." }
        require(platform.isNotBlank()) { "MetricTags.platform must not be blank." }
        require(commandType.isNotBlank()) { "MetricTags.commandType must not be blank." }
        attributes.forEach { (key, value) ->
            require(key.isSafeTelemetryKey()) { "Metric tag '$key' may contain sensitive data." }
            require(value.length <= MaxTagValueLength) { "Metric tag '$key' is too long." }
        }
    }

    fun withCommandType(value: String): MetricTags =
        copy(commandType = value.ifBlank { UnknownCommandType })

    fun withOutcome(outcome: String): MetricTags =
        withAttribute("outcome", outcome)

    fun withAttribute(key: String, value: String): MetricTags =
        copy(attributes = attributes + (key to value))

    companion object {
        const val UnknownCommandType = "unknown"
        val Default = MetricTags(appVersion = "unknown", platform = "unknown", commandType = UnknownCommandType)
    }
}

@Serializable
data class MetricSample(
    val metric: OperationMetric,
    val value: Double,
    val tags: MetricTags,
    val timestampMillis: Long,
    val unit: MetricUnit = MetricCatalog.definitionFor(metric).unit,
) {
    init {
        require(value >= 0.0) { "MetricSample.value cannot be negative." }
    }
}

fun interface MetricSink {
    fun record(sample: MetricSample)
}

object NoOpMetricSink : MetricSink {
    override fun record(sample: MetricSample) = Unit
}

class RecordingMetricSink : MetricSink {
    private val _samples = ArrayList<MetricSample>()
    val samples: List<MetricSample> get() = _samples.toList()

    override fun record(sample: MetricSample) {
        _samples += sample
    }

    fun clear() = _samples.clear()
}

internal val RequiredMetricTags = setOf("appVersion", "platform", "commandType")

internal const val MaxTagValueLength = 128

internal fun String.isSafeTelemetryKey(): Boolean {
    val normalized = lowercase().replace("-", "_")
    val sensitiveFragments = listOf(
        "input",
        "raw_text",
        "normalized_text",
        "email",
        "phone",
        "token",
        "password",
        "secret",
    )
    return sensitiveFragments.none { normalized.contains(it) }
}
