// classifyLapse usa guard-clauses (varios return tempranos) — la forma más legible.
@file:Suppress("ReturnCount")

package com.mindos.app.features.mindboost.speed.domain

import com.mindos.app.features.mindboost.speed.domain.metrics.MeasurementContext
import com.mindos.app.features.mindboost.speed.domain.metrics.SpeedRtStatistics
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord

/**
 * Motor de trials de MVP-0 (§4). Sin adaptación: la dificultad es FIJA.
 *
 * Adaptación de firma respecto al plan: `startTrial` recibe además
 * `stimulusUptimeNanos` (el `frameTimeNanos` real del Choreographer capturado por la
 * pantalla) en lugar de fabricarlo internamente; lo devuelve para preservar la semántica
 * "startTrial establece y retorna el timestamp de estímulo". `onResponse` no recibe la
 * precisión porque es fija por bloque (el nivel de API no cambia) y viaja en [context].
 */
interface TrialEngine {
    /** Inicia un trial con el estímulo ya renderizado en [stimulusUptimeNanos]; lo retorna. */
    fun startTrial(difficulty: Double, deadlineNanos: Long?, stimulusUptimeNanos: Long): Long

    /** Registra la respuesta y produce el [TrialRecord]. Lanza si el RT es negativo. */
    fun onResponse(responseUptimeNanos: Long, isCorrect: Boolean): TrialRecord

    fun getTrialHistory(): List<TrialRecord>
}

/**
 * Implementación fija de un bloque. Cada instancia gobierna UN bloque (mismo `blockId`,
 * `exerciseId`, [context] y dificultad). No aplica staircase ni adaptación (MVP-1+).
 *
 * - `rtNanos = responseUptimeNanos - stimulusUptimeNanos` vía [TimebasePolicy].
 * - Anticipación: `rtNanos < anticipationThresholdNanos`.
 * - Lapso (marca cruda del record): `rtNanos > lapseMultiplier × mediana(TR crudos de
 *   aciertos previos)`, solo con >= [MIN_HITS_FOR_LAPSE] aciertos previos. La autoridad
 *   final de `lapseRate` es [com.mindos.app.features.mindboost.speed.domain.metrics.MetricCalculator],
 *   que la recalcula sobre la mediana final del bloque.
 */
class FixedTrialEngine(
    private val blockId: String,
    private val exerciseId: SpeedExerciseId,
    private val context: MeasurementContext,
    private val thresholds: MetricThresholds = MetricThresholds.DEFAULT,
    private val difficultyEstimator: DifficultyEstimator = FixedDifficultyEstimator(),
    private val idFactory: (blockId: String, trialIndex: Int) -> String = { id, i -> "$id:t$i" },
) : TrialEngine {

    private data class Pending(
        val stimulusUptimeNanos: Long,
        val deadlineNanos: Long?,
        val difficultyEstimate: Double,
    )

    private val history = mutableListOf<TrialRecord>()
    private val priorHitRtsNanos = mutableListOf<Double>()
    private var pending: Pending? = null

    override fun startTrial(difficulty: Double, deadlineNanos: Long?, stimulusUptimeNanos: Long): Long {
        pending = Pending(
            stimulusUptimeNanos = stimulusUptimeNanos,
            deadlineNanos = deadlineNanos,
            difficultyEstimate = difficultyEstimator.estimateDifficultyBefore(difficulty, deadlineNanos),
        )
        return stimulusUptimeNanos
    }

    override fun onResponse(responseUptimeNanos: Long, isCorrect: Boolean): TrialRecord {
        val current = requireNotNull(pending) { "onResponse called before startTrial." }
        val rtNanos = TimebasePolicy.computeRtNanos(current.stimulusUptimeNanos, responseUptimeNanos)
        require(TimebasePolicy.isValidRt(rtNanos)) {
            "Negative RT ($rtNanos ns): response precedes stimulus — invalid measurement."
        }

        val isAnticipation = rtNanos < thresholds.anticipationThresholdNanos
        val isLapse = classifyLapse(rtNanos, isAnticipation, isCorrect)

        val record = TrialRecord(
            id = idFactory(blockId, history.size),
            blockId = blockId,
            trialIndex = history.size,
            exerciseId = exerciseId.code,
            stimulusUptimeNanos = current.stimulusUptimeNanos,
            responseUptimeNanos = responseUptimeNanos,
            rtNanos = rtNanos,
            isCorrect = isCorrect,
            isAnticipation = isAnticipation,
            isLapse = isLapse,
            lapseMultiplier = thresholds.lapseMultiplier,
            deadlineNanos = current.deadlineNanos,
            difficultyEstimate = current.difficultyEstimate,
            timestampSource = context.timestampSource,
            inputTimestampPrecision = context.inputTimestampPrecision,
            timebase = context.timebase,
        )
        history += record
        // Solo los aciertos no-anticipados alimentan la mediana móvil de lapsos.
        if (isCorrect && !isAnticipation) priorHitRtsNanos += rtNanos.toDouble()
        pending = null
        return record
    }

    override fun getTrialHistory(): List<TrialRecord> = history.toList()

    private fun classifyLapse(rtNanos: Long, isAnticipation: Boolean, isCorrect: Boolean): Boolean {
        if (isAnticipation || !isCorrect) return false
        if (priorHitRtsNanos.size < MIN_HITS_FOR_LAPSE) return false
        val median = SpeedRtStatistics.medianOrNull(priorHitRtsNanos) ?: return false
        return rtNanos > thresholds.lapseMultiplier * median
    }

    private companion object {
        // Mínimo de aciertos previos para que la mediana móvil de lapsos sea estable.
        const val MIN_HITS_FOR_LAPSE = 3
    }
}
