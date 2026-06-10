package com.mindos.app.features.mindboost.memory.domain.content

import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryContentTest {

    private val content = defaultMemoryContent()

    @Test
    fun supportedExercises_areTheV1FirstWave() {
        assertEquals(
            setOf(
                MemoryExerciseId.EX1_SPAN_COMPLEJO,
                MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                MemoryExerciseId.EX4_PARES_ASOCIADOS,
            ),
            content.supportedExercises(),
        )
    }

    @Test
    fun supportedLanguages_isSpanishOnly() {
        assertEquals(setOf(Language.SPANISH_ES), content.supportedLanguages())
    }

    @Test
    fun trainingWordPool_excludesEveryReservedProbe() {
        val reserved = content.transferProbePartition().reservedWords.map { it.id }.toSet()
        val training = content.trainingWordPool().ids()
        assertTrue(training.intersect(reserved).isEmpty(), "Training must never contain a reserved probe.")
    }

    @Test
    fun samplerFor_supportedExercise_drawsOnlyFromTraining() {
        val reserved = content.transferProbePartition().reservedWords.map { it.id }.toSet()
        val sample = content.samplerFor(MemoryExerciseId.EX1_SPAN_COMPLEJO).sampleWords(count = 20, seed = 1)
        assertTrue(sample.none { it.id in reserved })
    }

    @Test
    fun samplerFor_unsupportedExercise_throws() {
        assertFailsWith<IllegalArgumentException> {
            content.samplerFor(MemoryExerciseId.EX5_GRID_ESPACIAL)
        }
    }

    @Test
    fun trainingWordPool_stillMeetsMinimumAfterReserve() {
        assertTrue(content.trainingWordPool().size >= WordPool.MIN_POOL_SIZE)
    }

    @Test
    fun defaultMemoryContent_constructsWithoutThrowing() {
        // Constructing the production content proves the ES pools satisfy all invariants.
        defaultMemoryContent()
    }
}
