@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC9-D-R · E8 — the official run-equivalent adapter: derives from the same outcomes, never lies about NO-GO. */
class GoNoGoOfficialRunAdapterTest {

    private fun stim(type: GoNoGoTrialType, pos: Int) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = 500L,
        foreperiodMs = 600L,
        difficultyLevel = 0,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun out(type: GoNoGoTrialType, rt: Long?, pos: Int = 0) =
        GoNoGoOutcome.resolve(stim(type, pos), GoNoGoResponse(false, rt))

    private fun adapt(outcomes: List<GoNoGoOutcome>, planned: Int = 40, completed: Int = outcomes.size, duration: Long = 1234L):
        OfficialExerciseRunAdapterResult {
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        val score = GoNoGoScorer.score(metrics)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes)
        return GoNoGoOfficialRunAdapter.toOfficialRun(
            sessionId = "s1",
            exerciseId = "E8",
            exerciseVersion = "1.0.0-mc9d",
            summary = summary,
            metrics = metrics,
            score = score,
            diagnostics = diagnostics,
            plannedTrials = planned,
            completedTrials = completed,
            durationActualMs = duration,
        )
    }

    @Test
    fun the_adapter_does_not_hide_omissions_or_commissions() {
        val outcomes = List(4) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.GO, null) } +
            List(2) { out(GoNoGoTrialType.NO_GO, null) } + List(2) { out(GoNoGoTrialType.NO_GO, 200L) }
        val run = adapt(outcomes)
        assertEquals(2, run.omissions, "omissions are explicit, not hidden")
        assertEquals(2, run.commissions, "commissions are explicit, not hidden")
        assertEquals(10, run.totalTrials)
        assertEquals(6, run.goTrials)
        assertEquals(4, run.noGoTrials)
        assertEquals(4 + 2, run.correctTrials, "correct GO (4) + correct NO-GO inhibitions (2)")
        assertEquals(4, run.incorrectTrials)
        assertEquals(0.6, run.accuracy!!, 1e-9, "overall accuracy = 6/10")
    }

    @Test
    fun the_adapter_carries_versions_durations_and_a_deferred_profile_marker() {
        val run = adapt(List(6) { out(GoNoGoTrialType.GO, 300L) } + List(4) { out(GoNoGoTrialType.NO_GO, null) })
        assertEquals("E8", run.exerciseId)
        assertEquals("1.0.0-mc9d", run.exerciseVersion)
        assertEquals(GONOGO_SCORE_VERSION, run.scoreVersion)
        assertEquals(40, run.plannedTrials)
        assertEquals(10, run.completedTrials)
        assertEquals(1234L, run.durationActualMs)
        assertTrue(!run.contributesToOfficialProfile, "profile contribution is DEFERRED")
        assertEquals("s1", run.telemetrySummarySessionId)
    }

    @Test
    fun the_adapter_and_telemetry_summary_derive_from_the_same_outcomes() {
        val outcomes = List(6) { out(GoNoGoTrialType.GO, 300L) } + List(3) { out(GoNoGoTrialType.NO_GO, null) } +
            List(1) { out(GoNoGoTrialType.NO_GO, 150L) }
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes)
        val summaryV1 = GoNoGoTelemetrySummaryV1.of("s1", "E8", metrics, summary, diagnostics, 0L)
        val run = adapt(outcomes)
        assertEquals(summaryV1.totalTrials, run.totalTrials)
        assertEquals(summaryV1.goTrials, run.goTrials)
        assertEquals(summaryV1.noGoTrials, run.noGoTrials)
        assertEquals((summaryV1.commissionRate as GoNoGoSignal.Measured).value, run.commissionRate!!, 1e-9)
        assertEquals((summaryV1.omissionRate as GoNoGoSignal.Measured).value, run.omissionRate!!, 1e-9)
    }

    @Test
    fun a_calibrating_run_has_a_null_preliminary_score_never_a_fabricated_zero() {
        val run = adapt(List(3) { out(GoNoGoTrialType.GO, 300L) } + List(2) { out(GoNoGoTrialType.NO_GO, null) })
        assertNull(run.preliminaryScore)
        assertTrue(run.lowConfidence)
    }

    @Test
    fun anomaly_flags_surface_a_tap_all_run() {
        val run = adapt(List(6) { out(GoNoGoTrialType.GO, 200L) } + List(4) { out(GoNoGoTrialType.NO_GO, 200L) })
        assertTrue("tapAllStrategy" in run.anomalyFlags, "a tap-all run raises the diagnostic flag: ${run.anomalyFlags}")
    }
}
