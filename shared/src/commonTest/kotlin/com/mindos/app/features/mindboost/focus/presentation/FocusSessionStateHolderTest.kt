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
import kotlin.test.assertTrue

/**
 * A finished training session must attach the archetype-personalized score (08_ §6). The IMPULSIVE
 * profile de-emphasizes speed; the neutral BEGINNER profile does not — proving the `ScoringModifiers`
 * are actually applied to the live session, not just defined.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionStateHolderTest {

    private suspend fun runSessionFor(archetype: UserArchetype, scope: kotlinx.coroutines.CoroutineScope) =
        run {
            val profiles = InMemoryFocusProfileRepository()
            profiles.save(UserCognitiveProfile(userId = "u1", archetype = archetype))
            val graph = FocusTestFixtures.focusGraph(profileRepository = profiles)
            val holder = FocusSessionStateHolder(
                graph = graph,
                exerciseId = ExerciseId.E3_GATILLO,
                userId = "u1",
                scope = scope,
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
}
