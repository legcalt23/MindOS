@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** MC9-B · E4 — the flexibility-with-precision feedback: honest mapping + safe, non-arcade copy. */
class RuleSwitchFeedbackTest {

    private fun score(cat: SpeedMacroCategory) = CognitiveProfileScore(0.5, 0.5, 0.5, 0.5, cat, ceilingPenaltyApplied = false)
    private fun metrics(switchCount: Int, cost: Double?, post: Double?, stable: Double?, recovery: Double?) =
        RuleSwitchMetrics(switchCount, cost, post, stable, recovery)

    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    private fun fold(s: String) = buildString { for (c in s.lowercase()) append(accents[c] ?: c) }

    private val feedbackStrings = listOf(
        SpeedStrings.RS_FEEDBACK_CALIBRATING, SpeedStrings.RS_FEEDBACK_LOW_PRECISION,
        SpeedStrings.RS_FEEDBACK_HIGH_SWITCH_COST, SpeedStrings.RS_FEEDBACK_GOOD_ADAPTATION,
        SpeedStrings.RS_FEEDBACK_BALANCED,
    )

    @Test
    fun no_switches_or_uncalibrated_reads_as_calibrating() {
        assertEquals(RuleSwitchFeedbackKind.CALIBRATING, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.BALANCED), metrics(0, null, null, null, null)))
        assertEquals(RuleSwitchFeedbackKind.CALIBRATING, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.CALIBRATING), metrics(2, 40.0, 0.9, 0.9, 0.0)))
    }

    @Test
    fun fast_impulsive_reads_as_low_precision() {
        assertEquals(RuleSwitchFeedbackKind.LOW_PRECISION, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.FAST_IMPULSIVE), metrics(2, 40.0, 0.5, 0.9, 1.0)))
    }

    @Test
    fun a_high_switch_cost_or_a_post_switch_accuracy_drop_reads_as_high_switch_cost() {
        assertEquals(RuleSwitchFeedbackKind.HIGH_SWITCH_COST, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.BALANCED), metrics(2, 300.0, 0.9, 0.95, 1.0)))
        assertEquals(RuleSwitchFeedbackKind.HIGH_SWITCH_COST, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.BALANCED), metrics(2, 40.0, 0.6, 0.9, 1.0)))
    }

    @Test
    fun low_cost_with_held_accuracy_reads_as_good_adaptation() {
        assertEquals(RuleSwitchFeedbackKind.GOOD_ADAPTATION, RuleSwitchFeedback.forRun(score(SpeedMacroCategory.BALANCED), metrics(2, 40.0, 0.9, 0.92, 0.0)))
    }

    @Test
    fun the_feedback_copy_is_safe_and_not_arcade() {
        val arcade = listOf("turbo", "gamer", "eres lento", "reflejos", "superior")
        val forbidden = ForbiddenLexicon.ALL.map { fold(it) }
        feedbackStrings.forEach { s ->
            val f = fold(s)
            (arcade + forbidden).forEach { bad -> assertFalse(f.contains(bad), "unsafe/arcade token '$bad' in feedback: '$s'") }
        }
    }
}
