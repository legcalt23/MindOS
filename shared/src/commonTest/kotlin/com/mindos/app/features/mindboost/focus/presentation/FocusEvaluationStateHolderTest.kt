package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.CleanLifecycleProbe
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.InterruptedLifecycleProbe
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeReliabilityEstimator
import com.mindos.app.features.mindboost.focus.domain.evaluation.ReliabilityEstimate
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.driveWithResponses
import com.mindos.app.features.mindboost.focus.presentation.state.FocusEvaluationUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.retestClaimText
import com.mindos.app.features.mindboost.focus.virtualMonotonicClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The evaluation retest needs a baseline to compare against; without one it must say so. With one, it
 * runs a reserved probe in EVALUATION mode, records it apart from training, and produces an honest —
 * RCI-gated — verdict that never claims a reliable change before there is evidence for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusEvaluationStateHolderTest {

    @Test
    fun prepare_withoutBaseline_reportsNeedsBaseline() = runTest {
        val graph = FocusTestFixtures.focusGraph()
        val holder = FocusEvaluationStateHolder(graph = graph, userId = "u1", scope = this)

        holder.prepare()

        assertTrue(holder.uiState is FocusEvaluationUiState.NeedsBaseline)
    }

    @Test
    fun retest_withBaseline_recordsProbe_andIsGated() = runTest {
        val baselines = InMemoryBaselineRepository()
        val probes = InMemoryProbeBankRepository()
        baselines.save(
            BaselineSnapshot(
                userId = "u1",
                perMetric = mapOf(MetricKey.ACCURACY to 0.8, MetricKey.RT_MEDIAN to 400.0),
                capturedAtMs = 1_000L,
            ),
        )
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        val holder = FocusEvaluationStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            lifecycleProbe = CleanLifecycleProbe,
        )

        holder.start()
        drive(holder)

        val state = holder.uiState
        assertTrue(state is FocusEvaluationUiState.Finished, "Evaluation must finish, was $state")
        // Honest gate: without trusted reliability + enough administrations, no reliable change.
        assertFalse(state.result.hasReliableChange)
        assertFalse(state.transfer.hasStrongEvidence)
        // The probe run was recorded apart from training history.
        assertTrue(probes.probeHistory("u1", 10).isNotEmpty())
        // The user SAW this form, so it stays spent: the next administration must move on.
        assertNotEquals(FIRST_VIGILANCE_FORM, probes.nextProbe("u1", week = 1).formSeed)
        coroutineContext.cancelChildren()
    }

    /**
     * A reserved form is spent at PICK time and the supply is finite, so quitting before
     * the form is on screen must not consume one. The revert lives in the run's own `finally`, which is the
     * only placement that also covers scope disposal and an uncaught throw — not just `abort()`.
     */
    @Test
    fun abortBeforeTheFormIsSeen_givesTheReservedFormBack() = runTest {
        val baselines = InMemoryBaselineRepository()
        val probes = InMemoryProbeBankRepository()
        baselines.save(
            BaselineSnapshot(
                userId = "u1",
                perMetric = mapOf(MetricKey.ACCURACY to 0.8, MetricKey.RT_MEDIAN to 400.0),
                capturedAtMs = 1_000L,
            ),
        )
        val graph = FocusTestFixtures.focusGraph(baselineRepository = baselines, probeBankRepository = probes)
        val holder = FocusEvaluationStateHolder(graph = graph, userId = "u1", scope = this)

        holder.start()
        runCurrent() // far enough to burn the form, not far enough to finish the block
        // Anti-vacuity: `Running` is only emitted from inside the run, i.e. AFTER the pick. Without this the
        // form coming back is indistinguishable from a form that was never taken, and the test would pass on
        // a holder that stopped burning altogether. The baseline twin gets the same proof from its wait-loop.
        assertTrue(
            holder.uiState is FocusEvaluationUiState.Running,
            "the run must have started, or nothing was burned and there is nothing to give back",
        )
        holder.abort()
        advanceUntilIdle() // let the cancelled run reach its finally

        assertTrue(probes.probeHistory("u1", 10).isEmpty(), "An aborted retest must record nothing")
        assertEquals(FIRST_VIGILANCE_FORM, probes.nextProbe("u1", week = 1).formSeed)
        coroutineContext.cancelChildren()
    }

    /**
     * MB-FOCUS-0114 · an interrupted retest must not publish a comparison. It cannot be a [
     * FocusEvaluationUiState.Finished] with withheld metrics either: a withheld metric has
     * `measurementSufficient == null`, so it is neither a change nor a withheld-for-sample row and would simply
     * vanish from the report — the omissive half of `null != 0` (`MB-CATALOG-0020`).
     */
    @Test
    fun interruptedRetest_isWithheld_notReportedAsNoChange() = runTest {
        val probes = InMemoryProbeBankRepository()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = savedBaseline(),
            probeBankRepository = probes,
        )
        val holder = FocusEvaluationStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            lifecycleProbe = InterruptedLifecycleProbe,
        )

        holder.start()
        drive(holder)

        assertTrue(holder.uiState is FocusEvaluationUiState.NotMeasurable, "was ${holder.uiState}")
        // The form WAS administered, so the run is still recorded — carrying its own not-measurable verdict.
        // On the REASON: the user responded, so a pass by NO_RESPONSES would be a pass for the wrong reason.
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED),
            probes.probeHistory("u1", 10).single().admission,
        )
        coroutineContext.cancelChildren()
    }

    /**
     * Discriminating half: with NO probe wired (the fail-closed default) the outcome must be the same
     * withholding. Without this, "the sensor is off" could quietly become "the measurement is fine".
     */
    @Test
    fun withoutALifecycleSensor_theRetestIsWithheld_notPublished() = runTest {
        val probes = InMemoryProbeBankRepository()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = savedBaseline(),
            probeBankRepository = probes,
        )
        val holder = FocusEvaluationStateHolder(graph = graph, userId = "u1", scope = this)

        holder.start()
        drive(holder)

        assertTrue(holder.uiState is FocusEvaluationUiState.NotMeasurable, "was ${holder.uiState}")
        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED),
            probes.probeHistory("u1", 10).single().admission,
        )
        coroutineContext.cancelChildren()
    }

    /**
     * The result screen asserts the retest used different material. The reserved supply is finite, so that
     * assertion stops holding the moment the rotation wraps — and before the scheduler declared the wrap,
     * nothing downstream could tell. Both halves are pinned: a fresh supply must NOT be reported as a repeat
     * (or "declare everything a repeat" would pass), and a wrapped one MUST be.
     *
     * MB-FOCUS-0122 (R13) adds the consequence: the declaration reaches the RCI. With a populated reliability
     * source and enough prior administrations, the wrapped retest still cannot be a reliable change — and the
     * fresh twin below, same source and same prior count, IS one, so the denial is attributable to the wrap.
     */
    @Test
    fun whenTheReservedSupplyWraps_theRetestDeclaresIt_soTheResultCopyCannotClaimNewMaterial() = runTest {
        val probes = InMemoryProbeBankRepository()
        // Spend the whole reserved supply, so the administration the holder takes comes round to form one.
        repeat(ProbeBank.VIGILANCE_PROBE.reservedForms.size) { probes.nextProbe("u1", week = 1) }
        recordAdmittedAdministrations(probes, ProbeReliabilityEstimator.MIN_ADMINISTRATIONS)
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = savedBaseline(),
            probeBankRepository = probes,
            evaluationService = FocusEvaluationService(reliableAccuracy()),
        )
        val holder = FocusEvaluationStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            lifecycleProbe = CleanLifecycleProbe,
        )

        holder.start()
        drive(holder)

        val state = holder.uiState
        assertTrue(state is FocusEvaluationUiState.Finished, "Evaluation must finish, was $state")
        assertTrue(state.repeatsEarlierForm, "a wrapped supply must reach the screen as a repeat")
        assertFalse(state.result.hasReliableChange, "a repeated form must never carry a reliable change")
        assertFalse(state.transfer.hasStrongEvidence)
        coroutineContext.cancelChildren()
    }

    @Test
    fun withAFreshReservedSupply_theRetestIsNotReportedAsARepeat_andCanBeReliable() = runTest {
        val probes = InMemoryProbeBankRepository()
        recordAdmittedAdministrations(probes, ProbeReliabilityEstimator.MIN_ADMINISTRATIONS)
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = virtualMonotonicClock(),
            baselineRepository = savedBaseline(),
            probeBankRepository = probes,
            evaluationService = FocusEvaluationService(reliableAccuracy()),
        )
        val holder = FocusEvaluationStateHolder(
            graph = graph,
            userId = "u1",
            scope = this,
            lifecycleProbe = CleanLifecycleProbe,
        )

        holder.start()
        drive(holder)

        val state = holder.uiState
        assertTrue(state is FocusEvaluationUiState.Finished, "Evaluation must finish, was $state")
        assertFalse(state.repeatsEarlierForm, "a first showing must not be reported as a repeat")
        // Discriminating half of the wrap test above: same source, same prior count, fresh material → the
        // chain is live end to end and the wrap is the only thing the other test's denial can rest on.
        assertTrue(state.result.hasReliableChange, "fresh material with reliability and enough administrations")
        coroutineContext.cancelChildren()
    }

    /**
     * Pins the COPY CONTRACT that the two tests above feed: the disclosure is APPENDED and the frozen base
     * stays byte-identical. Structural on purpose — it fails if the disclosure is dropped (the two texts
     * become equal) AND if the frozen base is reworded to carry it (the prefix stops holding), which is the
     * discipline TransferReport's KDoc sets out and WITHHELD_CLAUSE demonstrates.
     *
     * DECLARED CEILING: it does NOT verify that the state reaches the user. It calls [retestClaimText]
     * directly, so nothing here guards the hop state.repeatsEarlierForm -> FinishedContent(...) ->
     * retestClaimText(...) in FocusEvaluationScreen. That hop is live and correct today (read, not
     * assumed), but it is reachable only from a Compose harness this module does not have, so the gap is
     * registered rather than papered over. See MB-FOCUS-0136.
     */
    @Test
    fun theRepeatedFormDisclosureIsAppended_andNeverRewordsTheFrozenBase() {
        val fresh = retestClaimText(repeatsEarlierForm = false)
        val repeated = retestClaimText(repeatsEarlierForm = true)

        assertTrue(repeated.startsWith(fresh), "the disclosure must be APPENDED to the frozen base")
        assertNotEquals(fresh, repeated, "a repeated reserved form must be disclosed at all")
    }

    /** Runs the whole retest responding to every stimulus (see driveWithResponses). */
    private fun TestScope.drive(holder: FocusEvaluationStateHolder) =
        driveWithResponses({ (holder.uiState as? FocusEvaluationUiState.Running)?.model?.field }, holder::onResponse)

    /**
     * A populated reliability source for ACCURACY: the baseline fixture says 0.8 and a driven run answers every
     * stimulus, so the retest lands far below it; with sdPre this small the RCI is far past 1.96 whichever way.
     */
    private fun reliableAccuracy() = ProbeReliabilityEstimator(
        source = { mapOf(MetricKey.ACCURACY to ReliabilityEstimate(MetricKey.ACCURACY, 0.9, 0.02)) },
    )

    /** Prior ADMITTED probe rows, which is what the holder counts toward the reliability gate. */
    private suspend fun recordAdmittedAdministrations(probes: InMemoryProbeBankRepository, count: Int) {
        repeat(count) { index ->
            probes.recordProbe(
                "u1",
                SessionResult(
                    sessionId = SessionId("u1:prior:$index"),
                    mode = Mode.EVALUATION,
                    exerciseFocus = ExerciseId.E1_VIGIA,
                    blocks = emptyList(),
                    startedAtMs = 2_000L + index,
                    endedAtMs = 2_000L + index,
                    sessionMetrics = emptyMap(),
                    admission = SessionAdmission.Admitted,
                ),
            )
        }
    }

    private suspend fun savedBaseline(): InMemoryBaselineRepository =
        InMemoryBaselineRepository().apply {
            save(
                BaselineSnapshot(
                    userId = "u1",
                    perMetric = mapOf(MetricKey.ACCURACY to 0.8, MetricKey.RT_MEDIAN to 400.0),
                    capturedAtMs = 1_000L,
                ),
            )
        }

    private companion object {
        /** Week 1 selects the vigilance probe; its first reserved parallel form. */
        val FIRST_VIGILANCE_FORM = ProbeBank.VIGILANCE_PROBE.reservedForms.first()
    }
}
