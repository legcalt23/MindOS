@file:Suppress("MagicNumber") // 0.85/8 are named gate constants; 0.0 baselines are domain neutrals.

package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * P1 EX1 · One complex-span trial outcome: a memoranda sequence interleaved with a processing task.
 * [processingCorrect] is the concurrent-processing gate; [serialCorrectItems] is memoranda recalled in
 * correct serial position (accuracy-based — RT is never part of the span).
 */
data class SpanTrial(
    val sequenceLength: Int,
    val processingCorrect: Boolean,
    val serialCorrectItems: Int,
    val intrusions: Int = 0,
)

/** Block-level honesty state for a complex-span block. */
enum class SpanBlockState { VALID, INVALIDATED, CALIBRATING }

/** Scored complex-span block. [span] is null when not computable — NEVER 0 for "no data". */
data class SpanBlockResult(
    val state: SpanBlockState,
    val span: Double?,
    val processingAccuracy: Double,
    val serialOrderAccuracy: Double?,
    val intrusionRate: Double?,
    val validTrials: Int,
)

/**
 * P1 EX1 (V1_01 §10) · Complex-span scorer. The processing-validity GATE (≥0.85) is what makes this a
 * complex span and not "a simple span disguised": below it the block is INVALIDATED (the span of that
 * block does not count). The span is accuracy-based — the mean memoranda recalled in correct serial
 * order over PROCESSING-VALID trials only — so RT never feeds it.
 *
 * V1 simplification (documented): span = accuracy-based mean serial-correct over valid trials; the full
 * staircase-reversal threshold is a refinement for when the live adaptive run-data is wired.
 */
object ComplexSpanScorer {
    const val PROCESSING_VALIDITY = 0.85
    const val MIN_VALID_TRIALS = 8

    fun scoreBlock(trials: List<SpanTrial>): SpanBlockResult {
        if (trials.isEmpty()) return calibrating(0.0, 0)
        val procAcc = trials.count { it.processingCorrect }.toDouble() / trials.size
        val valid = trials.filter { it.processingCorrect }
        return when {
            procAcc < PROCESSING_VALIDITY -> invalidated(procAcc)
            valid.size < MIN_VALID_TRIALS -> calibrating(procAcc, valid.size)
            else -> validBlock(procAcc, valid)
        }
    }

    /** Inter-block aggregation: valid-trials-weighted mean span over VALID blocks only. null if none. */
    fun sessionSpan(blocks: List<SpanBlockResult>): Double? {
        val good = blocks.filter { it.state == SpanBlockState.VALID && it.span != null }
        if (good.isEmpty()) return null
        val weight = good.sumOf { it.validTrials.toDouble() }
        return good.sumOf { checkNotNull(it.span) * it.validTrials } / weight
    }

    private fun validBlock(procAcc: Double, valid: List<SpanTrial>) = SpanBlockResult(
        state = SpanBlockState.VALID,
        span = valid.map { it.serialCorrectItems }.average(),
        processingAccuracy = procAcc,
        serialOrderAccuracy = serialOrderAccuracy(valid),
        intrusionRate = intrusionRate(valid),
        validTrials = valid.size,
    )

    private fun serialOrderAccuracy(valid: List<SpanTrial>): Double {
        val totalItems = valid.sumOf { it.sequenceLength }
        return if (totalItems == 0) 0.0 else valid.sumOf { it.serialCorrectItems }.toDouble() / totalItems
    }

    private fun intrusionRate(valid: List<SpanTrial>): Double {
        val totalItems = valid.sumOf { it.sequenceLength }
        return if (totalItems == 0) 0.0 else valid.sumOf { it.intrusions }.toDouble() / totalItems
    }

    private fun invalidated(procAcc: Double) =
        SpanBlockResult(SpanBlockState.INVALIDATED, null, procAcc, null, null, 0)

    private fun calibrating(procAcc: Double, validTrials: Int) =
        SpanBlockResult(SpanBlockState.CALIBRATING, null, procAcc, null, null, validTrials)
}
