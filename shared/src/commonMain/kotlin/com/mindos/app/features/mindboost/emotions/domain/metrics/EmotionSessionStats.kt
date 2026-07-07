@file:Suppress("MaxLineLength") // Doc lines read best whole.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import kotlin.math.sqrt

/**
 * Explicit, EXPLICITLY-PERSISTED session counts + reaction-time aggregates (MC8u-C §3). The PRD session contract wants
 * these as first-class fields, not silently re-derived from `metricsSummary` — so they are computed once at seal and
 * stored on the summary. Pure and deterministic. These are TASK-PERFORMANCE aggregates (answers, accuracy, RT), never
 * emotional/clinical signals.
 */
data class EmotionSessionStats(
    val totalAnswers: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val omissions: Int,
    val accuracy: Double,
    val averageReactionTimeMs: Double,
    val bestReactionTimeMs: Double,
    val worstReactionTimeMs: Double,
    val reactionTimeVariability: Double,
)

/** Pure calculator for [EmotionSessionStats] from the classified trials of a whole session. */
object EmotionSessionStatsCalculator {

    fun from(classified: List<ClassifiedEmotionTrial>): EmotionSessionStats {
        val total = classified.size
        val correct = classified.count {
            it.outcome == EmotionTrialOutcome.CORRECT ||
                it.outcome == EmotionTrialOutcome.HIT ||
                it.outcome == EmotionTrialOutcome.CORRECT_REJECTION
        }
        val incorrect = classified.count {
            it.outcome == EmotionTrialOutcome.INCORRECT ||
                it.outcome == EmotionTrialOutcome.FALSE_ALARM ||
                it.outcome == EmotionTrialOutcome.MISS
        }
        val omissions = classified.count { it.trial.responseKey == EmotionResponseKey.NONE }
        val rts = classified
            .filter { it.trial.responseKey != EmotionResponseKey.NONE }
            .mapNotNull { it.trial.latencyMs?.toDouble() }
        return EmotionSessionStats(
            totalAnswers = total,
            correctAnswers = correct,
            incorrectAnswers = incorrect,
            omissions = omissions,
            accuracy = if (total == 0) 0.0 else correct.toDouble() / total,
            averageReactionTimeMs = if (rts.isEmpty()) 0.0 else rts.average(),
            bestReactionTimeMs = rts.minOrNull() ?: 0.0,
            worstReactionTimeMs = rts.maxOrNull() ?: 0.0,
            reactionTimeVariability = coefficientOfVariation(rts),
        )
    }

    private fun coefficientOfVariation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return if (mean == 0.0) 0.0 else sqrt(variance) / mean
    }
}
