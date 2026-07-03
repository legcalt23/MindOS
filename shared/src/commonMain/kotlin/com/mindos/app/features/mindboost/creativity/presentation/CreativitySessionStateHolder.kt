package com.mindos.app.features.mindboost.creativity.presentation

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
) {
    private val _state = MutableStateFlow<CreativityUiState>(CreativityUiState.Idle)
    val state: StateFlow<CreativityUiState> = _state.asStateFlow()

    private var startMs: Long = 0L

    fun startExercise(exerciseId: CreativityExerciseId) {
        startMs = monotonic.now()
        _state.value = CreativityUiState.InProgress(coordinator.start(userId, exerciseId, monotonicStartMs = startMs))
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
            _state.value = when (val result = coordinator.submit(active, ideas, elapsed)) {
                is CreativitySubmitResult.Completed -> CreativityUiState.Completed(active, result.session)
                is CreativitySubmitResult.Rejected -> CreativityUiState.Rejected(active, result.reason)
            }
        }
    }

    fun reset() {
        _state.value = CreativityUiState.Idle
    }
}
