package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.presentation.state.FocusHomeUiState

/**
 * Loads the MindBoost home: the user's profile, a short history for streak/last
 * index, and the recommended exercise for today. Thin and testable with fakes.
 */
class FocusHomeStateHolder(
    private val profileRepository: FocusProfileRepository,
    private val historyRepository: SessionHistoryRepository,
) {
    var uiState: FocusHomeUiState by mutableStateOf(FocusHomeUiState.Loading)
        private set

    suspend fun load(userId: String, displayName: String) {
        val profile = profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val recent = historyRepository.recent(userId, RECENT_LIMIT)
        val lastIndex = recent.firstOrNull()
            ?.sessionMetrics
            ?.get(MetricKey.SUSTAINED_FOCUS_INDEX)
            ?.takeIf { it.confident }
            ?.value

        // Today's recommendation: the only implemented exercise for now (E1).
        val recommended = ExerciseCatalog.implemented.firstOrNull() ?: ExerciseId.E1_VIGIA
        val definition = ExerciseCatalog.definition(recommended)

        uiState = FocusHomeUiState.Ready(
            displayName = displayName,
            cycleWeek = profile.cycleWeek,
            streakDays = estimateStreak(recent.map { it.endedAtMs }),
            totalSessions = profile.totalSessions,
            lastSustainedFocusIndex = lastIndex,
            needsBaseline = profile.baseline == null,
            recommendedExercise = recommended,
            recommendedExerciseName = definition.displayName,
            recommendedDescription = definition.plainDescription,
        )
    }

    /** Coarse streak estimate: count of distinct recent days with a session, newest-first. */
    private fun estimateStreak(endTimestamps: List<Long>): Int {
        if (endTimestamps.isEmpty()) return 0
        val days = endTimestamps.map { it / MILLIS_PER_DAY }.toSortedSet().toList().asReversed()
        var streak = 1
        for (i in 1 until days.size) {
            if (days[i - 1] - days[i] == 1L) streak++ else break
        }
        return streak
    }

    private companion object {
        const val RECENT_LIMIT = 30
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
