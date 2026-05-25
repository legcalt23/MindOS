package com.example.neuv3.features.element_selection.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElementMasterCatalogTest {
    @Test
    fun catalogContainsTheFiveSanctuaryMastersWithLightAtCenter() {
        val masters = ElementMasterCatalog.masters

        assertEquals(5, masters.size)
        assertEquals(ElementType.Light, masters[2].type)
        assertEquals(
            listOf(
                ElementType.Gravity,
                ElementType.Shadow,
                ElementType.Light,
                ElementType.Lightning,
                ElementType.Air,
            ),
            masters.map { it.type },
        )
        assertTrue(masters.all { it.skyLore.length > 120 })
    }
}
