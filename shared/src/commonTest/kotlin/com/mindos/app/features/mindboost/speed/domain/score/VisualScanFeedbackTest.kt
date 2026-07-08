@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** MC9-C · E5 — the visual-search feedback lens maps the preliminary profile to serious, actionable categories. */
class VisualScanFeedbackTest {

    private fun score(macro: SpeedMacroCategory, ceiling: Boolean = false) = CognitiveProfileScore(
        usefulSpeed = 0.5,
        control = 0.5,
        stability = 0.5,
        total = 0.5,
        macroCategory = macro,
        ceilingPenaltyApplied = ceiling,
    )

    @Test
    fun each_macro_pattern_maps_to_a_visual_feedback_kind() {
        assertEquals(VisualScanFeedbackKind.CALIBRATING, VisualScanFeedback.forScore(score(SpeedMacroCategory.CALIBRATING)))
        assertEquals(VisualScanFeedbackKind.FAST_LOW_PRECISION, VisualScanFeedback.forScore(score(SpeedMacroCategory.FAST_IMPULSIVE)))
        assertEquals(VisualScanFeedbackKind.UNSTABLE_SEARCH, VisualScanFeedback.forScore(score(SpeedMacroCategory.VARIABLE)))
        assertEquals(VisualScanFeedbackKind.GOOD_VISUAL_CONTROL, VisualScanFeedback.forScore(score(SpeedMacroCategory.SLOW_PRECISE)))
        assertEquals(VisualScanFeedbackKind.BALANCED, VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED)))
    }

    @Test
    fun the_ceiling_penalty_reads_as_high_visual_load_cost_even_when_balanced_or_precise() {
        // §7.2/§9: precision/stability penalty under this run's load, distinct from the pure impulsive/variable pattern.
        assertEquals(VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST, VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED, ceiling = true)))
        assertEquals(VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST, VisualScanFeedback.forScore(score(SpeedMacroCategory.SLOW_PRECISE, ceiling = true)))
    }

    @Test
    fun forScore_reaches_every_macro_category_except_the_signal_only_ones() {
        // forScore is the macro-only fallback: it covers all kinds EXCEPT STABLE_REDUCE_LATENCY, which needs the
        // real per-trial telemetry (a measured slow correct-RT), only reachable via forRun.
        val reached = setOf(
            VisualScanFeedback.forScore(score(SpeedMacroCategory.CALIBRATING)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.FAST_IMPULSIVE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.VARIABLE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED, ceiling = true)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.SLOW_PRECISE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED)),
        )
        val expected = VisualScanFeedbackKind.entries.toSet() - VisualScanFeedbackKind.STABLE_REDUCE_LATENCY
        assertEquals(expected, reached, "forScore covers every macro-reachable category")
    }

    // --- MC9-C-R: forRun uses the run's REAL telemetry when present, and never rewards fast-but-wrong. ---

    private fun trial(items: Int, target: SpeedChoice, response: SpeedChoice?, rtMs: Long, correct: Boolean) =
        VisualScanTrialOutcome(items, target, response, rtMs * 1_000_000L, correct, anticipation = false)

    @Test
    fun forRun_fast_but_wrong_is_never_a_positive_category() {
        // Mostly fast-wrong ⇒ FAST_LOW_PRECISION even when the macro score would say BALANCED.
        val fastWrong = List(6) { trial(4, SpeedChoice.LEFT, SpeedChoice.RIGHT, 150L, correct = false) }
        val someRight = List(4) { trial(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val metrics = VisualScanMetricsMapper.map(fastWrong + someRight)
        val kind = VisualScanFeedback.forRun(metrics, score(SpeedMacroCategory.BALANCED))
        assertEquals(VisualScanFeedbackKind.FAST_LOW_PRECISION, kind)
        assertTrue(kind != VisualScanFeedbackKind.GOOD_VISUAL_CONTROL && kind != VisualScanFeedbackKind.BALANCED)
    }

    @Test
    fun forRun_high_precision_and_controlled_speed_reads_as_good_visual_control() {
        val outcomes = List(10) { trial(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 600L, correct = true) }
        val metrics = VisualScanMetricsMapper.map(outcomes)
        assertEquals(VisualScanFeedbackKind.GOOD_VISUAL_CONTROL, VisualScanFeedback.forRun(metrics, score(SpeedMacroCategory.SLOW_PRECISE)))
    }

    @Test
    fun forRun_measured_high_load_cost_reads_as_high_visual_load_cost() {
        // load 3 perfect; load 6 mostly wrong (slow, so not fast-wrong) ⇒ measured highLoadCost drives the feedback.
        val low = List(4) { trial(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) }
        val high = (0 until 4).map { i -> trial(6, SpeedChoice.LEFT, if (i < 1) SpeedChoice.LEFT else SpeedChoice.RIGHT, 500L, correct = i < 1) }
        val metrics = VisualScanMetricsMapper.map(low + high)
        assertEquals(VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST, VisualScanFeedback.forRun(metrics, score(SpeedMacroCategory.BALANCED)))
    }

    @Test
    fun forRun_falls_back_to_calibrating_when_the_run_is_too_short() {
        val metrics = VisualScanMetricsMapper.map(List(3) { trial(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 400L, correct = true) })
        assertEquals(VisualScanFeedbackKind.CALIBRATING, VisualScanFeedback.forRun(metrics, score(SpeedMacroCategory.BALANCED)))
    }

    @Test
    fun forRun_high_precision_but_slow_reads_as_reduce_latency() {
        // MC9-C-R2 §8: accurate but slow correct RT (>700ms) ⇒ STABLE_REDUCE_LATENCY (not GOOD_VISUAL_CONTROL).
        val outcomes = List(10) { trial(4, SpeedChoice.LEFT, SpeedChoice.LEFT, 900L, correct = true) }
        val metrics = VisualScanMetricsMapper.map(outcomes)
        assertEquals(VisualScanFeedbackKind.STABLE_REDUCE_LATENCY, VisualScanFeedback.forRun(metrics, score(SpeedMacroCategory.SLOW_PRECISE)))
    }
}
