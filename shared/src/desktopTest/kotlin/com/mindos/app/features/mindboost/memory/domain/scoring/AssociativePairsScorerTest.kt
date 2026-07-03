package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * P1 EX4 · the associative RECOGNITION scorer. The discrimination gate makes a raw hit rate un-gameable:
 * a "select everything" session over-selects lures → INVALIDATED, and only discriminating trials count.
 */
class AssociativePairsScorerTest {

    /** A discriminating trial: recalled [hits]/5 targets, selected NO lures (false-alarm rate 0). */
    private fun discriminating(hits: Int) =
        PairsTrial(targetCount = 5, hits = hits, distractorCount = 6, falseAlarms = 0)

    /** A "select everything" trial: every target AND every lure selected → false-alarm rate 1.0. */
    private fun selectAll() = PairsTrial(targetCount = 5, hits = 5, distractorCount = 6, falseAlarms = 6)

    @Test
    fun emptyBlock_isCalibrating() {
        val r = AssociativePairsScorer.scoreBlock(emptyList())
        assertEquals(PairsBlockState.CALIBRATING, r.state)
        assertNull(r.associativeIndex)
    }

    @Test
    fun belowMinDiscriminatingTrials_isCalibrating() {
        val r = AssociativePairsScorer.scoreBlock(List(7) { discriminating(4) })
        assertEquals(PairsBlockState.CALIBRATING, r.state)
        assertNull(r.associativeIndex)
    }

    @Test
    fun selectEverythingSession_isInvalidated_notAFree100() {
        val r = AssociativePairsScorer.scoreBlock(List(10) { selectAll() }) // FAR 1.0 on every trial
        assertEquals(PairsBlockState.INVALIDATED, r.state)
        assertNull(r.associativeIndex)
    }

    @Test
    fun sufficientDiscriminating_isValid_withMeanHitRateIndex() {
        val r = AssociativePairsScorer.scoreBlock(List(8) { discriminating(hits = 4) }) // hit rate 0.8 each
        assertEquals(PairsBlockState.VALID, r.state)
        assertEquals(0.8, assertNotNull(r.associativeIndex), absoluteTolerance = 1e-9)
        assertEquals(8, r.validTrials)
    }

    @Test
    fun nonDiscriminatingTrials_areExcludedFromTheIndex() {
        // 8 discriminating (hit rate 1.0) + 1 over-selected → 8/9 = 0.889 ≥ 0.85 ⇒ VALID over the 8.
        val trials = List(8) { discriminating(hits = 5) } + selectAll()
        val r = AssociativePairsScorer.scoreBlock(trials)
        assertEquals(PairsBlockState.VALID, r.state)
        assertEquals(8, r.validTrials, "only discriminating trials count")
        assertEquals(1.0, assertNotNull(r.associativeIndex), absoluteTolerance = 1e-9)
    }

    @Test
    fun sessionIndex_isTrialsWeighted_andNullWithoutAValidBlock() {
        val tooSmall = AssociativePairsScorer.scoreBlock(List(3) { discriminating(4) })
        assertNull(AssociativePairsScorer.sessionIndex(listOf(tooSmall)))
        val perfect = AssociativePairsScorer.scoreBlock(List(8) { discriminating(hits = 5) }) // 1.0
        val zero = AssociativePairsScorer.scoreBlock(List(8) { discriminating(hits = 0) }) // 0.0
        assertEquals(0.5, assertNotNull(AssociativePairsScorer.sessionIndex(listOf(perfect, zero))))
    }
}
