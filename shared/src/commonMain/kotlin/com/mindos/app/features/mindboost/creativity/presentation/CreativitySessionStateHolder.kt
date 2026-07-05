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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // Opaque write-ahead id for the canonical SessionRecord — regenerated per round, never derived from
    // userId/exercise/timestamp and kept separate from ActiveCreativitySession.sessionId. Free text (ideas,
    // prompts) never reaches the recorder: only this id + lifecycle counters do.
    private var recorderSessionId: String = ""

    fun startExercise(exerciseId: CreativityExerciseId) {
        startMs = monotonic.now()
        val active = coordinator.start(userId, exerciseId, monotonicStartMs = startMs)
        _state.value = CreativityUiState.InProgress(active)
        val sessionId = sessionIdGenerator.newSessionId()
        recorderSessionId = sessionId
        // Write-ahead STARTED: lifecycle only (opaque id, block, one planned round). The epoch start reuses the
        // coordinator's wall-clock stamp (active.createdAtMs) so the holder never touches Clock.System and needs
        // no extra clock. Best-effort: the recorder is total, so a storage failure can never break the round.
        val startedAtMs = active.createdAtMs
        scope.launch { recorder.onStart(sessionId, ScoreModuleId.CREATIVITY, startedAtMs, plannedTrials = 1) }
    }

    fun submit(ideas: List<String>) {
        val active = when (val current = _state.value) {
            is CreativityUiState.InProgress -> current.active
            is CreativityUiState.Rejected -> current.active
            else -> return
        }
        val elapsed = monotonic.now() - startMs
        _state.value = CreativityUiState.Submitting(active)
        scope.launch {
            val result = coordinator.submit(active, ideas, elapsed)
            _state.value = when (result) {
                is CreativitySubmitResult.Completed -> CreativityUiState.Completed(active, result.session)
                is CreativitySubmitResult.Rejected -> CreativityUiState.Rejected(active, result.reason)
            }
            // FINALIZED only on a real completion (a rejection keeps the round open to resubmit). Counts only:
            // completedTrials is a fixed round count (1), deliberately NOT ideas.size — idea count IS the fluency
            // rubric signal, and the session log must carry no cognitive or content signal.
            if (result is CreativitySubmitResult.Completed) {
                recorder.onFinalize(recorderSessionId, completedTrials = 1, activeDurationMs = elapsed)
            }
        }
    }

    fun reset() {
        _state.value = CreativityUiState.Idle
    }
}
