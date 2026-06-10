package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Ex3ListGeneratorTest {

    private val content = defaultMemoryContent()
    private val generator = Ex3ListGenerator()

    private fun list(d: MemoryDifficultyVector, seed: Long = 1): RecallListTrial =
        generator.generate(d, content, seed) as RecallListTrial

    @Test
    fun isDeterministicForSameSeed() {
        val d = MemoryDifficultyVector.EASIEST
        assertEquals(generator.generate(d, content, 4), generator.generate(d, content, 4))
    }

    @Test
    fun listLengthGrowsWithLoad() {
        val easy = list(MemoryDifficultyVector.EASIEST)
        val hard = list(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel.MAX))
        assertEquals(8, easy.studyList.size)
        assertTrue(hard.studyList.size > easy.studyList.size)
    }

    @Test
    fun retrievalAxisControlsRecallTypeAndCue() {
        val cued = list(MemoryDifficultyVector.EASIEST) // retrieval MIN ⇒ CUED
        assertEquals(RecallType.CUED, cued.recallType)
        assertNotNull(cued.cueCategory)

        val free = list(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.RETRIEVAL, AxisLevel.MAX))
        assertEquals(RecallType.FREE, free.recallType)
        assertNull(free.cueCategory)
    }

    @Test
    fun deeperEncodingDemandShortensStudyTime() {
        val shallow = list(MemoryDifficultyVector.EASIEST)
        val deep = list(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.DEPTH, AxisLevel.MAX))
        assertTrue(deep.studyMsPerItem < shallow.studyMsPerItem)
    }

    @Test
    fun delayAxisIncreasesDelay() {
        val immediate = list(MemoryDifficultyVector.EASIEST)
        val delayed = list(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.DELAY, AxisLevel.MAX))
        assertEquals(0L, immediate.delayMs)
        assertTrue(delayed.delayMs > immediate.delayMs)
    }

    @Test
    fun expectedEqualsStudyListAndExcludesReserved() {
        val reserved = content.transferProbePartition().reservedWords.map { it.id }.toSet()
        val trial = list(MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel.MAX))
        assertEquals(trial.studyList, trial.expected)
        assertTrue(trial.studyList.none { it.id in reserved })
    }
}
