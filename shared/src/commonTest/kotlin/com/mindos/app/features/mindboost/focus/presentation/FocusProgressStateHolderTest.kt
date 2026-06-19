package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendDirection
import com.mindos.app.features.mindboost.focus.presentation.state.FocusProgressUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
