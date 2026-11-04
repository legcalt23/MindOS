package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.features.mindboost.speed.domain.score.CognitiveProfileScore
import com.mindos.app.features.mindboost.speed.domain.score.SpeedMacroCategory

/** Una barra del dashboard: etiqueta + valor 0–1 (null = no disponible) + texto a mostrar. */
data class SpeedDashboardRow(val label: String, val value: Double?, val displayValue: String)

/**
 * Modelo de presentación PURO del Dashboard v1 (MVP-3). Mapea un [CognitiveProfileScore] a
 * texto y barras. Testeable sin Compose. [preliminary] viaja para que la pantalla muestre
 * SIEMPRE el aviso (no autoritativo). [ceilingNote] no nulo = el techo del §9 limitó el índice.
 */
data class SpeedDashboardModel(
    val rows: List<SpeedDashboardRow>,
    val patternLabel: String,
    val totalDisplay: String,
    val totalValue: Double?,
    val ceilingNote: String?,
    val preliminary: Boolean,
)

/** Mapea el score a un modelo de dashboard. Macro → etiqueta DESCRIPTIVA de la corrida. */
fun CognitiveProfileScore.toDashboardModel(): SpeedDashboardModel = SpeedDashboardModel(
    rows = listOf(
        SpeedDashboardRow(SpeedStrings.DASHBOARD_BAR_SPEED, usefulSpeed, percentOrNa(usefulSpeed)),
        SpeedDashboardRow(SpeedStrings.DASHBOARD_BAR_CONTROL, control, percentOrNa(control)),
        SpeedDashboardRow(SpeedStrings.DASHBOARD_BAR_STABILITY, stability, percentOrNa(stability)),
    ),
    patternLabel = patternLabel(macroCategory),
    totalDisplay = percentOrNa(total),
    totalValue = total,
    ceilingNote = if (ceilingPenaltyApplied) SpeedStrings.DASHBOARD_CEILING_NOTE else null,
    preliminary = preliminary,
)

private const val PERCENT = 100.0

private fun percentOrNa(value: Double?): String =
    value?.let { "${(it * PERCENT).toLong()}%" } ?: SpeedStrings.NOT_AVAILABLE

private fun patternLabel(category: SpeedMacroCategory): String = when (category) {
    SpeedMacroCategory.CALIBRATING -> SpeedStrings.PATTERN_CALIBRATING
    SpeedMacroCategory.FAST_IMPULSIVE -> SpeedStrings.PATTERN_FAST_IMPULSIVE
    SpeedMacroCategory.SLOW_PRECISE -> SpeedStrings.PATTERN_SLOW_PRECISE
    SpeedMacroCategory.VARIABLE -> SpeedStrings.PATTERN_VARIABLE
    SpeedMacroCategory.BALANCED -> SpeedStrings.PATTERN_BALANCED
}
