// score() es un fixture amplio para construir CognitiveProfileScore en los tests.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.features.mindboost.speed.domain.score.CognitiveProfileScore
import com.mindos.app.features.mindboost.speed.domain.score.SpeedMacroCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedDashboardModelTest {

    private fun score(
        usefulSpeed: Double? = 0.6,
        control: Double? = 0.7,
        stability: Double? = 0.8,
        total: Double? = 0.7,
        macro: SpeedMacroCategory = SpeedMacroCategory.BALANCED,
        ceiling: Boolean = false,
        preliminary: Boolean = true,
    ) = CognitiveProfileScore(usefulSpeed, control, stability, total, macro, ceiling, preliminary = preliminary)

    @Test
    fun mapsThreeBarsWithLabels() {
        val model = score().toDashboardModel()
        assertEquals(3, model.rows.size)
        assertEquals(SpeedStrings.DASHBOARD_BAR_SPEED, model.rows[0].label)
        assertEquals(SpeedStrings.DASHBOARD_BAR_CONTROL, model.rows[1].label)
        assertEquals(SpeedStrings.DASHBOARD_BAR_STABILITY, model.rows[2].label)
    }

    @Test
    fun formatsPercent_andNotAvailable() {
        val model = score(usefulSpeed = 0.5, stability = null).toDashboardModel()
        assertEquals("50%", model.rows[0].displayValue)
        assertEquals(SpeedStrings.NOT_AVAILABLE, model.rows[2].displayValue)
    }

    @Test
    fun alwaysCarriesPreliminaryFlag() {
        assertTrue(score(preliminary = true).toDashboardModel().preliminary)
    }

    @Test
    fun ceilingNote_presentOnlyWhenPenalized() {
        assertNull(score(ceiling = false).toDashboardModel().ceilingNote)
        assertEquals(SpeedStrings.DASHBOARD_CEILING_NOTE, score(ceiling = true).toDashboardModel().ceilingNote)
    }

    @Test
    fun mapsEveryMacroCategoryToADescriptiveLabel() {
        val labels = SpeedMacroCategory.entries.map { score(macro = it).toDashboardModel().patternLabel }
        // Todas mapean a una etiqueta no vacía y distinta de las demás.
        assertEquals(SpeedMacroCategory.entries.size, labels.toSet().size)
        assertTrue(labels.all { it.isNotBlank() })
    }

    @Test
    fun patternLabel_isDescriptive_notDiagnostic() {
        // Ninguna etiqueta debe afirmar algo sobre la persona (p. ej. "tienes", "eres").
        val forbidden = listOf("tienes", "eres", "diagn", "trastor" + "no", "déficit", "td" + "ah")
        SpeedMacroCategory.entries.forEach { cat ->
            val label = score(macro = cat).toDashboardModel().patternLabel.lowercase()
            assertTrue(forbidden.none { label.contains(it) }, "etiqueta no descriptiva: $label")
        }
    }
}
