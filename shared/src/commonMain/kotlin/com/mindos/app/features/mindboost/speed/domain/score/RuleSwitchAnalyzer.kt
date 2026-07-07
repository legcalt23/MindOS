package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.exercise.RuleSwitchLogic

/** Un trial reducido a lo que el analizador de cambio de regla necesita (deriva de un `TrialRecord` persistido). */
data class RuleSwitchOutcome(val rtNanos: Long, val isCorrect: Boolean)

/**
 * MC9-B — métricas de cambio de regla de E4. Todas son `null` cuando no hay datos suficientes (nunca 0 fabricado).
 * NO es un score: es análisis descriptivo de la corrida para el feedback. El score autoritativo sigue siendo el
 * `SpeedScoreEngine` 0–1 preliminar.
 */
data class RuleSwitchMetrics(
    val switchCount: Int,
    val switchCostMs: Double?,
    val postSwitchAccuracy: Double?,
    val stableAccuracy: Double?,
    val recoveryTrialsMedian: Double?,
)

/**
 * Calcula [RuleSwitchMetrics] a partir de la secuencia ORDENADA de trials de la corrida (rt + acierto) y del
 * `trialsPerRule` con el que se generaron. El calendario de cambios es determinista por índice
 * ([RuleSwitchLogic.isSwitch]), así que no hace falta persistir la regla: el índice del trial basta. Puro y testeable.
 *
 * - **switchCost**: mediana de RT en la ventana post-cambio menos mediana de RT en trials estables (ms). Un cambio
 *   de regla que cuesta se ve como RT más lento justo tras el cambio.
 * - **postSwitchAccuracy / stableAccuracy**: precisión en la ventana post-cambio vs. en los trials estables.
 * - **recoveryTrialsMedian**: nº de trials desde cada cambio hasta el primer acierto en la regla nueva (mediana).
 */
object RuleSwitchAnalyzer {

    const val POST_SWITCH_WINDOW = 2

    fun analyze(outcomes: List<RuleSwitchOutcome>, trialsPerRule: Int): RuleSwitchMetrics {
        val switchIndices = outcomes.indices.filter { RuleSwitchLogic.isSwitch(it, trialsPerRule) }
        val postSwitchIndices = switchIndices
            .flatMap { s -> (s until (s + POST_SWITCH_WINDOW)).filter { it in outcomes.indices } }
            .toSet()
        val stableIndices = outcomes.indices.filterNot { it in postSwitchIndices }

        return RuleSwitchMetrics(
            switchCount = switchIndices.size,
            switchCostMs = switchCostMs(outcomes, postSwitchIndices, stableIndices),
            postSwitchAccuracy = accuracy(outcomes, postSwitchIndices),
            stableAccuracy = accuracy(outcomes, stableIndices),
            recoveryTrialsMedian = recoveryTrialsMedian(outcomes, switchIndices, trialsPerRule),
        )
    }

    private fun switchCostMs(
        outcomes: List<RuleSwitchOutcome>,
        postSwitch: Set<Int>,
        stable: List<Int>,
    ): Double? {
        val postMedian = medianRtMs(postSwitch.map { outcomes[it].rtNanos })
        val stableMedian = medianRtMs(stable.map { outcomes[it].rtNanos })
        return if (postMedian == null || stableMedian == null) null else postMedian - stableMedian
    }

    private fun accuracy(outcomes: List<RuleSwitchOutcome>, indices: Collection<Int>): Double? {
        if (indices.isEmpty()) return null
        return indices.count { outcomes[it].isCorrect }.toDouble() / indices.size
    }

    private fun recoveryTrialsMedian(
        outcomes: List<RuleSwitchOutcome>,
        switchIndices: List<Int>,
        trialsPerRule: Int,
    ): Double? {
        if (switchIndices.isEmpty()) return null
        val recoveries = switchIndices.map { s ->
            val ruleBlockEnd = minOf(s + trialsPerRule, outcomes.size)
            val firstCorrect = (s until ruleBlockEnd).firstOrNull { outcomes[it].isCorrect }
            if (firstCorrect == null) (ruleBlockEnd - s) else (firstCorrect - s)
        }
        return median(recoveries.map { it.toDouble() })
    }

    private fun medianRtMs(rtNanos: List<Long>): Double? =
        median(rtNanos.map { it / NANOS_PER_MS })

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    private const val NANOS_PER_MS = 1_000_000.0
}

/** Lente de feedback de E4 · Cambio de Regla. */
enum class RuleSwitchFeedbackKind { CALIBRATING, LOW_PRECISION, HIGH_SWITCH_COST, GOOD_ADAPTATION, BALANCED }

/**
 * Traduce el perfil preliminar 0–1 ([SpeedScoreEngine]) + las [RuleSwitchMetrics] a una lente de feedback seria y
 * accionable (flexibilidad con precisión). No calcula score nuevo ni emite claims. El texto vive en `SpeedStrings`.
 */
object RuleSwitchFeedback {

    const val HIGH_SWITCH_COST_MS = 150.0
    const val POST_SWITCH_DROP = 0.15
    const val MIN_HELD_ACCURACY = 0.75

    fun forRun(score: CognitiveProfileScore, metrics: RuleSwitchMetrics): RuleSwitchFeedbackKind {
        val stable = metrics.stableAccuracy
        val post = metrics.postSwitchAccuracy
        return when {
            score.macroCategory == SpeedMacroCategory.CALIBRATING || metrics.switchCount == 0 ->
                RuleSwitchFeedbackKind.CALIBRATING
            score.macroCategory == SpeedMacroCategory.FAST_IMPULSIVE -> RuleSwitchFeedbackKind.LOW_PRECISION
            highSwitchCost(metrics, stable, post) -> RuleSwitchFeedbackKind.HIGH_SWITCH_COST
            goodAdaptation(metrics, stable, post) -> RuleSwitchFeedbackKind.GOOD_ADAPTATION
            else -> RuleSwitchFeedbackKind.BALANCED
        }
    }

    private fun highSwitchCost(metrics: RuleSwitchMetrics, stable: Double?, post: Double?): Boolean {
        val costHigh = (metrics.switchCostMs ?: 0.0) > HIGH_SWITCH_COST_MS
        val accuracyDrop = stable != null && post != null && (stable - post) > POST_SWITCH_DROP
        return costHigh || accuracyDrop
    }

    private fun goodAdaptation(metrics: RuleSwitchMetrics, stable: Double?, post: Double?): Boolean {
        val costLow = (metrics.switchCostMs ?: Double.MAX_VALUE) <= HIGH_SWITCH_COST_MS
        val heldAccuracy = stable != null && post != null &&
            (stable - post) <= POST_SWITCH_DROP && post >= MIN_HELD_ACCURACY
        return costLow && heldAccuracy
    }
}
