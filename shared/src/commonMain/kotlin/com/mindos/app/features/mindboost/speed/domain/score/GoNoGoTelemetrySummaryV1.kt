package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics
import kotlinx.serialization.Serializable

/**
 * MC9-D — versión del ESQUEMA de este summary E8. Ortogonal a `metricsVersion` (versión de las FÓRMULAS del
 * mapper) y a `ExerciseTelemetryRecord.schemaVersion`/`telemetryVersion` (esquema/formato del record genérico).
 * v1.2.0-mc9d-r3: + conteos crudos, distribución de RT completa y el resultado SCORED (índice + elegibilidad),
 * de modo que la corrida es reconstruible desde persistencia y no solo un proyectado de rates.
 */
const val GONOGO_TELEMETRY_SUMMARY_VERSION: String = "1.2.0-mc9d-r3"

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
    // MC9-D-R: run lifecycle (para el run oficial equivalente). Defaults ⇒ compat de decode con blobs V1.0.
    val plannedTrials: Int = 0,
    val completedTrials: Int = 0,
    val durationActualMs: Long = 0L,
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
    // MC9-D-R3 — conteos CRUDOS (el summary ya no es solo rates). Defaults ⇒ compat de decode con blobs pre-R3.
    val goCorrect: Int = 0,
    val noGoCorrect: Int = 0,
    val commissions: Int = 0,
    val omissions: Int = 0,
    val anticipations: Int = 0,
    val lateResponses: Int = 0,
    // MC9-D-R3 — distribución de RT de aciertos GO completa (PRD: RT medio/mejor/peor/variabilidad).
    val correctGoRtMeanMs: GoNoGoSignal = GoNoGoSignal.Unavailable("not persisted (pre-R3 summary)"),
    val correctGoRtMinMs: GoNoGoSignal = GoNoGoSignal.Unavailable("not persisted (pre-R3 summary)"),
    val correctGoRtMaxMs: GoNoGoSignal = GoNoGoSignal.Unavailable("not persisted (pre-R3 summary)"),
    // MC9-D-R3 — resultado SCORED persistido (índice + patrón + versión + integridad + elegibilidad por capa).
    val preliminaryScore: Double? = null,
    val scoreCategory: String = "",
    val scoreVersion: String = "",
    val timingIntegrityLevel: String = TimingIntegrityLevel.CLEAN.name,
    val scoreLowConfidence: Boolean = false,
    val dataQualityCompromised: Boolean = false,
    val calibrationEligible: Boolean = false,
    val profileEligible: Boolean = false,
    val rankingEligible: Boolean = false,
) {
    companion object {
        @Suppress("LongParameterList") // A telemetry summary aggregates many named signals; that width is the point.
        fun of(
            sessionId: String,
            exerciseId: String,
            metrics: GoNoGoMetrics,
            summary: GoNoGoRunSummary,
            diagnostics: GoNoGoDiagnostics,
            score: GoNoGoScore,
            createdAtEpochMs: Long,
            plannedTrials: Int = summary.totalTrials,
            completedTrials: Int = summary.totalTrials,
            durationActualMs: Long = 0L,
        ): GoNoGoTelemetrySummaryV1 {
            val lateRate = lateRate(summary)
            val rts = summary.correctGoRtsMs
            return GoNoGoTelemetrySummaryV1(
                sessionId = sessionId,
                exerciseId = exerciseId,
                totalTrials = summary.totalTrials,
                goTrials = summary.goTrials,
                noGoTrials = summary.noGoTrials,
                plannedTrials = plannedTrials,
                completedTrials = completedTrials,
                durationActualMs = durationActualMs,
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
                unavailableSignals = unavailableSignals(metrics, lateRate),
                metricsVersion = metrics.version,
                createdAtEpochMs = createdAtEpochMs,
                goCorrect = summary.goCorrect,
                noGoCorrect = summary.noGoCorrect,
                commissions = summary.commissions,
                omissions = summary.omissions,
                anticipations = summary.anticipations,
                lateResponses = summary.lateResponses,
                correctGoRtMeanMs = rtStat(rts, SpeedRtStatistics.meanOrNull(rts)),
                correctGoRtMinMs = rtStat(rts, rts.minOrNull()),
                correctGoRtMaxMs = rtStat(rts, rts.maxOrNull()),
                preliminaryScore = score.index,
                scoreCategory = score.category.name,
                scoreVersion = score.version,
                timingIntegrityLevel = score.timingIntegrity.name,
                scoreLowConfidence = score.lowConfidence,
                dataQualityCompromised = score.dataQualityCompromised,
                calibrationEligible = score.calibrationEligible,
                profileEligible = score.profileEligible,
                rankingEligible = score.rankingEligible,
            )
        }

        /** RT stat como señal honesta: Measured solo con muestra de aciertos GO suficiente; si no, Unavailable. */
        private fun rtStat(correctGoRtsMs: List<Double>, value: Double?): GoNoGoSignal =
            if (correctGoRtsMs.size >= GoNoGoMetricsMapper.MIN_CORRECT_GO && value != null) {
                GoNoGoSignal.Measured(value)
            } else {
                GoNoGoSignal.Unavailable("insufficient correct GO responses")
            }

        /** Nombres de las señales no disponibles (para escaneo rápido del summary), derivados de las métricas. */
        private fun unavailableSignals(metrics: GoNoGoMetrics, lateRate: GoNoGoSignal): List<String> = listOf(
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
        ).filter { it.second is GoNoGoSignal.Unavailable }.map { it.first }

        /** lateRate = respuestas GO tardías / trials GO — Measured solo con muestra GO suficiente. */
        private fun lateRate(summary: GoNoGoRunSummary): GoNoGoSignal =
            if (summary.goTrials >= GoNoGoMetricsMapper.MIN_GO && summary.goTrials > 0) {
                GoNoGoSignal.Measured(summary.lateResponses.toDouble() / summary.goTrials)
            } else {
                GoNoGoSignal.Unavailable("insufficient GO trials")
            }
    }
}
