@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC9-D · E8 — the honest scorer: caps guarantee velocity cannot buy back commissions or omissions (§7). */
class GoNoGoScoreTest {

    private fun stim(type: GoNoGoTrialType, pos: Int = 0) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = 500L,
        foreperiodMs = 600L,
        difficultyLevel = 0,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun out(type: GoNoGoTrialType, rt: Long?, foreperiodTap: Boolean = false) =
        GoNoGoOutcome.resolve(stim(type), GoNoGoResponse(foreperiodTap, rt))

    private fun score(outcomes: List<GoNoGoOutcome>): GoNoGoScore =
        GoNoGoScorer.score(GoNoGoMetricsMapper.map(outcomes))

    @Test
    fun a_strong_run_scores_high_with_no_caps() {
        val s = score(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertNotNull(s.index)
        assertTrue(s.index!! > 0.9, "near-perfect control + good speed ⇒ high index: ${s.index}")
        assertEquals(GoNoGoCategory.STRONG_CONTROL, s.category)
        assertTrue(!s.commissionCapApplied && !s.goAccuracyCapApplied && !s.anticipationCapApplied)
    }

    @Test
    fun tapping_everything_fast_still_scores_low_velocity_never_rescues_commissions() {
        // Excellent, fast GO responses but a commission on EVERY NO-GO.
        val s = score(List(6) { out(GoNoGoTrialType.GO, 200L) } + List(4) { out(GoNoGoTrialType.NO_GO, 200L) })
        assertNotNull(s.index)
        assertTrue(s.index!! < 0.35, "tap-all must score low despite fast GO: ${s.index}")
        assertTrue(s.commissionCapApplied)
        assertEquals(GoNoGoCategory.IMPULSIVE_COMMISSION, s.category)
    }

    @Test
    fun tapping_nothing_scores_low_even_with_perfect_inhibition() {
        val s = score(List(6) { out(GoNoGoTrialType.GO, null) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertNotNull(s.index)
        assertTrue(s.index!! < 0.3, "tap-none must score low despite perfect NO-GO: ${s.index}")
        assertTrue(s.goAccuracyCapApplied)
        assertEquals(GoNoGoCategory.OMISSION_PRONE, s.category)
    }

    @Test
    fun high_anticipation_caps_the_index_and_flags_the_pattern() {
        // All GO correct, but 2 of 4 NO-GO are foreperiod taps ⇒ anticipationRate = 0.20.
        val outcomes = List(6) { out(GoNoGoTrialType.GO, 300L) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null, foreperiodTap = true) }
        val s = score(outcomes)
        assertTrue(s.anticipationCapApplied)
        assertEquals(GoNoGoCategory.ANTICIPATORY, s.category)
    }

    @Test
    fun an_insufficient_run_is_calibrating_with_a_null_index() {
        val s = score(List(3) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.NO_GO, null) })
        assertNull(s.index, "insufficient sample ⇒ null index, never a fabricated 0")
        assertEquals(GoNoGoCategory.CALIBRATING, s.category)
        assertTrue(s.lowConfidence)
    }

    @Test
    fun the_score_is_always_preliminary() {
        val s = score(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertTrue(s.preliminary)
        assertEquals(GONOGO_SCORE_VERSION, s.version)
    }
}
