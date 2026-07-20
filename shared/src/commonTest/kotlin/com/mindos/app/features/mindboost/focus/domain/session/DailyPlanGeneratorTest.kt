package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyPlanGeneratorTest {

    private val cycle = TrainingCycles.STANDARD_8_WEEK

    /**
     * Personalization is explicitly ENABLED here. S0 contains the archetype routes by default
     * (`FeatureFlags.focusScoreSurfacesEnabled` = false ⇒ `PersonalizationProfiles.NEUTRAL`), but these
     * tests exist to verify the weakness-first ranking feature ITSELF, which containment hides rather
     * than deletes. Leaving them on the default would silently turn them into assertions about the
     * contained behaviour and they would fail — as they did when the gate landed.
     *
     * The contained half of this behaviour is asserted in `FocusArchetypeRouteContainmentTest`
     * (`contained_daily_plan_does_not_let_the_archetype_reorder_the_recommendation`), so the two files
     * together form the discriminating pair: ranking works when released, and is inert when contained.
     */
    private val allExercises = DefaultDailyPlanGenerator(
        availableExercises = ExerciseId.entries.toSet(),
        archetypePersonalizationEnabled = { true },
    )

    private fun profile(week: Int) = UserCognitiveProfile(userId = "user", cycleWeek = week)

    private fun profile(week: Int, archetype: UserArchetype) =
        UserCognitiveProfile(userId = "user", cycleWeek = week, archetype = archetype)

    private fun fatigueSession(endedAtMs: Long, fatigue: Double) = SessionResult(
        sessionId = SessionId("user:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = mapOf(
            MetricKey.FATIGUE_INDEX to MetricValue(MetricKey.FATIGUE_INDEX, fatigue, 6, true, MetricUnit.INDEX),
        ),
    )

    @Test
    fun deloadWeek_reducesVolume_andCapsDifficulty() {
        val plan = allExercises.generate(profile(week = 4), cycle, availableMinutes = 10, recent = emptyList())
        assertEquals(0.5, plan.volumeMultiplier, absoluteTolerance = 1e-9)
        assertTrue(plan.capDifficulty)
        assertTrue(plan.includeProbe) // deload week has FULL evaluation
    }

    @Test
    fun normalWeek_fullVolume_noCap() {
        val plan = allExercises.generate(profile(week = 2), cycle, availableMinutes = 10, recent = emptyList())
        assertEquals(1.0, plan.volumeMultiplier, absoluteTolerance = 1e-9)
        assertTrue(!plan.capDifficulty)
    }

    @Test
    fun restRecommended_yieldsRestDay() {
        val recent = listOf(fatigueSession(200, 0.9), fatigueSession(100, 0.85))
        val plan = allExercises.generate(profile(week = 2), cycle, availableMinutes = 10, recent = recent)
        assertTrue(plan.isRestDay)
        assertTrue(plan.recoveryNote != null)
    }

    @Test
    fun moderateFatigue_reducesVolumeBelowOne() {
        val recent = listOf(fatigueSession(100, 0.6))
        val plan = allExercises.generate(profile(week = 2), cycle, availableMinutes = 10, recent = recent)
        assertTrue(plan.volumeMultiplier < 1.0 && !plan.isRestDay)
    }

    @Test
    fun availableMinutes_selectTheNearestTemplate() {
        val plan = allExercises.generate(profile(week = 1), cycle, availableMinutes = 20, recent = emptyList())
        assertEquals(SessionTemplate.DEVELOPMENT_20, plan.template)
    }

    @Test
    fun planRespectsWeekEmphasis_whenExercisesAreAvailable() {
        val plan = allExercises.generate(profile(week = 3), cycle, availableMinutes = 20, recent = emptyList())
        // Week 3 emphasizes E3/E4/E5; a 20-min session plans 3 core blocks.
        assertEquals(
            listOf(ExerciseId.E3_GATILLO, ExerciseId.E4_FRENO, ExerciseId.E5_CONFLICTO),
            plan.blocks.map { it.exerciseId },
        )
    }

    @Test
    fun weaknessFirst_reordersTheWeekPool_byArchetypeWeights() {
        // Week 1 emphasizes [E1, E2, E3]. Impulsive weights E3 (1.6) highest → E3 first; beginner weights
        // E1 (1.5) highest → E1 first. Same pool, different first block = the archetype weights are applied.
        val impulsive = allExercises.generate(
            profile(week = 1, archetype = UserArchetype.IMPULSIVE),
            cycle,
            availableMinutes = 5,
            recent = emptyList(),
        )
        val beginner = allExercises.generate(
            profile(week = 1, archetype = UserArchetype.BEGINNER),
            cycle,
            availableMinutes = 5,
            recent = emptyList(),
        )
        assertEquals(ExerciseId.E3_GATILLO, impulsive.blocks.first().exerciseId)
        assertEquals(ExerciseId.E1_VIGIA, beginner.blocks.first().exerciseId)
    }

    @Test
    fun weaknessFirst_keepsMinimumVariety() {
        // 20 min = 3 blocks: impulsive ranks E3 first but still covers the rest of the week (no repetition).
        val plan = allExercises.generate(
            profile(week = 1, archetype = UserArchetype.IMPULSIVE),
            cycle,
            availableMinutes = 20,
            recent = emptyList(),
        )
        assertEquals(
            listOf(ExerciseId.E3_GATILLO, ExerciseId.E1_VIGIA, ExerciseId.E2_FILTRO),
            plan.blocks.map { it.exerciseId },
        )
    }

    @Test
    fun generation_isDeterministic() {
        val a = allExercises.generate(profile(week = 5), cycle, availableMinutes = 30, recent = emptyList())
        val b = allExercises.generate(profile(week = 5), cycle, availableMinutes = 30, recent = emptyList())
        assertEquals(a, b)
    }

    @Test
    fun defaultGenerator_onlySchedulesImplementedExercises() {
        // The default pool is ExerciseCatalog.implemented. Week 3 emphasizes E3/E4/E5, but only the
        // implemented subset (E3 today) may be scheduled; unimplemented ids (E4/E5) never leak in,
        // and an all-unavailable emphasis falls back to the implemented pool. Robust as more
        // exercises are exposed.
        val plan = DefaultDailyPlanGenerator()
            .generate(profile(week = 3), cycle, availableMinutes = 5, recent = emptyList())
        assertTrue(plan.blocks.isNotEmpty())
        assertTrue(plan.blocks.all { it.exerciseId in ExerciseCatalog.implemented })
    }
}
