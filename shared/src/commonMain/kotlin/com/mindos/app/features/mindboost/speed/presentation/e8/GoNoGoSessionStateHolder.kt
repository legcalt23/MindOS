package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.speed.data.persistence.ExerciseTelemetryStore
import com.mindos.app.features.mindboost.speed.data.persistence.InMemoryExerciseTelemetryStore
import com.mindos.app.features.mindboost.speed.data.persistence.saveGoNoGoSummary
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoVelocityGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.XorShiftSpeedRng
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedback
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedbackKind
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetrics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOfficialRunAdapter
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOutcome
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoResponse
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScore
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScorer
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTelemetrySummaryV1
import com.mindos.app.features.mindboost.speed.domain.score.OfficialExerciseRunAdapterResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Fase del bucle de E8. Idle → (Foreperiod → Stimulus → Feedback)×N → Finished. */
enum class GoNoGoPhase { Idle, Foreperiod, Stimulus, Feedback, Finished }

/** Estado de UI de E8 · Inhibición Rápida. Score/feedback null hasta terminar (nunca fabricados). */
data class GoNoGoUiState(
    val phase: GoNoGoPhase = GoNoGoPhase.Idle,
    val trialIndex: Int = 0,
    val totalTrials: Int = 0,
    val currentStimulus: GoNoGoStimulus? = null,
    val lastOutcome: GoNoGoOutcome? = null,
    val score: GoNoGoScore? = null,
    val metrics: GoNoGoMetrics? = null,
    val feedback: GoNoGoFeedbackKind? = null,
    val officialRun: OfficialExerciseRunAdapterResult? = null,
    val summaryPersisted: Boolean = false,
    val sessionId: String? = null,
)

/**
 * Driver de sesión de E8 · Go/No-Go Velocity. State machine PURA guiada por eventos de tiempo desde la pantalla
 * (no toca `Clock.System` ni mide tiempo por su cuenta: la pantalla calcula el RT desde el onset y lo pasa). Al
 * terminar deriva outcomes → summary → métricas → score → diagnostics → feedback y persiste el summary histórico
 * vía el store genérico (mismo patrón que E5), best-effort. NO usa el pipeline `MeasurementRun`/`TrialRecord`
 * (el paradigma NO-GO —inhibir hasta el deadline— no encaja en la captura RT-tap compartida).
 */
class GoNoGoSessionStateHolder(
    private val scope: CoroutineScope,
    private val userId: String,
    private val telemetryStore: ExerciseTelemetryStore = InMemoryExerciseTelemetryStore(),
    private val config: GoNoGoConfig = GoNoGoConfig.mixedDifficulty(),
    private val clock: EpochClock = SystemEpochClock,
    private val trialCount: Int = DEFAULT_TRIALS,
) {
    private val generator = GoNoGoVelocityGenerator(config)
    private val stimuli = mutableListOf<GoNoGoStimulus>()
    private val responses = mutableListOf<GoNoGoResponse>()
    private var runSeq = 0
    private var startedAtEpochMs = 0L

    private val _state = MutableStateFlow(GoNoGoUiState(totalTrials = trialCount))
    val state: StateFlow<GoNoGoUiState> = _state.asStateFlow()

    /** Inicia (o reinicia) la corrida: baraja los estímulos y arranca el primer foreperiodo. */
    fun startSession() {
        generator.reset()
        stimuli.clear()
        responses.clear()
        startedAtEpochMs = clock.nowMillis()
        val seed = startedAtEpochMs + runSeq
        runSeq += 1
        val rng = XorShiftSpeedRng(if (seed == 0L) 1L else seed)
        repeat(trialCount) { stimuli.add(generator.next(rng)) }
        _state.value = GoNoGoUiState(
            phase = GoNoGoPhase.Foreperiod,
            trialIndex = 0,
            totalTrials = trialCount,
            currentStimulus = stimuli.firstOrNull(),
            sessionId = "e8-$seed",
        )
    }

    /** El foreperiodo terminó: se muestra el estímulo (onset). La pantalla empieza a medir el RT desde aquí. */
    fun onStimulusShown() {
        if (_state.value.phase == GoNoGoPhase.Foreperiod) {
            _state.value = _state.value.copy(phase = GoNoGoPhase.Stimulus)
        }
    }

    /** Tap durante el foreperiodo (antes del onset): respuesta prematura → resuelve el trial como anticipación. */
    fun onPrematureTap() {
        if (_state.value.phase == GoNoGoPhase.Foreperiod) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = true, responseTimeMs = null))
        }
    }

    /** Tap tras el onset, con RT (ms) desde el onset. Resuelve el trial (GO en plazo, GO tarde, o NO-GO comisión). */
    fun onStimulusTap(rtMs: Long) {
        if (_state.value.phase == GoNoGoPhase.Stimulus) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = false, responseTimeMs = rtMs))
        }
    }

    /** Se agotó la ventana de respuesta sin tap: GO ⇒ omisión, NO-GO ⇒ inhibición correcta. */
    fun onResponseWindowElapsed() {
        if (_state.value.phase == GoNoGoPhase.Stimulus) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = false, responseTimeMs = null))
        }
    }

    /** Avanza al siguiente trial, o termina la corrida si no quedan. */
    fun beginNextTrial() {
        if (_state.value.phase != GoNoGoPhase.Feedback) return
        val next = _state.value.trialIndex + 1
        if (next >= trialCount) {
            finishSession()
        } else {
            _state.value = _state.value.copy(
                phase = GoNoGoPhase.Foreperiod,
                trialIndex = next,
                currentStimulus = stimuli.getOrNull(next),
                lastOutcome = null,
            )
        }
    }

    private fun resolveCurrent(response: GoNoGoResponse) {
        val index = _state.value.trialIndex
        val stimulus = stimuli.getOrNull(index) ?: return
        val outcome = GoNoGoOutcome.resolve(stimulus, response)
        responses.add(response)
        _state.value = _state.value.copy(phase = GoNoGoPhase.Feedback, lastOutcome = outcome)
    }

    /** Aborta la corrida (nav-away/reinicio): vuelve a Idle SIN persistir — una corrida incompleta NO es dato. */
    fun abort() {
        stimuli.clear()
        responses.clear()
        _state.value = GoNoGoUiState(totalTrials = trialCount)
    }

    private fun finishSession() {
        val outcomes = stimuli.indices.mapNotNull { i ->
            responses.getOrNull(i)?.let { GoNoGoOutcome.resolve(stimuli[i], it) }
        }
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        val score = GoNoGoScorer.score(metrics)
        val feedback = GoNoGoFeedback.forRun(metrics, score)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes)
        val sessionId = _state.value.sessionId
        val now = clock.nowMillis()
        val durationActualMs = (now - startedAtEpochMs).coerceAtLeast(0L)
        val completedTrials = outcomes.size
        // The official run-equivalent and the telemetry summary derive from the SAME run summary (no two truths).
        val officialRun = sessionId?.let {
            GoNoGoOfficialRunAdapter.toOfficialRun(
                sessionId = it,
                exerciseId = SpeedExerciseId.E8_INHIBICION.code,
                exerciseVersion = GoNoGoVelocityGenerator.CONFIG_VERSION,
                summary = summary,
                metrics = metrics,
                score = score,
                diagnostics = diagnostics,
                plannedTrials = trialCount,
                completedTrials = completedTrials,
                durationActualMs = durationActualMs,
            )
        }
        _state.value = _state.value.copy(
            phase = GoNoGoPhase.Finished,
            currentStimulus = null,
            score = score,
            metrics = metrics,
            feedback = feedback,
            officialRun = officialRun,
        )
        if (sessionId != null && outcomes.isNotEmpty()) {
            persist(
                GoNoGoTelemetrySummaryV1.of(
                    sessionId = sessionId,
                    exerciseId = SpeedExerciseId.E8_INHIBICION.code,
                    metrics = metrics,
                    summary = summary,
                    diagnostics = diagnostics,
                    createdAtEpochMs = now,
                    plannedTrials = trialCount,
                    completedTrials = completedTrials,
                    durationActualMs = durationActualMs,
                ),
            )
        }
    }

    private fun persist(summaryV1: GoNoGoTelemetrySummaryV1) {
        scope.launch {
            runCatching { telemetryStore.saveGoNoGoSummary(userId, summaryV1) }
                .onFailure { if (it is CancellationException) throw it }
            _state.value = _state.value.copy(summaryPersisted = true)
        }
    }

    companion object {
        const val DEFAULT_TRIALS = 40

        /** Ventana de gracia (ms) tras el deadline para capturar una respuesta GO tardía antes de cerrar el trial. */
        const val LATE_GRACE_MS = 400L
    }
}
