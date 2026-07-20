package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.presentation.state.FocusHomeUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * S0 · **Containment of the v1 Focus score surfaces** (`MB-ENG-0007`), asserted as a discriminating pair.
 *
 * Every test here has BOTH halves: contained (the production default) and released. A one-sided suite
 * would pass just as happily if the feature had been deleted, or if the flag were ignored entirely and
 * the number simply never appeared — neither of which is what S0 promises. S0 promises a *reversible*
 * hide: nothing numeric while OFF, everything back while ON, and the persisted history untouched either
 * way. The third test is the one that makes "reversible" more than a claim.
 */
class FocusScoreContainmentTest {

    private fun sessionWithFocusIndex(user: String, endedAtMs: Long, index: Double) = SessionResult(
        sessionId = SessionId("$user:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = mapOf(
            MetricKey.SUSTAINED_FOCUS_INDEX to
                MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, index, 40, true, MetricUnit.INDEX),
        ),
    )

    private suspend fun loadHome(
        history: InMemorySessionHistoryRepository,
        scoreSurfacesEnabled: Boolean,
    ): FocusHomeUiState.Ready {
        val holder = FocusHomeStateHolder(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = history,
            scoreSurfacesEnabled = { scoreSurfacesEnabled },
        )
        holder.load(userId = "u1", displayName = "Ada")
        val state = holder.uiState
        assertTrue(state is FocusHomeUiState.Ready, "Expected Ready, was $state")
        return state
    }

    private suspend fun historyWithOneScoredSession(): InMemorySessionHistoryRepository {
        val history = InMemorySessionHistoryRepository()
        history.append(sessionWithFocusIndex("u1", endedAtMs = 1_000L, index = STORED_INDEX))
        return history
    }

    @Test
    fun contained_home_exposes_no_focus_index_number() = runTest {
        val state = loadHome(historyWithOneScoredSession(), scoreSurfacesEnabled = false)

        // The number must be withheld at the SOURCE, not merely skipped by the view: nothing numeric about
        // capability reaches the UI state at all, so no renderer can reintroduce it by accident.
        assertNull(
            state.lastSustainedFocusIndex,
            "Contained: the raw SUSTAINED_FOCUS_INDEX must never reach the UI state.",
        )
        assertFalse(
            state.scoreSurfacesVisible,
            "Contained: the home must not render the FOCO chip or the 'Ver progreso' entry point.",
        )
    }

    @Test
    fun released_home_restores_the_focus_index_number() = runTest {
        val state = loadHome(historyWithOneScoredSession(), scoreSurfacesEnabled = true)

        // DISCRIMINATING HALF of the test above. Same repository, same session, only the flag differs —
        // so a green "contained" result cannot be explained by the fixture simply having no data.
        assertEquals(
            STORED_INDEX,
            assertNotNull(state.lastSustainedFocusIndex),
            "Released: the stored index must come back — the hide is reversible, not destructive.",
        )
        assertTrue(state.scoreSurfacesVisible, "Released: the score surfaces must render again.")
    }

    @Test
    fun containment_does_not_touch_persistence_so_the_hide_is_fully_reversible() = runTest {
        val history = historyWithOneScoredSession()

        // Load the home under containment first. If containment were implemented by dropping, purging or
        // rewriting stored metrics rather than by withholding them at read time, this pass would corrupt
        // the history and the release below would come back empty.
        loadHome(history, scoreSurfacesEnabled = false)

        val stored = history.recent("u1", limit = 10)
        assertEquals(1, stored.size, "Containment must not delete session history.")
        assertEquals(
            STORED_INDEX,
            assertNotNull(stored.first().sessionMetrics[MetricKey.SUSTAINED_FOCUS_INDEX]).value,
            "Containment must not rewrite persisted metrics; the flag is render/read-side only.",
        )

        // And the round trip proves it end-to-end: flip the flag on the SAME repository and the number is
        // there, byte-identical, with no migration involved.
        val released = loadHome(history, scoreSurfacesEnabled = true)
        assertEquals(STORED_INDEX, assertNotNull(released.lastSustainedFocusIndex))
    }

    private companion object {
        const val STORED_INDEX = 72.0
    }
}
