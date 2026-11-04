package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.SampleVerdict
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId

/**
 * The sample a MEASUREMENT claim rests on: the smallest cell that feeds it, and whether every cell the
 * comparison needs actually exists.
 *
 * A difference score is limited by its weaker cell, never by the total: 40 congruent + 2 incongruent trials
 * is a sample of 42 and still cannot support a congruency effect. Passing the sum here is the defect this
 * type exists to make impossible to write by accident.
 */
data class MeasurementSample(val nEffective: Int, val cellsPresent: Boolean)

/**
 * Whether a metric's sample supports a **measurement claim**, per the central [MinSampleGate] table.
 *
 * This is deliberately NOT [MetricValue.confident]. The two answer different questions:
 *
 * - `confident` — may the in-session control loop act on this? (adaptive difficulty, recovery, the UI's
 *   caption). Its floors live in `DefaultMetricsEngine` and are 3..6, which is the right order of magnitude
 *   for steering a session and far too small for anything else.
 * - `measurementSufficient` — may a claim ABOUT THE USER rest on this? Above all the reliable-change index,
 *   which is the one place the product says a person's capacity moved.
 *
 * One boolean was answering both. Raising it to the central floors would have silenced the control loop;
 * leaving it alone let a 3-trial-per-cell difference score support a capacity claim. Two questions, two
 * named facts.
 *
 * Tri-state on purpose. `null` means "this metric has no ratified floor, so the question does not apply" —
 * it is never reused as "no", because a sentinel that carries two meanings is exactly how a gate ends up
 * with a ceiling below its own threshold (`MB-CATALOG-0009`).
 */
object MeasurementSufficiency {

    /**
     * The metrics the central table governs, and the id each maps onto. Nine of [MetricKey]'s entries are
     * absent and that is the declared answer, not an oversight: no floor has been ratified for a vigilance
     * slope, a set-size slope, a fatigue index or a raw error tally, so none of them may carry a claim of
     * reliable change. Fail-closed, with an owner.
     */
    private val central: Map<MetricKey, ScoreMetricId> = mapOf(
        MetricKey.ACCURACY to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.RT_MEDIAN to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.RT_CV to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.LAPSES to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.IES to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.RATE_CORRECT_SCORE to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.LISAS to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.BLOCK_STABILITY to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.SUSTAINED_FOCUS_INDEX to ScoreMetricId.ACCURACY_RT_BASE,
        MetricKey.CONGRUENCY_EFFECT to ScoreMetricId.CONGRUENCY_EFFECT,
        MetricKey.SWITCH_COST to ScoreMetricId.SWITCH_COST,
        MetricKey.DUAL_TASK_COST to ScoreMetricId.DUAL_TASK_COST,
        MetricKey.PRESSURE_DELTA to ScoreMetricId.PRESSURE_DELTA,
        MetricKey.POST_ERROR_SLOWING to ScoreMetricId.POST_ERROR,
        MetricKey.POST_ERROR_ACCURACY to ScoreMetricId.POST_ERROR,
        MetricKey.SSRT to ScoreMetricId.SSRT,
    )

    /** The central id governing [key], or `null` when no floor has been ratified for it. */
    fun centralMetricFor(key: MetricKey): ScoreMetricId? = central[key]

    /**
     * `true`/`false` = the central gate's verdict on this sample. `null` = [key] has no ratified floor, so
     * the question does not apply and no measurement claim may rest on it.
     */
    fun evaluate(key: MetricKey, nEffective: Int, cellsPresent: Boolean): Boolean? {
        val metric = centralMetricFor(key) ?: return null
        return MinSampleGate.evaluate(metric, nEffective, cellsPresent) is SampleVerdict.Sufficient
    }

    /** Convenience for the engine: evaluate against an already-computed [MeasurementSample]. */
    fun evaluate(key: MetricKey, sample: MeasurementSample): Boolean? =
        evaluate(key, sample.nEffective, sample.cellsPresent)
}
