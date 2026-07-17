package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineBlock
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeExerciseMapping
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
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
import kotlinx.coroutines.launch
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
        uiState = FocusBaselineUiState.Aborted(reason)
    }

    private suspend fun runBaseline(subjectiveState: SubjectiveState?) {
        val profile = graph.profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val calibrationBlocks = ArrayList<BlockResult>()
        var probeBlock: BlockResult? = null
        var probeSession: SessionResult? = null

        protocol.blocks.forEachIndexed { index, block ->
            val plan = resolveBlock(block, index)
            val endedAtMs = graph.epochClock.nowMillis()
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
                probeBlock = assembled
                probeSession = graph.assembler.assembleSession(
                    sessionId = SessionId("$userId:$endedAtMs"),
                    mode = block.mode,
                    definition = plan.definition,
                    blocks = listOf(assembled),
                    startedAtMs = endedAtMs,
                    endedAtMs = endedAtMs,
                    subjectiveState = subjectiveState,
                )
            } else {
                calibrationBlocks += assembled
            }
        }

        persist(profile, calibrationBlocks, requireNotNull(probeBlock), probeSession, subjectiveState)
    }

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
        graph.profileRepository.save(outcome.profile)
        graph.baselineRepository.save(outcome.snapshot)
        probeSession?.let { graph.probeBankRepository.recordProbe(userId, it) }
        uiState = FocusBaselineUiState.Finished(summaryOf(outcome, calibrationBlocks.size))
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
            "Mediremos tu punto de partida con varios bloques a dificultad fija.",
            "No se adapta nada: es una foto honesta de hoy, no una nota.",
            "Hazlo con calma. La última prueba no se entrena: mide transferencia.",
        ),
    )

    private fun summaryOf(outcome: BaselineAssembler.Outcome, calibratedExercises: Int): FocusBaselineSummary =
        FocusBaselineSummary(
            lapseRtMs = outcome.profile.thresholds.lapseRtMs,
            baselineRtCv = outcome.profile.thresholds.baselineRtCv,
            calibratedExercises = calibratedExercises,
            probeMetrics = outcome.snapshot.perMetric.size,
            note = "Tu base quedó registrada. A partir de aquí, la mejora real se mide repitiendo " +
                "pruebas no entrenadas — nunca se da por hecha.",
        )

    /** Resolved presentation inputs for one baseline block. */
    private data class BlockPlan(
        val exerciseId: ExerciseId,
        val definition: ExerciseDefinition,
        val difficulty: DifficultyVector,
        val seed: Long,
        val label: String,
    )

    private companion object {
        const val MS_PER_MIN = 60_000L
        /** The initial baseline measures the vigilance construct (week-1 probe). */
        const val BASELINE_WEEK = 1
    }
}
