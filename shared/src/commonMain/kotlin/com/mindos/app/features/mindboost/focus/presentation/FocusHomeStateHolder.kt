package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultOvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.OvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.OvertrainingVerdict
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.session.DailyPlanGenerator
import com.mindos.app.features.mindboost.focus.domain.session.DefaultDailyPlanGenerator
import com.mindos.app.features.mindboost.focus.domain.session.TrainingCycle
import com.mindos.app.features.mindboost.focus.domain.session.TrainingCycles
import com.mindos.app.features.mindboost.focus.presentation.state.FocusExerciseCardUi
import com.mindos.app.features.mindboost.focus.presentation.state.FocusHomeUiState

/**
 * Loads the MindBoost home: the user's profile, a short history for streak/last
 * index, and the recommended exercise for today. Thin and testable with fakes.
 */
class FocusHomeStateHolder(
    private val profileRepository: FocusProfileRepository,
    private val historyRepository: SessionHistoryRepository,
    private val overtrainingGuard: OvertrainingGuard = DefaultOvertrainingGuard(),
    private val dailyPlanGenerator: DailyPlanGenerator = DefaultDailyPlanGenerator(),
    private val trainingCycle: TrainingCycle = TrainingCycles.STANDARD_8_WEEK,
    /**
     * S0 containment of the v1 score surfaces (`MB-ENG-0007`). Injected as a lambda, mirroring
     * `SyncingFocusSessionHistoryRepository.syncEnabled`, so a test can drive both states without
     * mutating the global hub. When it reads `false` the raw `SUSTAINED_FOCUS_INDEX` is never even
     * put into the UI state — the number is withheld at the source, not merely hidden by the view.
     */
    private val scoreSurfacesEnabled: () -> Boolean = { FeatureFlagsHub.current.focusScoreSurfacesEnabled },
) {
    var uiState: FocusHomeUiState by mutableStateOf(FocusHomeUiState.Loading)
        private set

    suspend fun load(userId: String, displayName: String) {
        val profile = profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val recent = historyRepository.recent(userId, RECENT_LIMIT)
        // S0: with the containment flag OFF the index is not read at all, so no v1 number can reach the UI.
        val surfacesVisible = scoreSurfacesEnabled()
        FocusScoreContainmentBreadcrumb.reportOnce(surfacesVisible)
        val lastIndex = recent.firstOrNull()
            ?.sessionMetrics
            ?.get(MetricKey.SUSTAINED_FOCUS_INDEX)
            ?.takeIf { it.confident }
            ?.value
            ?.takeIf { surfacesVisible }

        // Every exercise wired end-to-end is selectable; the first is today's recommendation.
        val exercises = ExerciseCatalog.implemented.map { id ->
            val def = ExerciseCatalog.definition(id)
            FocusExerciseCardUi(id = id, name = def.displayName, description = def.plainDescription)
        }
        // Today's recommendation comes from the daily plan. Availability and recovery ARE live inputs;
        // the WEEK factor is not: `cycleWeek` has no production writer, so the plan always resolves week 1
        // and the deload weeks never arrive (`MB-FOCUS-0103`). Nor is the rest of the plan consumed —
        // `volumeMultiplier`, `capDifficulty` and `includeProbe` have zero production readers
        // (`MB-FOCUS-0104`), so only `blocks`, `recoveryNote` and `isRestDay` reach the user.
        val plan = dailyPlanGenerator.generate(
            profile = profile,
            cycle = trainingCycle,
            availableMinutes = RECOMMENDED_MINUTES,
            recent = recent,
        )
        val recommended = plan.blocks.firstOrNull()?.exerciseId
            ?: ExerciseCatalog.implemented.firstOrNull()
            ?: ExerciseId.E1_VIGIA
        val definition = ExerciseCatalog.definition(recommended)
        val verdict = overtrainingGuard.check(profile, recent)
        val recoveryNote = plan.recoveryNote
            ?: if (plan.isRestDay) "Hoy se recomienda descanso." else null

        uiState = FocusHomeUiState.Ready(
            displayName = displayName,
            cycleWeek = plan.cycleWeek,
            streakDays = estimateStreak(recent.map { it.endedAtMs }),
            totalSessions = profile.totalSessions,
            lastSustainedFocusIndex = lastIndex,
            scoreSurfacesVisible = surfacesVisible,
            needsBaseline = profile.baseline == null,
            recommendedExercise = recommended,
            recommendedExerciseName = definition.displayName,
            recommendedDescription = definition.plainDescription,
            exercises = exercises,
            overtrainingNote = noteOf(verdict),
            forceRest = verdict is OvertrainingVerdict.ForceRest,
            recoveryNote = recoveryNote,
        )
    }

    private fun noteOf(verdict: OvertrainingVerdict): String? = when (verdict) {
        is OvertrainingVerdict.Ok -> null
        is OvertrainingVerdict.Caution -> verdict.reason
        is OvertrainingVerdict.ForceRest -> verdict.reason
    }

    /** Coarse streak estimate: count of distinct recent days with a session, newest-first. */
    private fun estimateStreak(endTimestamps: List<Long>): Int {
        if (endTimestamps.isEmpty()) return 0
        val days = endTimestamps.map { it / MILLIS_PER_DAY }.distinct().sorted().asReversed()
        var streak = 1
        for (i in 1 until days.size) {
            if (days[i - 1] - days[i] == 1L) streak++ else break
        }
        return streak
    }

    private companion object {
        const val RECENT_LIMIT = 30
        const val MILLIS_PER_DAY = 86_400_000L

        /** Nominal minutes used only to derive the recommended exercise; the user still picks length. */
        const val RECOMMENDED_MINUTES = 10
    }
}
