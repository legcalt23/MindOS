package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S4 — the ENGINE_GENERALIZED demonstration.
 *
 * This is the whole claim of Tracer B reduced to one test: the SAME `FocusSessionRuntime`, wrapping the SAME
 * `FocusSessionReducer`, driving a session whose only exercise-specific inputs are a `List<TrialFacts>` and a
 * `TaskSpec`, runs a vigilance block end to end and produces a scorable measurement — with ZERO vigilance code
 * anywhere in the engine. The reducer is not imported here having been changed for vigilance; it is the identical
 * object the flanker uses, and it never learns which mechanic it is running because the two facts it is handed —
 * "this trial is/ isn't a signal" and "here is how to classify a response" — are all it was ever built to need.
 *
 * The session is scored from the runtime's OWN accumulated tallies (`snapshot.cells()`), not from a separately
 * constructed stream, so the reducer's classification and the scorer's reading cannot disagree by construction.
 */
class SentinelEngineGeneralizationTest {

    private val fixedSeed = SeedSource { SessionSeed(0x0123456789ABCDEFL, RngVersion.V1) }

    /** Self-advancing monotonic clock: each reading is [STEP_NANOS] past the last, so a response lands one step
     *  after its presentation — 400 ms, comfortably past the anticipation floor and inside the window. */
    private class SteadyClock : FocusClock {
        private var nanos = 0L
        override fun elapsedNanos(): Long = nanos.also { nanos += STEP_NANOS }
        override fun wallClockMillis(): Long = 0L
    }

    @Test
    fun the_flanker_engine_runs_a_full_vigilance_session_and_scores_it() {
        val run = SentinelRunPlanner.prepare(SentinelRunKind.MEASUREMENT, "s-1", fixedSeed)
        val runtime = FocusSessionRuntime(
            descriptor = run.descriptor,
            trials = run.trialFacts,
            taskSpec = SentinelTask.SPEC, // the ONLY vigilance-specific thing the engine sees
            clock = SteadyClock(),
        )

        runtime.dispatch(FocusAction.START_SESSION)
        var firstSignalMissed = false
        var firstNoiseFalseAlarmed = false
        run.trialFacts.forEach { _ ->
            runtime.dispatch(FocusAction.PRESENT_TRIAL)
            val current = requireNotNull(runtime.currentTrial)
            // An honest watcher: tap targets, withhold on non-targets — but miss the first target and
            // false-alarm the first non-target, so MISS and FALSE_ALARM are exercised through the reducer too.
            val taps = when {
                current.isSignal && !firstSignalMissed -> { firstSignalMissed = true; false }
                !current.isSignal && !firstNoiseFalseAlarmed -> { firstNoiseFalseAlarmed = true; true }
                else -> current.isSignal
            }
            if (taps) runtime.respond(Response.TAP) else runtime.dispatch(FocusAction.EXPIRE_WINDOW)
        }
        runtime.dispatch(FocusAction.COMPLETE)

        assertEquals(FocusSessionState.COMPLETED, runtime.snapshot.state, "the session must complete")
        assertEquals(SentinelSpec().totalTrials, runtime.snapshot.resolvedTrials, "every trial resolved")

        // The reducer classified vigilance outcomes into the single VIGILANCE condition, with the two injected
        // errors landing exactly where they should.
        val cell = runtime.snapshot.cells().getValue(ConditionId.VIGILANCE)
        assertEquals(SentinelSpec.DEFAULT_SIGNAL_TRIALS - 1, cell.hits, "hits")
        assertEquals(1, cell.misses, "the one injected miss")
        assertEquals(1, cell.falseAlarms, "the one injected false alarm")
        assertEquals(SentinelSpec.DEFAULT_NOISE_TRIALS - 1, cell.correctRejections, "correct rejections")

        // And the engine's own tallies score to a valid, discriminating vigilance measurement.
        val result = DescriptiveScorer.scoreCells(runtime.snapshot.cells(), SentinelTask.SPEC)
        assertEquals(Verdict.Valid, result.verdict)
        assertTrue(DescriptiveScorer.discriminated(result.perCondition.getValue(ConditionId.VIGILANCE)))
    }

    private companion object {
        const val STEP_NANOS = 400L * 1_000_000L
    }
}
