package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome

/**
 * The one directed confusion that dominated the session's mistakes: [truth] items answered as [chosen],
 * [count] times out of [errors] total errors.
 *
 * Directed on purpose. Calling urges facts and calling facts urges are different mistakes with different
 * corrections, and a symmetric "you mix up X and Y" would name neither.
 */
data class DominantConfusion(
    val truth: EmotionCategory,
    val chosen: EmotionCategory,
    val count: Int,
    val errors: Int,
)

/** 4×4 confusion matrix for EM3 (truth → chosen → count). The assembler owns it as scoring authority. */
data class EmotionConfusionMatrix(val counts: Map<EmotionCategory, Map<EmotionCategory, Int>>) {
    fun correct(): Int = counts.entries.sumOf { (truth, row) -> row[truth] ?: 0 }
    fun total(): Int = counts.values.sumOf { row -> row.values.sum() }

    /**
     * Ola 2 · R3 · P4 — the matrix stops being a number nobody reads.
     *
     * It was built every session and stored on the result with no consumer at all, which meant the exercise
     * could see exactly WHICH boundary a user was missing and told them only their overall accuracy.
     *
     * Returns null far more often than not, and that is the point: a pattern is only named when the session
     * actually shows one. Three independent conditions, all of which a couple of unlucky slips would fail —
     * enough mistakes to have a shape at all ([MIN_ERRORS]), enough of them in this one direction
     * ([MIN_PAIR_COUNT] and [MIN_SHARE_OF_ERRORS]), and a runner-up that is strictly behind, so a tie names
     * nothing. Anything short of that is a session with no dominant confusion, not a session to invent one for.
     */
    fun dominantConfusion(): DominantConfusion? {
        val mistakes = counts.entries.flatMap { (truth, row) ->
            row.filterKeys { it != truth }.map { (chosen, count) -> Triple(truth, chosen, count) }
        }.filter { it.third > 0 }
        val errors = mistakes.sumOf { it.third }
        if (errors < MIN_ERRORS) return null

        val ranked = mistakes.sortedByDescending { it.third }
        val top = ranked.first()
        val runnerUp = ranked.getOrNull(1)?.third ?: 0
        val dominant = top.third >= MIN_PAIR_COUNT &&
            top.third.toDouble() / errors >= MIN_SHARE_OF_ERRORS &&
            top.third > runnerUp
        return if (dominant) DominantConfusion(top.first, top.second, top.third, errors) else null
    }

    private companion object {
        /** Below this many mistakes a session has no shape to read, only slips. */
        const val MIN_ERRORS = 4

        /** A direction taken twice is a coincidence; this is the floor at which it is worth naming. */
        const val MIN_PAIR_COUNT = 3

        /** At least half of every mistake in the session must go this same way. */
        const val MIN_SHARE_OF_ERRORS = 0.5
    }
}

/** The assembled result of one block: ordered outcomes, computed metrics, and (EM3) confusion matrix. */
data class EmotionBlockResult(
    val exerciseId: EmotionExerciseId,
    val outcomes: List<EmotionTrialOutcome>,
    val metrics: EmotionMetricSet,
    val confusion: EmotionConfusionMatrix?,
)
