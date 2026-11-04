package com.mindos.app.features.mindboost.core.calibration

import kotlin.math.abs

/**
 * Ola 1 · N1 — the second, coarser calibration probe: *"of the next [blockSize], how many will you get right?"*
 *
 * It measures something the per-trial strip cannot. The strip asks about an item the user has just seen and
 * answered; this asks them to forecast their own performance on a block they have not started. Those are different
 * metacognitive acts — local confidence versus a global self-model — and a user can be well calibrated on one and
 * badly wrong on the other.
 *
 * The forecast is made BEFORE the block and scored after it, and the user is never shown their trial-by-trial
 * outcome in between, so the number they give cannot be assembled from feedback they were fed along the way.
 */
data class BlockPrediction(
    val predictedCorrect: Int,
    val blockSize: Int,
) {
    init {
        require(blockSize > 0) { "A block must have at least one trial, was $blockSize" }
        require(predictedCorrect in 0..blockSize) {
            "A forecast must be reachable: 0..$blockSize, was $predictedCorrect"
        }
    }
}

/** A forecast paired with what actually happened. */
data class BlockPredictionOutcome(
    val prediction: BlockPrediction,
    val actualCorrect: Int,
) {
    init {
        require(actualCorrect in 0..prediction.blockSize) {
            "Observed correct answers must fit the block: 0..${prediction.blockSize}, was $actualCorrect"
        }
    }

    /**
     * Signed error as a proportion of the block: positive means the user predicted MORE than they achieved.
     *
     * Expressed as a proportion and not as a raw count so that blocks of different lengths — and the emotions
     * module now runs 10-trial and 20-trial blocks side by side — remain comparable. A raw count of "wrong by 3"
     * means something very different in a block of 10 than in one of 20.
     */
    fun signedError(): Double =
        (prediction.predictedCorrect - actualCorrect).toDouble() / prediction.blockSize

    /** Unsigned magnitude of the same error. Bias and inaccuracy are different questions; both are kept. */
    fun absoluteError(): Double = abs(signedError())
}

/**
 * Reads a run of block forecasts. Fails closed: with nothing forecast there is no reading, never a zero error —
 * a zero would read as a perfectly self-aware user, which is the most flattering possible reading of no data.
 */
object BlockPredictionReader {

    /** Minimum forecasts before the aggregate is reported. One block is an anecdote, not a self-model. */
    const val MIN_BLOCKS = 3

    sealed interface Reading {
        data class Absent(val nHave: Int, val nNeeded: Int) : Reading
        data class Present(
            val n: Int,
            /** Positive = habitually predicts more than achieved. */
            val meanSignedError: Double,
            val meanAbsoluteError: Double,
        ) : Reading
    }

    fun read(outcomes: List<BlockPredictionOutcome>): Reading =
        if (outcomes.size < MIN_BLOCKS) {
            Reading.Absent(outcomes.size, MIN_BLOCKS)
        } else {
            Reading.Present(
                n = outcomes.size,
                meanSignedError = outcomes.map { it.signedError() }.average(),
                meanAbsoluteError = outcomes.map { it.absoluteError() }.average(),
            )
        }
}
