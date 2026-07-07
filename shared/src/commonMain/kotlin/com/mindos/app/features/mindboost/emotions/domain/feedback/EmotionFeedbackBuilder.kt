package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier

/**
 * Builds the sober post-session feedback (MC8u P0-1). It is now capacity-, direction- and confidence-aware:
 *  - copy varies by [EmotionExerciseDefinition.capacity] AND by the confident reading of the governing metric,
 *    so no two exercises (or performance levels) share a reading;
 *  - the measured slots are populated ONLY when the governing metric is confident (`null ≠ 0`);
 *  - a safety stop suppresses every performance judgment and surfaces a protective, non-clinical note;
 *  - a next-exercise recommendation is attached (always an implemented exercise), never a push after safety.
 * All copy is scanner-clean and session-scoped — never a person-level trait or a life/clinical claim.
 */
class EmotionFeedbackBuilder {

    @Suppress("CyclomaticComplexMethod") // conditional copy-slot assembly; each branch is a simple selection
    fun build(
        definition: EmotionExerciseDefinition,
        primary: EmotionMetricValue?,
        tier: ProfileTier,
        stoppedForSafety: Boolean = false,
    ): FourPartFeedback {
        val confident = primary?.confident == true && tier != ProfileTier.EXPLORATORY
        val strength = if (confident && primary != null) strengthOf(primary) else null
        val recommendation = EmotionNextRecommendationEngine.recommend(definition, strength, stoppedForSafety)

        val performance = when {
            stoppedForSafety -> ""
            strength != null -> EmotionFeedbackCopy.performance(definition.capacity, strength)
            else -> EmotionFeedbackCopy.INSUFFICIENT_DATA_PERFORMANCE
        }
        return FourPartFeedback(
            whatHappened = "Has completado un bloque de ${definition.displayName}.",
            whatItMeans = "Refleja tu práctica de ${definition.displayName.lowercase()} dentro del entrenamiento.",
            whatTrained = EmotionFeedbackCopy.whatTrained(definition.capacity),
            whatPerformanceShowed = performance,
            strongSignal = if (!stoppedForSafety && strength == SignalStrength.STRONG) {
                EmotionFeedbackCopy.strongSignal(definition.capacity)
            } else {
                null
            },
            weakSignal = if (!stoppedForSafety && strength == SignalStrength.WEAK) {
                EmotionFeedbackCopy.weakSignal(definition.capacity)
            } else {
                null
            },
            whatToPractice = if (stoppedForSafety) {
                ""
            } else {
                EmotionFeedbackCopy.practice(definition.capacity, strength ?: SignalStrength.NEUTRAL)
            },
            whatNotToConclude = EmotionFeedbackCopy.WHAT_NOT_TO_CONCLUDE,
            transferLabel = definition.transferLabel,
            confidenceNote = if (confident) {
                "Lectura con datos suficientes en esta sesión."
            } else {
                "Datos preliminares de esta sesión; no es una lectura estable."
            },
            nextRecommendedExerciseId = recommendation.exerciseId,
            nextRecommendationReason = recommendation.reason,
            safetyNote = if (stoppedForSafety) EmotionFeedbackCopy.SAFETY_NOTE else null,
            confident = confident && !stoppedForSafety,
        )
    }

    /**
     * The confident reading of [primary] against its [directionOf] direction. STRONG/WEAK/NEUTRAL over honest
     * bands; a RANGE/INFORMATIONAL metric never reads as strong/weak (NEUTRAL). Only called when confident.
     */
    private fun strengthOf(primary: EmotionMetricValue): SignalStrength = when (directionOf(primary.key)) {
        MetricDirection.HIGHER_IS_BETTER -> when {
            primary.value >= STRONG_HIGH -> SignalStrength.STRONG
            primary.value <= WEAK_HIGH -> SignalStrength.WEAK
            else -> SignalStrength.NEUTRAL
        }
        MetricDirection.LOWER_IS_BETTER -> when {
            primary.value <= STRONG_LOW -> SignalStrength.STRONG
            primary.value >= WEAK_LOW -> SignalStrength.WEAK
            else -> SignalStrength.NEUTRAL
        }
        MetricDirection.RANGE_IS_BETTER, MetricDirection.INFORMATIONAL_ONLY -> SignalStrength.NEUTRAL
    }

    private companion object {
        // Honest bands for governing metrics normalized to ~0..1 (accuracy/recovery/commission).
        const val STRONG_HIGH = 0.66
        const val WEAK_HIGH = 0.40
        const val STRONG_LOW = 0.15
        const val WEAK_LOW = 0.40
    }
}
