package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricAbsence
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricsEngine
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.toMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialId
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import com.mindos.app.features.mindboost.focus.domain.model.isResponse
import com.mindos.app.features.mindboost.focus.domain.quality.FocusAntiGaming
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.quality.SessionQualityGate
import kotlin.math.roundToInt

/**
 * Turns the raw timing collected by the live runtime (a latency per stimulus)
 * into classified trials, metrics, a [BlockResult]/[SessionResult] and an honest
 * [FocusReport]. Pure (no Compose, no timing) so it is fully unit-testable; the
 * runtime is left to do only orchestration, never computation.
 */
class FocusSessionAssembler(private val metricsEngine: MetricsEngine) {

    /**
     * FOC-P0-05 · The single entry through which an assembled session becomes a RECORDED one. Attaches what the
     * lifecycle sensor saw, asks [SessionQualityGate] for its verdict and stamps it on the session. A session that
     * is NOT admitted keeps no confident metric: an OBSERVED interruption re-publishes every block and session
     * metric as [MetricAbsence.INTERRUPTED] (FOC-P0-03), and anything still confident after that is withdrawn as
     * [MetricAbsence.NOT_ADMITTED] — sample sizes kept — so the adaptive engine, the RCI, the trend and the profile
     * source go dark through the `confident == false` path they already honour, while a metric already absent
     * keeps its own, more specific reason. Declared ceiling: on a platform without a sensor the gate DENIES
     * (`LIFECYCLE_NOT_OBSERVED`) instead of guessing; the omission gate of `DefaultMetricsEngine` (FOC-P0-04) stays
     * the per-metric backstop. No other path may record a session: `focusQualityGateScanner` fails the build on one.
     *
     * R15 · The anti-gaming verdict is DERIVED here from the session's own trials — the only place that already
     * holds every trial of every block — and handed to the gate. Deriving it rather than accepting it as an
     * argument keeps a single owner of the fact: no call-site can pass a clean verdict for a session whose
     * trials say otherwise. A fired detector withholds the session; it never writes a score and never accuses.
     */
    fun admit(session: SessionResult, lifecycle: LifecycleObservation): SessionResult {
        val gaming = FocusAntiGaming.evaluate(session.blocks.flatMap { it.trials })
        val admission = SessionQualityGate.admit(lifecycle, session.respondedTrials(), gaming)
        val recorded = withLifecycle(session, lifecycle).copy(admission = admission)
        return when (admission) {
            SessionAdmission.Admitted -> recorded
            is SessionAdmission.NotMeasurable -> recorded.copy(
                blocks = recorded.blocks.map { it.copy(metrics = it.metrics.withdrawn()) },
                sessionMetrics = recorded.sessionMetrics.withdrawn(),
            )
        }
    }

    /** FOC-P0-03 · An OBSERVED foreground loss turns every metric into an [MetricAbsence.INTERRUPTED] absence. */
    private fun withLifecycle(session: SessionResult, observation: LifecycleObservation): SessionResult {
        val interrupted = observation is LifecycleObservation.Observed && observation.interruptions.isNotEmpty()
        return if (!interrupted) {
            session.copy(lifecycle = observation)
        } else {
            session.copy(
                lifecycle = observation,
                blocks = session.blocks.map { it.copy(metrics = it.metrics.interrupted()) },
                sessionMetrics = session.sessionMetrics.interrupted(),
            )
        }
    }

    private fun MetricSet.interrupted(): MetricSet =
        mapValues { (key, value) -> MetricValue.unavailable(key, value.unit, value.n, MetricAbsence.INTERRUPTED) }

    /**
     * A stimulus paired with the user's response latency (ms, or null for no response) and the key
     * pressed ([responseKey], null for no response). The key is only consulted for directional
     * two-choice trials (E2/E5); tap-or-withhold trials (E1/E3) ignore it.
     */
    data class RecordedTrial(
        val stimulus: Stimulus,
        val responseLatencyMs: Long?,
        val responseKey: ResponseKey? = null,
        /** The actual stop-signal delay used this trial (E4 only; online, so not in the stimulus). */
        val stopSignalDelayMs: Long? = null,
    )

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
            val correctKey = rec.stimulus.correctResponseKey
            val classification = when {
                rec.stimulus.requiresStop ->
                    // Stop-signal (E4): responding is a failed inhibition, withholding a success.
                    TrialClassifier.classifyStop(rec.responseLatencyMs, rec.stimulus.responseWindowMs)

                correctKey != null && correctKey.isDirectional ->
                    // Two-choice paradigms (E2/E5): scored by which side was chosen.
                    TrialClassifier.classifyChoice(
                        correctKey = correctKey,
                        responseKey = rec.responseKey,
                        responseLatencyMs = rec.responseLatencyMs,
                        anticipationFloorMs = thresholds.anticipationFloorMs,
                        responseWindowMs = rec.stimulus.responseWindowMs,
                    )

                else ->
                    // Tap-or-withhold paradigms (E1/E3): scored by responding (or not) to a target.
                    TrialClassifier.classify(
                        isTarget = rec.stimulus.isTarget,
                        responseLatencyMs = rec.responseLatencyMs,
                        anticipationFloorMs = thresholds.anticipationFloorMs,
                        responseWindowMs = rec.stimulus.responseWindowMs,
                    )
            }
            TrialResult(
                trialId = TrialId("$sessionIdPrefix:${blockId.value}:$index"),
                exerciseId = definition.id,
                mode = mode,
                stimulusIsTarget = rec.stimulus.isTarget,
                requiredStop = rec.stimulus.requiresStop,
                outcome = classification.outcome,
                reactionTime = classification.reactionTime,
                difficulty = difficultyEnd,
                stopSignalDelayMs = rec.stopSignalDelayMs,
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
        val admission = session.admission
        val confident = session.sessionMetrics.values.any { it.confident }
        return if (admission is SessionAdmission.NotMeasurable) {
            // FOC-P0-05 · the honest reason, never "too short": a denied session may have had plenty of trials.
            notMeasurableNote(admission.reason) + " Es un registro de hoy, no una nota ni una conclusión sobre ti."
        } else if (!confident || session.totalTrials < MIN_TRIALS_FOR_SIGNAL) {
            "Esta sesión es demasiado corta para sacar conclusiones. Es un registro de hoy, " +
                "no una nota ni una conclusión sobre ti."
        } else {
            "Esto describe tu práctica de hoy en este ejercicio — influida por sueño, hora y " +
                "ánimo. Por ahora se registra el entrenamiento; no se concluye nada sobre ti."
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

/** FOC-P0-05 · Withdraws the claim of every metric still confident; an already-absent metric keeps its own reason. */
private fun MetricSet.withdrawn(): MetricSet = mapValues { (key, value) ->
    if (value.confident) MetricValue.unavailable(key, value.unit, value.n, MetricAbsence.NOT_ADMITTED) else value
}

/** Responses the user actually made across the whole session (the gate's response guard, FOC-P0-04 rule). */
private fun SessionResult.respondedTrials(): Int =
    blocks.sumOf { block -> block.trials.count { it.outcome.isResponse } }

/** FOC-P0-05 · Why the session does not count, in the user's words. The admitted note is left untouched. */
private fun notMeasurableNote(reason: NotMeasurableReason): String = "Esta sesión no cuenta para tu progreso: " +
    when (reason) {
        NotMeasurableReason.INTERRUPTED -> "la app salió de primer plano durante el ejercicio."
        NotMeasurableReason.NO_RESPONSES -> "no hubo ninguna respuesta."
        NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE ->
            "el patrón de respuestas no nos deja leerla como una medida."
        NotMeasurableReason.LIFECYCLE_NOT_OBSERVED -> "no pudimos comprobar que la app siguiera en primer plano."
    }
