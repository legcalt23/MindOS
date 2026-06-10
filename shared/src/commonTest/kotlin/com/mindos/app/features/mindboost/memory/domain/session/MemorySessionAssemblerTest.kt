package com.mindos.app.features.mindboost.memory.domain.session

import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemorySessionAssemblerTest {

    private val assembler = MemorySessionAssembler()
    private val difficulty = MemoryDifficultyVector.EASIEST
        .adjust(MemoryDifficultyAxis.LOAD, AxisLevel(0.5))
        .adjust(MemoryDifficultyAxis.DELAY, AxisLevel(0.4))

    private fun phasesOf(plan: MemorySessionPlan) = plan.blocks.map { it.phase }

    @Test
    fun micro_hasOnlyWarmupAndEncoding() {
        val plan = assembler.assemble(
            MemorySessionTemplate.MICRO_5,
            focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            difficulty = difficulty,
        )
        assertEquals(
            listOf(MemorySessionPhase.WARMUP, MemorySessionPhase.DEEP_ENCODING),
            phasesOf(plan),
        )
    }

    @Test
    fun standard_includesInterferenceAndDeferredDelayed() {
        val plan = assembler.assemble(
            MemorySessionTemplate.STANDARD_10,
            focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            difficulty = difficulty,
            interference = MemoryExerciseId.EX4_PARES_ASOCIADOS,
        )
        assertTrue(MemorySessionPhase.INTERFERENCE in phasesOf(plan))
        val delayed = plan.blocks.first { it.phase == MemorySessionPhase.DELAYED_RECALL }
        assertTrue(delayed.deferred, "Delayed-recall block must be marked deferred.")
        val interference = plan.blocks.first { it.phase == MemorySessionPhase.INTERFERENCE }
        assertEquals(MemoryExerciseId.EX4_PARES_ASOCIADOS, interference.exerciseId)
    }

    @Test
    fun interferenceOmittedWhenNoExerciseProvided() {
        val plan = assembler.assemble(
            MemorySessionTemplate.STANDARD_10,
            focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            difficulty = difficulty,
            interference = null,
        )
        assertTrue(MemorySessionPhase.INTERFERENCE !in phasesOf(plan))
    }

    @Test
    fun warmupIsEasierThanMain() {
        val plan = assembler.assemble(
            MemorySessionTemplate.STANDARD_10,
            focus = MemoryExerciseId.EX1_SPAN_COMPLEJO,
            difficulty = difficulty,
        )
        val warmup = plan.blocks.first { it.phase == MemorySessionPhase.WARMUP }
        val main = plan.blocks.first { it.phase == MemorySessionPhase.DEEP_ENCODING }
        assertTrue(warmup.difficulty.load.value < main.difficulty.load.value)
    }

    @Test
    fun immediateBlockHasZeroDelay_delayedBlockHasRaisedDelay() {
        val plan = assembler.assemble(
            MemorySessionTemplate.FULL_20,
            focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            difficulty = difficulty,
            interference = MemoryExerciseId.EX4_PARES_ASOCIADOS,
        )
        val main = plan.blocks.first { it.phase == MemorySessionPhase.DEEP_ENCODING }
        val delayed = plan.blocks.first { it.phase == MemorySessionPhase.DELAYED_RECALL }
        assertEquals(0.0, main.difficulty.delay.value)
        assertTrue(delayed.difficulty.delay.value > 0.0)
    }

    @Test
    fun noTransferBlock_isProducedInV1() {
        val plan = assembler.assemble(
            MemorySessionTemplate.FULL_20,
            focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            difficulty = difficulty,
            interference = MemoryExerciseId.EX4_PARES_ASOCIADOS,
        )
        assertTrue(MemorySessionPhase.TRANSFER !in phasesOf(plan))
    }

    @Test
    fun interferenceEqualToFocus_throws() {
        assertFailsWith<IllegalArgumentException> {
            assembler.assemble(
                MemorySessionTemplate.STANDARD_10,
                focus = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                difficulty = difficulty,
                interference = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            )
        }
    }
}
