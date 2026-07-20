package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.personalization.ArchetypePersonalization
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizationProfiles
import com.mindos.app.features.mindboost.focus.domain.session.DefaultDailyPlanGenerator
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.domain.session.TrainingCycles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * S0 · **Containment of the archetype INFLUENCE routes** (`MB-ENG-0008`), as opposed to the score
 * SURFACES covered by `FocusScoreContainmentTest`.
 *
 * S0's outcome has two halves — "the v1 score stops being visible" AND "it stops influencing future
 * sessions". The second half is the one that matters most and the one that is easiest to claim without
 * evidence: the archetype is inferred from `RT_CV` and `LAPSES` — precisely the two contaminated inputs
 * behind `MB-ENG-0007` — so a key-masher is classified IMPULSIVE and that label then steers what the
 * user trains next.
 *
 * These tests exist because the S0 efficacy gate found a SIXTH influence route that the plan's own
 * five-route inventory (a–e) missed: `DefaultDailyPlanGenerator` re-ranks which exercise the home screen
 * recommends. It survived precisely because the cuts were asserted in KDoc instead of in tests. Each
 * test below is therefore a discriminating pair — contained vs released — over the ROUTE, not the pixel.
 *
 * Route (a), an ALREADY-STORED starting difficulty, is deliberately preserved in both modes: it is the
 * user's own empirical value, not an inference, and containment must not destroy it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusArchetypeRouteContainmentTest {

    private fun impulsiveProfile(storedDifficulty: Map<ExerciseId, DifficultyVector> = emptyMap()) =
        UserCognitiveProfile(
            userId = "u1",
            archetype = UserArchetype.IMPULSIVE,
            startingDifficulty = storedDifficulty,
        )

    private fun planner(personalizationEnabled: Boolean) = DefaultDailyPlanGenerator(
        archetypePersonalizationEnabled = { personalizationEnabled },
    )

    private fun plan(profile: UserCognitiveProfile, personalizationEnabled: Boolean) =
        planner(personalizationEnabled).generate(
            profile = profile,
            cycle = TrainingCycles.STANDARD_8_WEEK,
            availableMinutes = AVAILABLE_MINUTES,
            recent = emptyList(),
        )

    @Test
    fun contained_daily_plan_does_not_apply_the_archetype_biased_difficulty() {
        val profile = impulsiveProfile()

        val contained = plan(profile, personalizationEnabled = false)
        assertTrue(contained.blocks.isNotEmpty(), "The planner must still produce a plan when contained.")
        contained.blocks.forEach { block ->
            assertEquals(
                DifficultyVector.EASIEST,
                block.startingDifficulty,
                "Contained: with no stored value, difficulty must fall back to EASIEST, not the " +
                    "archetype's biased default — ${block.exerciseId} was biased.",
            )
        }
    }

    @Test
    fun released_daily_plan_restores_the_archetype_biased_difficulty() {
        // DISCRIMINATING HALF. If the bias never applied in the first place — a broken fixture, or an
        // IMPULSIVE profile whose bias happens to be empty for these exercises — the test above would
        // pass for the wrong reason. This proves the containment is what changed the outcome.
        val profile = impulsiveProfile()
        val released = plan(profile, personalizationEnabled = true)

        val biased = released.blocks.filter { block ->
            ArchetypePersonalization.startingDifficulty(
                profile,
                ExerciseCatalog.definition(block.exerciseId),
            ) != DifficultyVector.EASIEST
        }
        assertTrue(
            biased.isNotEmpty(),
            "Fixture check: the IMPULSIVE archetype must bias at least one planned exercise, otherwise " +
                "the contained assertion above proves nothing.",
        )
        biased.forEach { block ->
            assertNotEquals(
                DifficultyVector.EASIEST,
                block.startingDifficulty,
                "Released: the archetype bias must come back for ${block.exerciseId}.",
            )
        }
    }

    @Test
    fun a_stored_starting_difficulty_wins_in_both_modes_because_it_is_the_users_own_value() {
        // Route (a): preserved by design. Containment removes INFERENCES, never the user's real history.
        val stored = DifficultyVector.EASIEST.stepUp(DifficultyAxis.INHIBITORY_DEMAND, STORED_STEP)
        val exercise = ExerciseCatalog.implemented.first()
        val profile = impulsiveProfile(storedDifficulty = mapOf(exercise to stored))

        listOf(false, true).forEach { enabled ->
            // assertNotNull, NOT `if (block != null)`: a conditional here would make the whole test pass
            // with ZERO assertions the day the periodization stops scheduling this exercise — the exact
            // silent-no-op defect these route tests exist to prevent.
            val block = assertNotNull(
                plan(profile, personalizationEnabled = enabled).blocks.firstOrNull { it.exerciseId == exercise },
                "Fixture check: $exercise must be scheduled (enabled=$enabled), or this test asserts nothing.",
            )
            assertEquals(
                stored,
                block.startingDifficulty,
                "A stored difficulty must win regardless of containment (enabled=$enabled).",
            )
        }
    }

    @Test
    fun contained_daily_plan_does_not_let_the_archetype_reorder_the_recommendation() {
        // The most consequential route: `selectExercises` sorts by the archetype's exercise weights, so
        // the archetype picks the hero card. Contained, the ranking must be the periodization's own
        // order — identical to what a profile with NO archetype inference would receive.
        val impulsive = plan(impulsiveProfile(), personalizationEnabled = false).blocks.map { it.exerciseId }
        val neutral = plan(
            UserCognitiveProfile(userId = "u2", archetype = UserArchetype.BEGINNER),
            personalizationEnabled = false,
        ).blocks.map { it.exerciseId }

        assertEquals(
            neutral,
            impulsive,
            "Contained: two different archetypes must receive the SAME schedule — otherwise the " +
                "archetype is still choosing what the user trains next.",
        )
    }

    /**
     * Route (b) end-to-end through the REAL session holder, not through a helper in isolation: an
     * IMPULSIVE profile with no stored difficulty must actually RUN at EASIEST while contained. Asserted
     * on `BlockResult.difficultyStart` of the persisted session, so it reflects what the user trained on.
     */
    @Test
    fun contained_session_runs_at_easiest_and_does_not_persist_a_new_archetype() = runTest {
        val profiles = InMemoryFocusProfileRepository()
        profiles.save(impulsiveProfile())
        val history = InMemorySessionHistoryRepository()
        val graph = FocusTestFixtures.focusGraph(profileRepository = profiles, historyRepository = history)

        FocusSessionStateHolder(
            graph = graph,
            exerciseId = ExerciseId.E3_GATILLO,
            userId = "u1",
            scope = this,
            scoreSurfacesEnabled = { false },
        ).start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        val session = history.recent("u1", limit = 1).firstOrNull()
        assertNotNull(session, "The session must still be recorded when contained.")
        session.blocks.forEach { block ->
            assertEquals(
                DifficultyVector.EASIEST,
                block.difficultyStart,
                "Contained: the session must RUN at EASIEST, not at the archetype-biased difficulty.",
            )
        }

        // Producer side: the archetype must not be re-inferred from this session's contaminated metrics.
        assertEquals(
            UserArchetype.IMPULSIVE,
            assertNotNull(profiles.load("u1")).archetype,
            "Contained: no new archetype may be inferred and persisted from RT_CV/LAPSES.",
        )
        // ...but the legitimate empirical updates must still be saved, or containment would be destroying
        // real user data rather than withholding an inference. Asserted on BOTH updates that `evolve`
        // would otherwise have carried: the session count AND the adapted starting difficulty — the
        // latter is route (a), the value containment explicitly promises to preserve.
        val saved = assertNotNull(profiles.load("u1"))
        assertTrue(saved.totalSessions > 0, "Containment must not block the session count update.")
        assertNotNull(
            saved.startingDifficulty[ExerciseId.E3_GATILLO],
            "The adapted starting difficulty must still be persisted while contained (route (a)).",
        )
        coroutineContext.cancelChildren()
    }

    /**
     * The NEUTRAL profile is what containment substitutes for the archetype's, so "it applies no bias"
     * must be a MECHANISM, not a KDoc promise. Asserted exhaustively over every exercise and every axis:
     * if someone later gives NEUTRAL a non-empty weight or bias map — or if `weightFor`'s fallback stops
     * being the neutral weight — containment would silently start steering again and this fails.
     */
    @Test
    fun the_neutral_profile_applies_no_weight_and_no_bias_to_anything() {
        ExerciseId.entries.forEach { id ->
            assertEquals(
                NEUTRAL_WEIGHT,
                PersonalizationProfiles.NEUTRAL.weightFor(id),
                "NEUTRAL must not re-rank $id.",
            )
        }
        DifficultyAxis.entries.forEach { axis ->
            assertEquals(0.0, PersonalizationProfiles.NEUTRAL.biasFor(axis), "NEUTRAL must not nudge $axis.")
        }
    }

    private companion object {
        const val AVAILABLE_MINUTES = 15
        const val STORED_STEP = 0.4
        const val NEUTRAL_WEIGHT = 1.0
    }
}
