// Las firmas de cálculo y el holder intermedio agrupan muchas señales psicométricas;
// esa amplitud es inherente al dominio (no un code smell). Se suprime LongParameterList.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.metrics

import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord

/**
 * Metadata temporal que acompaña a cada métrica persistida (§4.1 regla 9, acceptance #3).
 */
data class MeasurementContext(
    val timestampSource: TimestampSource,
    val inputTimestampPrecision: InputTimestampPrecision,
    val timebase: Timebase = Timebase.ANDROID_UI_UPTIME_NANOS,
)

/** Resultado intermedio del cálculo núcleo, compartido por bloque y sesión. */
private data class CoreMetrics(
    val totalTrials: Int,
    val pc: Double,
    val rtMedianNanos: Double?,
    val rtMeanNanos: Double?,
    val cv: Double?,
    val lapseRate: Double,
    val anticipationRate: Double,
    val faRate: Double?,
    val omissionRate: Double,
    val impIdx: Double?,
    val vce: Double?,
)

/**
 * Calcula [BlockMetrics] y [SessionMetrics] a partir de [TrialRecord]s, en `Double` y
 * con manejo de nulos explícito (§7). Delega los bordes en [MetricEdgeCasePolicy].
 *
 * Reparto de responsabilidades de filtrado (CLAUDE.md 2026-06-04):
 * - tendencia central (mediana/media/CV) sobre TR limpios por MAD;
 * - lapsos sobre TR crudos (solo anticipaciones excluidas como categoría).
 *
 * MVP-0: E1/E6 son ejercicios SIN condición no-go ⇒ `faRate = null` y la impulsividad
 * usa la re-normalización de pesos sin no-go.
 */
class MetricCalculator(
    private val thresholds: MetricThresholds = MetricThresholds.DEFAULT,
) {

    /**
     * Métricas de un bloque. Devuelve **null** si el bloque debe descartarse: 0 trials
     * registrados y 0 omisiones (caso "0 trials en bloque" del §7).
     */
    fun blockMetrics(
        blockId: String,
        trials: List<TrialRecord>,
        context: MeasurementContext,
        noGoTrials: Int = 0,
        falseAlarms: Int = 0,
        omissions: Int = 0,
    ): BlockMetrics? {
        if (trials.isEmpty() && omissions == 0) return null
        val core = computeCore(trials, noGoTrials, falseAlarms, omissions)
        return BlockMetrics(
            blockId = blockId,
            totalTrials = core.totalTrials,
            pc = core.pc,
            rtMedianNanos = core.rtMedianNanos,
            rtMeanNanos = core.rtMeanNanos,
            cv = core.cv,
            lapseRate = core.lapseRate,
            anticipationRate = core.anticipationRate,
            faRate = core.faRate,
            omissionRate = core.omissionRate,
            impIdx = core.impIdx,
            // FatIdx es longitudinal (requiere >= 3 bloques); a nivel de bloque siempre null.
            fatIdx = null,
            vce = core.vce,
            timestampSource = context.timestampSource,
            inputTimestampPrecision = context.inputTimestampPrecision,
            timebase = context.timebase,
        )
    }

    /**
     * Métricas de la corrida completa. Recalcula el núcleo sobre todos los trials y añade
     * `fatIdx` a partir de la serie por bloque ([orderedBlockMetrics], ordenada por
     * `blockIndex`). `fatIdx = null` con menos de 3 bloques (§7).
     */
    fun sessionMetrics(
        sessionId: String,
        allTrials: List<TrialRecord>,
        orderedBlockMetrics: List<BlockMetrics>,
        context: MeasurementContext,
        noGoTrials: Int = 0,
        falseAlarms: Int = 0,
        omissions: Int = 0,
    ): SessionMetrics {
        val core = computeCore(allTrials, noGoTrials, falseAlarms, omissions)
        return SessionMetrics(
            sessionId = sessionId,
            totalTrials = core.totalTrials,
            pc = core.pc,
            rtMedianNanos = core.rtMedianNanos,
            rtMeanNanos = core.rtMeanNanos,
            cv = core.cv,
            lapseRate = core.lapseRate,
            anticipationRate = core.anticipationRate,
            faRate = core.faRate,
            omissionRate = core.omissionRate,
            impIdx = core.impIdx,
            fatIdx = fatigueIndex(orderedBlockMetrics),
            vce = core.vce,
            timestampSource = context.timestampSource,
            inputTimestampPrecision = context.inputTimestampPrecision,
            timebase = context.timebase,
        )
    }

    private fun computeCore(
        trials: List<TrialRecord>,
        noGoTrials: Int,
        falseAlarms: Int,
        omissions: Int,
    ): CoreMetrics {
        val anticipations = trials.filter { it.isAnticipation }
        val nonAnticipations = trials.filter { !it.isAnticipation }
        val correct = nonAnticipations.filter { it.isCorrect }
        val errors = nonAnticipations.filter { !it.isCorrect }

        val rawHitRts = correct.map { it.rtNanos.toDouble() }
        val cleanHitRts = SpeedRtStatistics.rejectOutliers(rawHitRts)
        val errorRts = errors.map { it.rtNanos.toDouble() }

        val total = trials.size
        val pc = MetricEdgeCasePolicy.proportionCorrect(correct.size, nonAnticipations.size)
        val rtMedianNanos = if (correct.isEmpty()) null else SpeedRtStatistics.medianOrNull(cleanHitRts)
        val rtMeanNanos = if (correct.isEmpty()) null else SpeedRtStatistics.meanOrNull(cleanHitRts)
        val sd = SpeedRtStatistics.standardDeviationOrNull(cleanHitRts)
        val cv = MetricEdgeCasePolicy.coefficientOfVariation(sd, rtMeanNanos)

        val lapseRate = MetricEdgeCasePolicy.lapseRate(rawHitRts, thresholds.lapseMultiplier)
        val anticipationRate = if (total == 0) 0.0 else anticipations.size.toDouble() / total
        val faRate = MetricEdgeCasePolicy.falseAlarmRate(falseAlarms, noGoTrials)
        val omissionRate = MetricEdgeCasePolicy.omissionRate(omissions, total + omissions)
        val fastErrorShare = MetricEdgeCasePolicy.fastErrorShare(errorRts, rtMedianNanos)
        // MVP-0: faRate siempre null en E1/E6 ⇒ rama de re-normalización sin no-go.
        val impIdx = MetricEdgeCasePolicy.impulsivityIndexNoNoGo(anticipationRate, fastErrorShare)
        val vce = MetricEdgeCasePolicy.vce(pc, rtMedianNanos, cv)

        return CoreMetrics(
            totalTrials = total,
            pc = pc,
            rtMedianNanos = rtMedianNanos,
            rtMeanNanos = rtMeanNanos,
            cv = cv,
            lapseRate = lapseRate,
            anticipationRate = anticipationRate,
            faRate = faRate,
            omissionRate = omissionRate,
            impIdx = impIdx,
            vce = vce,
        )
    }

    /** Índice de fatiga intra-sesión (§7). Delega en [FatigueIndex] (compartido con el staircase). */
    private fun fatigueIndex(orderedBlockMetrics: List<BlockMetrics>): Double? =
        FatigueIndex.compute(orderedBlockMetrics)
}
