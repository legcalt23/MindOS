package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.session.DailyFocusPlan
import com.mindos.app.features.mindboost.focus.domain.session.DailyPlanGenerator
import com.mindos.app.features.mindboost.focus.domain.session.PlannedBlock
import com.mindos.app.features.mindboost.focus.domain.session.SessionPhase
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.domain.session.TrainingCycle
import com.mindos.app.features.mindboost.focus.presentation.state.FocusHomeUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The home menu must surface every end-to-end exercise — no silent filter. These tests fail if the
 * menu drifts from [ExerciseCatalog.implemented] (e.g. an exposed exercise stops appearing).
 */
class FocusHomeStateHolderTest {

    private fun holder() = FocusHomeStateHolder(
        profileRepository = InMemoryFocusProfileRepository(),
        historyRepository = InMemorySessionHistoryRepository(),
    )

    @Test
    fun home_menu_lists_every_implemented_exercise_in_order() = runTest {
        val holder = holder()
        holder.load(userId = "u1", displayName = "Ada")

        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready, "Expected Ready, was $state")
        assertEquals(
            ExerciseCatalog.implemented,
            state.exercises.map { it.id },
            "Home menu must mirror ExerciseCatalog.implemented exactly.",
        )
        // Display names come from the catalogue definitions, never hardcoded in the holder.
        state.exercises.forEach { card ->
            assertEquals(ExerciseCatalog.definition(card.id).displayName, card.name)
        }
    }

    @Test
    fun recommended_is_the_first_implemented_exercise() = runTest {
        val holder = holder()
        holder.load(userId = "u1", displayName = "Ada")

        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready)
        assertEquals(ExerciseCatalog.implemented.first(), state.recommendedExercise)
    }

    @Test
    fun home_surfaces_forceRest_on_chronic_fatigue() = runTest {
        val history = InMemorySessionHistoryRepository()
        repeat(3) { i -> history.append(fatigueSession("u1", endedAtMs = (i + 1) * 100L, fatigue = 0.9)) }
        val holder = FocusHomeStateHolder(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = history,
        )

        holder.load(userId = "u1", displayName = "Ada")

        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready)
        assertTrue(state.forceRest, "Chronic high fatigue must force rest.")
        assertNotNull(state.overtrainingNote)
    }

    private class FakePlanGenerator(private val plan: DailyFocusPlan) : DailyPlanGenerator {
        override fun generate(
            profile: UserCognitiveProfile,
            cycle: TrainingCycle,
            availableMinutes: Int,
            recent: List<SessionResult>,
        ): DailyFocusPlan = plan
    }

    @Test
    fun recommendation_comes_from_the_daily_plan() = runTest {
        val plan = DailyFocusPlan(
            cycleWeek = 3,
            template = SessionTemplate.STANDARD_10,
            blocks = listOf(
                PlannedBlock(ExerciseId.E5_CONFLICTO, Mode.TRAINING, DifficultyVector.EASIEST, SessionPhase.CORE),
            ),
            volumeMultiplier = 1.0,
            capDifficulty = false,
            includeProbe = false,
        )
        val holder = FocusHomeStateHolder(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = InMemorySessionHistoryRepository(),
            dailyPlanGenerator = FakePlanGenerator(plan),
        )

        holder.load("u1", "Ada")

        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready)
        assertEquals(ExerciseId.E5_CONFLICTO, state.recommendedExercise, "Recommendation must follow the plan.")
        assertEquals(3, state.cycleWeek)
    }

    @Test
    fun restDayPlan_surfaces_recoveryNote() = runTest {
        val plan = DailyFocusPlan(
            cycleWeek = 4,
            template = SessionTemplate.MAINTENANCE_5,
            blocks = emptyList(),
            volumeMultiplier = 0.0,
            capDifficulty = true,
            includeProbe = false,
            recoveryNote = "Descansa hoy.",
        )
        val holder = FocusHomeStateHolder(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = InMemorySessionHistoryRepository(),
            dailyPlanGenerator = FakePlanGenerator(plan),
        )

        holder.load("u1", "Ada")

        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready)
        assertEquals("Descansa hoy.", state.recoveryNote)
    }

    private fun fatigueSession(user: String, endedAtMs: Long, fatigue: Double) = SessionResult(
        sessionId = SessionId("$user:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = mapOf(
            MetricKey.FATIGUE_INDEX to MetricValue(MetricKey.FATIGUE_INDEX, fatigue, 6, true, MetricUnit.INDEX),
        ),
    )
}
