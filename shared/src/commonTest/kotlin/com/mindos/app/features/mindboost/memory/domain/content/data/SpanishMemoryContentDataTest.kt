package com.mindos.app.features.mindboost.memory.domain.content.data

import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpanishMemoryContentDataTest {

    private val words = SpanishMemoryContentData.wordPool
    private val pairs = SpanishMemoryContentData.pairPool

    @Test
    fun wordPool_meetsMinimum() {
        assertTrue(words.size >= WordPool.MIN_POOL_SIZE, "ES word pool below floor: ${words.size}")
    }

    @Test
    fun wordPool_coversEveryCategoryWithSeveralItems() {
        WordCategory.entries.forEach { category ->
            val count = words.byCategory(category).size
            assertTrue(count >= 10, "Category $category has only $count words (need >= 10).")
        }
    }

    @Test
    fun wordPool_hasBothConcreteAndAbstract() {
        assertTrue(words.byConcreteness(Concreteness.CONCRETE).size > 30)
        assertTrue(words.byConcreteness(Concreteness.ABSTRACT).size > 30)
    }

    @Test
    fun wordPool_representsAllThreeFrequencies() {
        WordFrequency.entries.forEach { freq ->
            assertTrue(words.byFrequency(freq).isNotEmpty(), "No words with frequency $freq.")
        }
    }

    @Test
    fun wordPool_lengthCharsMatchesText() {
        // Guards against hand-typed length drift (derived from text.length).
        assertTrue(words.items.all { it.lengthChars == it.text.length })
    }

    @Test
    fun pairPool_meetsMinimum() {
        assertTrue(pairs.size >= PairPool.MIN_POOL_SIZE, "ES pair pool below floor: ${pairs.size}")
    }

    @Test
    fun pairPool_hasNoSelfPairsOrDuplicateIds() {
        assertTrue(pairs.items.all { it.cue.id != it.target.id })
        assertEquals(pairs.size, pairs.ids().size)
    }

    @Test
    fun pairPool_referentialIntegrity_allWordsExistInWordPool() {
        val wordIds = words.ids()
        pairs.items.forEach { pair ->
            assertTrue(pair.cue.id in wordIds, "Pair ${pair.id} cue ${pair.cue.id} not in word pool.")
            assertTrue(pair.target.id in wordIds, "Pair ${pair.id} target ${pair.target.id} not in word pool.")
        }
    }
}
