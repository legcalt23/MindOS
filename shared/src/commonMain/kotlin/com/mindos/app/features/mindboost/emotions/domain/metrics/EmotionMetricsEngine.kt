package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial

/** A recorded trial paired with its classified outcome (the assembler is the sole authority). */
data class ClassifiedEmotionTrial(val trial: RecordedEmotionTrial, val outcome: EmotionTrialOutcome)

/**
 * Computes block metrics from classified trials. Pure, deterministic, no platform. The honesty
 * guarantees live here: confidence gating by sample size, MAD on central tendency only, extreme events
 * counted on raw data, and never a NaN (under-powered estimates are returned `unavailable`).
 */
interface EmotionMetricsEngine {
    fun blockMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet
}
