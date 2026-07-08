package com.mindos.app.features.mindboost.speed.domain.score

import kotlinx.serialization.Serializable

/**
 * MC9-D — versión del ESQUEMA de este summary E8. Ortogonal a `metricsVersion` (versión de las FÓRMULAS del
 * mapper) y a `ExerciseTelemetryRecord.schemaVersion`/`telemetryVersion` (esquema/formato del record genérico).
 */
const val GONOGO_TELEMETRY_SUMMARY_VERSION: String = "1.0.0-mc9d"

/**
 * MC9-D — resumen PERSISTIBLE de una corrida de E8 · Go/No-Go Velocity. Cada métrica es un [GoNoGoSignal]
 * (Measured/Unavailable) — nunca un 0 fabricado. Se serializa a JSON y se guarda por sesión en el store de
 * telemetría genérico (`ExerciseTelemetryStore`, namespace `mindboost_speed` ya cubierto por el borrado de cuenta),
 * SIN migrar `TrialRecord`. `unavailableSignals` lista las señales no disponibles para escaneo rápido.
 * [summaryVersion] versiona ESTE esquema; [metricsVersion] las fórmulas del mapper (ejes independientes).
 */
@Serializable
data class GoNoGoTelemetrySummaryV1(
    val sessionId: String,
    val exerciseId: String,
    val totalTrials: Int,
    val goTrials: Int,
    val noGoTrials: Int,
    val goAccuracy: GoNoGoSignal,
    val noGoAccuracy: GoNoGoSignal,
    val commissionRate: GoNoGoSignal,
    val omissionRate: GoNoGoSignal,
    val anticipationRate: GoNoGoSignal,
    val lateRate: GoNoGoSignal,
    val correctGoRtMedianMs: GoNoGoSignal,
    val correctGoRtVariability: GoNoGoSignal,
    val inhibitoryControlIndex: GoNoGoSignal,
    val speedAccuracyTradeoff: GoNoGoSignal,
    val stabilityIndex: GoNoGoSignal,
    val difficultyAdjustedControl: GoNoGoSignal,
    val diagnostics: GoNoGoDiagnostics,
    val unavailableSignals: List<String>,
    val metricsVersion: String,
    val summaryVersion: String = GONOGO_TELEMETRY_SUMMARY_VERSION,
    val createdAtEpochMs: Long,
) {
    companion object {
        @Suppress("LongParameterList") // A telemetry summary aggregates many named signals; that width is the point.
        fun of(
            sessionId: String,
            exerciseId: String,
            metrics: GoNoGoMetrics,
            summary: GoNoGoRunSummary,
            diagnostics: GoNoGoDiagnostics,
            createdAtEpochMs: Long,
        ): GoNoGoTelemetrySummaryV1 {
            val lateRate = lateRate(summary)
            val named = listOf(
                "goAccuracy" to metrics.goAccuracy,
                "noGoAccuracy" to metrics.noGoAccuracy,
                "commissionRate" to metrics.commissionRate,
                "omissionRate" to metrics.omissionRate,
                "anticipationRate" to metrics.anticipationRate,
                "lateRate" to lateRate,
                "correctGoRtMedianMs" to metrics.correctGoSpeedMs,
                "correctGoRtVariability" to metrics.correctGoRtVariability,
                "inhibitoryControlIndex" to metrics.inhibitoryControlIndex,
                "speedAccuracyTradeoff" to metrics.speedAccuracyTradeoff,
                "stabilityIndex" to metrics.stabilityIndex,
                "difficultyAdjustedControl" to metrics.difficultyAdjustedControl,
            )
            return GoNoGoTelemetrySummaryV1(
                sessionId = sessionId,
                exerciseId = exerciseId,
                totalTrials = summary.totalTrials,
                goTrials = summary.goTrials,
                noGoTrials = summary.noGoTrials,
                goAccuracy = metrics.goAccuracy,
                noGoAccuracy = metrics.noGoAccuracy,
                commissionRate = metrics.commissionRate,
                omissionRate = metrics.omissionRate,
                anticipationRate = metrics.anticipationRate,
                lateRate = lateRate,
                correctGoRtMedianMs = metrics.correctGoSpeedMs,
                correctGoRtVariability = metrics.correctGoRtVariability,
                inhibitoryControlIndex = metrics.inhibitoryControlIndex,
                speedAccuracyTradeoff = metrics.speedAccuracyTradeoff,
                stabilityIndex = metrics.stabilityIndex,
                difficultyAdjustedControl = metrics.difficultyAdjustedControl,
                diagnostics = diagnostics,
                unavailableSignals = named.filter { it.second is GoNoGoSignal.Unavailable }.map { it.first },
                metricsVersion = metrics.version,
                createdAtEpochMs = createdAtEpochMs,
            )
        }

        /** lateRate = respuestas GO tardías / trials GO — Measured solo con muestra GO suficiente. */
        private fun lateRate(summary: GoNoGoRunSummary): GoNoGoSignal =
            if (summary.goTrials >= GoNoGoMetricsMapper.MIN_GO && summary.goTrials > 0) {
                GoNoGoSignal.Measured(summary.lateResponses.toDouble() / summary.goTrials)
            } else {
                GoNoGoSignal.Unavailable("insufficient GO trials")
            }
    }
}
