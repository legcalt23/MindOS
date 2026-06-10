package com.mindos.app.features.mindboost.memory.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryTrainingCycleTest {

    private val cycle = MemoryTrainingCycles.STANDARD_8_WEEK

    @Test
    fun hasEightSequentialWeeks() {
        assertEquals(8, cycle.weeks.size)
        assertEquals((1..8).toList(), cycle.weeks.map { it.index })
    }

    @Test
    fun deloadWeeksAreFourAndEight() {
        assertEquals(listOf(4, 8), cycle.weeks.filter { it.isDeload }.map { it.index })
    }

    @Test
    fun deloadWeeksCarryFullEvaluation() {
        listOf(4, 8).forEach {
            assertEquals(MemoryEvaluationRequirement.FULL, cycle.weekFor(it).evaluation)
        }
    }

    @Test
    fun everyWeekEmphasizesAtLeastOneExercise() {
        assertTrue(cycle.weeks.all { it.emphasizedExercises.isNotEmpty() })
    }

    @Test
    fun weekFor_clampsOutOfRange() {
        assertEquals(cycle.weeks.first(), cycle.weekFor(0))
        assertEquals(cycle.weeks.last(), cycle.weekFor(99))
    }
}
