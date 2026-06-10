package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrialClassifierTest {

    private val floor = 150L
    private val window = 1000L

    @Test
    fun targetWithinWindow_isHitWithRt() {
        val c = TrialClassifier.classify(isTarget = true, responseLatencyMs = 320, floor, window)
        assertEquals(TrialOutcome.HIT, c.outcome)
        assertEquals(320L, c.reactionTime?.value)
    }

    @Test
    fun targetNoResponse_isMiss() {
        val c = TrialClassifier.classify(isTarget = true, responseLatencyMs = null, floor, window)
        assertEquals(TrialOutcome.MISS, c.outcome)
        assertNull(c.reactionTime)
    }

    @Test
    fun nonTargetResponded_isFalseAlarm_keepingLatency() {
        val c = TrialClassifier.classify(isTarget = false, responseLatencyMs = 280, floor, window)
        assertEquals(TrialOutcome.FALSE_ALARM, c.outcome)
        // Latency retained so impulsivity can be measured.
        assertEquals(280L, c.reactionTime?.value)
    }

    @Test
    fun nonTargetNoResponse_isCorrectRejection() {
        val c = TrialClassifier.classify(isTarget = false, responseLatencyMs = null, floor, window)
        assertEquals(TrialOutcome.CORRECT_REJECTION, c.outcome)
    }

    @Test
    fun responseBelowFloor_isAnticipation_regardlessOfTarget() {
        val onTarget = TrialClassifier.classify(isTarget = true, responseLatencyMs = 90, floor, window)
        assertEquals(TrialOutcome.ANTICIPATION, onTarget.outcome)
        val offTarget = TrialClassifier.classify(isTarget = false, responseLatencyMs = 90, floor, window)
        assertEquals(TrialOutcome.ANTICIPATION, offTarget.outcome)
    }

    @Test
    fun responseAfterWindow_isTreatedAsNoResponse() {
        val c = TrialClassifier.classify(isTarget = true, responseLatencyMs = 1500, floor, window)
        assertEquals(TrialOutcome.MISS, c.outcome)
    }
}
