package com.mindos.app.features.mindboost.focus.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FocusValueObjectsTest {

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
    fun axisLevel_stepsStayClamped() {
        assertEquals(1.0, AxisLevel(0.95).stepUp(0.2).value)
        assertEquals(0.0, AxisLevel(0.05).stepDown(0.2).value)
    }

    @Test
    fun difficultyVector_adjustChangesOnlyTargetedAxis() {
        val base = DifficultyVector.EASIEST
        val adjusted = base.adjust(DifficultyAxis.PRESSURE, AxisLevel(0.5))
        assertEquals(0.5, adjusted.level(DifficultyAxis.PRESSURE).value)
        assertEquals(0.0, adjusted.level(DifficultyAxis.INTERFERENCE).value)
        // Original is untouched (immutability).
        assertEquals(0.0, base.level(DifficultyAxis.PRESSURE).value)
    }

    @Test
    fun difficultyVector_stepUpDownAreInverseWithinRange() {
        val v = DifficultyVector.EASIEST.stepUp(DifficultyAxis.TIME_ON_TASK, 0.3)
        assertTrue(v.level(DifficultyAxis.TIME_ON_TASK).value > 0.0)
        val back = v.stepDown(DifficultyAxis.TIME_ON_TASK, 0.3)
        assertEquals(0.0, back.level(DifficultyAxis.TIME_ON_TASK).value, absoluteTolerance = 1e-9)
    }

    @Test
    fun subjectiveState_validatesRanges() {
        assertFailsWith<IllegalArgumentException> { SubjectiveState(mood1to5 = 7) }
        assertFailsWith<IllegalArgumentException> { SubjectiveState(localHour = 25) }
    }
}
