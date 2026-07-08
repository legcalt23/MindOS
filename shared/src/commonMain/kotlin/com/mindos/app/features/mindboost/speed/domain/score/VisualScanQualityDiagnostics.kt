package com.mindos.app.features.mindboost.speed.domain.score

import kotlinx.serialization.Serializable

/**
 * Chequeo de CALIDAD DE DATOS de una corrida de E5 (MC9-C-R §7). Señala patrones sospechosos
 * (gaming / medición no fiable) SIN acusar ni patologizar al usuario y SIN integrarse en el perfil oficial:
 * es una lente de quality-diagnostics de E5, no un gate de perfil (eso lo gobierna `SpeedProfileSource` +
 * `SpeedAntiGaming`, transversal, no tocado aquí). Puro y testeable. `@Serializable` para el summary (MC9-C-R2).
 */
@Serializable
data class VisualScanDiagnostics(
    val impossiblyFastTrials: Int,
    val extremeSideBias: Boolean,
    val anticipationFlood: Boolean,
    val fastWrongBurst: Boolean,
    val improbablePerfectUnderLoad: Boolean,
) {
    /** true si NINGUNA señal se disparó (corrida sin anomalías de calidad detectadas). */
    val clean: Boolean
        get() = impossiblyFastTrials == 0 && !extremeSideBias && !anticipationFlood &&
            !fastWrongBurst && !improbablePerfectUnderLoad
}

object VisualScanQualityDiagnostics {

    /** RT por debajo del cual una respuesta es implausible para una búsqueda visual genuina. */
    const val IMPOSSIBLE_RT_MS = 120.0
    const val SIDE_BIAS_MAX = 0.35
    const val ANTICIPATION_MAX = 0.30
    const val FAST_WRONG_MAX = 0.30
    const val MIN_TRIALS = 8
    private const val PERFECT_ACCURACY = 1.0
    private const val NANOS_PER_MS = 1_000_000.0

    fun evaluate(outcomes: List<VisualScanTrialOutcome>): VisualScanDiagnostics {
        val summary = VisualScanRunSummary.from(outcomes)
        val total = summary.totalTrials
        val impossiblyFast = outcomes.count { it.rtNanos / NANOS_PER_MS < IMPOSSIBLE_RT_MS }
        val enough = total >= MIN_TRIALS
        return VisualScanDiagnostics(
            impossiblyFastTrials = impossiblyFast,
            extremeSideBias = enough && sideBiasMagnitude(summary) > SIDE_BIAS_MAX,
            anticipationFlood = enough && summary.anticipations.toDouble() / total > ANTICIPATION_MAX,
            fastWrongBurst = enough && summary.fastWrong.toDouble() / total > FAST_WRONG_MAX,
            improbablePerfectUnderLoad = improbablePerfectUnderLoad(summary),
        )
    }

    private fun sideBiasMagnitude(summary: VisualScanRunSummary): Double {
        if (summary.respondedTrials == 0) return 0.0
        return kotlin.math.abs(summary.respondedLeft.toDouble() / summary.respondedTrials - HALF)
    }

    /** Precisión perfecta Y muy rápida en el nivel de carga MÁS alto = sospechoso (perfecto-improbable bajo carga). */
    private fun improbablePerfectUnderLoad(summary: VisualScanRunSummary): Boolean {
        val hardest = summary.byLoad.filter { it.trials >= VisualScanMetricsMapper.MIN_LOAD_BUCKET }
            .maxByOrNull { it.itemsPerSide }
        val median = hardest?.medianRtMs
        return hardest != null && median != null &&
            hardest.accuracy >= PERFECT_ACCURACY && median < VisualScanRunSummary.FAST_WRONG_MS
    }

    private const val HALF = 0.5
}
