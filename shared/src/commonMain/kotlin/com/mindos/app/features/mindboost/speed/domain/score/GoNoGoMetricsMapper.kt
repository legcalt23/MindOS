package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics

/** Versión del mapper de métricas de E8 (para trazabilidad; se bumpea si cambia una fórmula). */
const val GONOGO_METRICS_VERSION: String = "1.0.0-mc9d"

/**
 * Métricas específicas de E8 · Go/No-Go Velocity, derivadas de la corrida. Cada campo es un [GoNoGoSignal]:
 * [GoNoGoSignal.Measured] solo si hay muestra real; en otro caso [GoNoGoSignal.Unavailable] con la razón exacta.
 * NO es un score (el índice honesto lo calcula [GoNoGoScore]): es análisis descriptivo para feedback y adaptación.
 *
 * `inhibitoryControlIndex` = PRECISIÓN BALANCEADA (media de acierto GO y NO-GO): tap-all ⇒ 0.5, tap-none ⇒ 0.5,
 * perfecto ⇒ 1.0 — por diseño la velocidad en GO no compensa las comisiones en NO-GO.
 */
data class GoNoGoMetrics(
    val totalTrials: Int,
    val goTrials: Int,
    val noGoTrials: Int,
    val goAccuracy: GoNoGoSignal,
    val noGoAccuracy: GoNoGoSignal,
    val commissionRate: GoNoGoSignal,
    val omissionRate: GoNoGoSignal,
    val anticipationRate: GoNoGoSignal,
    val correctGoSpeedMs: GoNoGoSignal,
    val correctGoRtVariability: GoNoGoSignal,
    val inhibitoryControlIndex: GoNoGoSignal,
    val speedAccuracyTradeoff: GoNoGoSignal,
    val stabilityIndex: GoNoGoSignal,
    val difficultyAdjustedControl: GoNoGoSignal,
    val version: String = GONOGO_METRICS_VERSION,
)

/**
 * Deriva [GoNoGoMetrics] de los outcomes reales. Reglas de honestidad: sin muestra suficiente ⇒ `Unavailable`
 * (por tipo: GO, NO-GO, aciertos-GO, total, o <2 niveles de dificultad), NUNCA un 0 fabricado.
 */
// One pure derivation function per metric is the idiomatic shape here (not a code smell).
@Suppress("TooManyFunctions")
object GoNoGoMetricsMapper {

    const val MIN_GO = 6
    const val MIN_NO_GO = 4
    const val MIN_CORRECT_GO = 4
    const val MIN_TRIALS = 8
    const val MIN_DIFFICULTY_BUCKET = 4

    private const val UNAVAILABLE_GO = "insufficient GO trials"
    private const val UNAVAILABLE_NO_GO = "insufficient NO-GO trials"
    private const val UNAVAILABLE_CORRECT_GO = "insufficient correct GO responses"
    private const val UNAVAILABLE_TRIALS = "insufficient trials"
    private const val UNAVAILABLE_DIFFICULTY = "fewer than 2 difficulty levels with enough trials in this run"

    fun map(outcomes: List<GoNoGoOutcome>): GoNoGoMetrics = metrics(GoNoGoRunSummary.from(outcomes))

    fun metrics(summary: GoNoGoRunSummary): GoNoGoMetrics = GoNoGoMetrics(
        totalTrials = summary.totalTrials,
        goTrials = summary.goTrials,
        noGoTrials = summary.noGoTrials,
        goAccuracy = ratio(summary.goCorrect, summary.goTrials, summary.goTrials >= MIN_GO, UNAVAILABLE_GO),
        noGoAccuracy = ratio(
            summary.noGoCorrect, summary.noGoTrials, summary.noGoTrials >= MIN_NO_GO, UNAVAILABLE_NO_GO,
        ),
        commissionRate = ratio(
            summary.commissions, summary.noGoTrials, summary.noGoTrials >= MIN_NO_GO, UNAVAILABLE_NO_GO,
        ),
        omissionRate = ratio(summary.omissions, summary.goTrials, summary.goTrials >= MIN_GO, UNAVAILABLE_GO),
        anticipationRate = ratio(
            summary.anticipations, summary.totalTrials, summary.totalTrials >= MIN_TRIALS, UNAVAILABLE_TRIALS,
        ),
        correctGoSpeedMs = correctGoSpeed(summary),
        correctGoRtVariability = cvOrNull(summary)?.let { GoNoGoSignal.Measured(it) }
            ?: GoNoGoSignal.Unavailable(UNAVAILABLE_CORRECT_GO),
        inhibitoryControlIndex = inhibitoryControl(summary),
        speedAccuracyTradeoff = ratio(
            summary.fastCommissions, summary.noGoTrials, summary.noGoTrials >= MIN_NO_GO, UNAVAILABLE_NO_GO,
        ),
        stabilityIndex = stability(summary),
        difficultyAdjustedControl = difficultyAdjustedControl(summary),
    )

    private fun ratio(numerator: Int, denominator: Int, enough: Boolean, reason: String): GoNoGoSignal =
        if (!enough || denominator <= 0) {
            GoNoGoSignal.Unavailable(reason)
        } else {
            GoNoGoSignal.Measured(numerator.toDouble() / denominator)
        }

    /** Precisión balanceada = (goAccuracy + noGoAccuracy) / 2 — requiere muestra suficiente de AMBOS tipos. */
    private fun inhibitoryControl(summary: GoNoGoRunSummary): GoNoGoSignal =
        if (summary.goTrials < MIN_GO || summary.noGoTrials < MIN_NO_GO) {
            GoNoGoSignal.Unavailable(UNAVAILABLE_NO_GO)
        } else {
            val go = summary.goCorrect.toDouble() / summary.goTrials
            val noGo = summary.noGoCorrect.toDouble() / summary.noGoTrials
            GoNoGoSignal.Measured((go + noGo) / 2.0)
        }

    private fun correctGoSpeed(summary: GoNoGoRunSummary): GoNoGoSignal {
        val median = SpeedRtStatistics.medianOrNull(summary.correctGoRtsMs)
        return if (summary.correctGoRtsMs.size < MIN_CORRECT_GO || median == null) {
            GoNoGoSignal.Unavailable(UNAVAILABLE_CORRECT_GO)
        } else {
            GoNoGoSignal.Measured(median)
        }
    }

    private fun cvOrNull(summary: GoNoGoRunSummary): Double? {
        val rts = summary.correctGoRtsMs
        if (rts.size < MIN_CORRECT_GO) return null
        val mean = SpeedRtStatistics.meanOrNull(rts)
        val sd = SpeedRtStatistics.standardDeviationOrNull(rts)
        return if (mean == null || sd == null || mean <= 0.0) null else sd / mean
    }

    private fun stability(summary: GoNoGoRunSummary): GoNoGoSignal =
        cvOrNull(summary)?.let { GoNoGoSignal.Measured((1.0 - it).coerceIn(0.0, 1.0)) }
            ?: GoNoGoSignal.Unavailable(UNAVAILABLE_CORRECT_GO)

    /** Media de la precisión balanceada por nivel de dificultad — requiere ≥2 buckets con muestra y ambos tipos. */
    private fun difficultyAdjustedControl(summary: GoNoGoRunSummary): GoNoGoSignal {
        val balanced = summary.byDifficulty
            .filter { it.trials >= MIN_DIFFICULTY_BUCKET }
            .mapNotNull { it.balancedAccuracyOrNull }
        return if (balanced.size < 2) {
            GoNoGoSignal.Unavailable(UNAVAILABLE_DIFFICULTY)
        } else {
            GoNoGoSignal.Measured(balanced.average())
        }
    }
}
