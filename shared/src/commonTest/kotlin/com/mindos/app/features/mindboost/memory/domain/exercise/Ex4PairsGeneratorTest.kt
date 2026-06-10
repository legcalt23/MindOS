package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Ex4PairsGeneratorTest {

    private val content = defaultMemoryContent()
    private val generator = Ex4PairsGenerator()

    private fun trial(d: MemoryDifficultyVector, seed: Long = 1): PairedAssociatesTrial =
        generator.generate(d, content, seed) as PairedAssociatesTrial

    @Test
    fun isDeterministicForSameSeed() {
        val d = MemoryDifficultyVector.EASIEST
        assertEquals(generator.generate(d, content, 6), generator.generate(d, content, 6))
    }

    @Test
    fun pairCountGrowsWithLoad() {
        val easy = trial(MemoryDifficultyVector.EASIEST)
        val hard = trial(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel.MAX))
        assertEquals(4, easy.studyPairs.size)
        assertTrue(hard.studyPairs.size > easy.studyPairs.size)
    }

    @Test
    fun unidirectionalHasOneProbePerPair() {
        val uni = trial(MemoryDifficultyVector.EASIEST) // retrieval MIN ⇒ unidirectional
        assertEquals(uni.studyPairs.size, uni.probes.size)
        assertTrue(uni.probes.all { it.direction == RecallDirection.FORWARD })
    }

    @Test
    fun bidirectionalHasTwoProbesPerPair() {
        val bi = trial(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.RETRIEVAL, AxisLevel.MAX))
        assertEquals(bi.studyPairs.size * 2, bi.probes.size)
        assertTrue(bi.probes.any { it.direction == RecallDirection.BACKWARD })
    }

    @Test
    fun probeExpectedTargetsAreConsistentWithStudiedPairs() {
        val t = trial(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.RETRIEVAL, AxisLevel.MAX))
        val forwardByCue = t.studyPairs.associate { it.cue.id to it.target.id }
        val backwardByCue = t.studyPairs.associate { it.target.id to it.cue.id }
        t.probes.forEach { probe ->
            val expected = when (probe.direction) {
                RecallDirection.FORWARD -> forwardByCue[probe.cue.id]
                RecallDirection.BACKWARD -> backwardByCue[probe.cue.id]
            }
            assertEquals(expected, probe.expectedTarget.id)
        }
    }

    @Test
    fun similarityHighSelectsWeakPairs() {
        val t = trial(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.SIMILARITY, AxisLevel.MAX))
        assertTrue(
            t.studyPairs.all {
                it.associationStrength ==
                    com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength.WEAK
            },
        )
    }

    @Test
    fun neverDrawsReservedPairs() {
        val reserved = content.transferProbePartition().reservedPairs.map { it.id }.toSet()
        val t = trial(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel.MAX))
        assertTrue(t.studyPairs.none { it.id in reserved })
    }
}
