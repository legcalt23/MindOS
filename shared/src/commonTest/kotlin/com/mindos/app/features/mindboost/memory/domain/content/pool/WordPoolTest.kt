package com.mindos.app.features.mindboost.memory.domain.content.pool

import com.mindos.app.features.mindboost.memory.domain.content.ContentFixtures
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WordPoolTest {

    @Test
    fun belowMinimum_throwsWithVolumeMessage() {
        val tooFew = (1..(WordPool.MIN_POOL_SIZE - 1)).map { ContentFixtures.word(it) }
        val ex = assertFailsWith<IllegalArgumentException> {
            WordPool(version = 1, language = Language.SPANISH_ES, items = tooFew)
        }
        assertTrue(ex.message!!.contains("minimum"), "Message should explain the volume floor.")
    }

    @Test
    fun duplicateId_throws() {
        val items = (1..WordPool.MIN_POOL_SIZE).map { ContentFixtures.word(1) } // all same id
        assertFailsWith<IllegalArgumentException> {
            WordPool(version = 1, language = Language.SPANISH_ES, items = items)
        }
    }

    @Test
    fun byCategory_returnsOnlyThatCategory() {
        val pool = ContentFixtures.wordPool()
        val animals = pool.byCategory(WordCategory.ANIMALS)
        assertTrue(animals.isNotEmpty())
        assertTrue(animals.all { it.category == WordCategory.ANIMALS })
    }

    @Test
    fun byConcretenessAndFrequency_filter() {
        val pool = ContentFixtures.wordPool()
        assertTrue(pool.byConcreteness(Concreteness.ABSTRACT).all { it.concreteness == Concreteness.ABSTRACT })
        assertTrue(pool.byFrequency(WordFrequency.LOW).all { it.frequency == WordFrequency.LOW })
    }

    @Test
    fun excluding_removesExactlyThoseIds() {
        val pool = ContentFixtures.wordPool()
        val toRemove = pool.items.take(5).map { it.id }.toSet()
        val reduced = pool.excluding(toRemove)
        assertEquals(pool.size - 5, reduced.size)
        assertTrue(reduced.ids().none { it in toRemove })
    }

    @Test
    fun qualityReport_exposesGapHonestly() {
        val pool = ContentFixtures.wordPool()
        val report = pool.qualityReport()
        assertEquals(pool.size, report.totalItems)
        assertEquals(WordPool.SCIENTIFIC_IDEAL, report.recommendedMinimum)
        assertTrue(report.belowScientificIdeal, "A small fixture pool must be flagged below ideal.")
        assertEquals(pool.size, report.perCategoryCount.values.sum())
    }

    @Test
    fun ids_areUniqueAndComplete() {
        val pool = ContentFixtures.wordPool()
        assertEquals(pool.size, pool.ids().size)
    }
}
