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
        GoNoGoScorer.score(GoNoGoMetricsMapper.map(outcomes), GoNoGoQualityDiagnostics.evaluate(outcomes))

    /** A clean (no timing violations) diagnostics of the SAME shape, to isolate the data-quality gate's effect. */
    private fun cleanDiagnostics(trials: Int) =
        GoNoGoQualityDiagnostics.evaluate(
            List(trials) { out(GoNoGoTrialType.GO, 300L) } + List(GoNoGoMetricsMapper.MIN_NO_GO) { out(GoNoGoTrialType.NO_GO, null) },
        )

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

    @Test
    fun impossibly_fast_trials_compromise_data_quality_and_strongly_cap_the_index() {
        // 6 GO tapped at 50ms (< 120ms = physically impossible) + 4 correct NO-GO would otherwise look like elite control.
        val outcomes = List(6) { out(GoNoGoTrialType.GO, 50L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) }
        val metrics = GoNoGoMetricsMapper.map(outcomes)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes)
        // Baseline: same metrics scored under CLEAN timing (what the run WOULD score if timing were reliable).
        val ungated = GoNoGoScorer.score(metrics, cleanDiagnostics(6))
        val gated = GoNoGoScorer.score(metrics, diagnostics)    // sees the impossible RTs ⇒ COMPROMISED
        assertTrue(!ungated.dataQualityCompromised)
        assertTrue(gated.dataQualityCompromised, "impossibly-fast RTs flag compromised measurement")
        assertTrue(gated.lowConfidence)
        assertNotNull(ungated.index)
        assertNotNull(gated.index)
        assertTrue(gated.index!! < ungated.index!! * 0.5, "gated index strongly capped: ${gated.index} vs ${ungated.index}")
        assertTrue(gated.index!! < 0.35, "a fabricated/impossible fast run can never score positive: ${gated.index}")
        assertEquals(TimingIntegrityLevel.COMPROMISED, gated.timingIntegrity)
        assertTrue(!gated.profileEligible && !gated.calibrationEligible && !gated.rankingEligible)
    }

    // ---- MC9-D-R3 — tiered timing integrity + layered eligibility (rule 6: readiness in the data) ----

    @Test
    fun a_clean_confident_strong_run_is_profile_calibration_and_ranking_eligible() {
        val s = score(List(12) { out(GoNoGoTrialType.GO, 300L) } + List(8) { out(GoNoGoTrialType.NO_GO, null) })
        assertNotNull(s.index)
        assertEquals(TimingIntegrityLevel.CLEAN, s.timingIntegrity)
        assertTrue(!s.lowConfidence, "≥8 NO-GO ⇒ confident")
        assertTrue(s.profileEligible && s.calibrationEligible && s.rankingEligible)
    }

    @Test
    fun low_no_go_sample_stays_profile_eligible_but_not_calibration_or_ranking_eligible() {
        // Strong but only 4 NO-GO ⇒ lowConfidence ⇒ calibration/ranking need MORE evidence, profile can still take it.
        val s = score(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertNotNull(s.index)
        assertTrue(s.lowConfidence)
        assertTrue(s.profileEligible)
        assertTrue(!s.calibrationEligible && !s.rankingEligible, "calibration/ranking demand confidence")
    }

    @Test
    fun a_single_impossible_trial_is_a_minor_anomaly_that_dents_not_zeroes_and_blocks_calibration_ranking() {
        // 1 impossible RT in 20 trials ⇒ violationRate 0.05 ≤ 0.10 ⇒ MINOR (proportional penalty, not the ×0.30 floor).
        val outcomes = List(1) { out(GoNoGoTrialType.GO, 50L) } + List(11) { out(GoNoGoTrialType.GO, 300L) } +
            List(8) { out(GoNoGoTrialType.NO_GO, null) }
        val s = score(outcomes)
        assertNotNull(s.index)
        assertEquals(TimingIntegrityLevel.MINOR_ANOMALIES, s.timingIntegrity)
        assertTrue(s.index!! > 0.5, "one stray event must not zero the whole session: ${s.index}")
        assertTrue(!s.dataQualityCompromised)
        assertTrue(s.profileEligible, "minor anomalies still allow profile contribution")
        assertTrue(!s.calibrationEligible && !s.rankingEligible, "but not the stricter bars")
    }

    @Test
    fun missing_onset_taps_are_timing_violations_that_gate_eligibility() {
        val outcomes = List(12) { out(GoNoGoTrialType.GO, 300L) } + List(8) { out(GoNoGoTrialType.NO_GO, null) }
        val metrics = GoNoGoMetricsMapper.map(outcomes)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes, missingOnsetTaps = 1)
        val s = GoNoGoScorer.score(metrics, diagnostics)
        assertTrue("missingOnsetTaps" in diagnostics.activeFlags(), "a lost input is flagged, never hidden")
        assertEquals(TimingIntegrityLevel.MINOR_ANOMALIES, s.timingIntegrity)
        assertTrue(!s.calibrationEligible, "lost input blocks calibration eligibility")
    }

    @Test
    fun the_minor_boundary_is_inclusive_at_exactly_ten_percent() {
        // Exactly 4 impossible RTs in 40 trials ⇒ 0.10, the inclusive upper bound of MINOR (not COMPROMISED).
        val outcomes = List(4) { out(GoNoGoTrialType.GO, 50L) } + List(28) { out(GoNoGoTrialType.GO, 300L) } +
            List(8) { out(GoNoGoTrialType.NO_GO, null) }
        val s = score(outcomes)
        assertEquals(TimingIntegrityLevel.MINOR_ANOMALIES, s.timingIntegrity, "4/40 = 0.10 is inclusive ⇒ MINOR")
        assertTrue(!s.dataQualityCompromised)
    }

    @Test
    fun violations_exceeding_total_are_compromised_without_crashing() {
        // Degenerate defensive case: more lost inputs than trials ⇒ COMPROMISED, no divide-by-zero / no throw.
        val outcomes = List(12) { out(GoNoGoTrialType.GO, 300L) } + List(8) { out(GoNoGoTrialType.NO_GO, null) }
        val metrics = GoNoGoMetricsMapper.map(outcomes)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes, missingOnsetTaps = 100)
        val s = GoNoGoScorer.score(metrics, diagnostics)
        assertEquals(TimingIntegrityLevel.COMPROMISED, s.timingIntegrity)
        assertTrue(s.dataQualityCompromised && !s.profileEligible && !s.calibrationEligible && !s.rankingEligible)
    }
}
