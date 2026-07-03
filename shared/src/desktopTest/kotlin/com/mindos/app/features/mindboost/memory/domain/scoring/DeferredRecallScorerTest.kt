package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * EX3 deferred recall (P1) · the 24h delayed-recall scorer. The discrimination gate makes a raw hit rate
 * un-gameable: a "select everything" delayed test over-selects lures → INVALIDATED; only discriminating
 * outcomes count toward DELAYED_RECALL_ACCURACY.
 */
class DeferredRecallScorerTest {

    /** A discriminating outcome: recalled [hits]/8 studied words, selected no lures (false-alarm rate 0). */
    private fun discriminating(hits: Int) =
        DeferredRecallOutcome(studiedCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0)

    /** A "select everything" outcome: every studied word AND every lure selected → false-alarm rate 1.0. */
    private fun selectAll() = DeferredRecallOutcome(studiedCount = 8, hits = 8, distractorCount = 8, falseAlarms = 8)

    @Test
    fun emptyBlock_isCalibrating() {
        val r = DeferredRecallScorer.scoreBlock(emptyList())
        assertEquals(DeferredRecallBlockState.CALIBRATING, r.state)
        assertNull(r.delayedRecallIndex)
    }

    @Test
    fun belowMinDiscriminatingTrials_isCalibrating() {
        val r = DeferredRecallScorer.scoreBlock(List(7) { discriminating(6) })
        assertEquals(DeferredRecallBlockState.CALIBRATING, r.state)
        assertNull(r.delayedRecallIndex)
    }

    @Test
    fun selectEverythingDelayedTest_isInvalidated_notAFree100() {
        val r = DeferredRecallScorer.scoreBlock(List(10) { selectAll() }) // FAR 1.0 on every outcome
        assertEquals(DeferredRecallBlockState.INVALIDATED, r.state)
        assertNull(r.delayedRecallIndex)
    }

    @Test
    fun sufficientDiscriminating_isValid_withMeanHitRateIndex() {
        val r = DeferredRecallScorer.scoreBlock(List(8) { discriminating(hits = 6) }) // hit rate 0.75 each
        assertEquals(DeferredRecallBlockState.VALID, r.state)
        assertEquals(0.75, assertNotNull(r.delayedRecallIndex), absoluteTolerance = 1e-9)
        assertEquals(8, r.validTrials)
    }

    @Test
    fun nonDiscriminatingOutcomes_areExcludedFromTheIndex() {
        // 8 discriminating (hit rate 1.0) + 1 over-selected → 8/9 = 0.889 ≥ 0.85 ⇒ VALID over the 8.
        val outcomes = List(8) { discriminating(hits = 8) } + selectAll()
        val r = DeferredRecallScorer.scoreBlock(outcomes)
        assertEquals(DeferredRecallBlockState.VALID, r.state)
        assertEquals(8, r.validTrials, "only discriminating outcomes count")
        assertEquals(1.0, assertNotNull(r.delayedRecallIndex), absoluteTolerance = 1e-9)
    }

    @Test
    fun sessionIndex_isTrialsWeighted_andNullWithoutAValidBlock() {
        val tooSmall = DeferredRecallScorer.scoreBlock(List(3) { discriminating(6) })
        assertNull(DeferredRecallScorer.sessionIndex(listOf(tooSmall)))
        val perfect = DeferredRecallScorer.scoreBlock(List(8) { discriminating(hits = 8) }) // 1.0
        val zero = DeferredRecallScorer.scoreBlock(List(8) { discriminating(hits = 0) }) // 0.0
        assertEquals(0.5, assertNotNull(DeferredRecallScorer.sessionIndex(listOf(perfect, zero))))
    }
}
