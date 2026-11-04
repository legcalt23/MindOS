package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.NoOpAppLifecycleProbe
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineBlock
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeExerciseMapping
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.presentation.exercise.FocusFixedBlockRunner
import com.mindos.app.features.mindboost.focus.presentation.exercise.FocusResponseEvent
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineSummary
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineUiState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusMeasureRunningModel
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Drives the initial baseline (06_ §2–§3): runs the [FocusGraph.baselineProtocol] blocks at FIXED
 * difficulty — calibration exercises in CALIBRATION mode, the reserved probe in EVALUATION mode — never
 * adapting, then folds the finished blocks via the pure [BaselineAssembler] into personalized thresholds,
 * a per-exercise starting difficulty and the [com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot]
 * (the pre side of pre/post change). The snapshot + profile persist encrypted, and the reserved-probe run
 * is recorded apart from training history.
 *
 * The holder only ORCHESTRATES (timing, response capture, persistence); the [FocusFixedBlockRunner] runs
 * each block and the assembler/calibration engine do all computation. The protocol uses tap/choice
 * paradigms only (E1/E2/E3 + a PVT probe), so the runner — not the richer training holder — is enough.
 *
 * TooManyFunctions is suppressed: this is one cohesive orchestrator split into small, readable helpers.
 */
@Suppress("TooManyFunctions")
class FocusBaselineStateHolder(
    private val graph: FocusGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
    /**
     * Fail-closed by default (mirrors `FocusSessionStateHolder`): a call-site that wires nothing hands the gate
     * a TYPED absence, so the *pre* snapshot is withheld rather than published on an unwatched window.
     */
    private val lifecycleProbe: AppLifecycleProbe = NoOpAppLifecycleProbe,
) {
    private val runner = FocusFixedBlockRunner(graph.monotonicClock)
    private val protocol = graph.baselineProtocol

    var uiState: FocusBaselineUiState by mutableStateOf(introState())
        private set

    private var job: Job? = null
    private var pendingResponse: CompletableDeferred<FocusResponseEvent>? = null

    fun start(subjectiveState: SubjectiveState? = null) {
        if (job?.isActive == true) return
        job = scope.launch { runBaseline(subjectiveState) }
    }

    /** UI single-tap response (E1/E3 calibration + PVT probe). */
    fun onTap() = onResponse(ResponseKey.PRIMARY)

    /** UI directional/tap response (LEFT/RIGHT for the flanker calibration block, PRIMARY for taps). */
    fun onResponse(key: ResponseKey) {
        val pending = pendingResponse ?: return
        if (!pending.isCompleted) pending.complete(FocusResponseEvent(graph.monotonicClock.nowNanos(), key))
    }

    fun abort(reason: String = "Base interrumpida") {
        job?.cancel()
        job = null
        pendingResponse = null
        // Close the lifecycle window with the run it belonged to; an aborted baseline publishes nothing.
        lifecycleProbe.stop()
        uiState = FocusBaselineUiState.Aborted(reason)
    }

    private suspend fun runBaseline(subjectiveState: SubjectiveState?) {
        val profile = graph.profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val calibrationBlocks = ArrayList<BlockResult>()
        var probeBlock: BlockResult? = null
        var probeSession: SessionResult? = null
        // A reserved form is spent the moment it is PICKED, and the supply is finite (see ProbeBank).
        // Everything between the pick and the user actually seeing the form — abort(), disposal of the
        // holder's scope, an uncaught throw from the block runner — must give it back, so the revert lives
        // in the run's own finally and not in abort() (abort is only one of those three paths). Local to
        // this call because releaseProbe inverts exactly ONE burn and is not idempotent.
        var unspentProbeId: String? = null

        try {
            protocol.blocks.forEachIndexed { index, block ->
                val plan = resolveBlock(block, index)
                if (plan.probeId != null) unspentProbeId = plan.probeId
                val endedAtMs = graph.epochClock.nowMillis()
                // MB-FOCUS-0114 · the window covers the reserved-probe block and nothing else: that block IS the
                // pre snapshot, so an interruption during the earlier calibration says nothing about it.
                if (block.isProbe) lifecycleProbe.start()
                val result = runner.run(
                    stimuli = graph.fixedBlockForm(
                        id = plan.exerciseId,
                        difficulty = plan.difficulty,
                        spec = BlockSpec(targetTrialCount = block.targetTrialCount, mode = block.mode),
                        // The seed is still a wall-clock value the caller minted and never persisted, so a baseline
                        // form remains non-reproducible after the fact. Naming its RNG version does not fix that; it
                        // only stops the draw being unversioned. The gap is declared, not closed here.
                        seed = SessionSeed(plan.seed, RngVersion.V1),
                    ),
                    trialCount = block.targetTrialCount,
                    mode = block.mode,
                    thresholds = profile.thresholds,
                    awaitResponse = ::awaitResponse,
                    onProgress = { emitRunning(index, plan.label, it) },
                )
                // The user has now SEEN this form: it stays spent whatever happens downstream. Releasing a
                // seen form would trade a supply loss for a practice effect, which is the worse of the two.
                unspentProbeId = null
                val assembled = graph.assembler.assembleBlock(
                    blockId = BlockId("$userId:baseline:$index"),
                    definition = plan.definition,
                    mode = block.mode,
                    difficultyStart = plan.difficulty,
                    difficultyEnd = plan.difficulty,
                    thresholds = profile.thresholds,
                    recorded = result.recorded,
                    sessionIdPrefix = userId,
                    epochMillis = endedAtMs,
                )
                if (block.isProbe) {
                    val admitted = admitProbe(assembled, plan, block.mode, endedAtMs, subjectiveState)
                    probeSession = admitted
                    // The gate's OUTPUT, never the pre-gate local. Measured: swapping this for `assembled`
                    // leaves the suite green, because `persist` short-circuits first — so it is not a second
                    // guard; it only stops a future reader of `probeBlock` getting pre-gate metrics.
                    probeBlock = admitted.blocks.single()
                } else {
                    calibrationBlocks += assembled
                }
            }

            persist(profile, calibrationBlocks, requireNotNull(probeBlock), probeSession, subjectiveState)
        } finally {
            // NonCancellable: the commonest path here is the run's own job being cancelled, and a
            // cancelled coroutine cannot suspend — without this the compensating write never happens.
            unspentProbeId?.let { probeId ->
                withContext(NonCancellable) { graph.probeBankRepository.releaseProbe(userId, probeId) }
            }
        }
    }

    /**
     * MB-FOCUS-0114 · the ONE gate for this lane, same as the training lane's. The lifecycle window is closed
     * HERE, before the session is admitted, so the observation covers exactly the trials just recorded.
     */
    private fun admitProbe(
        assembled: BlockResult,
        plan: BlockPlan,
        mode: Mode,
        endedAtMs: Long,
        subjectiveState: SubjectiveState?,
    ): SessionResult = graph.assembler.admit(
        graph.assembler.assembleSession(
            sessionId = SessionId("$userId:$endedAtMs"),
            mode = mode,
            definition = plan.definition,
            blocks = listOf(assembled),
            startedAtMs = endedAtMs,
            endedAtMs = endedAtMs,
            subjectiveState = subjectiveState,
        ),
        lifecycleProbe.stop(),
    )

    private suspend fun persist(
        profile: UserCognitiveProfile,
        calibrationBlocks: List<BlockResult>,
        probeBlock: BlockResult,
        probeSession: SessionResult?,
        subjectiveState: SubjectiveState?,
    ) {
        val capturedAtMs = graph.epochClock.nowMillis()
        val outcome = graph.baselineAssembler.assemble(
            profile = profile,
            calibrationBlocks = calibrationBlocks,
            probeBlock = probeBlock,
            capturedAtMs = capturedAtMs,
            state = subjectiveState,
            deviceFingerprint = null,
        )
        // Recorded either way: the reserved form WAS administered, and hiding that would lose the only trace of
        // a spent form. The admission travels with the row, so a reader can tell the two apart.
        probeSession?.let { graph.probeBankRepository.recordProbe(userId, it) }

        val admission = probeSession?.admission
        if (admission !is SessionAdmission.Admitted) {
            // Calibration still stands — thresholds and starting difficulty come from the calibration trials,
            // which the gate never touched — so the profile is saved WITHOUT a baseline. The snapshot is not
            // persisted at all: an empty perMetric would let a later retest render "nothing changed" instead of
            // "there is nothing to compare against", which is the omissive half of `null != 0`.
            graph.profileRepository.save(outcome.profile.copy(baseline = null))
            uiState = FocusBaselineUiState.NotMeasurable(notMeasurableCopy(admission))
            return
        }
        graph.profileRepository.save(outcome.profile)
        graph.baselineRepository.save(outcome.snapshot)
        uiState = FocusBaselineUiState.Finished(summaryOf(outcome, calibrationBlocks.size))
    }

    /**
     * User-facing copy for a withheld baseline. Exhaustive over [NotMeasurableReason] so a reason added later
     * cannot fall through to a raw identifier, and `null` (the gate did not run) is not treated as a pass.
     */
    private fun notMeasurableCopy(admission: SessionAdmission?): String = when (admission) {
        is SessionAdmission.NotMeasurable -> when (admission.reason) {
            NotMeasurableReason.INTERRUPTED ->
                "La app pasó a segundo plano durante la prueba final, " +
                    "así que aún no tienes una base con la que comparar."
            NotMeasurableReason.NO_RESPONSES ->
                "No registramos ninguna respuesta, así que aún no tienes una base con la que comparar."
            NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE ->
                "El patrón de respuestas no nos deja leer esta prueba como una medida, " +
                    "así que aún no tienes una base con la que comparar."
            NotMeasurableReason.LIFECYCLE_NOT_OBSERVED ->
                "No pudimos comprobar que la prueba final corriera sin interrupciones, así que aún no tienes base."
        }
        else -> "No pudimos confirmar que la prueba final fuera válida, así que aún no tienes base."
    }

    private suspend fun awaitResponse(windowMs: Long): FocusResponseEvent? {
        val deferred = CompletableDeferred<FocusResponseEvent>()
        pendingResponse = deferred
        val result = withTimeoutOrNull(windowMs) { deferred.await() }
        pendingResponse = null
        return result
    }

    /** Resolves the generator/definition/difficulty/seed for a baseline block (calibration or probe). */
    private suspend fun resolveBlock(block: BaselineBlock, index: Int): BlockPlan {
        if (block.isProbe) {
            // Rotate to the next reserved parallel form (advances the cursor so retests differ).
            val administration = graph.probeBankRepository.nextProbe(userId, BASELINE_WEEK)
            val exerciseId = requireNotNull(ProbeExerciseMapping.exerciseFor(administration.probe)) {
                "Probe ${administration.probe.probeId} has no renderable exercise."
            }
            return BlockPlan(
                exerciseId = exerciseId,
                definition = graph.definitionFor(exerciseId),
                difficulty = administration.probe.fixedDifficulty,
                seed = administration.formSeed,
                label = graph.definitionFor(exerciseId).displayName,
                probeId = administration.probe.probeId,
            )
        }
        val exerciseId = requireNotNull(block.exerciseId) { "Calibration block needs an exercise." }
        return BlockPlan(
            exerciseId = exerciseId,
            definition = graph.definitionFor(exerciseId),
            difficulty = block.fixedDifficulty,
            seed = seed + index,
            label = graph.definitionFor(exerciseId).displayName,
        )
    }

    private fun emitRunning(blockIndex: Int, label: String, progress: FocusFixedBlockRunner.Progress) {
        uiState = FocusBaselineUiState.Running(
            FocusMeasureRunningModel(
                title = "Tu base",
                blockLabel = label,
                field = progress.field,
                blockIndex = blockIndex,
                blockCount = protocol.blocks.size,
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

    private fun introState(): FocusBaselineUiState.Intro = FocusBaselineUiState.Intro(
        estimatedMinutes = (protocol.estimatedDurationMs / MS_PER_MIN).toInt(),
        blockCount = protocol.blocks.size,
        instructions = listOf(
            "Registraremos tu punto de partida con varios bloques a dificultad fija.",
            "No se adapta nada: es una foto honesta de hoy, no una nota.",
            "Hazlo con calma. La última prueba es del mismo tipo que el entrenamiento, con material distinto.",
        ),
    )

    private fun summaryOf(outcome: BaselineAssembler.Outcome, calibratedExercises: Int): FocusBaselineSummary =
        FocusBaselineSummary(
            lapseRtMs = outcome.profile.thresholds.lapseRtMs,
            baselineRtCv = outcome.profile.thresholds.baselineRtCv,
            calibratedExercises = calibratedExercises,
            probeMetrics = outcome.snapshot.perMetric.size,
            note = "Tu base quedó registrada. A partir de aquí, el progreso se compara siempre " +
                "contra esta base, y siempre de forma orientativa.",
        )

    /** Resolved presentation inputs for one baseline block. */
    private data class BlockPlan(
        val exerciseId: ExerciseId,
        val definition: ExerciseDefinition,
        val difficulty: DifficultyVector,
        val seed: Long,
        val label: String,
        /** Non-null only for the reserved-probe block: the burn this plan owes a release for. */
        val probeId: String? = null,
    )

    private companion object {
        const val MS_PER_MIN = 60_000L
        /** The initial baseline measures the vigilance construct (week-1 probe). */
        const val BASELINE_WEEK = 1
    }
}
