@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC9-D · E8 — honest outcome resolution (the five Go/No-Go result classes) + run-summary aggregation. */
class GoNoGoTelemetryTest {

    private fun stim(
        type: GoNoGoTrialType,
        deadlineMs: Long = 500L,
        foreperiodMs: Long = 600L,
        difficulty: Int = 0,
        pos: Int = 0,
    ) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = deadlineMs,
        foreperiodMs = foreperiodMs,
        difficultyLevel = difficulty,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun resolve(type: GoNoGoTrialType, foreperiodTap: Boolean, rtMs: Long?, deadlineMs: Long = 500L) =
        GoNoGoOutcome.resolve(stim(type, deadlineMs), GoNoGoResponse(foreperiodTap, rtMs))

    @Test
    fun go_tap_within_deadline_is_correct() {
        val o = resolve(GoNoGoTrialType.GO, foreperiodTap = false, rtMs = 300L)
        assertTrue(o.correct)
        assertFalse(o.omission || o.commission || o.anticipation || o.lateResponse)
    }

    @Test
    fun go_without_a_tap_is_an_omission() {
        val o = resolve(GoNoGoTrialType.GO, foreperiodTap = false, rtMs = null)
        assertTrue(o.omission)
        assertFalse(o.correct)
    }

    @Test
    fun go_tap_after_the_deadline_is_a_late_response_not_correct() {
        val o = resolve(GoNoGoTrialType.GO, foreperiodTap = false, rtMs = 700L, deadlineMs = 500L)
        assertTrue(o.lateResponse)
        assertFalse(o.correct)
        assertFalse(o.omission)
    }

    @Test
    fun go_tap_during_foreperiod_is_an_anticipation_not_correct() {
        val o = resolve(GoNoGoTrialType.GO, foreperiodTap = true, rtMs = null)
        assertTrue(o.anticipation)
        assertFalse(o.correct || o.omission || o.commission)
    }

    @Test
    fun no_go_without_a_tap_is_a_correct_inhibition() {
        val o = resolve(GoNoGoTrialType.NO_GO, foreperiodTap = false, rtMs = null)
        assertTrue(o.correct)
        assertFalse(o.commission)
    }

    @Test
    fun no_go_tap_after_onset_is_a_commission() {
        val o = resolve(GoNoGoTrialType.NO_GO, foreperiodTap = false, rtMs = 300L)
        assertTrue(o.commission)
        assertFalse(o.correct)
    }

    @Test
    fun no_go_tap_during_foreperiod_is_an_anticipation_and_not_a_commission() {
        val o = resolve(GoNoGoTrialType.NO_GO, foreperiodTap = true, rtMs = null)
        assertTrue(o.anticipation)
        assertFalse(o.correct)
        assertFalse(o.commission, "a foreperiod tap is premature (anticipation), not a post-onset commission")
    }

    @Test
    fun run_summary_aggregates_each_result_class() {
        val outcomes = listOf(
            resolve(GoNoGoTrialType.GO, false, 300L),         // correct GO
            resolve(GoNoGoTrialType.GO, false, null),         // omission
            resolve(GoNoGoTrialType.GO, false, 700L),         // late
            resolve(GoNoGoTrialType.NO_GO, false, null),      // correct inhibition
            resolve(GoNoGoTrialType.NO_GO, false, 150L),      // fast commission
            resolve(GoNoGoTrialType.NO_GO, true, null),       // anticipation on NO-GO
        )
        val s = GoNoGoRunSummary.from(outcomes)
        assertEquals(6, s.totalTrials)
        assertEquals(3, s.goTrials)
        assertEquals(3, s.noGoTrials)
        assertEquals(1, s.goCorrect)
        assertEquals(1, s.noGoCorrect)
        assertEquals(1, s.commissions)
        assertEquals(1, s.omissions)
        assertEquals(1, s.lateResponses)
        assertEquals(1, s.anticipations)
        assertEquals(1, s.fastCommissions)
        assertEquals(listOf(300.0), s.correctGoRtsMs)
    }

    @Test
    fun difficulty_buckets_expose_balanced_accuracy_per_level() {
        val outcomes = listOf(
            GoNoGoOutcome.resolve(stim(GoNoGoTrialType.GO, difficulty = 0), GoNoGoResponse(false, 300L)),     // GO ok lvl0
            GoNoGoOutcome.resolve(stim(GoNoGoTrialType.NO_GO, difficulty = 0), GoNoGoResponse(false, null)),  // NO-GO ok lvl0
            GoNoGoOutcome.resolve(stim(GoNoGoTrialType.GO, difficulty = 1), GoNoGoResponse(false, 300L)),     // GO ok lvl1
            GoNoGoOutcome.resolve(stim(GoNoGoTrialType.NO_GO, difficulty = 1), GoNoGoResponse(false, 200L)),  // NO-GO commission lvl1
        )
        val buckets = GoNoGoRunSummary.from(outcomes).byDifficulty
        assertEquals(2, buckets.size)
        assertEquals(1.0, buckets[0].balancedAccuracyOrNull) // lvl0: goAcc 1, noGoAcc 1 -> 1.0
        assertEquals(0.5, buckets[1].balancedAccuracyOrNull) // lvl1: goAcc 1, noGoAcc 0 -> 0.5
    }
}
