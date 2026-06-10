package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory.ANIMALS
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory.FOODS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArcClusteringTest {

    @Test
    fun perfectClustering_isOne() {
        val cats = listOf(ANIMALS, ANIMALS, ANIMALS, FOODS, FOODS, FOODS)
        assertEquals(1.0, ArcClustering.arcIndex(cats)!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun strictAlternation_isBelowChance() {
        val cats = listOf(ANIMALS, FOODS, ANIMALS, FOODS, ANIMALS, FOODS)
        val arc = ArcClustering.arcIndex(cats)!!
        assertTrue(arc < 0.0, "Strict alternation is below-chance clustering (was $arc).")
    }

    @Test
    fun singleCategory_isUndefined() {
        assertNull(ArcClustering.arcIndex(listOf(ANIMALS, ANIMALS, ANIMALS)))
    }

    @Test
    fun tooFewItems_isUndefined() {
        assertNull(ArcClustering.arcIndex(listOf(ANIMALS)))
    }
}
