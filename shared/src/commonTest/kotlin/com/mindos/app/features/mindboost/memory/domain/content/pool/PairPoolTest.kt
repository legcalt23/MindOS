package com.mindos.app.features.mindboost.memory.domain.content.pool

import com.mindos.app.features.mindboost.memory.domain.content.ContentFixtures
import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PairPoolTest {

    @Test
    fun belowMinimum_throws() {
        val pool = ContentFixtures.pairPool()
        val tooFew = pool.items.take(PairPool.MIN_POOL_SIZE - 1)
        assertFailsWith<IllegalArgumentException> {
            PairPool(version = 1, language = Language.SPANISH_ES, items = tooFew)
        }
    }

    @Test
    fun duplicatePairId_throws() {
        val pool = ContentFixtures.pairPool()
        val dup = pool.items.map { it.copy(id = PairId("same")) }
        assertFailsWith<IllegalArgumentException> {
            PairPool(version = 1, language = Language.SPANISH_ES, items = dup)
        }
    }

    @Test
    fun byStrength_filters() {
        val pool = ContentFixtures.pairPool()
        val weak = pool.byStrength(AssociationStrength.WEAK)
        assertTrue(weak.all { it.associationStrength == AssociationStrength.WEAK })
    }

    @Test
    fun excluding_removesExactlyThosePairs() {
        val pool = ContentFixtures.pairPool()
        val toRemove = pool.items.take(3).map { it.id }.toSet()
        val reduced = pool.excluding(toRemove)
        assertEquals(pool.size - 3, reduced.size)
        assertTrue(reduced.ids().none { it in toRemove })
    }

    @Test
    fun qualityReport_exposesStrengthAndGap() {
        val pool = ContentFixtures.pairPool()
        val report = pool.qualityReport()
        assertTrue(report.belowScientificIdeal)
        assertEquals(pool.size, report.perStrengthCount.values.sum())
    }
}
