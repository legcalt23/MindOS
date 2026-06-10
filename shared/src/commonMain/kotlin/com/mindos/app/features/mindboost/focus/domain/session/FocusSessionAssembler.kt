package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricsEngine
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.toMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialId
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.math.roundToInt

/**
 * Turns the raw timing collected by the live runtime (a latency per stimulus)
 * into classified trials, metrics, a [BlockResult]/[SessionResult] and an honest
 * [FocusReport]. Pure (no Compose, no timing) so it is fully unit-testable; the
 * runtime is left to do only orchestration, never computation.
 */
class FocusSessionAssembler(private val metricsEngine: MetricsEngine) {

    /** A stimulus paired with the user's response latency (ms), or null for no response. */
    data class RecordedTrial(val stimulus: Stimulus, val responseLatencyMs: Long?)

    @Suppress("LongParameterList") // Each argument is an intrinsic field of the block being assembled.
    fun assembleBlock(
        blockId: BlockId,
        definition: ExerciseDefinition,
        mode: Mode,
        difficultyStart: DifficultyVector,
        difficultyEnd: DifficultyVector,
        thresholds: IndividualThresholds,
        recorded: List<RecordedTrial>,
        sessionIdPrefix: String,
        epochMillis: Long,
    ): BlockResult {
        val trials = recorded.mapIndexed { index, rec ->
            val classification = TrialClassifier.classify(
                isTarget = rec.stimulus.isTarget,
                responseLatencyMs = rec.responseLatencyMs,
                anticipationFloorMs = thresholds.anticipationFloorMs,
                responseWindowMs = rec.stimulus.responseWindowMs,
            )
            TrialResult(
                trialId = TrialId("$sessionIdPrefix:${blockId.value}:$index"),
                exerciseId = definition.id,
                mode = mode,
                stimulusIsTarget = rec.stimulus.isTarget,
                requiredStop = rec.stimulus.requiresStop,
                outcome = classification.outcome,
                reactionTime = classification.reactionTime,
                difficulty = difficultyEnd,
                timestampMs = epochMillis,
                markers = rec.stimulus.toMarkers(),
            )
        }
        val metrics = metricsEngine.blockMetrics(trials, definition, thresholds)
        return BlockResult(
            blockId = blockId,
            exerciseId = definition.id,
            mode = mode,
            trials = trials,
            difficultyStart = difficultyStart,
            difficultyEnd = difficultyEnd,
            metrics = metrics,
        )
    }

    @Suppress("LongParameterList") // Each argument is an intrinsic field of the session being assembled.
    fun assembleSession(
        sessionId: SessionId,
        mode: Mode,
        definition: ExerciseDefinition,
        blocks: List<BlockResult>,
        startedAtMs: Long,
        endedAtMs: Long,
        subjectiveState: SubjectiveState?,
    ): SessionResult = SessionResult(
        sessionId = sessionId,
        mode = mode,
        exerciseFocus = definition.id,
        blocks = blocks,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        subjectiveState = subjectiveState,
        sessionMetrics = metricsEngine.sessionMetrics(blocks),
    )

    /** Builds the honest, user-facing report for a finished session. */
    fun buildReport(session: SessionResult): FocusReport {
        val accuracy = session.sessionMetrics[MetricKey.ACCURACY]
        val rtMedian = session.sessionMetrics[MetricKey.RT_MEDIAN]
        val rtCv = session.sessionMetrics[MetricKey.RT_CV]
        val lapses = session.sessionMetrics[MetricKey.LAPSES]

        val lines = buildList {
            accuracy?.let {
                val pct = (it.value * PERCENT).roundToInt()
                add(ReportLine(MetricKey.ACCURACY, "Precisión", it, captionFor(it.confident, "$pct%")))
            }
            rtMedian?.let {
                val caption = captionFor(it.confident, "${it.value.roundToInt()} ms")
                add(ReportLine(MetricKey.RT_MEDIAN, "Reacción (mediana)", it, caption))
            }
            rtCv?.let {
                add(ReportLine(MetricKey.RT_CV, "Estabilidad (RT-CV)", it, stabilityCaption(it.confident, it.value)))
            }
            lapses?.let {
                add(ReportLine(MetricKey.LAPSES, "Lapsos", it, captionFor(it.confident, "${it.value.roundToInt()}")))
            }
        }
        return FocusReport(headline = lines, honestNote = honestNote(session))
    }

    private fun captionFor(confident: Boolean, value: String): String =
        if (confident) value else "Datos insuficientes aún"

    private fun stabilityCaption(confident: Boolean, cv: Double): String {
        if (!confident) return "Datos insuficientes aún"
        val pct = (cv * PERCENT).roundToInt()
        val quality = when {
            cv < CV_VERY_STABLE -> "muy estable"
            cv < CV_STABLE -> "estable"
            cv < CV_IRREGULAR -> "algo irregular"
            else -> "irregular"
        }
        return "$pct% · $quality"
    }

    private fun honestNote(session: SessionResult): String {
        val confident = session.sessionMetrics.values.any { it.confident }
        return if (!confident || session.totalTrials < MIN_TRIALS_FOR_SIGNAL) {
            "Esta sesión es demasiado corta para sacar conclusiones. Es una foto de hoy, " +
                "no una medida de tu capacidad. La mejora real se confirma con pruebas no entrenadas."
        } else {
            "Esto refleja tu rendimiento de hoy en este ejercicio — influido por sueño, hora y " +
                "ánimo. No demuestra todavía mejora general: eso se mide con pruebas no entrenadas."
        }
    }

    private companion object {
        const val MIN_TRIALS_FOR_SIGNAL = 20
        const val PERCENT = 100
        const val CV_VERY_STABLE = 0.18
        const val CV_STABLE = 0.28
        const val CV_IRREGULAR = 0.40
    }
}
