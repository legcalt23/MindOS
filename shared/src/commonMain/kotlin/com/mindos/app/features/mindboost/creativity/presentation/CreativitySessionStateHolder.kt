@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.creativity.presentation

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.creativity.data.persistence.PersistedCreativitySessionDto
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.session.ActiveCreativitySession
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySessionCoordinator
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySubmitResult
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityValidationError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Purely descriptive UI state for one local creativity round. No score claims beyond the rubric. */
sealed interface CreativityUiState {
    data object Idle : CreativityUiState
    data class InProgress(val active: ActiveCreativitySession) : CreativityUiState
    data class Submitting(val active: ActiveCreativitySession) : CreativityUiState
    data class Completed(
        val active: ActiveCreativitySession,
        val session: PersistedCreativitySessionDto,
    ) : CreativityUiState
    data class Rejected(
        val active: ActiveCreativitySession,
        val reason: CreativityValidationError,
    ) : CreativityUiState
}

/**
 * Presentation orchestrator over [CreativitySessionCoordinator]: start a round → capture ideas →
 * submit → reflect the deterministic, offline outcome. Timing uses the injected [MonotonicMillis];
 * persistence/validation/scoring all live in the domain. No AI, no network.
 */
class CreativitySessionStateHolder(
    private val scope: CoroutineScope,
    private val coordinator: CreativitySessionCoordinator,
    private val userId: String,
    private val monotonic: MonotonicMillis = SystemMonotonicMillis,
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
) {
    private val _state = MutableStateFlow<CreativityUiState>(CreativityUiState.Idle)
    val state: StateFlow<CreativityUiState> = _state.asStateFlow()

    private var startMs: Long = 0L

    // Serializes every recorder lifecycle op for this holder so STARTED and FINALIZED never overlap.
    private val recorderMutex = Mutex()

    // Opaque write-ahead id + its STARTED job for the CURRENT round — regenerated per round, never derived from
    // userId/exercise/timestamp and kept separate from ActiveCreativitySession.sessionId. submit() captures both
    // into locals before any suspension, so a later round reassigning them can never cross ids or ordering. Free
    // text (ideas, prompts) never reaches the recorder: only this id + lifecycle counters do.
    private var recorderSessionId: String = ""
    private var recorderStartJob: Job? = null

    fun startExercise(exerciseId: CreativityExerciseId) {
        startMs = monotonic.now()
        val active = coordinator.start(userId, exerciseId, monotonicStartMs = startMs)
        _state.value = CreativityUiState.InProgress(active)
        val sessionId = sessionIdGenerator.newSessionId()
        recorderSessionId = sessionId
        // Write-ahead STARTED: lifecycle only (opaque id, block, one planned round). The epoch start reuses the
        // coordinator's wall-clock stamp so the holder never touches Clock.System. Serialized under the mutex; the
        // job is kept so submit() can join it before finalizing. The recorder is total — a storage failure can't
        // break the round.
        val startedAtMs = active.createdAtMs
        recorderStartJob = scope.launch {
            recorderMutex.withLock {
                recorder.onStart(sessionId, ScoreModuleId.CREATIVITY, startedAtMs, plannedTrials = 1)
            }
        }
    }

    fun submit(ideas: List<String>) {
        val active = when (val current = _state.value) {
            is CreativityUiState.InProgress -> current.active
            is CreativityUiState.Rejected -> current.active
            else -> return
        }
        val elapsed = monotonic.now() - startMs
        // Capture the round's id and STARTED job BEFORE launching, so a later round can never cross them.
        val sessionId = recorderSessionId
        val startJob = recorderStartJob
        _state.value = CreativityUiState.Submitting(active)
        scope.launch {
            val result = coordinator.submit(active, ideas, elapsed)
            _state.value = when (result) {
                is CreativitySubmitResult.Completed -> CreativityUiState.Completed(active, result.session)
                is CreativitySubmitResult.Rejected -> CreativityUiState.Rejected(active, result.reason)
            }
            // FINALIZED only on a real completion (a rejection keeps the round open to resubmit). Wait for this
            // round's STARTED to persist first, then finalize under the mutex — guaranteeing STARTED → FINALIZED
            // with the captured id. Counts only: completedTrials is a fixed round count (1), never ideas.size.
            if (result is CreativitySubmitResult.Completed) {
                startJob?.join()
                recorderMutex.withLock {
                    recorder.onFinalize(sessionId, completedTrials = 1, activeDurationMs = elapsed)
                }
            }
        }
    }

    fun reset() {
        _state.value = CreativityUiState.Idle
    }
}
