package com.mindos.app.features.mindboost.speed.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyEstimatorTest {

    private val estimator = FixedDifficultyEstimator()

    @Test
    fun nullDeadline_doesNotBreakAndReturnsFixedDifficulty() {
        // acceptance #5: deadlineNanos nullable nunca entra en aritmética.
        assertEquals(0.5, estimator.estimateDifficultyBefore(cce = 0.5, deadlineNanos = null))
    }

    @Test
    fun nonNullDeadline_stillFixedInMvp0() {
        assertEquals(0.5, estimator.estimateDifficultyBefore(cce = 0.5, deadlineNanos = 250_000_000L))
    }

    @Test
    fun difficulty_isCoercedIntoUnitRange() {
        assertEquals(1.0, estimator.estimateDifficultyBefore(cce = 1.5, deadlineNanos = null))
        assertEquals(0.0, estimator.estimateDifficultyBefore(cce = -0.5, deadlineNanos = null))
    }
}
