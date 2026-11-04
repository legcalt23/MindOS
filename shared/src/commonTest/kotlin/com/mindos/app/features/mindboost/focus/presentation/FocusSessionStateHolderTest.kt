package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.honestResponseTo
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
 *
 * FOC-P0-05: a score now also requires the session to be ADMITTED by `SessionQualityGate` (watched, answered,
 * and — R15 — with a response pattern that carries signal). These sessions are therefore driven with a watching
 * probe and a driver that PLAYS the task (go answered, no-go withheld) — otherwise
 * the GATE, not the flag, would be what hides the score, and the pair above would stop discriminating.
 * The gate itself is pinned in `FocusQualityGateWiringTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionStateHolderTest {

    private object WatchingProbe : AppLifecycleProbe {
        override fun start() = Unit
        override fun stop(): LifecycleObservation = LifecycleObservation.Observed(emptyList())
    }

    private class SteppingClock : MonotonicClock {
        var nanos = 0L
        override fun nowNanos(): Long = nanos
    }

    private suspend fun TestScope.runSessionFor(
        archetype: UserArchetype,
        scoreSurfacesEnabled: Boolean = true,
    ): FocusSessionUiState.Finished {
        val profiles = InMemoryFocusProfileRepository()
        profiles.save(UserCognitiveProfile(userId = USER, archetype = archetype))
        val history = InMemorySessionHistoryRepository()
        val clock = SteppingClock()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = clock,
            profileRepository = profiles,
            historyRepository = history,
        )
        val holder = FocusSessionStateHolder(
            graph = graph,
            exerciseId = ExerciseId.E3_GATILLO,
            userId = USER,
            scope = this,
            lifecycleProbe = WatchingProbe,
            scoreSurfacesEnabled = { scoreSurfacesEnabled },
        )
        holder.start(SessionTemplate.MAINTENANCE_5)
        var guard = 0
        while (holder.uiState !is FocusSessionUiState.Finished && guard++ < MAX_TICKS) {
            runCurrent()
            // R15 · PLAY the task: answer a go signal, withhold on a no-go. Tapping every tick regardless of the
            // field is the blind "respond to everything" strategy, which `FocusAntiGaming` now withholds.
            val field = (holder.uiState as? FocusSessionUiState.Running)?.model?.field
            if (field != null && honestResponseTo(field) != null) {
                clock.nanos += TAP_RT_NANOS
                holder.onTap()
            }
            advanceTimeBy(TICK_MS)
        }
        val finished = assertIs<FocusSessionUiState.Finished>(holder.uiState, "session must finish for $archetype")
        val recorded = history.all(USER).single()
        assertEquals(SessionAdmission.Admitted, recorded.admission, "precondition: the session must be admitted")
        return finished
    }

    @Test
    fun finishedReport_carriesPersonalizedScore_bentByArchetype() = runTest {
        val impulsive = runSessionFor(UserArchetype.IMPULSIVE)
        val beginner = runSessionFor(UserArchetype.BEGINNER)

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
        val state = runSessionFor(UserArchetype.IMPULSIVE, scoreSurfacesEnabled = false)

        assertNull(
            state.report.personalizedScore,
            "With focusScoreSurfacesEnabled=false the v1 score must not be computed at all.",
        )
        coroutineContext.cancelChildren()
    }

    private companion object {
        const val USER = "u1"
        const val MAX_TICKS = 200_000
        const val TICK_MS = 50L
        const val TAP_RT_NANOS = 300_000_000L
    }
}
