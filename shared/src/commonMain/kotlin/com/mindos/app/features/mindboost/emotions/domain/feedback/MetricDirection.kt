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
    EmotionMetricKey.CATEGORY_COVERAGE,
    EmotionMetricKey.CATEGORY_DPRIME_MIN,
    EmotionMetricKey.STATE_THOUGHT_ACCURACY,
    EmotionMetricKey.AFFECTIVE_DECISION_QUALITY,
    EmotionMetricKey.SAFE_PERSISTENCE_INDEX,
    EmotionMetricKey.REGULATION_STABILITY,
    EmotionMetricKey.FRUSTRATION_TOLERANCE_INDEX,
    EmotionMetricKey.ACCURACY,
    EmotionMetricKey.HIT_RATE,
    EmotionMetricKey.POST_ERROR_ACCURACY,
    EmotionMetricKey.POST_CORRECT_ACCURACY,
    EmotionMetricKey.STRATEGY_PERSISTENCE,
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
    EmotionMetricKey.POST_ERROR_ABANDONMENT,
    -> MetricDirection.LOWER_IS_BETTER

    EmotionMetricKey.RT_MEDIAN -> MetricDirection.RANGE_IS_BETTER

    // Composite/derived signals we display but never phrase as raw strong/weak performance.
    //
    // Ola 3 · R1 adds the two latency readings of the fused inhibition exercise. A shorter SSRT genuinely IS a
    // faster brake, so LOWER_IS_BETTER is the scientifically right adjective — and declaring it here would be a
    // defect, because both consumers of this enum compare against thresholds expressed in [0,1] "goodness": a
    // 280 ms reading would sit permanently past every floor and drive an endless step-down, and the block score
    // would map it through a unit-interval normalisation it does not live in. A latency gets reported, never
    // ranked by machinery built for proportions.
    EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
    EmotionMetricKey.POST_ERROR_SLOWING,
    EmotionMetricKey.SSRT,
    EmotionMetricKey.AFFECTIVE_SSRT_DELTA,
    -> MetricDirection.INFORMATIONAL_ONLY
}
