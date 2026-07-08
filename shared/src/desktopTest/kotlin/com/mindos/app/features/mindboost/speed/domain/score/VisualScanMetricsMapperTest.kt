package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.exercise.VisualSimilarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** MC9-C-R2 · E5 — the pure metrics mapper: real signals derived, unavailable ones explicitly flagged (no fake 0). */
class VisualScanMetricsMapperTest {

    private fun outcome(
        items: Int,
        target: SpeedChoice,
        response: SpeedChoice?,
        rtMs: Long,
        correct: Boolean,
        anticipation: Boolean = false,
        similarity: VisualSimilarity? = null,
    ) = VisualScanTrialOutcome(items, target, response, rtMs * 1_000_000L, correct, anticipation, similarity)

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
        val correct = List(6) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val fastWrong = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 150L, correct = false) }
        val m = VisualScanMetricsMapper.map(correct + fastWrong)
        assertEquals(2.0 / 8.0, measured(m.fastWrongRate), 1e-9)
        assertEquals(2.0 / 8.0, measured(m.speedAccuracyTradeoff), 1e-9)
    }

    @Test
    fun a_late_wrong_answer_is_not_counted_as_fast_wrong() {
        val correct = List(6) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val slowWrong = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 900L, correct = false) }
        assertEquals(0.0, measured(VisualScanMetricsMapper.map(correct + slowWrong).fastWrongRate), 1e-9)
    }

    // --- MC9-C-R2 §4: split target-side imbalance (of the stimuli) from the user's response bias ---

    @Test
    fun balanced_response_relative_to_targets_yields_zero_side_response_bias() {
        // targetLeft 60%, responseLeft 60% ⇒ sideResponseBias ≈ 0, targetSideImbalance > 0.
        val outcomes = (0 until 10).map { i ->
            val targetLeft = i < 6
            val respondLeft = i < 6
            outcome(
                4,
                if (targetLeft) SpeedChoice.LEFT else SpeedChoice.RIGHT,
                if (respondLeft) SpeedChoice.LEFT else SpeedChoice.RIGHT,
                400L,
                correct = targetLeft == respondLeft,
            )
        }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertEquals(0.0, measured(m.sideResponseBias), 1e-9)
        assertTrue(measured(m.targetSideImbalance) > 0.0, "60% left targets ⇒ positive target imbalance")
    }

    @Test
    fun tapping_one_side_more_than_the_targets_shows_positive_response_bias() {
        // targetLeft 50%, responseLeft 80% ⇒ sideResponseBias = 0.3, targetSideImbalance = 0.
        val outcomes = (0 until 10).map { i ->
            val targetLeft = i < 5
            val respondLeft = i < 8
            outcome(
                4,
                if (targetLeft) SpeedChoice.LEFT else SpeedChoice.RIGHT,
                if (respondLeft) SpeedChoice.LEFT else SpeedChoice.RIGHT,
                400L,
                correct = targetLeft == respondLeft,
            )
        }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertEquals(0.3, measured(m.sideResponseBias), 1e-9)
        assertEquals(0.0, measured(m.targetSideImbalance), 1e-9)
    }

    @Test
    fun side_bias_is_unavailable_with_too_few_trials() {
        val outcomes = List(3) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertIs<VisualScanSignal.Unavailable>(m.sideResponseBias)
        assertIs<VisualScanSignal.Unavailable>(m.targetSideImbalance)
    }

    // --- MC9-C-R2 §5: premature taps ---

    @Test
    fun premature_tap_rate_uses_the_run_premature_count() {
        val outcomes = List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val m = VisualScanMetricsMapper.map(outcomes, prematureTaps = 2)
        assertEquals(2.0 / 10.0, measured(m.prematureTapRate), 1e-9) // 2 / (8 trials + 2 premature)
    }

    // --- MC9-C-R2 §7: load cost clamped to >= 0, slope signed ---

    @Test
    fun load_cost_is_positive_and_slope_negative_when_accuracy_drops_with_load() {
        val low = List(4) { outcome(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) } // acc 1.0
        val highCorrect = List(2) { outcome(6, SpeedChoice.LEFT, SpeedChoice.LEFT, 500L, correct = true) }
        val highWrong = List(2) { outcome(6, SpeedChoice.LEFT, SpeedChoice.RIGHT, 500L, correct = false) } // acc 0.5
        val m = VisualScanMetricsMapper.map(low + highCorrect + highWrong)
        assertEquals(0.5, measured(m.highLoadCost), 1e-9)
        assertEquals(-0.5, measured(m.loadAccuracySlope), 1e-9)
        assertEquals(0.5, measured(m.visualLoadTolerance), 1e-9)
        assertEquals(0.75, measured(m.loadAdjustedAccuracy), 1e-9)
    }

    @Test
    fun load_cost_is_zero_and_slope_positive_when_accuracy_improves_with_load() {
        val lowWrong = List(2) { outcome(3, SpeedChoice.LEFT, SpeedChoice.RIGHT, 400L, correct = false) }
        val lowCorrect = List(2) { outcome(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) } // acc 0.5
        val high = List(4) { outcome(6, SpeedChoice.LEFT, SpeedChoice.LEFT, 500L, correct = true) } // acc 1.0
        val m = VisualScanMetricsMapper.map(lowWrong + lowCorrect + high)
        assertEquals(0.0, measured(m.highLoadCost), 1e-9, "a cost is never negative")
        assertEquals(0.5, measured(m.loadAccuracySlope), 1e-9, "improvement at high load ⇒ positive slope")
    }

    @Test
    fun single_load_level_leaves_load_metrics_unavailable() {
        val outcomes = List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val m = VisualScanMetricsMapper.map(outcomes)
        assertIs<VisualScanSignal.Unavailable>(m.highLoadCost)
        assertIs<VisualScanSignal.Unavailable>(m.loadAccuracySlope)
        assertIs<VisualScanSignal.Unavailable>(m.visualLoadTolerance)
        assertIs<VisualScanSignal.Unavailable>(m.loadAdjustedAccuracy)
    }

    // --- MC9-C-R2 §10: similarityCost ready when levels vary; unavailable (single level) in production ---

    @Test
    fun similarity_cost_is_measured_when_two_similarity_levels_are_present() {
        val low = List(4) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true, similarity = VisualSimilarity.LOW) }
        val highCorrect = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true, similarity = VisualSimilarity.HIGH) }
        val highWrong = List(2) { outcome(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 400L, correct = false, similarity = VisualSimilarity.HIGH) }
        val m = VisualScanMetricsMapper.map(low + highCorrect + highWrong)
        assertEquals(0.5, measured(m.similarityCost), 1e-9) // acc(LOW)=1.0 − acc(HIGH)=0.5
    }

    @Test
    fun similarity_cost_is_unavailable_with_a_single_level() {
        val outcomes = List(8) { outcome(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true, similarity = VisualSimilarity.LOW) }
        assertIs<VisualScanSignal.Unavailable>(VisualScanMetricsMapper.map(outcomes).similarityCost)
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
        assertIs<VisualScanSignal.Unavailable>(m.sideResponseBias)
        assertIs<VisualScanSignal.Unavailable>(m.targetSideImbalance)
    }
}
