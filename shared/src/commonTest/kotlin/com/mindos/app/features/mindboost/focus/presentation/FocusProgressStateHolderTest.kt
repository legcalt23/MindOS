package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendDirection
import com.mindos.app.features.mindboost.focus.presentation.state.FocusProgressUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The progress view must compute an honest Focus Score from the latest session and per-metric trends
 * across history — improving accuracy over sessions reads as IMPROVING.
 */
class FocusProgressStateHolderTest {

    @Test
    fun progress_computesFocusScore_andImprovingAccuracyTrend() = runTest {
        val history = InMemorySessionHistoryRepository()
        // Accuracy improving across four sessions (oldest → newest).
        listOf(0.60, 0.70, 0.80, 0.90).forEachIndexed { i, acc ->
            history.append(session(endedAtMs = (i + 1) * 100L, accuracy = acc))
        }
        val holder = FocusProgressStateHolder(historyRepository = history)

        holder.load("u1")

        val state = holder.uiState
        assertTrue(state is FocusProgressUiState.Ready)
        assertEquals(4, state.totalSessions)
        assertNotNull(state.focusScore.value, "Latest session has components → a score exists.")
        val accuracyTrend = state.trends.first { it.metric == MetricKey.ACCURACY }
        assertEquals(TrendDirection.IMPROVING, accuracyTrend.direction)
    }

    @Test
    fun nearTransfer_appearsWhenAProbeRetestFollowsTheBaseline_butStaysGated() = runTest {
        val history = InMemorySessionHistoryRepository()
        history.append(session(endedAtMs = 100L, accuracy = 0.7))
        val baselines = InMemoryBaselineRepository()
        baselines.save(
            BaselineSnapshot(userId = "u1", perMetric = mapOf(MetricKey.ACCURACY to 0.6), capturedAtMs = 1_000L),
        )
        val probes = InMemoryProbeBankRepository()
        // A reserved-probe retest recorded AFTER the baseline snapshot (endedAtMs 2000 > 1000).
        probes.recordProbe("u1", session(endedAtMs = 2_000L, accuracy = 0.9))
        val holder = FocusProgressStateHolder(
            historyRepository = history,
            baselineRepository = baselines,
            probeBankRepository = probes,
        )

        holder.load("u1")

        val state = holder.uiState
        assertTrue(state is FocusProgressUiState.Ready)
        val transfer = assertNotNull(state.transfer, "A retest after the baseline must surface near-transfer.")
        // Gated: a single retest without trusted reliability is preliminary, never reliable.
        assertFalse(transfer.hasStrongEvidence)
    }

    @Test
    fun nearTransfer_isNull_withoutABaseline() = runTest {
        val history = InMemorySessionHistoryRepository()
        history.append(session(endedAtMs = 100L, accuracy = 0.7))
        val holder = FocusProgressStateHolder(historyRepository = history)

        holder.load("u1")

        val state = holder.uiState
        assertTrue(state is FocusProgressUiState.Ready)
        assertEquals(null, state.transfer, "No baseline → no near-transfer yet.")
    }

    private fun session(endedAtMs: Long, accuracy: Double) = SessionResult(
        sessionId = SessionId("u1:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = mapOf(
            MetricKey.ACCURACY to MetricValue(MetricKey.ACCURACY, accuracy, 30, true, MetricUnit.PROPORTION),
            MetricKey.SUSTAINED_FOCUS_INDEX to
                MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, accuracy * 100, 30, true, MetricUnit.INDEX),
        ),
    )
}
