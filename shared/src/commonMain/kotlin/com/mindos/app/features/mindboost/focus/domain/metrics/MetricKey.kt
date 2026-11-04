package com.mindos.app.features.mindboost.focus.domain.metrics

/**
 * Catalogue of every metric the system can compute. Honest by construction:
 * RT_MEDIAN (not mean) and RT_CV are the primary speed/stability metrics, and
 * every value reports its sample size and a confidence flag so the UI never
 * presents an under-powered estimate as a hard fact.
 * See 03_metrics_engine_plan.md.
 */
enum class MetricKey {
    ACCURACY,
    RT_MEDIAN,
    RT_CV,
    LAPSES,
    FALSE_ALARMS,
    OMISSIONS,
    ANTICIPATIONS,
    POST_ERROR_SLOWING,
    POST_ERROR_ACCURACY,
    BLOCK_STABILITY,
    FATIGUE_INDEX,
    INHIBITORY_CONTROL_INDEX,
    SUSTAINED_FOCUS_INDEX,
    NEAR_TRANSFER_INDEX,
    IES,
    RATE_CORRECT_SCORE,
    LISAS,
    SSRT,
    CONGRUENCY_EFFECT,
    SWITCH_COST,
    SET_SIZE_SLOPE,
    DUAL_TASK_COST,
    DISTRACTION_RESISTANCE_INDEX,
    PRESSURE_DELTA,
    VIGILANCE_DECREMENT_SLOPE,
}

/** The unit a [MetricValue] is expressed in (drives formatting in the UI). */
enum class MetricUnit {
    PROPORTION,
    MILLISECONDS,
    RATIO,
    COUNT,
    SLOPE,
    INDEX,
}

/**
 * A single computed metric plus the metadata that keeps reporting honest.
 *
 * @param value the numeric value (NaN-safe: callers must pass a finite number,
 *   using [confident] = false when the underlying sample is too small).
 * @param n the sample size that supports this value.
 * @param confident false when [n] is below the threshold needed to trust it;
 *   the UI must then show it as "insufficient data", never as a hard number.
 *   This is the IN-SESSION control question — may the adaptive engine act on this, may the UI caption it —
 *   and its floors (3..6) are sized for steering a session, not for claiming anything about the user.
 * @param measurementSufficient whether the sample clears the CENTRAL `MinSampleGate` floor, i.e. whether a
 *   claim about the user (the reliable-change index above all) may rest on it. `null` means the metric has
 *   no ratified floor and the question does not apply — never "no". See [MeasurementSufficiency] for why
 *   this is a second fact rather than a stricter [confident].
 */
data class MetricValue(
    val key: MetricKey,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: MetricUnit,
    val measurementSufficient: Boolean? = null,
    /**
     * Why this value is an absence, when it is one. `null` on a measured value. Not persisted: a re-read absence
     * carries `null` here (reason unknown), which is honest — the alternative would be inventing one.
     */
    val absence: MetricAbsence? = null,
) {
    init {
        require(absence == null || !confident) { "a metric with an absence reason cannot be confident: $key" }
    }

    companion object {
        /** A value the system could not compute with confidence (e.g. zero hits). */
        fun unavailable(
            key: MetricKey,
            unit: MetricUnit,
            n: Int = 0,
            reason: MetricAbsence = MetricAbsence.INSUFFICIENT_SAMPLE,
        ): MetricValue =
            MetricValue(key = key, value = 0.0, n = n, confident = false, unit = unit, absence = reason)
    }
}

/**
 * The typed reason a [MetricValue] is absent. Absence must be visible AND explained: "the sample was too small"
 * and "nobody answered" are different facts about different things — the first about our block design, the
 * second about what the user did — and a consumer that merges them cannot tell the user which one happened.
 */
enum class MetricAbsence {
    /** Fewer observations than the metric's floor (hits, clean RTs, cells, blocks…). */
    INSUFFICIENT_SAMPLE,

    /**
     * The trials were enough but the user made NO response in them (no hit, no false alarm, no anticipation).
     * Accuracy over pure withholding is arithmetic about the stimulus schedule, not a measurement of the user
     * (`MB-FOCUS-0091`).
     */
    NO_RESPONSES,

    /**
     * FOC-P0-03 · the lifecycle sensor OBSERVED the app leaving the foreground during the block, so its trials
     * cannot be attributed to the user (lifecycle half of `MB-FOCUS-0091`).
     */
    INTERRUPTED,

    /**
     * FOC-P0-05 · the session was not admitted by the quality gate and this value was still confident, so its
     * claim is withdrawn (sample size kept). The session-level reason travels on `SessionResult.admission`.
     */
    NOT_ADMITTED,
}

/** A keyed bundle of computed metrics for one block / session / cycle. */
typealias MetricSet = Map<MetricKey, MetricValue>

/** Convenience accessor: numeric value of a metric, or [default] when absent/unconfident. */
fun MetricSet.valueOf(key: MetricKey, default: Double = 0.0): Double {
    val metric = this[key] ?: return default
    return if (metric.confident) metric.value else default
}

/** Raw numeric value of a metric regardless of confidence, or [default] when absent. */
fun MetricSet.rawValueOf(key: MetricKey, default: Double = 0.0): Double =
    this[key]?.value ?: default

/**
 * The value of a metric when — and only when — the sample supports it. `null` means "not measured".
 *
 * Deliberately has **no default**. [valueOf]'s `default` silently turns "not measured" into a number, and a
 * caller that must *decide* something cannot tell the two apart. That is not hypothetical: an unavailable
 * ACCURACY became `0.0`, which is below every step-down threshold, so a session that measured nothing stepped a
 * real user's difficulty down as if they had failed. Any consumer that acts on a metric — adaptation above all —
 * uses this and handles `null` explicitly.
 */
fun MetricSet.confidentValueOf(key: MetricKey): Double? =
    this[key]?.takeIf { it.confident }?.value
