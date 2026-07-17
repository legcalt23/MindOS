package com.mindos.app.features.mindboost.core.session.cognitive

/**
 * One coarse session-level metric: either a [Measured] value or an honest [Unavailable] reason — NEVER a
 * fabricated 0 (invariant `null != 0`): "no data" is [Unavailable], not `Measured(0.0)`. The UNIT of a
 * [Measured] value is fixed PER FIELD in [CognitiveSessionMetrics] and validated there.
 */
sealed interface SessionMetric {
    /** A real measured value. Its unit/range is fixed by the field that holds it. */
    data class Measured(val value: Double) : SessionMetric {
        init {
            require(value.isFinite()) { "Measured value must be finite (was $value)." }
        }
    }

    /** No measurable value, with a typed reason. Distinct from a low value — never `Measured(0.0)`. */
    data class Unavailable(val reason: MetricUnavailableReason) : SessionMetric
}

/** Why a session metric could not be measured. Distinct from a low value. */
enum class MetricUnavailableReason {
    NOT_APPLICABLE,
    INSUFFICIENT_SAMPLE,
    QUALITY_REJECTED,
    NOT_TIME_CRITICAL,
    UNKNOWN,
}

/**
 * Coarse, UNIT-EXPLICIT session-level metrics. Reaction times use ONE convention: MILLISECONDS. [accuracy] is
 * a 0..1 ratio; [reactionTimeVariabilityCv] is a unitless coefficient of variation (>= 0). Each
 * [SessionMetric.Measured] is validated against its field's unit range, so an impossible value cannot be sealed.
 */
data class CognitiveSessionMetrics(
    val accuracy: SessionMetric,
    val reactionTimeMedianMs: SessionMetric,
    val reactionTimeMeanMs: SessionMetric,
    val reactionTimeMinMs: SessionMetric,
    val reactionTimeMaxMs: SessionMetric,
    val reactionTimeVariabilityCv: SessionMetric,
) {
    init {
        requireRatio(accuracy, "accuracy")
        requireNonNegative(reactionTimeMedianMs, "reactionTimeMedianMs")
        requireNonNegative(reactionTimeMeanMs, "reactionTimeMeanMs")
        requireNonNegative(reactionTimeMinMs, "reactionTimeMinMs")
        requireNonNegative(reactionTimeMaxMs, "reactionTimeMaxMs")
        requireNonNegative(reactionTimeVariabilityCv, "reactionTimeVariabilityCv")
    }

    private companion object {
        fun requireRatio(metric: SessionMetric, name: String) {
            if (metric is SessionMetric.Measured) {
                require(metric.value in 0.0..1.0) { "$name (Measured) must be in 0.0..1.0 (was ${metric.value})." }
            }
        }

        fun requireNonNegative(metric: SessionMetric, name: String) {
            if (metric is SessionMetric.Measured) {
                require(metric.value >= 0.0) { "$name (Measured) must be >= 0 (was ${metric.value})." }
            }
        }
    }
}
