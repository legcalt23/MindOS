package com.mindos.app.features.mindboost.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SsrtCalculatorTest {

    @Test
    fun integrationMethod_matchesWorkedExample() {
        // 10 go-RTs, p(respond|signal) = 0.5, mean SSD = 200.
        // Sorted go-RTs; rank = round(0.5 * 10) = 5 → 5th value (1-indexed) = 250.
        // SSRT = 250 - 200 = 50.
        val input = SsrtCalculator.SsrtInput(
            goReactionTimes = listOf(210, 220, 230, 240, 250, 260, 270, 280, 290, 300),
            stopTrials = 10,
            stopFailures = 5,
            meanStopSignalDelayMs = 200.0,
        )
        val result = SsrtCalculator.compute(input)
        val value = assertIs<SsrtCalculator.SsrtResult.Value>(result)
        assertEquals(50.0, value.ssrtMs, absoluteTolerance = 0.001)
        assertEquals(0.5, value.probabilityRespondGivenSignal, absoluteTolerance = 0.001)
        assertTrue(value.trustworthy)
    }

    @Test
    fun flagsGaming_whenProbabilityFarFromHalf() {
        // p(respond|signal) = 0.1 → staircase broke down (strategic slowing).
        val input = SsrtCalculator.SsrtInput(
            goReactionTimes = listOf(300, 310, 320, 330, 340, 350, 360, 370, 380, 390),
            stopTrials = 10,
            stopFailures = 1,
            meanStopSignalDelayMs = 200.0,
        )
        val value = assertIs<SsrtCalculator.SsrtResult.Value>(SsrtCalculator.compute(input))
        assertFalse(value.trustworthy)
    }

    @Test
    fun notComputable_withNoStopTrials() {
        val input = SsrtCalculator.SsrtInput(
            goReactionTimes = listOf(250, 260),
            stopTrials = 0,
            stopFailures = 0,
            meanStopSignalDelayMs = 200.0,
        )
        assertIs<SsrtCalculator.SsrtResult.NotComputable>(SsrtCalculator.compute(input))
    }
}
