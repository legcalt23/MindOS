package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Ex1SpanGeneratorTest {

    private val content = defaultMemoryContent()
    private val generator = Ex1SpanGenerator()

    private fun memoranda(plan: MemoryTrialPlan): List<Memorandum> =
        (plan as ComplexSpanTrial).steps.filterIsInstance<Memorandum>()

    @Test
    fun isDeterministicForSameSeed() {
        val d = MemoryDifficultyVector.EASIEST
        assertEquals(
            generator.generate(d, content, seed = 7),
            generator.generate(d, content, seed = 7),
        )
    }

    @Test
    fun spanLengthGrowsWithLoad() {
        val easy = generator.generate(MemoryDifficultyVector.EASIEST, content, seed = 1)
        val hard = generator.generate(
            MemoryDifficultyVector.EASIEST.adjust(
                com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis.LOAD,
                AxisLevel.MAX,
            ),
            content,
            seed = 1,
        )
        assertTrue(memoranda(hard).size > memoranda(easy).size)
        assertEquals(2, memoranda(easy).size)
    }

    @Test
    fun retrievalAxisControlsRecallDirection() {
        val forward = generator.generate(MemoryDifficultyVector.EASIEST, content, seed = 3) as ComplexSpanTrial
        assertEquals(RecallType.SERIAL_FORWARD, forward.recallType)

        val backward = generator.generate(
            MemoryDifficultyVector.EASIEST.adjust(
                com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis.RETRIEVAL,
                AxisLevel.MAX,
            ),
            content,
            seed = 3,
        ) as ComplexSpanTrial
        assertEquals(RecallType.SERIAL_BACKWARD, backward.recallType)
        assertEquals(backward.expectedRecall, memoranda(backward).map { it.word }.reversed())
    }

    @Test
    fun hasInterleavedProcessingTasks() {
        val plan = generator.generate(MemoryDifficultyVector.EASIEST, content, seed = 5)
        assertIs<ComplexSpanTrial>(plan)
        assertTrue(plan.steps.any { it is ProcessingTask }, "Complex span must include processing tasks.")
    }

    @Test
    fun neverDrawsReservedProbes() {
        val reserved = content.transferProbePartition().reservedWords.map { it.id }.toSet()
        val plan = generator.generate(
            MemoryDifficultyVector.EASIEST.adjust(
                com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis.LOAD,
                AxisLevel.MAX,
            ),
            content,
            seed = 9,
        )
        assertTrue(memoranda(plan).none { it.word.id in reserved })
    }

    @Test
    fun respectsExclusion() {
        val first = memoranda(generator.generate(MemoryDifficultyVector.EASIEST, content, seed = 2)).map { it.word.id }
        val plan = generator.generate(
            MemoryDifficultyVector.EASIEST,
            content,
            seed = 2,
            excludeRecentWordIds = first.toSet(),
        )
        assertTrue(memoranda(plan).none { it.word.id in first.toSet() })
    }
}
