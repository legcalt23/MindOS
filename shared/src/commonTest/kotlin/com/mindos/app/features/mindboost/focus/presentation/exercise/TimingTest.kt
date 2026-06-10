package com.mindos.app.features.mindboost.focus.presentation.exercise

import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Timing guarantees (risk A1): reaction time is the delta between two MONOTONIC
 * marks, never wall-clock. A fake monotonic clock lets us assert the arithmetic
 * deterministically without a device.
 */
class TimingTest {

    private class FakeMonotonicClock(var nanos: Long = 0) : MonotonicClock {
        override fun nowNanos(): Long = nanos
        fun advanceMs(ms: Long) { nanos += ms * NANOS_PER_MS }
        private companion object { const val NANOS_PER_MS = 1_000_000L }
    }

    @Test
    fun reactionTime_isTheMonotonicDeltaInMillis() {
        val clock = FakeMonotonicClock()
        val onset = clock.nowNanos()
        clock.advanceMs(420)
        val response = clock.nowNanos()
        assertEquals(420L, (response - onset) / 1_000_000L)
    }

    @Test
    fun systemMonotonicClock_isNonDecreasing() {
        val clock = SystemMonotonicClock()
        val first = clock.nowNanos()
        val second = clock.nowNanos()
        assertTrue(second >= first)
    }

    @Test
    fun classifierTurnsMonotonicLatencyIntoHitReactionTime() {
        // A 380ms latency within the window classifies as a HIT carrying that RT.
        val classification = TrialClassifier.classify(
            isTarget = true, responseLatencyMs = 380, anticipationFloorMs = 150, responseWindowMs = 1_000,
        )
        assertEquals(TrialOutcome.HIT, classification.outcome)
        assertEquals(380L, classification.reactionTime?.value)
    }
}
