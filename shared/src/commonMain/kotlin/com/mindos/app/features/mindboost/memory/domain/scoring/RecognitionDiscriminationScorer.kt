@file:Suppress("MagicNumber") // 0.85/0.34/8 are named gate constants; 0.0 baselines are domain neutrals.

package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * Shared recognition-discrimination core: the single source of truth for false-alarm-corrected multi-select
 * RECOGNITION scoring, delegated to by EX4 paired-associates ([AssociativePairsScorer]) and EX3 24h delayed
 * recall ([DeferredRecallScorer]).
 *
 * Both live flows are flat multi-selects over targets + lures, so a raw hit rate is GAMEABLE (select
 * everything → 1.0). The honest signal corrects for over-selection: a trial only counts when the user
 * actually DISCRIMINATED (kept its false-alarm rate within [MAX_FALSE_ALARM_RATE]), and its score is then
 * the hit rate. A block whose discriminating fraction falls below [DISCRIMINATION_VALIDITY] is INVALIDATED
 * (the tested anti-gaming control: select-all → INVALIDATED). RT never feeds the index. Indices are null
 * when not computable — NEVER 0 for "no data".
 */
data class RecognitionTrial(
    val positiveCount: Int,
    val hits: Int,
    val distractorCount: Int,
    val falseAlarms: Int,
) {
    init {
        require(positiveCount > 0) { "RecognitionTrial.positiveCount must be > 0." }
        require(hits in 0..positiveCount) { "hits must be in 0..positiveCount." }
        require(distractorCount >= 0) { "distractorCount cannot be negative." }
        require(falseAlarms in 0..distractorCount) { "falseAlarms must be in 0..distractorCount." }
    }

    val hitRate: Double get() = hits.toDouble() / positiveCount

    /** No lures present (degenerate) ⇒ no over-selection signal ⇒ 0.0. */
    val falseAlarmRate: Double get() = if (distractorCount == 0) 0.0 else falseAlarms.toDouble() / distractorCount

    /** Discriminating iff the user did NOT over-select lures (false-alarm rate within bound). */
    val isDiscriminating: Boolean get() = falseAlarmRate <= RecognitionDiscriminationScorer.MAX_FALSE_ALARM_RATE
}

/** Block-level honesty state for a recognition-discrimination block. */
enum class RecognitionBlockState { VALID, INVALIDATED, CALIBRATING }

/** Scored recognition block. [index] is null when not computable — NEVER 0 for "no data". */
data class RecognitionBlockResult(
    val state: RecognitionBlockState,
    val index: Double?,
    val falseAlarmRate: Double,
    val validTrials: Int,
)

/**
 * Pure shared scorer: the single source of truth for the discrimination-validity GATE
 * (≥ [DISCRIMINATION_VALIDITY] of trials within [MAX_FALSE_ALARM_RATE]) and the false-alarm-corrected index
 * (mean HIT RATE over DISCRIMINATING trials only). EX4 [AssociativePairsScorer] and EX3
 * [DeferredRecallScorer] delegate to this so the anti-gaming logic and thresholds live in ONE place.
 */
object RecognitionDiscriminationScorer {
    const val MAX_FALSE_ALARM_RATE = 0.34
    const val DISCRIMINATION_VALIDITY = 0.85
    const val MIN_VALID_TRIALS = 8

    fun scoreBlock(trials: List<RecognitionTrial>): RecognitionBlockResult {
        if (trials.isEmpty()) return calibrating(0.0, 0)
        val meanFalseAlarm = trials.map { it.falseAlarmRate }.average()
        val discriminating = trials.filter { it.isDiscriminating }
        val discriminatingFraction = discriminating.size.toDouble() / trials.size
        return when {
            discriminatingFraction < DISCRIMINATION_VALIDITY -> invalidated(meanFalseAlarm)
            discriminating.size < MIN_VALID_TRIALS -> calibrating(meanFalseAlarm, discriminating.size)
            else -> validBlock(meanFalseAlarm, discriminating)
        }
    }

    /** Inter-block aggregation: valid-trials-weighted mean index over VALID blocks only. null if none. */
    fun sessionIndex(blocks: List<RecognitionBlockResult>): Double? {
        val good = blocks.filter { it.state == RecognitionBlockState.VALID && it.index != null }
        if (good.isEmpty()) return null
        val weight = good.sumOf { it.validTrials.toDouble() }
        return good.sumOf { checkNotNull(it.index) * it.validTrials } / weight
    }

    private fun validBlock(meanFalseAlarm: Double, discriminating: List<RecognitionTrial>) = RecognitionBlockResult(
        state = RecognitionBlockState.VALID,
        index = discriminating.map { it.hitRate }.average(),
        falseAlarmRate = meanFalseAlarm,
        validTrials = discriminating.size,
    )

    private fun invalidated(meanFalseAlarm: Double) =
        RecognitionBlockResult(RecognitionBlockState.INVALIDATED, null, meanFalseAlarm, 0)

    private fun calibrating(meanFalseAlarm: Double, validTrials: Int) =
        RecognitionBlockResult(RecognitionBlockState.CALIBRATING, null, meanFalseAlarm, validTrials)
}
