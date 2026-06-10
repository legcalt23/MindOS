package com.mindos.app.features.mindboost.memory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the taxonomy cardinalities to the functional design so an accidental
 * add/remove of an enum value is caught immediately (the taxonomy is the
 * backbone every other layer references).
 */
class MemoryTaxonomyTest {

    @Test
    fun twelveSubcapacities() {
        assertEquals(12, MemorySubcapacity.entries.size)
    }

    @Test
    fun sixModules() {
        assertEquals(6, MemoryModule.entries.size)
    }

    @Test
    fun sixteenExercises() {
        assertEquals(16, MemoryExerciseId.entries.size)
    }

    @Test
    fun eightDifficultyAxes() {
        assertEquals(8, MemoryDifficultyAxis.entries.size)
    }

    @Test
    fun enumNamesAreUnique() {
        assertTrue(MemorySubcapacity.entries.map { it.name }.toSet().size == 12)
        assertTrue(MemoryExerciseId.entries.map { it.name }.toSet().size == 16)
    }
}
