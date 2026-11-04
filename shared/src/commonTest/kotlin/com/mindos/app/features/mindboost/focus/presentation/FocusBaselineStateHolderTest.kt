package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.CleanLifecycleProbe
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.InterruptedLifecycleProbe
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.driveWithResponses
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineUiState
import com.mindos.app.features.mindboost.focus.virtualMonotonicClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
            monotonicClock = virtualMonotonicClock(),
            profileRepository = profiles,
            historyRepository = history,
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        val holder = FocusBaselineStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            seed = 42L,
            lifecycleProbe = CleanLifecycleProbe,
        )

        holder.start()
        drive(holder)

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
        // The user SAW this form, so it stays spent: the next administration must move on.
        assertNotEquals(FIRST_VIGILANCE_FORM, probes.nextProbe("u1", week = 1).formSeed)
        coroutineContext.cancelChildren()
    }

    /**
     * A reserved form is spent at PICK time and the supply is finite, so a baseline that
     * never produced an administration must not consume one. Quitting INSIDE the probe block is the case
     * that matters — quitting during calibration never touches the bank, so asserting that would be vacuous.
     */
    @Test
    fun abortInsideTheProbeBlock_givesTheReservedFormBack() = runTest {
        val probes = InMemoryProbeBankRepository()
        val graph = FocusTestFixtures.focusGraph(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = InMemorySessionHistoryRepository(),
            baselineRepository = InMemoryBaselineRepository(),
            probeBankRepository = probes,
        )
        val probeBlockIndex = graph.baselineProtocol.blocks.indexOfFirst { it.isProbe }
        assertTrue(probeBlockIndex >= 0, "The protocol must contain a reserved-probe block.")
        val holder = FocusBaselineStateHolder(graph = graph, userId = "u1", scope = this, seed = 42L)

        holder.start()
        var guard = 0
        while ((holder.uiState as? FocusBaselineUiState.Running)?.model?.blockIndex != probeBlockIndex) {
            advanceTimeBy(SETTLE_STEP_MS)
            assertTrue(guard++ < SETTLE_MAX_STEPS, "Baseline never reached the probe block")
        }
        holder.abort()
        advanceUntilIdle() // let the cancelled run reach its finally

        assertTrue(holder.uiState is FocusBaselineUiState.Aborted)
        assertTrue(probes.probeHistory("u1", 10).isEmpty(), "An aborted baseline must record no probe run.")
        assertEquals(FIRST_VIGILANCE_FORM, probes.nextProbe("u1", week = 1).formSeed)
        coroutineContext.cancelChildren()
    }

    /**
     * MB-FOCUS-0114 · a baseline whose reserved probe was not measurable must NOT leave a *pre* snapshot behind.
     * An empty snapshot is worse than none: a later retest would compare against it and render "nothing changed".
     * The calibration half still stands, so thresholds and starting difficulty are kept.
     */
    @Test
    fun probeNotMeasurable_keepsCalibration_butPersistsNoBaseline() = runTest {
        val profiles = InMemoryFocusProfileRepository()
        val baselines = InMemoryBaselineRepository()
        val probes = InMemoryProbeBankRepository()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            profileRepository = profiles,
            historyRepository = InMemorySessionHistoryRepository(),
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        // The sensor WATCHED and saw the app leave the foreground — an observed fact, not an unknown.
        val holder = FocusBaselineStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            seed = 42L,
            lifecycleProbe = InterruptedLifecycleProbe,
        )

        holder.start()
        drive(holder)

        assertTrue(
            holder.uiState is FocusBaselineUiState.NotMeasurable,
            "An interrupted probe must not report a finished baseline, was ${holder.uiState}",
        )
        assertNull(baselines.load("u1"), "No pre snapshot may be persisted from a probe that was not admitted.")
        val saved = assertNotNull(profiles.load("u1"))
        assertNull(saved.baseline, "The profile must not carry a snapshot the gate withheld.")
        // ...but the calibration work is untouched: it never went through the gate.
        assertEquals(3, saved.startingDifficulty.size)
        // The form WAS administered, so the run is still recorded — carrying its own not-measurable verdict.
        // Asserted on the REASON, not just the state: the user DID respond here, so a pass by NO_RESPONSES
        // (what an advanceUntilIdle-only fixture produces) would be a pass for the wrong reason.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED),
            probes.probeHistory("u1", 10).single().admission,
        )
        coroutineContext.cancelChildren()
    }

    /**
     * Discriminating half of the one above: with NO probe wired (the fail-closed default) the outcome must be
     * the same withholding. Without this, "the sensor is off" could quietly become "the measurement is fine".
     */
    @Test
    fun withoutALifecycleSensor_theBaselineIsWithheld_notPublished() = runTest {
        val baselines = InMemoryBaselineRepository()
        val probes = InMemoryProbeBankRepository()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        val holder = FocusBaselineStateHolder(graph = graph, userId = "u1", scope = this, seed = 42L)

        holder.start()
        drive(holder)

        assertTrue(holder.uiState is FocusBaselineUiState.NotMeasurable, "was ${holder.uiState}")
        assertNull(baselines.load("u1"))
        // Again the REASON, so an unresponsive fixture cannot make this pass by NO_RESPONSES.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED),
            probes.probeHistory("u1", 10).single().admission,
        )
        coroutineContext.cancelChildren()
    }

    /** Runs the whole baseline responding to every stimulus (see driveWithResponses). */
    private fun TestScope.drive(holder: FocusBaselineStateHolder) =
        driveWithResponses({ (holder.uiState as? FocusBaselineUiState.Running)?.model?.field }, holder::onResponse)

    private companion object {
        /** The baseline probe is the week-1 vigilance probe; this is its first reserved parallel form. */
        val FIRST_VIGILANCE_FORM = ProbeBank.VIGILANCE_PROBE.reservedForms.first()
        const val SETTLE_STEP_MS = 1_000L
        const val SETTLE_MAX_STEPS = 10_000
    }
}
