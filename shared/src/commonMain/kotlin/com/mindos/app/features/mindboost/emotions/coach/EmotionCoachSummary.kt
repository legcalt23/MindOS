@file:Suppress("MaxLineLength") // Neutral limit/disclaimer sentences read best whole.

package com.mindos.app.features.mindboost.emotions.coach

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionAggregateFactory
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackCopy
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Structured, PRIVACY-SAFE summary an AI Coach may receive about a user's Emociones training (MC8u-B+ §J). It carries
 * ONLY training PATTERNS and calibration state — never raw trials, self-report values, free text, or a person-level
 * trait. By charter there is no per-capacity emotional score, so "strengths/needs" are expressed as what was PRACTICED
 * (training frequency), never "you are good/bad at X". It always states its interpretation limits and non-clinical
 * disclaimer so a coach cannot over-read it.
 */
data class EmotionCoachSummary(
    val calibrationState: String,
    val confidenceLabel: String,
    val validSessions: Int,
    val target: Int,
    val calibrated: Boolean,
    val practicedCapacities: List<String>,
    val mostPracticed: String?,
    val interpretationLimits: String,
    val disclaimer: String,
)

/** Pure builder for [EmotionCoachSummary] from persisted summaries + the derived aggregate. */
object EmotionCoachSummaryBuilder {

    const val INTERPRETATION_LIMITS =
        "Describe patrones de entrenamiento en la tarea, no rasgos de la persona ni su estado emocional real. " +
            "Úsalo solo como orientación de práctica, nunca como una lectura sobre quién es el usuario."
    // Uses only the approved lexicon ("evaluación clínica" is an allowed disclaimer phrase); never names a forbidden
    // lexeme even to negate it (the copy/terminology scanners flag any occurrence).
    const val DISCLAIMER = "No es una evaluación clínica ni identifica condiciones de salud."

    fun build(summaries: List<EmotionSessionSummaryDto>, aggregate: EmotionAggregateDto?): EmotionCoachSummary {
        val validCount = aggregate?.sessionsCompleted
            ?: summaries.count { EmotionTerminalSemantics.isValidForAggregate(it) }
        val tier = EmotionAggregateFactory.tierFor(validCount)
        val definitions = summaries.mapNotNull { s -> EmotionExerciseCatalog.all.values.firstOrNull { it.id.name == s.exerciseId } }
        val byCapacity = definitions.groupingBy { it.capacity }.eachCount()
        val practiced = byCapacity.keys.map { EmotionFeedbackCopy.focusOf(it) }
        val mostPracticed = byCapacity.maxByOrNull { it.value }?.key?.let { EmotionFeedbackCopy.focusOf(it) }
        return EmotionCoachSummary(
            calibrationState = EmotionStrings.tierLabel(tier),
            confidenceLabel = EmotionStrings.tierLabel(tier),
            validSessions = validCount,
            target = EmotionStrings.VALID_SESSION_TARGET,
            calibrated = validCount >= EmotionStrings.VALID_SESSION_TARGET,
            practicedCapacities = practiced,
            mostPracticed = mostPracticed,
            interpretationLimits = INTERPRETATION_LIMITS,
            disclaimer = DISCLAIMER,
        )
    }
}
