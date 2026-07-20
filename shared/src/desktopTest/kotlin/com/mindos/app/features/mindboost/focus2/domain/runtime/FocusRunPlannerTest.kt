package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.Descriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SecondaryTiming
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialRecord
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 · tarea (5) — a warm-up can never become a measurement, by two independent routes.
 *
 * The pairing matters: each test asserts the barrier AND its discriminating half (the measurement run passing
 * the same check), so a bug that denied everything would not go unnoticed.
 */
class FocusRunPlannerTest {

    private val seedSource = SeedSource { SessionSeed(0xABCDEFL, RngVersion.V1) }

    private fun run(kind: FocusRunKind) = FocusRunPlanner.prepare(kind, "s-${kind.name}", seedSource)

    /** Answers every planned trial correctly — the strongest possible warm-up performance. */
    private fun perfectTrials(run: FocusRun): List<TrialRecord> = run.plan.trials.map { planned ->
        TrialRecord(
            index = planned.index,
            conditionId = planned.conditionId,
            isSignal = planned.isSignal,
            response = planned.correctResponse,
            timing = SecondaryTiming(rtMs = 500L, windowMs = planned.responseWindowMs),
        )
    }

    @Test
    fun barrier_one_a_warm_up_is_excluded_by_its_purpose() {
        listOf(FocusRunKind.TUTORIAL, FocusRunKind.FAMILIARIZATION).forEach { kind ->
            assertEquals(
                ProfileEligibilityDisposition.NotEligibleByPurpose(kind.purpose),
                run(kind).descriptor.evidenceDisposition(),
                "$kind must never be evidence",
            )
        }
        assertEquals(
            ProfileEligibilityDisposition.Eligible,
            run(FocusRunKind.MEASUREMENT).descriptor.evidenceDisposition(),
            "discriminating half: the measured run IS eligible, so the barrier is about purpose, not about everything",
        )
    }

    @Test
    fun barrier_two_a_flawless_warm_up_still_yields_no_statistic_because_its_cells_are_too_small() {
        listOf(FocusRunKind.TUTORIAL, FocusRunKind.FAMILIARIZATION).forEach { kind ->
            val warmUp = run(kind)
            assertTrue(kind.belowMeasurementFloor, "$kind must be structurally below the d' floor")

            val scored = DescriptiveScorer.score(perfectTrials(warmUp), VectorFilterTask.SPEC)
            assertEquals(Verdict.InsufficientData, scored.verdict, "$kind must not produce a verdict")
            assertIs<Descriptive.Absent>(scored.primaryStatistic, "$kind must not produce a d'")
        }

        // The discriminating half: the same flawless answering on a MEASUREMENT run does produce a statistic.
        val measured = run(FocusRunKind.MEASUREMENT)
        val scored = DescriptiveScorer.score(perfectTrials(measured), VectorFilterTask.SPEC)
        assertEquals(Verdict.Valid, scored.verdict)
        assertIs<Descriptive.Present<Double>>(scored.primaryStatistic)
    }

    @Test
    fun a_warm_up_is_the_same_exercise_shortened_not_a_different_one() {
        val tutorial = run(FocusRunKind.TUTORIAL)
        val measured = run(FocusRunKind.MEASUREMENT)
        assertEquals(measured.descriptor.exerciseId, tutorial.descriptor.exerciseId)
        assertEquals(measured.descriptor.generatorVersion, tutorial.descriptor.generatorVersion)
        assertEquals(
            measured.plan.trials.first().responseWindowMs,
            tutorial.plan.trials.first().responseWindowMs,
            "the participant practises the real thing, at the real pace",
        )
        assertEquals(
            FocusRunKind.TUTORIAL.trialsPerCell * 4,
            tutorial.plan.trials.size,
            "the quota is still constructed; only smaller",
        )
    }

    @Test
    fun the_plan_is_generated_from_the_sealed_seed_so_the_run_is_reproducible() {
        val first = run(FocusRunKind.MEASUREMENT)
        val second = run(FocusRunKind.MEASUREMENT)
        assertEquals(first.descriptor.seedHex, second.descriptor.seedHex)
        assertEquals(first.plan.trials, second.plan.trials, "same seal, same plan")
    }

    @Test
    fun the_engine_only_ever_sees_a_trials_condition_and_signal_side() {
        // The projection the runtime consumes. Narrower than the plan on purpose: an engine that received the
        // whole `PlannedTrial` would grow a dependency on the flanker generator that S4's vigilance tracer
        // could not satisfy.
        val measured = run(FocusRunKind.MEASUREMENT)
        assertEquals(measured.plan.trials.size, measured.trialFacts.size)
        measured.plan.trials.zip(measured.trialFacts).forEach { (planned, facts) ->
            assertEquals(planned.conditionId, facts.conditionId)
            assertEquals(planned.isSignal, facts.isSignal)
        }
    }
}
