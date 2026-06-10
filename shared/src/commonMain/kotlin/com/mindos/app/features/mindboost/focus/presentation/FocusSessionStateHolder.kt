package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveConfig
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.FatigueSignal
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusPhase
import com.mindos.app.features.mindboost.focus.presentation.state.FocusRunningModel
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Drives one E1 (Vigía) session in real time: presents stimuli with monotonic
 * timing, captures taps, and hands the raw timing to the pure
 * [FocusSessionAssembler] for classification, metrics and adaptation. The holder
 * ORCHESTRATES; it never computes metrics itself (05_/09_ separation).
 *
 * Reaction time is always `tapNanos - onsetNanos` from the graph's monotonic
 * clock; wall-clock is used only for persistence ordering.
 *
 * TooManyFunctions is suppressed deliberately: this is a single cohesive
 * real-time orchestrator whose private helpers (timing, classification, counters,
 * emission) read more clearly split than inlined into one long method.
 */
@Suppress("TooManyFunctions")
class FocusSessionStateHolder(
    private val graph: FocusGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
) {
    private val definition = graph.e1Definition

    var uiState: FocusSessionUiState by mutableStateOf(
        FocusSessionUiState.Intro(
            exerciseName = definition.displayName,
            exerciseDescription = definition.plainDescription,
            template = SessionTemplate.MAINTENANCE_5,
        ),
    )
        private set

    private var sessionJob: Job? = null
    private var pendingTap: CompletableDeferred<Long>? = null

    // Live counters (main-thread only, mutated inside the session coroutine).
    private var hits = 0
    private var lapses = 0
    private var falseAlarms = 0
    private var lastReactionMs: Long? = null

    fun prepare(template: SessionTemplate) {
        uiState = FocusSessionUiState.Intro(
            exerciseName = definition.displayName,
            exerciseDescription = definition.plainDescription,
            template = template,
        )
    }

    fun start(template: SessionTemplate, subjectiveState: SubjectiveState? = null) {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession(template, subjectiveState) }
    }

    /** UI calls this on every tap during the active field. */
    fun onTap() {
        val tap = pendingTap ?: return
        if (!tap.isCompleted) tap.complete(graph.monotonicClock.nowNanos())
    }

    fun abort(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        pendingTap = null
        uiState = FocusSessionUiState.Aborted(reason)
    }

    private suspend fun runSession(template: SessionTemplate, subjectiveState: SubjectiveState?) {
        val profile = graph.profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val setup = RunSetup(
            template = template,
            profile = profile,
            thresholds = profile.thresholds,
            difficulty = profile.startingDifficultyFor(definition.id),
            startedAtMs = graph.epochClock.nowMillis(),
            subjectiveState = subjectiveState,
        )
        resetCounters()

        val trialCount = template.estimatedTrialCount()
        emitRunning(FocusPhase.WARMUP, FocusFieldState.Idle, 0, trialCount)
        delay(WARMUP_COUNTDOWN_MS)

        val spec = BlockSpec(targetTrialCount = trialCount, mode = Mode.TRAINING)
        val stimuli = graph.e1Generator.generate(setup.difficulty, spec, seed)
        val recorded = ArrayList<FocusSessionAssembler.RecordedTrial>(stimuli.size)

        stimuli.forEachIndexed { index, stimulus ->
            emitRunning(FocusPhase.CORE, FocusFieldState.Idle, index, trialCount)
            delay(stimulus.interStimulusDelayMs)

            val onsetNanos = graph.monotonicClock.nowNanos()
            emitRunning(FocusPhase.CORE, fieldFor(stimulus), index, trialCount)
            val tapNanos = awaitTap(stimulus.responseWindowMs)
            val latencyMs = tapNanos?.let { ((it - onsetNanos) / NANOS_PER_MS).coerceAtLeast(0) }
            recorded += FocusSessionAssembler.RecordedTrial(stimulus, latencyMs)

            val classification = TrialClassifier.classify(
                isTarget = stimulus.isTarget,
                responseLatencyMs = latencyMs,
                anticipationFloorMs = setup.thresholds.anticipationFloorMs,
                responseWindowMs = stimulus.responseWindowMs,
            )
            applyCounters(classification.outcome, latencyMs, setup.thresholds)
            emitRunning(
                FocusPhase.CORE,
                FocusFieldState.Feedback(feedbackFor(classification.outcome)),
                index + 1, trialCount,
            )
            delay(FEEDBACK_MS)
        }

        emitRunning(FocusPhase.COOLDOWN, FocusFieldState.Idle, trialCount, trialCount)
        delay(COOLDOWN_MS)

        finishSession(setup, recorded)
    }

    private suspend fun finishSession(setup: RunSetup, recorded: List<FocusSessionAssembler.RecordedTrial>) {
        val endedAtMs = graph.epochClock.nowMillis()
        val sessionIdValue = "$userId:$endedAtMs"
        val block = graph.assembler.assembleBlock(
            blockId = BlockId("$sessionIdValue:b0"),
            definition = definition,
            mode = Mode.TRAINING,
            difficultyStart = setup.difficulty,
            difficultyEnd = setup.difficulty,
            thresholds = setup.thresholds,
            recorded = recorded,
            sessionIdPrefix = userId,
            epochMillis = endedAtMs,
        )
        val session = graph.assembler.assembleSession(
            sessionId = SessionId(sessionIdValue),
            mode = Mode.TRAINING,
            definition = definition,
            blocks = listOf(block),
            startedAtMs = setup.startedAtMs,
            endedAtMs = endedAtMs,
            subjectiveState = setup.subjectiveState,
        )

        val decision = graph.adaptiveEngine.decide(
            AdaptiveInput(
                exercise = definition,
                current = setup.difficulty,
                recentBlock = block.metrics,
                recentTrials = block.trials,
                perAxisMastery = emptyMap(),
                fatigue = FatigueSignal.NONE,
                blocksSinceChange = Int.MAX_VALUE,
                config = AdaptiveConfig(),
            ),
        )

        graph.historyRepository.append(session)
        graph.profileRepository.save(
            setup.profile.copy(
                startingDifficulty = setup.profile.startingDifficulty +
                    (definition.id to decision.nextDifficulty),
                totalSessions = setup.profile.totalSessions + 1,
                updatedAtMs = endedAtMs,
            ),
        )

        uiState = FocusSessionUiState.Finished(graph.assembler.buildReport(session), setup.template)
    }

    private suspend fun awaitTap(windowMs: Long): Long? {
        val deferred = CompletableDeferred<Long>()
        pendingTap = deferred
        val result = withTimeoutOrNull(windowMs) { deferred.await() }
        pendingTap = null
        return result
    }

    private fun fieldFor(stimulus: Stimulus): FocusFieldState {
        val payload = stimulus.payload
        val contrast = if (payload is StimulusPayload.Vigilance) payload.contrast.toFloat() else 1f
        return if (stimulus.isTarget) FocusFieldState.Signal(contrast) else FocusFieldState.Lure(contrast)
    }

    private fun feedbackFor(outcome: TrialOutcome): FeedbackKind = when (outcome) {
        TrialOutcome.HIT -> FeedbackKind.HIT
        TrialOutcome.MISS, TrialOutcome.TIMEOUT -> FeedbackKind.MISS
        TrialOutcome.FALSE_ALARM -> FeedbackKind.FALSE_ALARM
        TrialOutcome.CORRECT_REJECTION -> FeedbackKind.CORRECT_REJECTION
        TrialOutcome.ANTICIPATION -> FeedbackKind.TOO_EARLY
    }

    private fun applyCounters(outcome: TrialOutcome, latencyMs: Long?, thresholds: IndividualThresholds) {
        when (outcome) {
            TrialOutcome.HIT -> {
                hits++
                lastReactionMs = latencyMs
                if (latencyMs != null && latencyMs > thresholds.lapseRtMs) lapses++
            }
            TrialOutcome.FALSE_ALARM -> falseAlarms++
            else -> Unit
        }
    }

    private fun resetCounters() {
        hits = 0
        lapses = 0
        falseAlarms = 0
        lastReactionMs = null
    }

    private fun emitRunning(
        phase: FocusPhase,
        field: FocusFieldState,
        trialsDone: Int,
        trialsTotal: Int,
    ) {
        uiState = FocusSessionUiState.Running(
            FocusRunningModel(
                exerciseName = definition.displayName,
                phase = phase,
                field = field,
                progress = if (trialsTotal == 0) 0f else (trialsDone.toFloat() / trialsTotal),
                trialsDone = trialsDone,
                trialsTotal = trialsTotal,
                hits = hits,
                lapses = lapses,
                falseAlarms = falseAlarms,
                lastReactionMs = lastReactionMs,
            ),
        )
    }

    /** Immutable per-run setup, captured once so [finishSession] stays small. */
    private data class RunSetup(
        val template: SessionTemplate,
        val profile: UserCognitiveProfile,
        val thresholds: IndividualThresholds,
        val difficulty: DifficultyVector,
        val startedAtMs: Long,
        val subjectiveState: SubjectiveState?,
    )

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
        const val WARMUP_COUNTDOWN_MS = 4_000L
        const val COOLDOWN_MS = 2_500L
        const val FEEDBACK_MS = 320L
    }
}
