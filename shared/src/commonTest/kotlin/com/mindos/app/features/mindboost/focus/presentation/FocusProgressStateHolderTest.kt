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
        val accuracyTrend = state.trends.first { it.trend.metric == MetricKey.ACCURACY }
        assertEquals(TrendDirection.IMPROVING, accuracyTrend.trend.direction)
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

    /**
     * A1 pin: the four trend labels that ship. R15 moved them verbatim out of a second table in the
     * Compose file, and three of them are still the only names this row has ever shown.
     *
     * The fourth changed once, deliberately: FOC-P0-00 froze the product at C0 and "Índice de foco
     * sostenido" names a capacity, while the number behind it is 100 x (1 - RT-CV) x (1 - lapse rate) —
     * two things this block recorded, not a faculty of the person. The pin is not weaker for it; drift
     * is still a failure, a claim-ladder decision is still allowed to move a name ONCE and say so here.
     */
    @Test
    fun theFourShippedTrendLabelsArePinned() = runTest {
        val state = readyState()
        assertEquals(4, state.trends.size, "Anti-vacuity: the pin below says nothing over an empty list.")
        val byMetric = state.trends.associate { it.trend.metric to it.label }
        assertEquals("Precisión", byMetric[MetricKey.ACCURACY])
        assertEquals("Reacción (mediana)", byMetric[MetricKey.RT_MEDIAN])
        assertEquals("Estabilidad (RT-CV)", byMetric[MetricKey.RT_CV])
        assertEquals("Regularidad y lapsos", byMetric[MetricKey.SUSTAINED_FOCUS_INDEX])
    }

    /**
     * The armed defect this closes: the name came from a SECOND table whose `else` fell back to
     * `metric.name.lowercase().replace('_', ' ')`. Nothing tied that table to the tracked set, so a fifth
     * tracked metric would have leaked a raw identifier in silence. Naming is now the only way to track,
     * and no row may carry a name derived from the enum.
     */
    @Test
    fun noTrendRowCarriesANameDerivedFromTheEnum() = runTest {
        val state = readyState()
        assertEquals(4, state.trends.size, "Anti-vacuity: an empty list satisfies any per-row claim.")
        state.trends.forEach { row ->
            val raw = row.trend.metric.name
            val oldFallback = raw.lowercase().replace('_', ' ')
            assertTrue(row.label.isNotBlank(), "A trend row must carry a reviewed name.")
            val leaked = "Row for $raw leaked an identifier: ${row.label}"
            assertTrue(row.label != raw && row.label != oldFallback, leaked)
        }
    }

    /**
     * Discriminant half, and the real reason the two label tables must NOT be merged: "(prueba)" marks the
     * RESERVED PROBE. A trend row is computed over the user's own TRAINING sessions, so carrying that
     * marker would mislabel where the number came from. The transfer row for the SAME metric does carry it
     * — without that second half this test would pass over an empty report.
     */
    @Test
    fun trendLabelsNeverClaimTheProbe_whileTransferLabelsDo() = runTest {
        val history = InMemorySessionHistoryRepository()
        history.append(session(endedAtMs = 100L, accuracy = 0.7))
        val baselines = InMemoryBaselineRepository()
        baselines.save(
            BaselineSnapshot(userId = "u1", perMetric = mapOf(MetricKey.ACCURACY to 0.6), capturedAtMs = 1_000L),
        )
        val probes = InMemoryProbeBankRepository()
        probes.recordProbe("u1", session(endedAtMs = 2_000L, accuracy = 0.9, sufficient = true))
        val holder = FocusProgressStateHolder(
            historyRepository = history,
            baselineRepository = baselines,
            probeBankRepository = probes,
        )

        holder.load("u1")

        val state = holder.uiState
        assertTrue(state is FocusProgressUiState.Ready)
        assertEquals(4, state.trends.size, "Anti-vacuity: no rows would satisfy the claim vacuously.")
        state.trends.forEach {
            assertFalse(it.label.contains(PROBE_MARKER), "A training-session trend must not claim the probe.")
        }
        val transfer = assertNotNull(state.transfer, "The discriminant half needs a real transfer report.")
        val accuracyLine = transfer.preliminary.first { it.metric == MetricKey.ACCURACY }
        val marked = "The transfer row for the same metric must mark the probe: ${accuracyLine.label}"
        assertTrue(accuracyLine.label.contains(PROBE_MARKER), marked)
    }

    private suspend fun readyState(): FocusProgressUiState.Ready {
        val history = InMemorySessionHistoryRepository()
        listOf(0.60, 0.70, 0.80, 0.90).forEachIndexed { i, acc ->
            history.append(session(endedAtMs = (i + 1) * 100L, accuracy = acc))
        }
        val holder = FocusProgressStateHolder(historyRepository = history)
        holder.load("u1")
        val state = holder.uiState
        assertTrue(state is FocusProgressUiState.Ready)
        return state
    }
    private fun session(endedAtMs: Long, accuracy: Double, sufficient: Boolean? = null) = SessionResult(
        sessionId = SessionId("u1:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = mapOf(
            MetricKey.ACCURACY to
                MetricValue(MetricKey.ACCURACY, accuracy, 30, true, MetricUnit.PROPORTION, sufficient),
            MetricKey.SUSTAINED_FOCUS_INDEX to
                MetricValue(MetricKey.SUSTAINED_FOCUS_INDEX, accuracy * 100, 30, true, MetricUnit.INDEX),
        ),
    )

    private companion object {
        /** The only probe indicator these reports carry; see TransferReport.label. */
        const val PROBE_MARKER = "(prueba)"
    }
}
