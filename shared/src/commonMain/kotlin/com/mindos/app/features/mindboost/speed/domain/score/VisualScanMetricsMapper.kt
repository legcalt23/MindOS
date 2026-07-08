package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics

/** Versión del mapper de métricas de E5 (para trazabilidad; se bumpea si cambia una fórmula). */
const val VISUAL_SCAN_METRICS_VERSION: String = "1.1.0-mc9c-r2"

/**
 * Métricas específicas de E5 · Búsqueda Visual, derivadas de la corrida. Cada campo es un [VisualScanSignal]:
 * [VisualScanSignal.Measured] solo si hay datos reales; en otro caso [VisualScanSignal.Unavailable] con la razón
 * exacta. NO es un score (el autoritativo sigue siendo el `SpeedScoreEngine` 0–1 preliminar): es análisis
 * descriptivo para feedback y adaptación futura.
 */
data class VisualScanMetrics(
    val totalTrials: Int,
    val accuracy: VisualScanSignal,
    val correctResponseSpeedMs: VisualScanSignal,
    val rtVariability: VisualScanSignal,
    val stabilityIndex: VisualScanSignal,
    val anticipationRate: VisualScanSignal,
    val prematureTapRate: VisualScanSignal,
    val fastWrongRate: VisualScanSignal,
    val speedAccuracyTradeoff: VisualScanSignal,
    val targetSideImbalance: VisualScanSignal,
    val sideResponseBias: VisualScanSignal,
    val loadAdjustedAccuracy: VisualScanSignal,
    val visualLoadTolerance: VisualScanSignal,
    val highLoadCost: VisualScanSignal,
    val loadAccuracySlope: VisualScanSignal,
    val similarityCost: VisualScanSignal,
    val omissionPressureIndex: VisualScanSignal,
    val version: String = VISUAL_SCAN_METRICS_VERSION,
)

/**
 * Deriva [VisualScanMetrics] de los outcomes reales + el conteo de taps prematuros. Puro y testeable. Reglas de
 * honestidad: sin muestra suficiente ⇒ `Unavailable`; señales que el flujo/persistencia V1 no capturan (similitud
 * con un solo nivel, omisiones) ⇒ `Unavailable` con razón estructural, NUNCA un 0 fabricado.
 */
// One pure derivation function per metric is the idiomatic shape here (not a code smell).
@Suppress("TooManyFunctions")
object VisualScanMetricsMapper {

    const val MIN_TRIALS = 6
    const val MIN_CORRECT = 4
    const val MIN_LOAD_BUCKET = 4

    private const val UNAVAILABLE_SIMILARITY = "fewer than 2 similarity levels in this run"
    private const val UNAVAILABLE_OMISSION = "no omissions in the tap-to-advance flow (late taps are commission errors)"
    private const val UNAVAILABLE_SAMPLE = "insufficient trials"
    private const val UNAVAILABLE_LOAD = "fewer than 2 load levels with enough trials in this run"
    private const val HALF = 0.5

    fun map(outcomes: List<VisualScanTrialOutcome>, prematureTaps: Int = 0): VisualScanMetrics =
        metrics(VisualScanRunSummary.from(outcomes, prematureTaps))

    fun metrics(summary: VisualScanRunSummary): VisualScanMetrics {
        val total = summary.totalTrials
        val enough = total >= MIN_TRIALS
        return VisualScanMetrics(
            totalTrials = total,
            accuracy = ratio(summary.correctTrials, total, enough),
            correctResponseSpeedMs = correctResponseSpeed(summary),
            rtVariability = rtVariability(summary),
            stabilityIndex = stability(summary),
            anticipationRate = ratio(summary.anticipations, total, enough),
            prematureTapRate = prematureTapRate(summary, enough),
            fastWrongRate = ratio(summary.fastWrong, total, enough),
            speedAccuracyTradeoff = ratio(summary.fastWrong, total, enough),
            targetSideImbalance = targetSideImbalance(summary, enough),
            sideResponseBias = sideResponseBias(summary),
            loadAdjustedAccuracy = loadAdjustedAccuracy(summary),
            visualLoadTolerance = loadTolerance(summary),
            highLoadCost = highLoadCost(summary),
            loadAccuracySlope = loadAccuracySlope(summary),
            similarityCost = similarityCost(summary),
            omissionPressureIndex = VisualScanSignal.Unavailable(UNAVAILABLE_OMISSION),
        )
    }

    private fun ratio(numerator: Int, denominator: Int, enough: Boolean): VisualScanSignal =
        if (!enough || denominator <= 0) {
            VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)
        } else {
            VisualScanSignal.Measured(numerator.toDouble() / denominator)
        }

    private fun prematureTapRate(summary: VisualScanRunSummary, enough: Boolean): VisualScanSignal {
        val attempts = summary.totalTrials + summary.prematureTaps
        return if (!enough || attempts <= 0) {
            VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)
        } else {
            VisualScanSignal.Measured(summary.prematureTaps.toDouble() / attempts)
        }
    }

    private fun correctResponseSpeed(summary: VisualScanRunSummary): VisualScanSignal {
        val median = SpeedRtStatistics.medianOrNull(summary.correctRtsMs)
        return if (summary.correctRtsMs.size < MIN_CORRECT || median == null) {
            VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)
        } else {
            VisualScanSignal.Measured(median)
        }
    }

    private fun cvOrNull(summary: VisualScanRunSummary): Double? {
        val rts = summary.correctRtsMs
        if (rts.size < MIN_CORRECT) return null
        val mean = SpeedRtStatistics.meanOrNull(rts)
        val sd = SpeedRtStatistics.standardDeviationOrNull(rts)
        return if (mean == null || sd == null || mean <= 0.0) null else sd / mean
    }

    private fun rtVariability(summary: VisualScanRunSummary): VisualScanSignal =
        cvOrNull(summary)?.let { VisualScanSignal.Measured(it) } ?: VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)

    private fun stability(summary: VisualScanRunSummary): VisualScanSignal =
        cvOrNull(summary)?.let { VisualScanSignal.Measured((1.0 - it).coerceIn(0.0, 1.0)) }
            ?: VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)

    /** targetSideImbalance = targetLeftRate − 0.5 (SESGO DE LOS OBJETIVOS, no del usuario). */
    private fun targetSideImbalance(summary: VisualScanRunSummary, enough: Boolean): VisualScanSignal =
        if (!enough || summary.totalTrials <= 0) {
            VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)
        } else {
            VisualScanSignal.Measured(summary.targetLeft.toDouble() / summary.totalTrials - HALF)
        }

    /** sideResponseBias = responseLeftRate − targetLeftRate (SESGO DEL USUARIO, corregido por la distribución). */
    private fun sideResponseBias(summary: VisualScanRunSummary): VisualScanSignal {
        if (summary.respondedTrials < MIN_TRIALS || summary.totalTrials <= 0) {
            return VisualScanSignal.Unavailable(UNAVAILABLE_SAMPLE)
        }
        val responseLeftRate = summary.respondedLeft.toDouble() / summary.respondedTrials
        val targetLeftRate = summary.targetLeft.toDouble() / summary.totalTrials
        return VisualScanSignal.Measured(responseLeftRate - targetLeftRate)
    }

    private fun qualifyingBuckets(summary: VisualScanRunSummary): List<VisualScanLoadBucket> =
        summary.byLoad.filter { it.trials >= MIN_LOAD_BUCKET }

    private fun loadAdjustedAccuracy(summary: VisualScanRunSummary): VisualScanSignal {
        val buckets = qualifyingBuckets(summary)
        return if (buckets.size < 2) {
            VisualScanSignal.Unavailable(UNAVAILABLE_LOAD)
        } else {
            VisualScanSignal.Measured(buckets.map { it.accuracy }.average())
        }
    }

    private fun loadTolerance(summary: VisualScanRunSummary): VisualScanSignal {
        val buckets = qualifyingBuckets(summary)
        return if (buckets.size < 2) {
            VisualScanSignal.Unavailable(UNAVAILABLE_LOAD)
        } else {
            VisualScanSignal.Measured(buckets.maxBy { it.itemsPerSide }.accuracy)
        }
    }

    /** highLoadCost = max(0, acc(cargaBaja) − acc(cargaAlta)) — un COSTE nunca es negativo (Regla #4). */
    private fun highLoadCost(summary: VisualScanRunSummary): VisualScanSignal {
        val buckets = qualifyingBuckets(summary)
        return if (buckets.size < 2) {
            VisualScanSignal.Unavailable(UNAVAILABLE_LOAD)
        } else {
            val low = buckets.minBy { it.itemsPerSide }.accuracy
            val high = buckets.maxBy { it.itemsPerSide }.accuracy
            VisualScanSignal.Measured(maxOf(0.0, low - high))
        }
    }

    /** loadAccuracySlope = acc(cargaAlta) − acc(cargaBaja) — con signo: positivo = mejora al subir la carga. */
    private fun loadAccuracySlope(summary: VisualScanRunSummary): VisualScanSignal {
        val buckets = qualifyingBuckets(summary)
        return if (buckets.size < 2) {
            VisualScanSignal.Unavailable(UNAVAILABLE_LOAD)
        } else {
            val low = buckets.minBy { it.itemsPerSide }.accuracy
            val high = buckets.maxBy { it.itemsPerSide }.accuracy
            VisualScanSignal.Measured(high - low)
        }
    }

    /**
     * similarityCost = max(0, acc(similitud baja) − acc(similitud alta)). Measured SOLO con ≥2 niveles de
     * similitud con muestra suficiente; en producción V1 (un solo nivel por corrida) queda Unavailable — el
     * mapper ya está listo para cuando una corrida mezcle niveles (§10). Un COSTE nunca es negativo (Regla #4).
     */
    private fun similarityCost(summary: VisualScanRunSummary): VisualScanSignal {
        val buckets = summary.bySimilarity.filter { it.trials >= MIN_LOAD_BUCKET }
        return if (buckets.size < 2) {
            VisualScanSignal.Unavailable(UNAVAILABLE_SIMILARITY)
        } else {
            val low = buckets.minBy { it.similarityOrdinal }.accuracy
            val high = buckets.maxBy { it.similarityOrdinal }.accuracy
            VisualScanSignal.Measured(maxOf(0.0, low - high))
        }
    }
}
