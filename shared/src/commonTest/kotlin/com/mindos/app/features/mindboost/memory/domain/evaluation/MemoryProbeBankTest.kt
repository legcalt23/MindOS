package com.mindos.app.features.mindboost.memory.domain.evaluation

import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryProbeBankTest {

    @Test
    fun probeMustUseReservedContent() {
        assertFailsWith<IllegalArgumentException> {
            MemoryProbe(
                probeId = "x",
                subcapacity = MemorySubcapacity.WORKING_MEMORY,
                exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                fixedDifficulty = MemoryDifficultyVector.EASIEST,
                useReservedContent = false,
            )
        }
    }

    @Test
    fun probeRejectsBlankId() {
        assertFailsWith<IllegalArgumentException> {
            MemoryProbe(
                probeId = " ",
                subcapacity = MemorySubcapacity.WORKING_MEMORY,
                exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                fixedDifficulty = MemoryDifficultyVector.EASIEST,
            )
        }
    }

    @Test
    fun bankCoversV1Subcapacities() {
        assertNotNull(MemoryProbeBank.forSubcapacity(MemorySubcapacity.WORKING_MEMORY))
        assertNotNull(MemoryProbeBank.forSubcapacity(MemorySubcapacity.EPISODIC))
        assertNotNull(MemoryProbeBank.forSubcapacity(MemorySubcapacity.ASSOCIATIVE))
        assertEquals(3, MemoryProbeBank.all.size)
    }

    @Test
    fun allProbesAreReservedAndSameParadigmInV1() {
        assertTrue(MemoryProbeBank.all.all { it.useReservedContent })
        assertTrue(MemoryProbeBank.all.all { it.format == ProbeFormat.SAME_PARADIGM_RESERVED })
    }

    @Test
    fun probeIdsAreUnique() {
        assertEquals(MemoryProbeBank.all.size, MemoryProbeBank.all.map { it.probeId }.toSet().size)
    }

    @Test
    fun probeDifficultyIsFixedAboveFloor() {
        assertTrue(MemoryProbeBank.WORKING_MEMORY.fixedDifficulty.load.value > AxisLevel.MIN.value)
    }
}
