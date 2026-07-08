package com.mindos.app.features.mindboost.speed.domain.score

import kotlinx.serialization.Serializable

/**
 * MC9-C-R2 — resumen PERSISTIBLE de una corrida de E5 · Búsqueda Visual. Cada métrica es un [VisualScanSignal]
 * (Measured/Unavailable) — nunca un 0 fabricado. Este es el "run summary" histórico (§2): se serializa a JSON y
 * se guarda por sesión en el store de telemetría (sobre el namespace `mindboost_speed` ya cubierto por el borrado
 * de cuenta), SIN migrar `TrialRecord`. `unavailableSignals` lista los nombres de las señales no disponibles para
 * escaneo rápido.
 */
@Serializable
data class VisualScanTelemetrySummaryV1(
    val sessionId: String,
    val exerciseId: String,
    val totalTrials: Int,
    val correctTrials: Int,
    val accuracy: VisualScanSignal,
    val correctResponseSpeedMs: VisualScanSignal,
    val rtVariability: VisualScanSignal,
    val stabilityIndex: VisualScanSignal,
    val anticipationRate: VisualScanSignal,
    val prematureTapRate: VisualScanSignal,
    val fastWrongRate: VisualScanSignal,
    val targetSideImbalance: VisualScanSignal,
    val sideResponseBias: VisualScanSignal,
    val loadAdjustedAccuracy: VisualScanSignal,
    val visualLoadTolerance: VisualScanSignal,
    val highLoadCost: VisualScanSignal,
    val loadAccuracySlope: VisualScanSignal,
    val similarityCost: VisualScanSignal,
    val omissionPressureIndex: VisualScanSignal,
    val byLoad: List<VisualScanLoadBucket>,
    val diagnostics: VisualScanDiagnostics,
    val unavailableSignals: List<String>,
    val metricsVersion: String,
    val createdAtEpochMs: Long,
) {
    companion object {
        @Suppress("LongParameterList") // A telemetry summary aggregates many named signals; that width is the point.
        fun of(
            sessionId: String,
            exerciseId: String,
            metrics: VisualScanMetrics,
            summary: VisualScanRunSummary,
            diagnostics: VisualScanDiagnostics,
            createdAtEpochMs: Long,
        ): VisualScanTelemetrySummaryV1 {
            val named = listOf(
                "accuracy" to metrics.accuracy,
                "correctResponseSpeedMs" to metrics.correctResponseSpeedMs,
                "rtVariability" to metrics.rtVariability,
                "stabilityIndex" to metrics.stabilityIndex,
                "anticipationRate" to metrics.anticipationRate,
                "prematureTapRate" to metrics.prematureTapRate,
                "fastWrongRate" to metrics.fastWrongRate,
                "targetSideImbalance" to metrics.targetSideImbalance,
                "sideResponseBias" to metrics.sideResponseBias,
                "loadAdjustedAccuracy" to metrics.loadAdjustedAccuracy,
                "visualLoadTolerance" to metrics.visualLoadTolerance,
                "highLoadCost" to metrics.highLoadCost,
                "loadAccuracySlope" to metrics.loadAccuracySlope,
                "similarityCost" to metrics.similarityCost,
                "omissionPressureIndex" to metrics.omissionPressureIndex,
            )
            return VisualScanTelemetrySummaryV1(
                sessionId = sessionId,
                exerciseId = exerciseId,
                totalTrials = summary.totalTrials,
                correctTrials = summary.correctTrials,
                accuracy = metrics.accuracy,
                correctResponseSpeedMs = metrics.correctResponseSpeedMs,
                rtVariability = metrics.rtVariability,
                stabilityIndex = metrics.stabilityIndex,
                anticipationRate = metrics.anticipationRate,
                prematureTapRate = metrics.prematureTapRate,
                fastWrongRate = metrics.fastWrongRate,
                targetSideImbalance = metrics.targetSideImbalance,
                sideResponseBias = metrics.sideResponseBias,
                loadAdjustedAccuracy = metrics.loadAdjustedAccuracy,
                visualLoadTolerance = metrics.visualLoadTolerance,
                highLoadCost = metrics.highLoadCost,
                loadAccuracySlope = metrics.loadAccuracySlope,
                similarityCost = metrics.similarityCost,
                omissionPressureIndex = metrics.omissionPressureIndex,
                byLoad = summary.byLoad,
                diagnostics = diagnostics,
                unavailableSignals = named.filter { it.second is VisualScanSignal.Unavailable }.map { it.first },
                metricsVersion = metrics.version,
                createdAtEpochMs = createdAtEpochMs,
            )
        }
    }
}
