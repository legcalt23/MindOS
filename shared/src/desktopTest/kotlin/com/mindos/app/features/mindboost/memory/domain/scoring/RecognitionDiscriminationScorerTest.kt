package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shared recognition-discrimination core (single source of truth that EX4 pairs + EX3 deferred delegate
 * to). Pins: empty→CALIBRATING(null, never 0); select-all→INVALIDATED; discriminating-fraction boundary;
 * <MIN_VALID_TRIALS→CALIBRATING; ≥MIN_VALID_TRIALS→VALID mean-hit-rate; sessionIndex weighted/null;
 * degenerate distractorCount=0→FAR 0.
 */
class RecognitionDiscriminationScorerTest {

    /** A discriminating trial: recalled [hits]/8 positives, selected NO lures (false-alarm rate 0). */
    private fun discriminating(hits: Int = 6) =
        RecognitionTrial(positiveCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0)

    /** A select-everything trial: all positives AND all lures → false-alarm rate 1.0 (not discriminating). */
    private fun selectAll() = RecognitionTrial(positiveCount = 8, hits = 8, distractorCount = 8, falseAlarms = 8)

    @Test
    fun emptyBlock_isCalibrating_withNullIndex_neverZero() {
        val r = RecognitionDiscriminationScorer.scoreBlock(emptyList())
        assertEquals(RecognitionBlockState.CALIBRATING, r.state)
        assertNull(r.index, "no data → null index, never a silent 0")
    }

    @Test
    fun selectEverythingHistory_invalidatesTheBlock() {
        val r = RecognitionDiscriminationScorer.scoreBlock(List(20) { selectAll() })
        assertEquals(RecognitionBlockState.INVALIDATED, r.state)
        assertNull(r.index, "a gamed block contributes no number")
        assertEquals(1.0, r.falseAlarmRate, absoluteTolerance = 1e-9)
    }

    @Test
    fun belowMinValidTrials_isCalibrating_notValid() {
        // 5 fully-discriminating trials: fraction 1.0 (≥ validity) but 5 < MIN_VALID_TRIALS(8).
        val r = RecognitionDiscriminationScorer.scoreBlock(List(5) { discriminating() })
        assertEquals(RecognitionBlockState.CALIBRATING, r.state)
        assertNull(r.index)
        assertEquals(5, r.validTrials)
    }

    @Test
    fun enoughDiscriminatingTrials_isValid_withMeanHitRateIndex() {
        val r = RecognitionDiscriminationScorer.scoreBlock(List(8) { discriminating(hits = 6) })
        assertEquals(RecognitionBlockState.VALID, r.state)
        assertEquals(0.75, assertNotNull(r.index), absoluteTolerance = 1e-9) // mean hitRate 6/8
        assertEquals(8, r.validTrials)
    }

    @Test
    fun discriminatingFractionBelowValidity_invalidates_atOrAboveValidates() {
        // 16/20 = 0.80 < 0.85 → INVALIDATED.
        val below = RecognitionDiscriminationScorer.scoreBlock(List(16) { discriminating() } + List(4) { selectAll() })
        assertEquals(RecognitionBlockState.INVALIDATED, below.state)
        // 17/20 = 0.85 = validity, and 17 ≥ MIN_VALID_TRIALS → VALID over the 17 discriminating trials.
        val atBoundary =
            RecognitionDiscriminationScorer.scoreBlock(List(17) { discriminating() } + List(3) { selectAll() })
        assertEquals(RecognitionBlockState.VALID, atBoundary.state)
        assertEquals(17, atBoundary.validTrials)
        assertEquals(0.75, assertNotNull(atBoundary.index), absoluteTolerance = 1e-9)
    }

    @Test
    fun sessionIndex_isValidTrialsWeightedMean_overValidBlocksOnly() {
        val blocks = listOf(
            RecognitionBlockResult(RecognitionBlockState.VALID, index = 0.5, falseAlarmRate = 0.0, validTrials = 8),
            RecognitionBlockResult(RecognitionBlockState.VALID, index = 0.8, falseAlarmRate = 0.0, validTrials = 12),
            RecognitionBlockResult(RecognitionBlockState.INVALIDATED, index = null, falseAlarmRate = 1.0, validTrials = 0),
        )
        // (0.5*8 + 0.8*12) / 20 = 13.6/20 = 0.68; the INVALIDATED block is excluded.
        val index = assertNotNull(RecognitionDiscriminationScorer.sessionIndex(blocks))
        assertEquals(0.68, index, absoluteTolerance = 1e-9)
    }

    @Test
    fun sessionIndex_isNull_whenNoValidBlock() {
        val blocks = listOf(
            RecognitionBlockResult(RecognitionBlockState.CALIBRATING, index = null, falseAlarmRate = 0.0, validTrials = 3),
            RecognitionBlockResult(RecognitionBlockState.INVALIDATED, index = null, falseAlarmRate = 1.0, validTrials = 0),
        )
        assertNull(RecognitionDiscriminationScorer.sessionIndex(blocks))
    }

    @Test
    fun degenerateNoLures_hasZeroFalseAlarmRate_andIsDiscriminating() {
        val trial = RecognitionTrial(positiveCount = 8, hits = 4, distractorCount = 0, falseAlarms = 0)
        assertEquals(0.0, trial.falseAlarmRate, absoluteTolerance = 1e-9)
        assertTrue(trial.isDiscriminating)
    }
}
