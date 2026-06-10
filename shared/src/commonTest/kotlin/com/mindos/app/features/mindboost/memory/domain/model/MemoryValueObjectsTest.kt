package com.mindos.app.features.mindboost.memory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemoryValueObjectsTest {

    @Test
    fun reactionTime_rejectsNegative() {
        assertFailsWith<IllegalArgumentException> { ReactionTimeMs(-1) }
    }

    @Test
    fun proportion_rejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { Proportion(1.5) }
        assertFailsWith<IllegalArgumentException> { Proportion(-0.1) }
    }

    @Test
    fun proportion_clampedStaysInRange() {
        assertEquals(1.0, Proportion.clamped(2.0).value)
        assertEquals(0.0, Proportion.clamped(-2.0).value)
    }

    @Test
    fun proportion_percentRoundsDown() {
        assertEquals(50, Proportion(0.504).percent)
        assertEquals(100, Proportion.ONE.percent)
    }

    @Test
    fun axisLevel_stepsStayClamped() {
        assertEquals(1.0, AxisLevel(0.95).stepUp(0.2).value)
        assertEquals(0.0, AxisLevel(0.05).stepDown(0.2).value)
    }

    @Test
    fun serialPosition_isOneBased() {
        assertFailsWith<IllegalArgumentException> { SerialPosition(0) }
        assertEquals(1, SerialPosition(1).value)
    }

    @Test
    fun confidenceJudgment_rejectsLevelOutsideScale() {
        assertFailsWith<IllegalArgumentException> {
            ConfidenceJudgment(level = 5, scale = ConfidenceScale.FOUR_POINT)
        }
        assertFailsWith<IllegalArgumentException> {
            ConfidenceJudgment(level = 0, scale = ConfidenceScale.BINARY)
        }
    }

    @Test
    fun confidenceJudgment_normalizesEndpointsRegardlessOfScale() {
        // Lowest level maps to 0.0, highest to 1.0 for every scale.
        assertEquals(0.0, ConfidenceJudgment(1, ConfidenceScale.BINARY).normalized().value)
        assertEquals(1.0, ConfidenceJudgment(2, ConfidenceScale.BINARY).normalized().value)
        assertEquals(0.0, ConfidenceJudgment(1, ConfidenceScale.SIX_POINT).normalized().value)
        assertEquals(1.0, ConfidenceJudgment(6, ConfidenceScale.SIX_POINT).normalized().value)
    }

    @Test
    fun confidenceJudgment_normalizesMidpoint() {
        // 4-point scale, level 2 -> (2-1)/(4-1) = 1/3.
        assertEquals(
            1.0 / 3.0,
            ConfidenceJudgment(2, ConfidenceScale.FOUR_POINT).normalized().value,
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun idValueObjects_rejectBlank() {
        assertFailsWith<IllegalArgumentException> { SessionId(" ") }
        assertFailsWith<IllegalArgumentException> { BlockId("") }
        assertFailsWith<IllegalArgumentException> { TrialId("") }
    }
}
