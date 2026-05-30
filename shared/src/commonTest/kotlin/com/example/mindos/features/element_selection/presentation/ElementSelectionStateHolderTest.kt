package com.example.mindos.features.element_selection.presentation

import com.example.mindos.features.element_selection.domain.ElementMasterCatalog
import com.example.mindos.features.element_selection.domain.ElementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ElementSelectionStateHolderTest {
    @Test
    fun tappingAnIdleOrbExpandsThatElement() {
        val holder = ElementSelectionStateHolder()
        val gravity = ElementMasterCatalog.masters.first { it.type == ElementType.Gravity }

        holder.onOrbTapped(gravity)

        val expanded = assertIs<SelectionState.Expanded>(holder.uiState.selection)
        assertEquals(gravity, expanded.element)
    }

    @Test
    fun tappingTheExpandedOrbTriggersImpactWithoutCollapsing() {
        val holder = ElementSelectionStateHolder()
        val light = ElementMasterCatalog.masters.first { it.type == ElementType.Light }

        holder.onOrbTapped(light)
        holder.onOrbTapped(light)

        val expanded = assertIs<SelectionState.Expanded>(holder.uiState.selection)
        assertEquals(light, expanded.element)
        assertEquals(1, holder.uiState.impactSeed)
    }

    @Test
    fun tappingOutsideReturnsToIdleAndClearsIntensity() {
        val holder = ElementSelectionStateHolder()
        val lightning = ElementMasterCatalog.masters.first { it.type == ElementType.Lightning }

        holder.onLongPressStarted(lightning)
        holder.onOutsideTapped()

        assertEquals(SelectionState.Idle, holder.uiState.selection)
        assertNull(holder.uiState.intenseElement)
    }
}
