@file:Suppress("MagicNumber", "MaxLineLength") // Block sizes and forecasts ARE the fixtures under test.

package com.mindos.app.features.mindboost.core.calibration

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Ola 1 · N1 — the per-block forecast: a global self-model probe, distinct from the per-trial strip. */
class BlockPredictionTest {

    private fun outcome(predicted: Int, actual: Int, size: Int = 20) =
        BlockPredictionOutcome(BlockPrediction(predicted, size), actual)

    @Test
    fun a_forecast_outside_the_block_is_rejected() {
        assertFailsWith<IllegalArgumentException> { BlockPrediction(predictedCorrect = 21, blockSize = 20) }
        assertFailsWith<IllegalArgumentException> { BlockPrediction(predictedCorrect = -1, blockSize = 20) }
    }

    @Test
    fun an_impossible_observed_result_is_rejected() {
        assertFailsWith<IllegalArgumentException> { outcome(predicted = 10, actual = 25) }
    }

    @Test
    fun predicting_more_than_achieved_is_a_positive_error() {
        assertTrue(outcome(predicted = 18, actual = 12).signedError() > 0.0)
    }

    @Test
    fun predicting_less_than_achieved_is_a_negative_error() {
        assertTrue(outcome(predicted = 8, actual = 16).signedError() < 0.0)
    }

    @Test
    fun the_error_is_a_proportion_so_blocks_of_different_length_stay_comparable() {
        // The emotions module now runs 10-trial and 20-trial blocks side by side. Missing by 3 is a much bigger
        // failure of self-knowledge in a block of 10 than in a block of 20, and a raw count would hide that.
        val inShortBlock = outcome(predicted = 9, actual = 6, size = 10).signedError()
        val inLongBlock = outcome(predicted = 18, actual = 15, size = 20).signedError()
        assertTrue(
            inShortBlock > inLongBlock,
            "the same raw miss of 3 must weigh more in the shorter block ($inShortBlock vs $inLongBlock)",
        )
        assertTrue(abs(inShortBlock - 0.3) < 1e-9)
        assertTrue(abs(inLongBlock - 0.15) < 1e-9)
    }

    @Test
    fun a_perfect_forecast_scores_zero() {
        assertEquals(0.0, outcome(predicted = 14, actual = 14).signedError())
        assertEquals(0.0, outcome(predicted = 14, actual = 14).absoluteError())
    }

    @Test
    fun bias_and_inaccuracy_are_kept_apart() {
        // A user who overshoots one block and undershoots the next by the same amount has NO systematic bias but
        // is not accurate either. Collapsing the two would report them as perfectly self-aware.
        val swinging = listOf(outcome(predicted = 18, actual = 12), outcome(predicted = 6, actual = 12), outcome(predicted = 18, actual = 12), outcome(predicted = 6, actual = 12))
        val reading = BlockPredictionReader.read(swinging) as BlockPredictionReader.Reading.Present
        assertTrue(abs(reading.meanSignedError) < 1e-9, "the swings cancel, so there is no directional bias")
        assertTrue(reading.meanAbsoluteError > 0.0, "but the forecasts were wrong every single time")
    }

    @Test
    fun too_few_blocks_report_absent_rather_than_a_zero_error() {
        // Zero would be the most flattering possible reading of no data: a perfectly self-aware user.
        val reading = BlockPredictionReader.read(listOf(outcome(predicted = 20, actual = 4), outcome(predicted = 20, actual = 4)))
        assertTrue(reading is BlockPredictionReader.Reading.Absent, "expected Absent, got $reading")
        assertEquals(2, reading.nHave)
        assertEquals(BlockPredictionReader.MIN_BLOCKS, reading.nNeeded)
    }

    @Test
    fun no_blocks_at_all_report_absent() {
        assertTrue(BlockPredictionReader.read(emptyList()) is BlockPredictionReader.Reading.Absent)
    }

    @Test
    fun exactly_the_minimum_produces_a_reading() {
        // Discriminating half: without this, returning Absent unconditionally would pass the test above.
        val atFloor = List(BlockPredictionReader.MIN_BLOCKS) { outcome(predicted = 18, actual = 10) }
        val reading = BlockPredictionReader.read(atFloor) as BlockPredictionReader.Reading.Present
        assertEquals(BlockPredictionReader.MIN_BLOCKS, reading.n)
        assertTrue(reading.meanSignedError > 0.0, "a consistently optimistic forecaster must read as optimistic")
    }
}
