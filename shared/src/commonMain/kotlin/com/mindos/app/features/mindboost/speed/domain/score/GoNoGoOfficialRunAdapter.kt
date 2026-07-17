package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics

/**
 * MC9-D-R — resultado NEUTRAL de una corrida de un ejercicio, compatible con el sistema oficial de sesiones, SIN
 * mentir sobre el paradigma. E8 · Go/No-Go NO puede representarse como `MeasurementRun`/`TrialRecord` (esos exigen
 * un `responseUptimeNanos` real por trial y una semántica RT-tap de `isCorrect`; una inhibición NO-GO correcta NO
 * tiene respuesta y su corrección se INVIERTE) — forzarlo mentiría. Este DTO es el "run oficial equivalente": lleva
 * comisiones y omisiones EXPLÍCITAS, no las esconde. Se deriva de los MISMOS outcomes que el telemetry summary.
 *
 * Contribución al perfil oficial: **DIFERIDA** ([contributesToOfficialProfile] = false) mientras el gate de
 * validación física (§15) siga abierto — informativo, no autoritativo. La integración con el store de historial
 * oficial (`SpeedMeasurementStore`, que hoy solo persiste `MeasurementRun`) queda documentada como trabajo futuro.
 */
@kotlinx.serialization.Serializable
data class OfficialExerciseRunAdapterResult(
    val sessionId: String,
    val exerciseId: String,
    val exerciseVersion: String,
    val scoreVersion: String,
    val plannedTrials: Int,
    val completedTrials: Int,
    val durationActualMs: Long,
    val totalTrials: Int,
    val goTrials: Int,
    val noGoTrials: Int,
    val correctTrials: Int,
    val incorrectTrials: Int,
    val omissions: Int,
    val commissions: Int,
    val anticipations: Int,
    val lateResponses: Int,
    /**
     * Precisión AGRUPADA (aciertos totales / total). NO es el titular de calidad de E8: con GO frecuente (≈75%),
     * una corrida con todas las comisiones aún da ≈0.75. Un consumidor debe usar [balancedAccuracy] o
     * [preliminaryScore] como titular, nunca [pooledAccuracy].
     */
    val pooledAccuracy: Double?,
    /** Precisión BALANCEADA (media de acierto GO y NO-GO): tap-all/tap-none ⇒ 0.5. Titular honesto de control. */
    val balancedAccuracy: Double?,
    val rtMedianMs: Double?,
    val rtMeanMs: Double?,
    val rtCv: Double?,
    val anticipationRate: Double?,
    val lateRate: Double?,
    val commissionRate: Double?,
    val omissionRate: Double?,
    val preliminaryScore: Double?,
    val lowConfidence: Boolean,
    val anomalyFlags: List<String>,
    /** MC9-D-R3 — integridad de temporización de la corrida ([TimingIntegrityLevel]) como texto para el consumidor. */
    val timingIntegrityLevel: String,
    /** MC9-D-R3 — elegibilidad por capa (derivada del score); el consumidor decide qué exigir por caso de uso. */
    val calibrationEligible: Boolean,
    val rankingEligible: Boolean,
    val telemetrySummarySessionId: String,
    val contributesToOfficialProfile: Boolean = false,
)

/**
 * Convierte una corrida de dominio de E8 en el run oficial equivalente. PURO. Deriva TODO de [summary] (mismos
 * outcomes que el telemetry summary), [metrics] (rates gated) y [score]; nunca fabrica un 0 (rate no computable ⇒
 * `null`; `preliminaryScore` `null` en CALIBRATING). `omissions`/`commissions` van EXPLÍCITAS.
 */
object GoNoGoOfficialRunAdapter {

    /**
     * MC9-D-R3 — interruptor ÚNICO del wiring de contribución al perfil. FALSE hoy (§15 físico abierto + no hay
     * store de historial oficial que acepte el run-equivalente). Criterio EXACTO para ponerlo en `true`: (1) §15
     * validación física cerrada; (2) existe un store/pipeline oficial que consuma `OfficialExerciseRunAdapterResult`;
     * (3) `score.profileEligible == true` (integridad no comprometida + índice computable). Solo entonces una corrida
     * contribuye — y aun así por-corrida, no toda automáticamente.
     */
    const val PROFILE_WIRING_ENABLED: Boolean = false

    @Suppress("LongParameterList") // A run record aggregates many named lifecycle + measurement fields.
    fun toOfficialRun(
        sessionId: String,
        exerciseId: String,
        exerciseVersion: String,
        summary: GoNoGoRunSummary,
        metrics: GoNoGoMetrics,
        score: GoNoGoScore,
        diagnostics: GoNoGoDiagnostics,
        plannedTrials: Int,
        completedTrials: Int,
        durationActualMs: Long,
    ): OfficialExerciseRunAdapterResult {
        val total = summary.totalTrials
        val correct = summary.goCorrect + summary.noGoCorrect
        return OfficialExerciseRunAdapterResult(
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseVersion = exerciseVersion,
            scoreVersion = score.version,
            plannedTrials = plannedTrials,
            completedTrials = completedTrials,
            durationActualMs = durationActualMs,
            totalTrials = total,
            goTrials = summary.goTrials,
            noGoTrials = summary.noGoTrials,
            correctTrials = correct,
            incorrectTrials = total - correct,
            omissions = summary.omissions,
            commissions = summary.commissions,
            anticipations = summary.anticipations,
            lateResponses = summary.lateResponses,
            pooledAccuracy = if (total > 0) correct.toDouble() / total else null,
            balancedAccuracy = balancedAccuracy(metrics),
            rtMedianMs = SpeedRtStatistics.medianOrNull(summary.correctGoRtsMs),
            rtMeanMs = SpeedRtStatistics.meanOrNull(summary.correctGoRtsMs),
            rtCv = metrics.correctGoRtVariability.measuredOrNull(),
            anticipationRate = metrics.anticipationRate.measuredOrNull(),
            lateRate = lateRate(summary),
            commissionRate = metrics.commissionRate.measuredOrNull(),
            omissionRate = metrics.omissionRate.measuredOrNull(),
            preliminaryScore = score.index,
            lowConfidence = score.lowConfidence,
            anomalyFlags = diagnostics.activeFlags(),
            timingIntegrityLevel = score.timingIntegrity.name,
            calibrationEligible = score.calibrationEligible,
            rankingEligible = score.rankingEligible,
            telemetrySummarySessionId = sessionId,
            contributesToOfficialProfile = score.profileEligible && PROFILE_WIRING_ENABLED,
        )
    }

    private fun lateRate(summary: GoNoGoRunSummary): Double? =
        if (summary.goTrials >= GoNoGoMetricsMapper.MIN_GO && summary.goTrials > 0) {
            summary.lateResponses.toDouble() / summary.goTrials
        } else {
            null
        }

    /** balancedAccuracy = (goAccuracy + noGoAccuracy) / 2 — solo si ambas están medidas (muestra suficiente). */
    private fun balancedAccuracy(metrics: GoNoGoMetrics): Double? {
        val go = metrics.goAccuracy.measuredOrNull()
        val noGo = metrics.noGoAccuracy.measuredOrNull()
        return if (go != null && noGo != null) (go + noGo) / 2.0 else null
    }
}
