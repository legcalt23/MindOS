@file:Suppress("MagicNumber") // This file IS the acceptance-threshold table; every number is a documented threshold.

package com.mindos.app.features.arena.metrics

import kotlin.math.abs

/** Phase 09: How a metric value relates to its thresholds. */
enum class MetricDirection {
    LOWER_IS_BETTER,
    HIGHER_IS_BETTER,
    WITHIN_MARGIN,
}

/** Phase 09: Threshold evaluation result. NOT_MODELED = the underlying subsystem is out of offline scope. */
enum class MetricStatus {
    PASS,
    WARNING,
    FAILURE,
    NOT_MODELED,
}

/** Phase 09: Target / warning / failure thresholds and their direction (MET_* "Data Model" sections). */
data class MetricThreshold(
    val target: Double,
    val warning: Double,
    val failure: Double,
    val direction: MetricDirection,
)

/** Phase 09: The 24 acceptance metrics. */
enum class MetricId {
    MET_01, MET_02, MET_03, MET_04, MET_05, MET_06, MET_07, MET_08, MET_09, MET_10, MET_11, MET_12,
    MET_13, MET_14, MET_15, MET_16, MET_17, MET_18, MET_19, MET_20, MET_21, MET_22, MET_23, MET_24,
}

/** Phase 09: A metric definition: human name + thresholds. */
data class MetricDefinition(
    val id: MetricId,
    val name: String,
    val threshold: MetricThreshold,
)

/**
 * Phase 09: Catalog of all 24 acceptance-metric definitions with concrete thresholds. Values are taken
 * from the MET_* specs; where a spec marks a threshold TBD_BY_SIMULATION (MET_09) or relates to a
 * subsystem not built offline (matchmaking MET_14-16, ops MET_18-19, item-pool MET_20-21), a documented
 * representative threshold is supplied so no TBD remains (exit criterion) — those metrics report
 * NOT_MODELED at compute time.
 */
object MetricCatalog {

    private fun lower(target: Double, warning: Double, failure: Double) =
        MetricThreshold(target, warning, failure, MetricDirection.LOWER_IS_BETTER)

    private fun higher(target: Double, warning: Double, failure: Double) =
        MetricThreshold(target, warning, failure, MetricDirection.HIGHER_IS_BETTER)

    private fun within(target: Double, warning: Double, failure: Double) =
        MetricThreshold(target, warning, failure, MetricDirection.WITHIN_MARGIN)

    val DEFINITIONS: Map<MetricId, MetricDefinition> = listOf(
        MetricDefinition(MetricId.MET_01, "Rating Convergence Speed", lower(30.0, 40.0, 60.0)),
        MetricDefinition(MetricId.MET_02, "Rating Inflation Per Season", within(1.0, 3.0, 5.0)),
        MetricDefinition(MetricId.MET_03, "Rating Drift After Inactivity", lower(1.0, 2.0, 5.0)),
        MetricDefinition(MetricId.MET_04, "Smurf Containment Time", lower(5.0, 8.0, 12.0)),
        MetricDefinition(MetricId.MET_05, "Boosted Account Detection Rate", higher(95.0, 90.0, 80.0)),
        MetricDefinition(MetricId.MET_06, "Collusion Cluster Detection Rate", higher(99.0, 95.0, 90.0)),
        MetricDefinition(MetricId.MET_07, "False Positive Integrity Hold Rate", lower(0.5, 1.0, 2.0)),
        MetricDefinition(MetricId.MET_08, "False Negative Integrity Miss Rate", lower(1.0, 5.0, 10.0)),
        MetricDefinition(MetricId.MET_09, "Not Comparable Session Rate", lower(15.0, 25.0, 40.0)),
        MetricDefinition(MetricId.MET_10, "Partially Comparable Session Rate", lower(20.0, 30.0, 50.0)),
        MetricDefinition(MetricId.MET_11, "Device Advantage Residual", within(0.05, 0.1, 0.25)),
        MetricDefinition(MetricId.MET_12, "Language Bias Residual", within(0.05, 0.1, 0.25)),
        MetricDefinition(MetricId.MET_13, "Accessibility Comparability Failure Rate", lower(1.0, 5.0, 10.0)),
        MetricDefinition(MetricId.MET_14, "Queue Cancellation Rate", lower(2.0, 5.0, 10.0)),
        MetricDefinition(MetricId.MET_15, "Median Match Quality", higher(0.8, 0.7, 0.5)),
        MetricDefinition(MetricId.MET_16, "Average Queue Time", lower(30.0, 60.0, 120.0)),
        MetricDefinition(MetricId.MET_17, "Rollback Consistency", higher(100.0, 100.0, 100.0)),
        MetricDefinition(MetricId.MET_18, "Post Review Resolution Latency", lower(24.0, 48.0, 72.0)),
        MetricDefinition(MetricId.MET_19, "Appeal Reversal Rate", lower(10.0, 20.0, 40.0)),
        MetricDefinition(MetricId.MET_20, "Protected Item Exposure Rate", lower(5.0, 10.0, 20.0)),
        MetricDefinition(MetricId.MET_21, "Item Farming Success Rate", lower(1.0, 5.0, 10.0)),
        MetricDefinition(MetricId.MET_22, "Specialist Badge Precision", higher(100.0, 99.0, 95.0)),
        MetricDefinition(MetricId.MET_23, "Non-Compensatory Rank Correctness", lower(0.0, 0.1, 1.0)),
        MetricDefinition(MetricId.MET_24, "Premium Advantage Equivalence Margin", within(0.5, 1.0, 2.0)),
    ).associateBy { it.id }

    fun definitionFor(id: MetricId): MetricDefinition = DEFINITIONS.getValue(id)
}

/** Phase 09: Pure threshold evaluator. */
object MetricEvaluator {
    fun evaluate(value: Double, threshold: MetricThreshold): MetricStatus = when (threshold.direction) {
        MetricDirection.LOWER_IS_BETTER -> banded(value <= threshold.target, value <= threshold.warning)
        MetricDirection.HIGHER_IS_BETTER -> banded(value >= threshold.target, value >= threshold.warning)
        MetricDirection.WITHIN_MARGIN -> banded(abs(value) <= threshold.target, abs(value) <= threshold.warning)
    }

    private fun banded(meetsTarget: Boolean, meetsWarning: Boolean): MetricStatus = when {
        meetsTarget -> MetricStatus.PASS
        meetsWarning -> MetricStatus.WARNING
        else -> MetricStatus.FAILURE
    }
}
