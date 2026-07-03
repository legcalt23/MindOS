package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReportBuilder

/**
 * Pure coordinator for an evaluation retest (06_ §4–§6): compares a reserved-probe retest against the
 * stored [BaselineSnapshot] and turns the result into an honest near-transfer report.
 *
 * RCI is **gated**: reliabilities come from [ProbeReliabilityEstimator], which stays empty until enough
 * probe administrations exist, so [EvaluationResult.hasReliableChange] cannot be true prematurely. Until
 * then the changes are raw deltas labelled "preliminary" — never "reliable improvement".
 */
class FocusEvaluationService(
    private val reliabilityEstimator: ProbeReliabilityEstimator = ProbeReliabilityEstimator(),
) {

    /**
     * Compares [retestMetrics] (a probe run in EVALUATION mode) with [baseline].
     * [priorAdministrations] is how many probe administrations of this construct preceded this retest;
     * it gates whether reliability is trusted.
     */
    fun evaluate(
        baseline: BaselineSnapshot,
        retestMetrics: MetricSet,
        priorAdministrations: Int,
        capturedAtMs: Long,
    ): EvaluationResult = EvaluationComparator.compare(
        userId = baseline.userId,
        baseline = baseline,
        retestMetrics = retestMetrics,
        reliabilities = reliabilityEstimator.estimate(priorAdministrations),
        capturedAtMs = capturedAtMs,
    )

    /** The honest near-transfer report for [result] (strong only when RCI is reliable). */
    fun transferReport(result: EvaluationResult): TransferReport = TransferReportBuilder.build(result)
}
