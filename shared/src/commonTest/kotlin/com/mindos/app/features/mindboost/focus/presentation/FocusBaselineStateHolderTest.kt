package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Running the baseline end-to-end must establish the user's base: a persisted profile snapshot, a
 * per-exercise starting difficulty for each calibration exercise, and a reserved-probe run recorded
 * apart from training history — without ever appending a training session. Virtual time skips the
 * 15–20 min of real delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusBaselineStateHolderTest {

    @Test
    fun baseline_run_persistsSnapshot_seedsDifficulty_andRecordsProbe_butNoTrainingSession() = runTest {
        val profiles = InMemoryFocusProfileRepository()
        val baselines = InMemoryBaselineRepository()
        val probes = InMemoryProbeBankRepository()
        val history = InMemorySessionHistoryRepository()
        val graph = FocusTestFixtures.focusGraph(
            profileRepository = profiles,
            historyRepository = history,
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        val holder = FocusBaselineStateHolder(graph = graph, userId = "u1", scope = this, seed = 42L)

        holder.start()
        advanceUntilIdle()

        assertTrue(holder.uiState is FocusBaselineUiState.Finished, "Baseline must finish, was ${holder.uiState}")
        val saved = profiles.load("u1")
        assertNotNull(saved)
        assertNotNull(saved.baseline, "The baseline snapshot must be persisted on the profile.")
        // One starting difficulty per calibration exercise (E1/E2/E3).
        assertEquals(3, saved.startingDifficulty.size)
        assertNotNull(baselines.load("u1"), "The baseline snapshot must persist in the baseline repo.")
        assertTrue(probes.probeHistory("u1", 10).isNotEmpty(), "The reserved probe run must be recorded.")
        // The baseline is NOT a training session — training history stays empty.
        assertTrue(history.recent("u1", 10).isEmpty(), "Baseline must not write to training history.")
        coroutineContext.cancelChildren()
    }
}
