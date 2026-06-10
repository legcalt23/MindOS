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
 */
data class MetricValue(
    val key: MetricKey,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: MetricUnit,
) {
    companion object {
        /** A value the system could not compute with confidence (e.g. zero hits). */
        fun unavailable(key: MetricKey, unit: MetricUnit, n: Int = 0): MetricValue =
            MetricValue(key = key, value = 0.0, n = n, confident = false, unit = unit)
    }
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
