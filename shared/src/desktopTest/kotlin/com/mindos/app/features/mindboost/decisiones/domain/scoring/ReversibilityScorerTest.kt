package com.mindos.app.features.mindboost.decisiones.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * E2 Reversibilidad scorer (Decisiones · P1) · the honest discrimination signal and its anti-gaming gates.
 * Synthetic adversaries (charter #5): a constant responder and a base-rate guesser must NOT score above
 * chance; only genuine reversible/irreversible discrimination does.
 */
class ReversibilityScorerTest {

    private fun trial(actual: Boolean, selected: Boolean) = ReversibilityTrial(actual, selected)

    /** [reversibleCorrect] of the reversible cards and [irreversibleCorrect] of the irreversible ones right. */
    private fun block(
        reversibleTotal: Int,
        reversibleCorrect: Int,
        irreversibleTotal: Int,
        irreversibleCorrect: Int,
    ): List<ReversibilityTrial> {
        val rev = List(reversibleTotal) { i -> trial(actual = true, selected = i < reversibleCorrect) }
        val irr = List(irreversibleTotal) { i -> trial(actual = false, selected = i >= irreversibleCorrect) }
        return rev + irr
    }

    @Test
    fun emptyBlock_isCalibrating_withNullIndex() {
        val result = ReversibilityScorer.scoreBlock(emptyList())
        assertEquals(ReversibilityBlockState.CALIBRATING, result.state)
        assertNull(result.discriminationIndex, "no data is never a number")
    }

    @Test
    fun singleStimulusClass_isCalibrating_cannotMeasureDiscrimination() {
        // 8 reversible scenarios only, user mixes responses → no irreversible class to measure specificity.
        val trials = List(8) { i -> trial(actual = true, selected = i % 2 == 0) }
        val result = ReversibilityScorer.scoreBlock(trials)
        assertEquals(ReversibilityBlockState.CALIBRATING, result.state)
        assertNull(result.discriminationIndex)
    }

    @Test
    fun constantResponder_onBothClasses_isInvalidated_notLowScore() {
        // Both stimulus classes present, but the user said "reversible" to everything → no discrimination.
        val trials = block(reversibleTotal = 4, reversibleCorrect = 4, irreversibleTotal = 4, irreversibleCorrect = 0)
        // irreversibleCorrect=0 means every irreversible card was answered "reversible" → fully constant.
        val result = ReversibilityScorer.scoreBlock(trials)
        assertEquals(ReversibilityBlockState.INVALIDATED, result.state)
        assertNull(result.discriminationIndex, "a gamed block yields no number, never a low value")
    }

    @Test
    fun belowMinTrials_isCalibrating_withNullIndex() {
        val trials = block(reversibleTotal = 2, reversibleCorrect = 2, irreversibleTotal = 2, irreversibleCorrect = 2)
        val result = ReversibilityScorer.scoreBlock(trials) // 4 < MIN_VALID_TRIALS(8)
        assertEquals(ReversibilityBlockState.CALIBRATING, result.state)
        assertNull(result.discriminationIndex)
    }

    @Test
    fun perfectDiscrimination_isValid_indexOne() {
        val trials = block(reversibleTotal = 5, reversibleCorrect = 5, irreversibleTotal = 5, irreversibleCorrect = 5)
        val result = ReversibilityScorer.scoreBlock(trials)
        assertEquals(ReversibilityBlockState.VALID, result.state)
        assertEquals(1.0, assertNotNull(result.discriminationIndex), absoluteTolerance = 1e-9)
        assertEquals(10, result.validTrials)
    }

    @Test
    fun balancedAccuracy_isMeanOfPerClassAccuracy_notRawAccuracy() {
        // sensitivity 4/4 = 1.0, specificity 2/4 = 0.5 → balanced 0.75 (raw accuracy here is 6/8 = 0.75 too,
        // but the math is the per-class mean, verified by the base-rate test below where they diverge).
        val trials = block(reversibleTotal = 4, reversibleCorrect = 4, irreversibleTotal = 4, irreversibleCorrect = 2)
        val result = ReversibilityScorer.scoreBlock(trials)
        assertEquals(ReversibilityBlockState.VALID, result.state)
        assertEquals(0.75, assertNotNull(result.discriminationIndex), absoluteTolerance = 1e-9)
    }

    @Test
    fun baseRateGuessing_isPenalized_balancedFarBelowRawAccuracy() {
        // 6 reversible + 2 irreversible; user mostly answers "reversible" (uses both responses, so VALID):
        // 5/6 reversible right, 0/2 irreversible right. Raw accuracy = 5/8 = 0.625, but balanced =
        // (5/6 + 0/2)/2 ≈ 0.417 — base-rate guessing cannot inflate the discrimination signal above chance.
        val trials = block(reversibleTotal = 6, reversibleCorrect = 5, irreversibleTotal = 2, irreversibleCorrect = 0)
        val result = ReversibilityScorer.scoreBlock(trials)
        assertEquals(ReversibilityBlockState.VALID, result.state)
        val index = assertNotNull(result.discriminationIndex)
        assertEquals((5.0 / 6.0 + 0.0) / 2.0, index, absoluteTolerance = 1e-9)
        assertTrue(index < 0.5, "base-rate guessing scores below chance on the balanced index, got $index")
    }

    @Test
    fun sessionIndex_isValidTrialsWeightedMeanOverValidBlocksOnly() {
        val strong = ReversibilityScorer.scoreBlock(
            block(reversibleTotal = 5, reversibleCorrect = 5, irreversibleTotal = 5, irreversibleCorrect = 5),
        ) // VALID, index 1.0, 10 trials
        val weak = ReversibilityScorer.scoreBlock(
            block(reversibleTotal = 4, reversibleCorrect = 2, irreversibleTotal = 4, irreversibleCorrect = 2),
        ) // VALID, index 0.5, 8 trials
        val gamed = ReversibilityScorer.scoreBlock(
            block(reversibleTotal = 4, reversibleCorrect = 4, irreversibleTotal = 4, irreversibleCorrect = 0),
        ) // INVALIDATED, excluded
        val session = assertNotNull(ReversibilityScorer.sessionIndex(listOf(strong, weak, gamed)))
        assertEquals((1.0 * 10 + 0.5 * 8) / 18.0, session, absoluteTolerance = 1e-9)
    }

    @Test
    fun sessionIndex_isNull_whenNoValidBlock() {
        val gamed = ReversibilityScorer.scoreBlock(
            block(reversibleTotal = 4, reversibleCorrect = 4, irreversibleTotal = 4, irreversibleCorrect = 0),
        )
        assertNull(ReversibilityScorer.sessionIndex(listOf(gamed)))
    }
}
