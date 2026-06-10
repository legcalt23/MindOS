package com.mindos.app.features.mindboost.memory.domain.evaluation

import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import com.mindos.app.features.mindboost.memory.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryBaselineProtocolTest {

    private val initial = MemoryBaselineProtocols.INITIAL

    @Test
    fun includesAtLeastOneReservedProbe() {
        assertTrue(initial.blocks.any { it.isProbe })
    }

    @Test
    fun neverAdapts() {
        assertTrue(initial.blocks.none { it.mode == Mode.TRAINING })
    }

    @Test
    fun calibrationExercisesExistInCatalogue() {
        assertTrue(MemoryBaselineProtocols.calibrationExercises.isNotEmpty())
        assertTrue(MemoryBaselineProtocols.calibrationExercises.all { it in MemoryExerciseCatalog.all.keys })
    }

    @Test
    fun coversV1Subcapacities() {
        val subs = initial.blocks.map { it.subcapacity }.toSet()
        assertTrue(MemorySubcapacity.WORKING_MEMORY in subs)
        assertTrue(MemorySubcapacity.EPISODIC in subs)
        assertTrue(MemorySubcapacity.ASSOCIATIVE in subs)
    }

    @Test
    fun blockMustBeExactlyExerciseOrProbe() {
        assertFailsWith<IllegalArgumentException> {
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.WORKING_MEMORY,
                exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                probe = MemoryProbeBank.WORKING_MEMORY, // both set → invalid
                fixedDifficulty = MemoryDifficultyVector.EASIEST,
                mode = Mode.CALIBRATION,
            )
        }
    }

    @Test
    fun trainingModeBlockRejected() {
        assertFailsWith<IllegalArgumentException> {
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.WORKING_MEMORY,
                exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                probe = null,
                fixedDifficulty = MemoryDifficultyVector.EASIEST,
                mode = Mode.TRAINING,
            )
        }
    }

    @Test
    fun protocolWithoutProbeRejected() {
        assertFailsWith<IllegalArgumentException> {
            MemoryBaselineProtocol(
                blocks = listOf(
                    MemoryBaselineBlock(
                        subcapacity = MemorySubcapacity.WORKING_MEMORY,
                        exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                        probe = null,
                        fixedDifficulty = MemoryDifficultyVector.EASIEST,
                        mode = Mode.CALIBRATION,
                    ),
                ),
                stateQuestionnaire = MemoryStateQuestionnaire(),
            )
        }
    }
}
