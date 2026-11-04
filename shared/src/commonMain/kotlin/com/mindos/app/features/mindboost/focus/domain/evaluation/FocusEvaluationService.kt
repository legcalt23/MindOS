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
     * it gates whether reliability is trusted. [repeatsEarlierForm] is whether THIS retest re-served a
     * reserved form the user had already been given (`null` = not known for this row): a repeated form is
     * not an independent administration, so no reliability is handed to the RCI for it (MB-FOCUS-0122).
     * MANDATORY, no default — a default would let a caller claim fresh material on exactly the runs where
     * the supply wrapped.
     */
    fun evaluate(
        baseline: BaselineSnapshot,
        retestMetrics: MetricSet,
        priorAdministrations: Int,
        capturedAtMs: Long,
        repeatsEarlierForm: Boolean?,
    ): EvaluationResult = EvaluationComparator.compare(
        userId = baseline.userId,
        baseline = baseline,
        retestMetrics = retestMetrics,
        reliabilities = reliabilityEstimator.estimate(priorAdministrations, repeatsEarlierForm),
        capturedAtMs = capturedAtMs,
    )

    /** The honest near-transfer report for [result] (strong only when RCI is reliable). */
    fun transferReport(result: EvaluationResult): TransferReport = TransferReportBuilder.build(result)
}
