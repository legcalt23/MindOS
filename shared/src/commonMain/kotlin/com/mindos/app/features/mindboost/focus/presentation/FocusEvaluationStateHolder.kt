package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.NoOpAppLifecycleProbe
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeAdministration
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeExerciseMapping
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.presentation.exercise.FocusFixedBlockRunner
import com.mindos.app.features.mindboost.focus.presentation.exercise.FocusResponseEvent
import com.mindos.app.features.mindboost.focus.presentation.state.FocusEvaluationUiState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusMeasureRunningModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs ONE evaluation retest (06_ §4–§6): a reserved probe in EVALUATION mode (fixed difficulty, no
 * adaptation), recorded apart from training, then compared against the stored baseline via the pure
 * [com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService]. The retest measures
 * the same construct the baseline captured (the vigilance probe), so pre/post are comparable.
 *
 * Honesty is enforced downstream: RCI is gated until enough probe administrations and trusted
 * reliability exist, so the resulting [com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport]
 * shows preliminary deltas — never a "reliable improvement" — until that evidence accrues.
 */
@Suppress("TooManyFunctions")
class FocusEvaluationStateHolder(
    private val graph: FocusGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    /**
     * Fail-closed by default (mirrors `FocusSessionStateHolder`): a call-site that wires nothing hands the gate
     * a TYPED absence, so the retest is withheld rather than published on an unwatched window.
     */
    private val lifecycleProbe: AppLifecycleProbe = NoOpAppLifecycleProbe,
) {
    private val runner = FocusFixedBlockRunner(graph.monotonicClock)

    var uiState: FocusEvaluationUiState by mutableStateOf(loadingIntro())
        private set

    private var job: Job? = null
    private var pendingResponse: CompletableDeferred<FocusResponseEvent>? = null

    /** Resolves whether a retest is possible: it needs a baseline to compare against. */
    suspend fun prepare() {
        val baseline = graph.baselineRepository.load(userId)
        uiState = if (baseline == null) {
            FocusEvaluationUiState.NeedsBaseline(
                "Aún no tienes una base. Haz primero tu evaluación inicial para poder comparar.",
            )
        } else {
            loadingIntro()
        }
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runProbe() }
    }

    fun onTap() = onResponse(ResponseKey.PRIMARY)

    fun onResponse(key: ResponseKey) {
        val pending = pendingResponse ?: return
        if (!pending.isCompleted) pending.complete(FocusResponseEvent(graph.monotonicClock.nowNanos(), key))
    }

    fun abort(reason: String = "Evaluación interrumpida") {
        job?.cancel()
        job = null
        pendingResponse = null
        // Close the lifecycle window with the run it belonged to; an aborted retest publishes nothing.
        lifecycleProbe.stop()
        uiState = FocusEvaluationUiState.Aborted(reason)
    }

    /**
     * User-facing copy for a withheld retest. Exhaustive over [NotMeasurableReason] so a reason added later
     * cannot fall through to a raw identifier, and `null` (the gate did not run) is not treated as a pass.
     */
    private fun notMeasurableCopy(admission: SessionAdmission?): String = when (admission) {
        is SessionAdmission.NotMeasurable -> when (admission.reason) {
            NotMeasurableReason.INTERRUPTED ->
                "La app pasó a segundo plano durante la prueba, así que esta vez no se compara con tu base."
            NotMeasurableReason.NO_RESPONSES ->
                "No registramos ninguna respuesta, así que esta vez no se compara con tu base."
            NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE ->
                "El patrón de respuestas no nos deja leer esta prueba como una medida, " +
                    "así que esta vez no se compara con tu base."
            NotMeasurableReason.LIFECYCLE_NOT_OBSERVED ->
                "No pudimos comprobar que la prueba corriera sin interrupciones, así que no se compara con tu base."
        }
        else -> "No pudimos confirmar que esta prueba fuera comparable con tu base."
    }

    /**
     * The probe's form, resolved through the graph so a migrated exercise takes its versioned path.
     *
     * The seed comes from the reserved probe bank, so unlike the baseline's wall-clock seed it is at least
     * stable per administration. It is still not persisted with a form identity, so a probe form cannot be
     * rebuilt after the fact — declared here, not closed by this slice.
     */
    private fun probeForm(exerciseId: ExerciseId, administration: ProbeAdministration): List<Stimulus> =
        graph.fixedBlockForm(
            id = exerciseId,
            difficulty = administration.probe.fixedDifficulty,
            spec = BlockSpec(targetTrialCount = EVALUATION_PROBE_TRIALS, mode = Mode.EVALUATION),
            seed = SessionSeed(administration.formSeed, RngVersion.V1),
        )

    private suspend fun runProbe() {
        val baseline = graph.baselineRepository.load(userId)
        if (baseline == null) {
            uiState = FocusEvaluationUiState.NeedsBaseline("Falta tu base; no hay con qué comparar.")
            return
        }
        val profile = graph.profileRepository.load(userId)
        val thresholds = profile?.thresholds ?: IndividualThresholds()

        // Count prior probe administrations BEFORE recording this retest — this gates RCI reliability. Only
        // ADMITTED runs count: a run that was never measurable is not evidence that a measurement repeated,
        // and a row written before the gate existed carries a null admission, which is not a pass either.
        val priorAdministrations = graph.probeBankRepository
            .probeHistory(userId, PROBE_HISTORY_LIMIT)
            .count { it.admission is SessionAdmission.Admitted }

        // The reserved form is spent the moment it is PICKED, and the supply is finite (see ProbeBank).
        // Everything between the pick and the user actually seeing it — abort(), disposal of the holder's
        // scope, an uncaught throw (the requireNotNull below is one) — must give the form back, so the
        // revert lives here and not in abort(), which is only one of those three paths.
        val administration = graph.probeBankRepository.nextProbe(userId, EVALUATION_WEEK)
        var formSeen = false
        try {
            administerProbe(baseline, thresholds, priorAdministrations, administration) { formSeen = true }
        } finally {
            // NonCancellable: the commonest path here is this run's own job being cancelled, and a
            // cancelled coroutine cannot suspend — without this the compensating write never happens.
            if (!formSeen) {
                withContext(NonCancellable) {
                    graph.probeBankRepository.releaseProbe(userId, administration.probe.probeId)
                }
            }
        }
    }

    /**
     * Administers one reserved probe. [onFormSeen] fires the instant the user has SEEN the form: from then
     * on it stays spent whatever happens downstream, because releasing a seen form would trade a supply
     * loss for a practice effect — the worse of the two.
     */
    private suspend fun administerProbe(
        baseline: BaselineSnapshot,
        thresholds: IndividualThresholds,
        priorAdministrations: Int,
        administration: ProbeAdministration,
        onFormSeen: () -> Unit,
    ) {
        val exerciseId = requireNotNull(ProbeExerciseMapping.exerciseFor(administration.probe)) {
            "Probe ${administration.probe.probeId} has no renderable exercise."
        }
        val definition = graph.definitionFor(exerciseId)
        val endedAtMs = graph.epochClock.nowMillis()

        // MB-FOCUS-0114 · the window covers the probe block and nothing else: the block IS the measurement, so
        // an interruption outside it says nothing about these trials.
        lifecycleProbe.start()
        val result = runner.run(
            stimuli = probeForm(exerciseId, administration),
            trialCount = EVALUATION_PROBE_TRIALS,
            mode = Mode.EVALUATION,
            thresholds = thresholds,
            awaitResponse = ::awaitResponse,
            onProgress = { emitRunning(definition.displayName, it) },
        )
        onFormSeen()
        // Closed BEFORE anything is assembled, so the observation covers exactly the recorded trials.
        val lifecycle = lifecycleProbe.stop()
        val block = graph.assembler.assembleBlock(
            blockId = BlockId("$userId:eval:$endedAtMs"),
            definition = definition,
            mode = Mode.EVALUATION,
            difficultyStart = administration.probe.fixedDifficulty,
            difficultyEnd = administration.probe.fixedDifficulty,
            thresholds = thresholds,
            recorded = result.recorded,
            sessionIdPrefix = userId,
            epochMillis = endedAtMs,
        )
        // MB-FOCUS-0114 · the ONE gate, same as the training lane: what the lifecycle sensor saw plus the
        // response guard decide whether this retest may become a published comparison. Everything below reads
        // `session`, never `assembled` — `focusQualityGateScanner` fails the build on a second path.
        val session = graph.assembler.admit(
            graph.assembler.assembleSession(
                sessionId = SessionId("$userId:$endedAtMs"),
                mode = Mode.EVALUATION,
                definition = definition,
                blocks = listOf(block),
                startedAtMs = endedAtMs,
                endedAtMs = endedAtMs,
                subjectiveState = null,
            ),
            lifecycle,
        )
        // Recorded either way: the reserved form WAS administered, and hiding that would lose the only trace
        // of a spent form. The admission travels with the row, so a reader can tell the two apart.
        graph.probeBankRepository.recordProbe(userId, session)

        val admission = session.admission
        if (admission !is SessionAdmission.Admitted) {
            uiState = FocusEvaluationUiState.NotMeasurable(notMeasurableCopy(admission))
            return
        }

        val evaluation = graph.evaluationService.evaluate(
            baseline = baseline,
            retestMetrics = session.blocks.single().metrics,
            priorAdministrations = priorAdministrations,
            capturedAtMs = endedAtMs,
            repeatsEarlierForm = administration.repeatsEarlierForm,
        )
        uiState = FocusEvaluationUiState.Finished(
            result = evaluation,
            transfer = graph.evaluationService.transferReport(evaluation),
            repeatsEarlierForm = administration.repeatsEarlierForm,
        )
    }

    private suspend fun awaitResponse(windowMs: Long): FocusResponseEvent? {
        val deferred = CompletableDeferred<FocusResponseEvent>()
        pendingResponse = deferred
        val result = withTimeoutOrNull(windowMs) { deferred.await() }
        pendingResponse = null
        return result
    }

    private fun emitRunning(label: String, progress: FocusFixedBlockRunner.Progress) {
        uiState = FocusEvaluationUiState.Running(
            FocusMeasureRunningModel(
                title = "Evaluación",
                blockLabel = label,
                field = progress.field,
                blockIndex = 0,
                blockCount = 1,
                progress = if (progress.trialsTotal == 0) 0f else progress.trialsDone.toFloat() / progress.trialsTotal,
                trialsDone = progress.trialsDone,
                trialsTotal = progress.trialsTotal,
                hits = progress.counters.hits,
                lapses = progress.counters.lapses,
                falseAlarms = progress.counters.falseAlarms,
                lastReactionMs = progress.counters.lastReactionMs,
            ),
        )
    }

    private fun loadingIntro(): FocusEvaluationUiState.Intro = FocusEvaluationUiState.Intro(
        estimatedMinutes = (EVALUATION_PROBE_TRIALS * AVG_TRIAL_MS / MS_PER_MIN).toInt().coerceAtLeast(1),
        probeName = "Prueba de vigilancia",
        instructions = listOf(
            "Una prueba a dificultad fija: el mismo tipo de tarea que entrenas, con material distinto.",
            "Toca rápido en cada destello verde; ignora el violeta.",
            "Sirve para comparar con tu base, no para entrenar.",
        ),
    )

    private companion object {
        const val MS_PER_MIN = 60_000L
        const val AVG_TRIAL_MS = 3_800L
        const val EVALUATION_PROBE_TRIALS = 55
        const val PROBE_HISTORY_LIMIT = 50
        /** Retests measure the vigilance construct (week-1 probe), matching the initial baseline. */
        const val EVALUATION_WEEK = 1
    }
}
