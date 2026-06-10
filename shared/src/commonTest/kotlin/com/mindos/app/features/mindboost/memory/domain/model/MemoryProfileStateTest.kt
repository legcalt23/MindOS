package com.mindos.app.features.mindboost.memory.domain.model

import com.mindos.app.features.mindboost.memory.domain.personalization.MemoryArchetype
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryProfileStateTest {

    @Test
    fun initial_isBeginnerWeekOne() {
        val s = MemoryProfileState.initial("u1")
        assertEquals(MemoryArchetype.BEGINNER, s.archetype)
        assertEquals(1, s.currentCycleWeek)
        assertTrue(s.difficultyByExercise.isEmpty())
    }

    @Test
    fun difficultyFor_fallsBackToEasiest() {
        val s = MemoryProfileState.initial("u1")
        assertEquals(MemoryDifficultyVector.EASIEST, s.difficultyFor(MemoryExerciseId.EX1_SPAN_COMPLEJO))
    }

    @Test
    fun withDifficulty_isCopyOnWrite() {
        val s = MemoryProfileState.initial("u1")
        val raised = MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel(0.5))
        val updated = s.withDifficulty(MemoryExerciseId.EX1_SPAN_COMPLEJO, raised)
        assertEquals(raised, updated.difficultyFor(MemoryExerciseId.EX1_SPAN_COMPLEJO))
        assertTrue(s.difficultyByExercise.isEmpty(), "Original must be unchanged.")
    }

    @Test
    fun rejectsBlankUserOrZeroWeek() {
        assertFailsWith<IllegalArgumentException> { MemoryProfileState.initial(" ") }
        assertFailsWith<IllegalArgumentException> {
            MemoryProfileState("u1", MemoryArchetype.BEGINNER, CmeSensitivity.DEFAULT, emptyMap(), currentCycleWeek = 0)
        }
    }

    @Test
    fun summaryRejectsOutOfRangePerformance() {
        assertFailsWith<IllegalArgumentException> {
            MemorySessionSummary("u1:1", "u1", MemoryExerciseId.EX3_LISTA_ESTRATEGIA, 1, 0.7, 0.6, performance = 1.5)
        }
    }

    @Test
    fun summaryAllowsNullAccuracies() {
        val s = MemorySessionSummary("u1:1", "u1", MemoryExerciseId.EX1_SPAN_COMPLEJO, 1, null, null, 0.5)
        assertEquals(null, s.delayedAccuracy)
    }
}
