@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** MC9-D · E8 — feedback lens: 8 actionable cases, priority-ordered; commission never yields positive feedback. */
class GoNoGoFeedbackTest {

    private fun stim(type: GoNoGoTrialType, deadlineMs: Long = 500L, pos: Int = 0) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = deadlineMs,
        foreperiodMs = 600L,
        difficultyLevel = 0,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun out(type: GoNoGoTrialType, rt: Long?, foreperiodTap: Boolean = false, deadlineMs: Long = 500L) =
        GoNoGoOutcome.resolve(stim(type, deadlineMs), GoNoGoResponse(foreperiodTap, rt))

    private fun feedback(outcomes: List<GoNoGoOutcome>): GoNoGoFeedbackKind {
        val metrics = GoNoGoMetricsMapper.map(outcomes)
        return GoNoGoFeedback.forRun(metrics, GoNoGoScorer.score(metrics))
    }

    private val positiveKinds = setOf(
        GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED,
        GoNoGoFeedbackKind.STRONG_CONTROL_REDUCE_LATENCY,
        GoNoGoFeedbackKind.BALANCED,
    )

    @Test
    fun high_commission_yields_the_commission_case_and_never_a_positive_one() {
        val kind = feedback(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, 300L) })
        assertEquals(GoNoGoFeedbackKind.HIGH_COMMISSION, kind)
        assertTrue(kind !in positiveKinds, "a run with commissions must never get positive feedback")
    }

    @Test
    fun high_omission_is_reported_as_missed_go_signals() {
        val outcomes = List(4) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.GO, null) } +
            List(4) { out(GoNoGoTrialType.NO_GO, null) }
        assertEquals(GoNoGoFeedbackKind.HIGH_OMISSION, feedback(outcomes))
    }

    @Test
    fun high_anticipation_is_reported() {
        val outcomes = List(6) { out(GoNoGoTrialType.GO, 300L) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null, foreperiodTap = true) }
        assertEquals(GoNoGoFeedbackKind.HIGH_ANTICIPATION, feedback(outcomes))
    }

    @Test
    fun high_variability_is_reported_as_unstable() {
        val rts = listOf(100L, 700L, 100L, 700L, 100L, 700L)
        val outcomes = rts.map { out(GoNoGoTrialType.GO, it, deadlineMs = 800L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) }
        assertEquals(GoNoGoFeedbackKind.UNSTABLE, feedback(outcomes))
    }

    @Test
    fun strong_control_with_good_speed_is_positive() {
        val kind = feedback(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertEquals(GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED, kind)
    }

    @Test
    fun strong_control_but_slow_recommends_reducing_latency() {
        // Correct but slow GO responses (rt 600 within an 800ms deadline ⇒ correct AND above the slow threshold).
        val kind = feedback(
            List(6) { out(GoNoGoTrialType.GO, 600L, deadlineMs = 800L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) },
        )
        assertEquals(GoNoGoFeedbackKind.STRONG_CONTROL_REDUCE_LATENCY, kind)
    }

    @Test
    fun an_insufficient_run_is_calibrating() {
        assertEquals(
            GoNoGoFeedbackKind.CALIBRATING,
            feedback(List(3) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.NO_GO, null) }),
        )
    }

    @Test
    fun a_mixed_run_without_a_dominant_signal_falls_back_to_balanced() {
        val outcomes = List(10) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.GO, null) } +
            List(6) { out(GoNoGoTrialType.NO_GO, null) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null, foreperiodTap = true) }
        assertEquals(GoNoGoFeedbackKind.BALANCED, feedback(outcomes))
    }
}
