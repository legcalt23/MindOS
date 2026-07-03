package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** P1 EX1 · the processing-validity gate makes this a complex span; span is accuracy-based, never RT. */
class ComplexSpanScorerTest {

    private fun trial(len: Int = 4, proc: Boolean = true, correct: Int = 4, intr: Int = 0) =
        SpanTrial(sequenceLength = len, processingCorrect = proc, serialCorrectItems = correct, intrusions = intr)

    @Test
    fun validBlock_computesAccuracyBasedMeanSpan() {
        val trials = List(10) { trial(len = 5, proc = true, correct = 4) }
        val result = ComplexSpanScorer.scoreBlock(trials)
        assertEquals(SpanBlockState.VALID, result.state)
        assertEquals(4.0, result.span)
        assertEquals(10, result.validTrials)
        assertEquals(0.8, result.serialOrderAccuracy) // 4/5
    }

    @Test
    fun minEffortOnProcessing_invalidatesBlock_noSpan() {
        // processing correct only 50% (< 0.85) → not a complex span → INVALIDATED, span null.
        val trials = List(10) { i -> trial(proc = i % 2 == 0, correct = 5) }
        val result = ComplexSpanScorer.scoreBlock(trials)
        assertEquals(SpanBlockState.INVALIDATED, result.state)
        assertNull(result.span, "INVALIDATED block must not carry a span (null != 0)")
    }

    @Test
    fun shieldMemorandaRandomProcessing_doesNotRaiseSpan() {
        // High recall but chance processing → block invalidated, so the high recall does NOT count.
        val trials = List(10) { i -> trial(proc = i % 2 == 0, correct = 5, len = 5) }
        assertEquals(SpanBlockState.INVALIDATED, ComplexSpanScorer.scoreBlock(trials).state)
    }

    @Test
    fun belowMinValidTrials_isCalibratingNotZero() {
        val trials = List(5) { trial(proc = true, correct = 3) } // 5 valid < 8
        val result = ComplexSpanScorer.scoreBlock(trials)
        assertEquals(SpanBlockState.CALIBRATING, result.state)
        assertNull(result.span)
    }

    @Test
    fun sessionSpan_returnsNull_whenNoValidBlocks() {
        val blocks = listOf(ComplexSpanScorer.scoreBlock(List(10) { trial(proc = false) }))
        assertNull(ComplexSpanScorer.sessionSpan(blocks))
    }

    @Test
    fun sessionSpan_weightsBlocksByValidTrials() {
        val blockA = ComplexSpanScorer.scoreBlock(List(8) { trial(correct = 3) })   // span 3, 8 valid
        val blockB = ComplexSpanScorer.scoreBlock(List(16) { trial(correct = 6) })  // span 6, 16 valid
        // weighted: (3*8 + 6*16) / 24 = (24 + 96)/24 = 5.0
        assertEquals(5.0, ComplexSpanScorer.sessionSpan(listOf(blockA, blockB)))
    }

    @Test
    fun spanScore_goldenLock() {
        val trials = List(12) { i -> trial(len = 6, proc = true, correct = if (i < 6) 5 else 6) }
        val result = ComplexSpanScorer.scoreBlock(trials)
        // mean serialCorrect = (6*5 + 6*6)/12 = (30+36)/12 = 5.5 ; serialOrder = 66/72
        assertEquals(5.5, result.span)
        assertEquals(66.0 / 72.0, result.serialOrderAccuracy)
    }
}
