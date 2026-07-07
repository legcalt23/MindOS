@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun all_six_feedback_categories_are_reachable() {
        val reached = setOf(
            VisualScanFeedback.forScore(score(SpeedMacroCategory.CALIBRATING)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.FAST_IMPULSIVE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.VARIABLE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED, ceiling = true)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.SLOW_PRECISE)),
            VisualScanFeedback.forScore(score(SpeedMacroCategory.BALANCED)),
        )
        assertEquals(VisualScanFeedbackKind.entries.toSet(), reached, "every feedback category must be reachable")
    }
}
