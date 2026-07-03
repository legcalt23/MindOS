package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** INFRA-02 · signed RT preserves the sign of the delta WITHOUT throwing (regression vs the old guard). */
class TimingPolicyTest {

    @Test
    fun signedRt_late_isPositive() {
        val t = TimingPolicy.signedRt(stimulusNanos = 1_000L, responseNanos = 1_300L)
        assertEquals(300L, t.deltaNanos)
        assertEquals(1, t.sign)
    }

    @Test
    fun signedRt_anticipation_isNegative_andDoesNotThrow() {
        // Response BEFORE stimulus (anticipation) → negative delta, no exception.
        val t = TimingPolicy.signedRt(stimulusNanos = 2_000L, responseNanos = 1_500L)
        assertEquals(-500L, t.deltaNanos)
        assertEquals(-1, t.sign)
        assertTrue(t.deltaNanos < 0L)
    }

    @Test
    fun signedRt_exact_isZero() {
        val t = TimingPolicy.signedRt(stimulusNanos = 5_000L, responseNanos = 5_000L)
        assertEquals(0L, t.deltaNanos)
        assertEquals(0, t.sign)
    }
}
