package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics
import kotlinx.serialization.Serializable

/**
 * Chequeo de CALIDAD DE DATOS de una corrida de E8 (§10). Señala patrones sospechosos (gaming / medición no
 * fiable) SIN acusar ni patologizar al usuario y SIN integrarse en el perfil oficial: es una lente de
 * quality-diagnostics de E8, se guarda en el summary (no altera el perfil). Puro y testeable. `@Serializable`.
 */
@Serializable
data class GoNoGoDiagnostics(
    val impossiblyFastTrials: Int,
    val tapAllStrategy: Boolean,
    val tapNoneStrategy: Boolean,
    val noGoSampleTooLow: Boolean,
    val anticipationFlood: Boolean,
    val excessiveLateResponses: Boolean,
    val lowTrialCount: Boolean,
    val roboticTiming: Boolean,
    val improbablePerfectUnderDifficulty: Boolean,
) {
    /** true si NINGUNA señal se disparó (corrida sin anomalías de calidad detectadas). */
    val clean: Boolean
        get() = impossiblyFastTrials == 0 && !tapAllStrategy && !tapNoneStrategy && !noGoSampleTooLow &&
            !anticipationFlood && !excessiveLateResponses && !lowTrialCount && !roboticTiming &&
            !improbablePerfectUnderDifficulty

    /** Nombres de las señales de calidad ACTIVAS (para `anomalyFlags` del run oficial). Vacío ⇒ corrida limpia. */
    fun activeFlags(): List<String> = buildList {
        if (impossiblyFastTrials > 0) add("impossiblyFastTrials")
        if (tapAllStrategy) add("tapAllStrategy")
        if (tapNoneStrategy) add("tapNoneStrategy")
        if (noGoSampleTooLow) add("noGoSampleTooLow")
        if (anticipationFlood) add("anticipationFlood")
        if (excessiveLateResponses) add("excessiveLateResponses")
        if (lowTrialCount) add("lowTrialCount")
        if (roboticTiming) add("roboticTiming")
        if (improbablePerfectUnderDifficulty) add("improbablePerfectUnderDifficulty")
    }
}

object GoNoGoQualityDiagnostics {

    /** RT por debajo del cual una respuesta es implausible para una decisión genuina Go/No-Go. */
    const val IMPOSSIBLE_RT_MS = 120.0
    const val TAP_ALL_MIN = 0.90
    const val TAP_NONE_MAX = 0.10
    const val ANTICIPATION_MAX = 0.30
    const val LATE_MAX = 0.30
    const val MIN_TRIALS = 8

    /** SD (ms) por debajo de la cual el TR de aciertos GO es sospechosamente uniforme (patrón robótico/repetido). */
    const val ROBOTIC_SD_MS = 8.0
    private const val PERFECT_ACCURACY = 1.0

    fun evaluate(outcomes: List<GoNoGoOutcome>): GoNoGoDiagnostics {
        val summary = GoNoGoRunSummary.from(outcomes)
        val total = summary.totalTrials
        val enough = total >= MIN_TRIALS
        val respondedRate = if (total > 0) outcomes.count { it.responseMade }.toDouble() / total else 0.0
        return GoNoGoDiagnostics(
            impossiblyFastTrials = outcomes.count { impossiblyFast(it) },
            tapAllStrategy = enough && respondedRate >= TAP_ALL_MIN,
            tapNoneStrategy = enough && respondedRate <= TAP_NONE_MAX,
            noGoSampleTooLow = summary.noGoTrials < GoNoGoMetricsMapper.MIN_NO_GO,
            anticipationFlood = enough && summary.anticipations.toDouble() / total > ANTICIPATION_MAX,
            excessiveLateResponses = summary.goTrials >= GoNoGoMetricsMapper.MIN_GO &&
                summary.lateResponses.toDouble() / summary.goTrials > LATE_MAX,
            lowTrialCount = total < MIN_TRIALS,
            roboticTiming = roboticTiming(summary),
            improbablePerfectUnderDifficulty = improbablePerfectUnderDifficulty(summary),
        )
    }

    private fun impossiblyFast(outcome: GoNoGoOutcome): Boolean {
        val rt = outcome.responseTimeMs ?: return false
        return rt.toDouble() < IMPOSSIBLE_RT_MS
    }

    /** TR de aciertos GO con dispersión casi nula = patrón repetido/robótico (no una respuesta humana genuina). */
    private fun roboticTiming(summary: GoNoGoRunSummary): Boolean {
        if (summary.correctGoRtsMs.size < GoNoGoMetricsMapper.MIN_CORRECT_GO) return false
        val sd = SpeedRtStatistics.standardDeviationOrNull(summary.correctGoRtsMs)
        return sd != null && sd < ROBOTIC_SD_MS
    }

    /** Precisión balanceada perfecta en el nivel MÁS difícil Y muy rápida globalmente = perfecto-improbable. */
    private fun improbablePerfectUnderDifficulty(summary: GoNoGoRunSummary): Boolean {
        val hardest = summary.byDifficulty
            .filter { it.trials >= GoNoGoMetricsMapper.MIN_DIFFICULTY_BUCKET }
            .maxByOrNull { it.difficultyLevel }
        val balanced = hardest?.balancedAccuracyOrNull
        val median = SpeedRtStatistics.medianOrNull(summary.correctGoRtsMs)
        return balanced != null && balanced >= PERFECT_ACCURACY &&
            median != null && median < GoNoGoRunSummary.FAST_COMMISSION_MS.toDouble()
    }
}
