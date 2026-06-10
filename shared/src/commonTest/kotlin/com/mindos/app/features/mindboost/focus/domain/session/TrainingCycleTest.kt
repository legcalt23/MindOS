package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrainingCycleTest {

    private val cycle = TrainingCycles.STANDARD_8_WEEK

    @Test
    fun cycle_hasExactlyEightWeeks_indexedInOrder() {
        assertEquals(8, cycle.weeks.size)
        assertEquals((1..8).toList(), cycle.weeks.map { it.index })
    }

    @Test
    fun deloadWeeks_areFourAndEight_withFullEvaluationAndLowVolume() {
        val deloads = cycle.weeks.filter { it.isDeload }
        assertEquals(listOf(4, 8), deloads.map { it.index })
        deloads.forEach {
            assertEquals(Volume.LOW, it.volume)
            assertEquals(EvaluationRequirement.FULL, it.evaluation)
        }
    }

    @Test
    fun nonDeloadWeeks_areNotFlaggedDeload() {
        val nonDeload = cycle.weeks.filterNot { it.index == 4 || it.index == 8 }
        assertTrue(nonDeload.none { it.isDeload })
    }

    @Test
    fun weekFor_clampsOutOfRangeIndices() {
        assertEquals(1, cycle.weekFor(0).index)
        assertEquals(8, cycle.weekFor(99).index)
        assertEquals(3, cycle.weekFor(3).index)
    }

    @Test
    fun week3_emphasizesInhibitionAndInterference() {
        assertTrue(ExerciseId.E4_FRENO in cycle.weekFor(3).emphasizedExercises)
    }

    @Test
    fun cycle_rejectsWrongNumberOfWeeks() {
        assertFailsWith<IllegalArgumentException> { TrainingCycle(cycle.weeks.take(7)) }
    }
}
