package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey

/**
 * Declarative, immutable "tech sheet" of an emotion exercise. Carries NO render or stimulus-generation
 * logic (that lives in the exercise/ generators) — only the stable description that metrics, adaptation
 * and reporting depend on.
 *
 * @param transferLabel exactly ONE honest transfer-claim level (a test pins single-value).
 * @param adaptiveZone the zone the engine uses for this exercise (read, never guessed).
 * @param plainDescription an honest, jargon-free, scanner-clean sentence shown to the user.
 */
data class EmotionExerciseDefinition(
    val id: EmotionExerciseId,
    val capacity: EmotionCapacity,
    val governingMetric: EmotionMetricKey,
    val primaryMetrics: Set<EmotionMetricKey>,
    val secondaryMetrics: Set<EmotionMetricKey>,
    val adaptiveZone: AdaptiveZone,
    val transferLabel: TransferLabel,
    val displayName: String,
    val plainDescription: String,
) {
    init {
        require(governingMetric in primaryMetrics) {
            "governingMetric ($governingMetric) must be one of primaryMetrics for ${id.name}."
        }
    }
}
