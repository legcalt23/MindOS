package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot

/** Pre/post change of a single metric, with its (possibly gated) RCI verdict. */
data class MetricChange(
    val metric: MetricKey,
    val pre: Double,
    val post: Double,
    val rci: RciResult,
) {
    val rawDelta: Double get() = post - pre
}

/**
 * The result of comparing a retest against the baseline. The strong, honest claim
 * — "a reliable change" — only holds when at least one metric has [RciResult.Value]
 * with `reliable == true`. Everything else is a raw delta shown with caution.
 */
data class EvaluationResult(
    val userId: String,
    val capturedAtMs: Long,
    val changes: List<MetricChange>,
) {
    val hasReliableChange: Boolean
        get() = changes.any { (it.rci as? RciResult.Value)?.reliable == true }
}

/**
 * Compares a retest's metrics against a [BaselineSnapshot]. RCI is computed only
 * for metrics that have BOTH a baseline value, a confident retest value, AND a
 * reliability estimate — otherwise the change is recorded with
 * [RciResult.NotEnoughData] (the gate, 06_ §5).
 */
object EvaluationComparator {

    fun compare(
        userId: String,
        baseline: BaselineSnapshot,
        retestMetrics: MetricSet,
        reliabilities: Map<MetricKey, ReliabilityEstimate>,
        capturedAtMs: Long,
    ): EvaluationResult {
        val changes = baseline.perMetric.mapNotNull { (metric, pre) ->
            val retest = retestMetrics[metric]?.takeIf { it.confident } ?: return@mapNotNull null
            val rci = reliabilities[metric]
                ?.let { ReliableChangeIndex.compute(pre = pre, post = retest.value, reliability = it) }
                ?: RciResult.NotEnoughData
            MetricChange(metric = metric, pre = pre, post = retest.value, rci = rci)
        }
        return EvaluationResult(userId = userId, capturedAtMs = capturedAtMs, changes = changes)
    }
}
