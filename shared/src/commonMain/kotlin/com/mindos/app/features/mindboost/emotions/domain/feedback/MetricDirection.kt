package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey

/**
 * Whether a HIGHER or LOWER value of a metric is the stronger TRAINING signal (MC8u P0-1). This is a task-level
 * reading, never a person-level judgment. `RANGE_IS_BETTER` means an extreme in either direction is worse (e.g.
 * response time: too fast = impulsive, too slow = disengaged). `INFORMATIONAL_ONLY` metrics are shown/used but
 * never phrased as strong/weak performance.
 */
enum class MetricDirection { HIGHER_IS_BETTER, LOWER_IS_BETTER, RANGE_IS_BETTER, INFORMATIONAL_ONLY }

/** The SINGLE source of truth for a metric's direction — exhaustive over [EmotionMetricKey]. */
fun directionOf(key: EmotionMetricKey): MetricDirection = when (key) {
    EmotionMetricKey.POST_ERROR_RECOVERY,
    EmotionMetricKey.PAUSE_BEFORE_ACTION_RATE,
    EmotionMetricKey.CATEGORY_ACCURACY,
    EmotionMetricKey.GRANULARITY_INDEX,
    EmotionMetricKey.EMOTION_THOUGHT_SEPARATION,
    EmotionMetricKey.AFFECTIVE_DECISION_QUALITY,
    EmotionMetricKey.SAFE_PERSISTENCE_INDEX,
    EmotionMetricKey.REGULATION_STABILITY,
    EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX,
    EmotionMetricKey.ACCURACY,
    EmotionMetricKey.POST_ERROR_ACCURACY,
    -> MetricDirection.HIGHER_IS_BETTER

    EmotionMetricKey.COMMISSION_RATE,
    EmotionMetricKey.RT_CV,
    EmotionMetricKey.CALIBRATION_ERROR,
    EmotionMetricKey.FALSE_ALARMS,
    EmotionMetricKey.OMISSIONS,
    EmotionMetricKey.ANTICIPATIONS,
    EmotionMetricKey.IMPULSIVITY,
    EmotionMetricKey.LAPSES,
    EmotionMetricKey.ABANDONMENT,
    -> MetricDirection.LOWER_IS_BETTER

    EmotionMetricKey.RT_MEDIAN -> MetricDirection.RANGE_IS_BETTER

    // Composite/derived signals we display but never phrase as raw strong/weak performance.
    EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
    EmotionMetricKey.POST_ERROR_SLOWING,
    -> MetricDirection.INFORMATIONAL_ONLY
}
