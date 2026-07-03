package com.mindos.app.features.arena.metrics

/** Phase 09: Raw computed metric values plus the ids whose subsystem is out of offline scope. */
data class MetricInputs(
    val values: Map<MetricId, Double>,
    val notModeled: Set<MetricId> = emptySet(),
)

/** Phase 09: A computed acceptance metric: value (null if not modeled) + evaluated status. */
data class AcceptanceMetric(
    val id: MetricId,
    val name: String,
    val value: Double?,
    val status: MetricStatus,
)

/**
 * Phase 09: Computes all 24 acceptance metrics from [MetricInputs]. Every metric is always produced
 * (the dashboard renders all 24): a value present in [MetricInputs.values] is evaluated against its
 * threshold; anything in [MetricInputs.notModeled] (or missing) reports NOT_MODELED.
 */
class AcceptanceMetricCalculator {

    fun compute(inputs: MetricInputs): List<AcceptanceMetric> = MetricId.entries.map { id ->
        val definition = MetricCatalog.definitionFor(id)
        val value = inputs.values[id]
        when {
            id in inputs.notModeled || value == null ->
                AcceptanceMetric(id, definition.name, null, MetricStatus.NOT_MODELED)
            else ->
                AcceptanceMetric(id, definition.name, value, MetricEvaluator.evaluate(value, definition.threshold))
        }
    }

    /** True if every modeled metric is PASS (NOT_MODELED metrics are ignored for the offline gate). */
    fun allModeledPass(metrics: List<AcceptanceMetric>): Boolean =
        metrics.none { it.status == MetricStatus.WARNING || it.status == MetricStatus.FAILURE }
}
