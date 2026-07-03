package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.presentation.state.FocusEvaluationUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
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
            baselineRepository = baselines,
            probeBankRepository = probes,
        )
        val holder = FocusEvaluationStateHolder(graph = graph, userId = "u1", scope = this)

        holder.start()
        advanceUntilIdle()

        val state = holder.uiState
        assertTrue(state is FocusEvaluationUiState.Finished, "Evaluation must finish, was $state")
        // Honest gate: without trusted reliability + enough administrations, no reliable change.
        assertFalse(state.result.hasReliableChange)
        assertFalse(state.transfer.hasStrongEvidence)
        // The probe run was recorded apart from training history.
        assertTrue(probes.probeHistory("u1", 10).isNotEmpty())
        coroutineContext.cancelChildren()
    }
}
