package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredRecallRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemoryProfileRepository
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryExerciseCardUi
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryHomeUiState

/**
 * Loads the MindBoost · Memoria home: the user's profile (archetype, cycle week),
 * the implemented exercises, and how many delayed-recall items are due now. Thin
 * and testable with fakes.
 */
class MemoryHomeStateHolder(
    private val profileRepository: MemoryProfileRepository,
    private val deferredRecallRepository: DeferredRecallRepository,
    private val deferredScheduler: DeferredRecallScheduler,
    private val content: MemoryContent,
    private val epochClock: EpochClock,
) {
    var uiState: MemoryHomeUiState by mutableStateOf(MemoryHomeUiState.Loading)
        private set

    suspend fun load(userId: String, displayName: String) {
        val profile = profileRepository.load(userId) ?: MemoryProfileState.initial(userId)
        val deferred = deferredRecallRepository.load(userId)
        val dueCount = deferredScheduler.due(deferred, epochClock.nowMillis()).size

        val exercises = content.supportedExercises()
            .map { MemoryExerciseCatalog.definition(it) }
            .map { MemoryExerciseCardUi(it.id, it.displayName, it.plainDescription) }

        uiState = MemoryHomeUiState.Ready(
            displayName = displayName,
            archetypeName = profile.archetype.name,
            cycleWeek = profile.currentCycleWeek,
            dueDeferredCount = dueCount,
            exercises = exercises,
        )
    }
}
