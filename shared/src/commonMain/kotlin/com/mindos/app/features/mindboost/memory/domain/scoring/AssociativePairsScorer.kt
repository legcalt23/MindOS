@file:Suppress("MagicNumber") // 0.0 baselines in the trial getters are domain neutrals.

package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * P1 EX4 · One paired-associates RECOGNITION trial: of [targetCount] cued targets the user recalled
 * [hits], and of [distractorCount] lures the user wrongly selected [falseAlarms].
 *
 * The live recall is a multi-select over targets + distractors, so a raw hit rate is GAMEABLE (select
 * everything → 1.0). The honest signal therefore corrects for over-selection: a trial only counts when
 * the user actually DISCRIMINATED (kept its false-alarm rate within bound); its score is then the hit
 * rate. This mirrors the complex-span processing-validity gate (a trial counts only if the concurrent
 * demand was met). RT is never part of the signal.
 */
data class PairsTrial(
    val targetCount: Int,
    val hits: Int,
    val distractorCount: Int,
    val falseAlarms: Int,
) {
    init {
        require(targetCount > 0) { "PairsTrial.targetCount must be > 0." }
        require(hits in 0..targetCount) { "hits must be in 0..targetCount." }
        require(distractorCount >= 0) { "distractorCount cannot be negative." }
        require(falseAlarms in 0..distractorCount) { "falseAlarms must be in 0..distractorCount." }
    }

    val hitRate: Double get() = hits.toDouble() / targetCount

    /** No lures present (degenerate) ⇒ no over-selection signal ⇒ 0.0. */
    val falseAlarmRate: Double get() = if (distractorCount == 0) 0.0 else falseAlarms.toDouble() / distractorCount

    /** Discriminating iff the user did NOT over-select lures (false-alarm rate within bound). */
    val isDiscriminating: Boolean get() = falseAlarmRate <= AssociativePairsScorer.MAX_FALSE_ALARM_RATE
}

/** Block-level honesty state for a paired-associates block. */
enum class PairsBlockState { VALID, INVALIDATED, CALIBRATING }

/** Scored paired-associates block. [associativeIndex] is null when not computable — NEVER 0 for "no data". */
data class PairsBlockResult(
    val state: PairsBlockState,
    val associativeIndex: Double?,
    val falseAlarmRate: Double,
    val validTrials: Int,
)

/**
 * P1 EX4 (associative memory) · Paired-associates RECOGNITION scorer. DELEGATES the discrimination-validity
 * GATE and the false-alarm-corrected index to the shared [RecognitionDiscriminationScorer] (the single
 * source of truth for EX3+EX4): a block of over-selected trials is INVALIDATED ("select everything for a
 * free 100%" is blocked), and the associative index is the mean HIT RATE over DISCRIMINATING trials only —
 * accuracy-based, so RT never feeds it. Thresholds are adapter-owned honest defaults (no calibrated norm).
 */
object AssociativePairsScorer {
    const val MAX_FALSE_ALARM_RATE = RecognitionDiscriminationScorer.MAX_FALSE_ALARM_RATE
    const val DISCRIMINATION_VALIDITY = RecognitionDiscriminationScorer.DISCRIMINATION_VALIDITY
    const val MIN_VALID_TRIALS = RecognitionDiscriminationScorer.MIN_VALID_TRIALS

    fun scoreBlock(trials: List<PairsTrial>): PairsBlockResult {
        val result = RecognitionDiscriminationScorer.scoreBlock(
            trials.map { RecognitionTrial(it.targetCount, it.hits, it.distractorCount, it.falseAlarms) },
        )
        return PairsBlockResult(
            state = when (result.state) {
                RecognitionBlockState.VALID -> PairsBlockState.VALID
                RecognitionBlockState.INVALIDATED -> PairsBlockState.INVALIDATED
                RecognitionBlockState.CALIBRATING -> PairsBlockState.CALIBRATING
            },
            associativeIndex = result.index,
            falseAlarmRate = result.falseAlarmRate,
            validTrials = result.validTrials,
        )
    }

    /** Inter-block aggregation: valid-trials-weighted mean index over VALID blocks only. null if none. */
    fun sessionIndex(blocks: List<PairsBlockResult>): Double? =
        RecognitionDiscriminationScorer.sessionIndex(
            blocks.map {
                RecognitionBlockResult(
                    state = when (it.state) {
                        PairsBlockState.VALID -> RecognitionBlockState.VALID
                        PairsBlockState.INVALIDATED -> RecognitionBlockState.INVALIDATED
                        PairsBlockState.CALIBRATING -> RecognitionBlockState.CALIBRATING
                    },
                    index = it.associativeIndex,
                    falseAlarmRate = it.falseAlarmRate,
                    validTrials = it.validTrials,
                )
            },
        )
}
