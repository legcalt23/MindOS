package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A finished training session must attach the archetype-personalized score (08_ §6). The IMPULSIVE
 * profile de-emphasizes speed; the neutral BEGINNER profile does not — proving the `ScoringModifiers`
 * are actually applied to the live session, not just defined.
 *
 * S0 CONTAINMENT: that behaviour is now gated by `FeatureFlags.focusScoreSurfacesEnabled`, which is OFF
 * by default (`MB-ENG-0007`). Both halves are asserted here and they DISCRIMINATE each other: with the
 * gate open the score exists AND is still bent by the archetype (so containment did not silently break
 * the feature it hides), and with the gate closed — the production default — no score is produced at all.
 * Testing only the closed half would pass just as well if the scorer had been deleted outright.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionStateHolderTest {

    private suspend fun runSessionFor(
        archetype: UserArchetype,
        scope: kotlinx.coroutines.CoroutineScope,
        scoreSurfacesEnabled: Boolean = true,
    ) =
        run {
            val profiles = InMemoryFocusProfileRepository()
            profiles.save(UserCognitiveProfile(userId = "u1", archetype = archetype))
            val graph = FocusTestFixtures.focusGraph(profileRepository = profiles)
            val holder = FocusSessionStateHolder(
                graph = graph,
                exerciseId = ExerciseId.E3_GATILLO,
                userId = "u1",
                scope = scope,
                scoreSurfacesEnabled = { scoreSurfacesEnabled },
            )
            holder.start(SessionTemplate.MAINTENANCE_5)
            holder
        }

    @Test
    fun finishedReport_carriesPersonalizedScore_bentByArchetype() = runTest {
        val impulsiveHolder = runSessionFor(UserArchetype.IMPULSIVE, this)
        val beginnerHolder = runSessionFor(UserArchetype.BEGINNER, this)
        advanceUntilIdle()

        val impulsive = impulsiveHolder.uiState
        val beginner = beginnerHolder.uiState
        assertTrue(impulsive is FocusSessionUiState.Finished, "Impulsive session must finish, was $impulsive")
        assertTrue(beginner is FocusSessionUiState.Finished, "Beginner session must finish, was $beginner")

        val impulsiveScore = assertNotNull(impulsive.report.personalizedScore)
        val beginnerScore = assertNotNull(beginner.report.personalizedScore)
        assertTrue(impulsiveScore.deemphasizesSpeed, "Impulsive scoring must de-emphasize speed.")
        assertFalse(beginnerScore.deemphasizesSpeed, "Neutral (beginner) scoring must not de-emphasize speed.")
        coroutineContext.cancelChildren()
    }

    /**
     * S0 · the production default. `focusScoreSurfacesEnabled` is OFF, so `finishSession` must not even
     * COMPUTE the v1 score: `personalizedScore` is null and the summary's score card has nothing to
     * render. The session itself still finishes normally — containment hides a number, it never breaks
     * training. Discriminating pair of the test above, which proves the score still works when enabled.
     */
    @Test
    fun finishedReport_withScoreSurfacesContained_carriesNoPersonalizedScore() = runTest {
        val holder = runSessionFor(UserArchetype.IMPULSIVE, this, scoreSurfacesEnabled = false)
        advanceUntilIdle()

        val state = holder.uiState
        assertTrue(state is FocusSessionUiState.Finished, "Session must still finish when contained, was $state")
        assertNull(
            state.report.personalizedScore,
            "With focusScoreSurfacesEnabled=false the v1 score must not be computed at all.",
        )
        coroutineContext.cancelChildren()
    }
}
