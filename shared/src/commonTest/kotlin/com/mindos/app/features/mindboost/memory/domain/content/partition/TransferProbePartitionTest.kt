package com.mindos.app.features.mindboost.memory.domain.content.partition

import com.mindos.app.features.mindboost.memory.domain.content.ContentFixtures
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransferProbePartitionTest {

    private val wordPool = ContentFixtures.wordPool()
    private val pairPool = ContentFixtures.pairPool()

    @Test
    fun partition_wordsAreDisjoint() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1)
        val training = p.trainingWords.map { it.id }.toSet()
        val reserved = p.reservedWords.map { it.id }.toSet()
        assertTrue(training.intersect(reserved).isEmpty())
    }

    @Test
    fun partition_pairsAreDisjoint() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1)
        val training = p.trainingPairs.map { it.id }.toSet()
        val reserved = p.reservedPairs.map { it.id }.toSet()
        assertTrue(training.intersect(reserved).isEmpty())
    }

    @Test
    fun partition_isExhaustive() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1)
        assertEquals(wordPool.size, p.trainingWords.size + p.reservedWords.size)
        assertEquals(pairPool.size, p.trainingPairs.size + p.reservedPairs.size)
        val allWordIds = (p.trainingWords + p.reservedWords).map { it.id }.toSet()
        assertEquals(wordPool.ids(), allWordIds)
    }

    @Test
    fun partition_isDeterministicForSameSeed() {
        val a = TransferProbePartition.partition(wordPool, pairPool, seed = 99)
        val b = TransferProbePartition.partition(wordPool, pairPool, seed = 99)
        assertEquals(a.reservedWords.map { it.id }, b.reservedWords.map { it.id })
    }

    @Test
    fun reservedCount_matchesRatio() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1, wordReserveRatio = 0.20)
        assertEquals((wordPool.size * 0.20).toInt(), p.reservedWordCount)
    }

    @Test
    fun construction_withOverlap_throws() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1)
        assertFailsWith<IllegalArgumentException> {
            // Force overlap: put a training word into reserved too.
            p.copy(reservedWords = p.reservedWords + p.trainingWords.first())
        }
    }

    @Test
    fun trainingWordPool_excludesAllReserved() {
        val p = TransferProbePartition.partition(wordPool, pairPool, seed = 1)
        val trainingPool = p.trainingWordPool(version = 1)
        val reservedIds = p.reservedWords.map { it.id }.toSet()
        assertTrue(trainingPool.ids().intersect(reservedIds).isEmpty())
        assertEquals(Language.SPANISH_ES, trainingPool.language)
    }
}
