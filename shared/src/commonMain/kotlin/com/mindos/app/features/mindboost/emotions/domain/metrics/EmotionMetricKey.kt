package com.mindos.app.features.mindboost.emotions.domain.metrics

/**
 * Catalogue of every metric the emotions module can compute. Emotions OWNS this enum — it must NOT
 * import focus's `MetricKey` (a closed enum with no emotion keys; a compile test pins the absence of
 * that import). Honest by construction: every [EmotionMetricValue] reports its sample size and a
 * confidence flag so the UI never presents an under-powered estimate as a hard fact.
 */
enum class EmotionMetricKey {
    POST_ERROR_RECOVERY,
    COMMISSION_RATE,
    PAUSE_BEFORE_ACTION_RATE,
    CATEGORY_ACCURACY,
    GRANULARITY_INDEX,
    EMOTION_THOUGHT_SEPARATION,
    AFFECTIVE_INTERFERENCE_COST,
    CALIBRATION_ERROR,
    FRUSTRATION_TOLERANCE_INDEX,
    AFFECTIVE_DECISION_QUALITY,
    SAFE_PERSISTENCE_INDEX,
    REGULATION_STABILITY,
    ACCURACY,
    // MC8u-B §1A: the GO-trial hit rate for a go/no-go block. Its floor gates COMMISSION_RATE confidence so a
    // "never respond" strategy (commission = 0) can never look like perfect inhibition, and blocks difficulty step-up.
    HIT_RATE,
    RT_MEDIAN,
    RT_CV,
    FALSE_ALARMS,
    OMISSIONS,
    ANTICIPATIONS,
    POST_ERROR_ACCURACY,
    POST_ERROR_SLOWING,
    IMPULSIVITY,
    LAPSES,
    ABANDONMENT,
}

/** The unit an [EmotionMetricValue] is expressed in (drives formatting in the UI). */
enum class EmotionMetricUnit { PROPORTION, MILLISECONDS, RATIO, COUNT, INDEX }

/**
 * A single computed metric plus the metadata that keeps reporting honest.
 *
 * @param value finite numeric value (callers pass a finite number; use [confident] = false when the
 *   underlying sample is too small — never NaN).
 * @param n sample size that supports this value.
 * @param confident false when [n] is below the trust threshold; the UI must then show "datos
 *   insuficientes", never a hard number, and the engine/safety must NOT act on it.
 */
data class EmotionMetricValue(
    val key: EmotionMetricKey,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: EmotionMetricUnit,
) {
    companion object {
        /** A value the system could not compute with confidence (e.g. zero post-error pairs). */
        fun unavailable(key: EmotionMetricKey, unit: EmotionMetricUnit, n: Int = 0): EmotionMetricValue =
            EmotionMetricValue(key = key, value = 0.0, n = n, confident = false, unit = unit)
    }
}

/** A keyed bundle of computed metrics for one block / session. */
typealias EmotionMetricSet = Map<EmotionMetricKey, EmotionMetricValue>

/**
 * Confidence-SAFE accessor: returns the value ONLY when present AND confident, else null. Every
 * adaptive/escalation/safety branch MUST use this (never a "default = 0.0" accessor, which would let
 * an under-powered estimate masquerade as a real low value — the phantom-zero footgun).
 */
fun EmotionMetricSet.valueIfConfident(key: EmotionMetricKey): Double? {
    val metric = this[key] ?: return null
    return if (metric.confident) metric.value else null
}

/** Raw value regardless of confidence, or [default] when absent. For DISPLAY only, never for escalation. */
fun EmotionMetricSet.rawValueOf(key: EmotionMetricKey, default: Double = 0.0): Double =
    this[key]?.value ?: default
