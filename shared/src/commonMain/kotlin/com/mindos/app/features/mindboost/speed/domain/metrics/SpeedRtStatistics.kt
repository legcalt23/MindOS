// Constantes estadísticas documentadas en cada uso; guard-clauses con varios return.
@file:Suppress("MagicNumber", "ReturnCount")

package com.mindos.app.features.mindboost.speed.domain.metrics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Primitivas estadísticas robustas para la capa de métricas de Velocidad Mental.
 * Funciones puras, sin dependencia de plataforma. Espejo deliberado de la convención
 * ya probada en `focus` (mediana sobre media, MAD sobre SD para outliers), pero local
 * a este feature porque la versión de `focus` es `internal` a su paquete.
 *
 * Importante (regla de proyecto, CLAUDE.md 2026-06-04): el rechazo de outliers (MAD)
 * se aplica SOLO a métricas de tendencia central (mediana, media, CV). Los eventos
 * extremos por definición (lapsos) se cuentan sobre datos crudos.
 */
internal object SpeedRtStatistics {

    /** Factor que hace del MAD un estimador consistente de la SD bajo normalidad. */
    private const val MAD_SCALE = 1.4826

    /** Mediana. Devuelve null para lista vacía (el llamador decide el significado). */
    fun medianOrNull(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /** Media. Devuelve null para lista vacía. */
    fun meanOrNull(values: List<Double>): Double? =
        if (values.isEmpty()) null else values.sum() / values.size

    /** Desviación estándar muestral (n-1). Devuelve null con menos de 2 valores. */
    fun standardDeviationOrNull(values: List<Double>): Double? {
        if (values.size < 2) return null
        val mean = values.sum() / values.size
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    /** MAD escalado (1.4826). Robusto ante la cola derecha típica de los TR. */
    fun scaledMad(values: List<Double>): Double {
        val med = medianOrNull(values) ?: return 0.0
        val deviations = values.map { abs(it - med) }
        return (medianOrNull(deviations) ?: 0.0) * MAD_SCALE
    }

    /**
     * Elimina outliers de TR vía MAD: |x - mediana| / madEscalado > [zCutoff] se descartan.
     * Con menos de [minForFiltering] valores devuelve la entrada sin tocar (estimado
     * demasiado pequeño para filtrar de forma fiable).
     */
    fun rejectOutliers(values: List<Double>, zCutoff: Double = 3.0, minForFiltering: Int = 4): List<Double> {
        if (values.size < minForFiltering) return values
        val med = medianOrNull(values) ?: return values
        val mad = scaledMad(values)
        // MAD 0 (mayoría idénticos) no puede escalar desviaciones — se conservan.
        return if (mad == 0.0) values else values.filter { abs(it - med) / mad <= zCutoff }
    }

    /**
     * Pendiente de la regresión lineal simple de [ys] sobre [xs] (mínimos cuadrados).
     * Devuelve 0.0 con menos de 2 puntos o varianza nula en x.
     */
    fun linearSlope(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size < 2 || xs.size != ys.size) return 0.0
        val meanX = xs.sum() / xs.size
        val meanY = ys.sum() / ys.size
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            num += dx * (ys[i] - meanY)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
    }

    /**
     * Z-estandarización del último punto de una serie respecto a su propia media/SD
     * (z intra-serie). Devuelve 0.0 si la serie es demasiado corta o tiene SD nula.
     * Base de normalización población-norma diferida a post-MVP.
     */
    fun standardizedDelta(series: List<Double>): Double {
        val sd = standardDeviationOrNull(series) ?: return 0.0
        if (sd == 0.0) return 0.0
        val mean = series.sum() / series.size
        return (series.last() - mean) / sd
    }
}
