package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** MC9-C-R · E5 — the pure metrics mapper: real signals derived, unavailable ones explicitly flagged (no fake 0). */
class VisualScanMetricsMapperTest {

    private fun outcome(
        items: Int,
        target: SpeedChoice,
        response: SpeedChoice?,
        rtMs: Long,
        correct: Boolean,
        anticipation: Boolean = false,
    ) = VisualScanTrialOutcome(items, target, response, rtMs * 1_000_000L, correct, anticipation)

    private fun measured(signal: VisualScanSignal): Double {
        assertIs<VisualScanSignal.Measured>(signal)
        return signal.value
    }

    @Test
    fun accuracy_and_speed_are_measured_with_enough_trials() {
        val outcomes = List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertEquals(1.0, measured(m.accuracy), 1e-9)
        assertEquals(400.0, measured(m.correctResponseSpeedMs), 1e-9)
        assertEquals(0.0, measured(m.anticipationRate), 1e-9)
        assertEquals(VISUAL_SCAN_METRICS_VERSION, m.version)
    }

    @Test
    fun insufficient_trials_leave_accuracy_unavailable() {
        val outcomes = List(3) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        assertIs<VisualScanSignal.Unavailable>(VisualScanMetricsMapper.map(outcomes).accuracy)
    }

    @Test
    fun fast_wrong_rate_counts_only_fast_incorrect_trials() {
        // 6 correct (slow) + 2 fast-wrong (150ms, incorrect) ⇒ fastWrongRate = 2/8.
        val correct = List(6) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val fastWrong = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 150L, correct = false) }
        val m = VisualScanMetricsMapper.map(correct + fastWrong)
        assertEquals(2.0 / 8.0, measured(m.fastWrongRate), 1e-9)
        assertEquals(2.0 / 8.0, measured(m.speedAccuracyTradeoff), 1e-9)
    }

    @Test
    fun a_late_wrong_answer_is_not_counted_as_fast_wrong() {
        // Wrong but SLOW (900ms, e.g. past deadline) ⇒ commission error, not impulsive fast-wrong.
        val correct = List(6) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val slowWrong = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 900L, correct = false) }
        assertEquals(0.0, measured(VisualScanMetricsMapper.map(correct + slowWrong).fastWrongRate), 1e-9)
    }

    @Test
    fun side_bias_measures_response_side_skew() {
        // 8 trials, target balanced, but the user always taps LEFT ⇒ respondedLeftRate 1.0 ⇒ bias +0.5.
        val outcomes = (0 until 8).map { i ->
            val target = if (i % 2 == 0) SpeedChoice.LEFT else SpeedChoice.RIGHT
            outcome(4, target, SpeedChoice.LEFT, 400L, correct = target == SpeedChoice.LEFT)
        }
        assertEquals(0.5, measured(VisualScanMetricsMapper.map(outcomes).sideBiasIndex), 1e-9)
    }

    @Test
    fun load_cost_is_measured_only_with_at_least_two_load_buckets() {
        // load 3: 4/4 correct (acc 1.0); load 6: 2/4 correct (acc 0.5). highLoadCost = 1.0 - 0.5 = 0.5.
        val low = List(4) { outcome(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val highCorrect = List(2) { outcome(6, SpeedChoice.LEFT, SpeedChoice.LEFT, 500L, correct = true) }
        val highWrong = List(2) { outcome(6, SpeedChoice.LEFT, SpeedChoice.RIGHT, 500L, correct = false) }
        val m = VisualScanMetricsMapper.map(low + highCorrect + highWrong)
        assertEquals(0.5, measured(m.highLoadCost), 1e-9)
        assertEquals(0.5, measured(m.visualLoadTolerance), 1e-9, "accuracy retained at the highest load")
        assertEquals(0.75, measured(m.loadAdjustedAccuracy), 1e-9)
    }

    @Test
    fun single_load_level_leaves_load_metrics_unavailable() {
        val outcomes = List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertIs<VisualScanSignal.Unavailable>(m.highLoadCost)
        assertIs<VisualScanSignal.Unavailable>(m.visualLoadTolerance)
        assertIs<VisualScanSignal.Unavailable>(m.loadAdjustedAccuracy)
    }

    @Test
    fun similarity_and_omission_are_structurally_unavailable_with_a_reason() {
        val m = VisualScanMetricsMapper.map(List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, true) })
        val similarity = m.similarityCost
        val omission = m.omissionPressureIndex
        assertIs<VisualScanSignal.Unavailable>(similarity)
        assertIs<VisualScanSignal.Unavailable>(omission)
        assertTrue(similarity.reason.isNotBlank() && omission.reason.isNotBlank(), "must state why it is unavailable")
    }

    @Test
    fun an_empty_run_yields_all_unavailable_signals_never_fabricated_zero() {
        val m = VisualScanMetricsMapper.map(emptyList())
        assertEquals(0, m.totalTrials)
        assertIs<VisualScanSignal.Unavailable>(m.accuracy)
        assertIs<VisualScanSignal.Unavailable>(m.correctResponseSpeedMs)
        assertIs<VisualScanSignal.Unavailable>(m.stabilityIndex)
        assertIs<VisualScanSignal.Unavailable>(m.sideBiasIndex)
    }
}
