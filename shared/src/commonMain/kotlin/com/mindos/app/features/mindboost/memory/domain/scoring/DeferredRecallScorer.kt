@file:Suppress("MagicNumber") // 0.0 baselines in the outcome getters are domain neutrals.

package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * EX3 deferred recall (P1) · One 24h between-session delayed-recall outcome: of [studiedCount] words
 * studied a day earlier, the user recalled [hits], and of [distractorCount] lures selected [falseAlarms].
 *
 * The delayed recall is a flat multi-select over studied words + distractors (V1), so — exactly like the
 * EX4 associative recognition — a raw hit rate is GAMEABLE (select everything → 1.0). The honest signal
 * therefore corrects for over-selection: an outcome only counts when the user actually DISCRIMINATED
 * (kept its false-alarm rate within bound), and its score is then the hit rate. This is the same
 * discrimination shape as [AssociativePairsScorer]; the two are intentionally parallel (a future refactor
 * could extract a shared recognition-discrimination scorer). RT is never part of the signal.
 */
data class DeferredRecallOutcome(
    val studiedCount: Int,
    val hits: Int,
    val distractorCount: Int,
    val falseAlarms: Int,
) {
    init {
        require(studiedCount > 0) { "DeferredRecallOutcome.studiedCount must be > 0." }
        require(hits in 0..studiedCount) { "hits must be in 0..studiedCount." }
        require(distractorCount >= 0) { "distractorCount cannot be negative." }
        require(falseAlarms in 0..distractorCount) { "falseAlarms must be in 0..distractorCount." }
    }

    val hitRate: Double get() = hits.toDouble() / studiedCount

    /** No lures present (degenerate) ⇒ no over-selection signal ⇒ 0.0. */
    val falseAlarmRate: Double get() = if (distractorCount == 0) 0.0 else falseAlarms.toDouble() / distractorCount

    /** Discriminating iff the user did NOT over-select lures (false-alarm rate within bound). */
    val isDiscriminating: Boolean get() = falseAlarmRate <= DeferredRecallScorer.MAX_FALSE_ALARM_RATE
}

/** Block-level honesty state for a delayed-recall block. */
enum class DeferredRecallBlockState { VALID, INVALIDATED, CALIBRATING }

/** Scored delayed-recall block. [delayedRecallIndex] is null when not computable — NEVER 0 for "no data". */
data class DeferredRecallBlockResult(
    val state: DeferredRecallBlockState,
    val delayedRecallIndex: Double?,
    val falseAlarmRate: Double,
    val validTrials: Int,
)

/**
 * EX3 deferred recall (P1) · The honest DELAYED_RECALL_ACCURACY scorer — the catalog's governing metric
 * for episodic memory (24h retention without re-study). DELEGATES the discrimination-validity GATE and the
 * false-alarm-corrected index to the shared [RecognitionDiscriminationScorer] (the single source of truth
 * for EX3+EX4): a block of over-selected outcomes is INVALIDATED (un-gameable), and the index is the mean
 * HIT RATE over DISCRIMINATING outcomes only — accuracy-based, RT never feeds it. A 24h signal accumulates
 * slowly by design (one outcome per registered item).
 */
object DeferredRecallScorer {
    const val MAX_FALSE_ALARM_RATE = RecognitionDiscriminationScorer.MAX_FALSE_ALARM_RATE
    const val DISCRIMINATION_VALIDITY = RecognitionDiscriminationScorer.DISCRIMINATION_VALIDITY
    const val MIN_VALID_TRIALS = RecognitionDiscriminationScorer.MIN_VALID_TRIALS

    fun scoreBlock(outcomes: List<DeferredRecallOutcome>): DeferredRecallBlockResult {
        val result = RecognitionDiscriminationScorer.scoreBlock(
            outcomes.map { RecognitionTrial(it.studiedCount, it.hits, it.distractorCount, it.falseAlarms) },
        )
        return DeferredRecallBlockResult(
            state = when (result.state) {
                RecognitionBlockState.VALID -> DeferredRecallBlockState.VALID
                RecognitionBlockState.INVALIDATED -> DeferredRecallBlockState.INVALIDATED
                RecognitionBlockState.CALIBRATING -> DeferredRecallBlockState.CALIBRATING
            },
            delayedRecallIndex = result.index,
            falseAlarmRate = result.falseAlarmRate,
            validTrials = result.validTrials,
        )
    }

    /** Inter-block aggregation: valid-trials-weighted mean index over VALID blocks only. null if none. */
    fun sessionIndex(blocks: List<DeferredRecallBlockResult>): Double? =
        RecognitionDiscriminationScorer.sessionIndex(
            blocks.map {
                RecognitionBlockResult(
                    state = when (it.state) {
                        DeferredRecallBlockState.VALID -> RecognitionBlockState.VALID
                        DeferredRecallBlockState.INVALIDATED -> RecognitionBlockState.INVALIDATED
                        DeferredRecallBlockState.CALIBRATING -> RecognitionBlockState.CALIBRATING
                    },
                    index = it.delayedRecallIndex,
                    falseAlarmRate = it.falseAlarmRate,
                    validTrials = it.validTrials,
                )
            },
        )
}
