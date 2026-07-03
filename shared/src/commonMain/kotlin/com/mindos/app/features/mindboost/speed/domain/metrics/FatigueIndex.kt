package com.mindos.app.features.mindboost.speed.domain.metrics

import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics

/**
 * Índice de Fatiga intra-sesión (§7), reutilizable por [MetricCalculator] (a nivel de
 * sesión) y por el staircase multi-señal de MVP-2 (REGLA 5 COOLDOWN), que lo recalcula
 * sobre la serie de bloques acumulados.
 *
 * Requiere >= [MIN_BLOCKS] bloques; con menos, **null** (y la regla de cooldown por fatiga
 * no puede dispararse). MVP: combinación estandarizada intra-sesión de tres señales —
 * caída de VCE, aumento del TR mediano y aumento de la tasa de lapsos a lo largo de los
 * bloques. Positivo = patrón de fatiga. La normalización por norma poblacional se difiere
 * a post-MVP; aquí la base es la propia serie de la corrida.
 *
 * Nota de consistencia del plan: el §6 (REGLA 5) lee `fatIdx` "de BlockMetrics", pero el §7
 * define `BlockMetrics.fatIdx = null` siempre (solo es computable a nivel de sesión con >=3
 * bloques). Se resuelve aquí: el staircase acumula los `BlockMetrics` que ve y calcula un
 * `fatIdx` corrido con esta función — disponible recién a partir del 3er bloque.
 */
object FatigueIndex {

    const val MIN_BLOCKS: Int = 3

    fun compute(orderedBlockMetrics: List<BlockMetrics>): Double? {
        if (orderedBlockMetrics.size < MIN_BLOCKS) return null
        val vceSeries = orderedBlockMetrics.mapNotNull { it.vce }
        val rtSeries = orderedBlockMetrics.mapNotNull { it.rtMedianNanos }
        val lapseSeries = orderedBlockMetrics.map { it.lapseRate }
        val vceDecline = -SpeedRtStatistics.standardizedDelta(vceSeries)
        val tauRise = SpeedRtStatistics.standardizedDelta(rtSeries)
        val lapseRise = SpeedRtStatistics.standardizedDelta(lapseSeries)
        return vceDecline + tauRise + lapseRise
    }
}
