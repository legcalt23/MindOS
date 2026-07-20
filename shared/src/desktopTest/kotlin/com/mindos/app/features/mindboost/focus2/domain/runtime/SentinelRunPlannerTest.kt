package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Focus V5 · S4 · tarea (2) — the vigilance run-planner and the SUSTAIN classification rule. */
class SentinelRunPlannerTest {

    private val fixedSeed = SeedSource { SessionSeed(0x0123456789ABCDEFL, RngVersion.V1) }

    @Test
    fun the_measurement_run_presents_the_ratified_quota_as_pure_vigilance_facts() {
        val run = SentinelRunPlanner.prepare(SentinelRunKind.MEASUREMENT, "s-1", fixedSeed)
        assertEquals(SentinelSpec.DEFAULT_SIGNAL_TRIALS + SentinelSpec.DEFAULT_NOISE_TRIALS, run.trialFacts.size)
        assertTrue(run.trialFacts.all { it.conditionId == ConditionId.VIGILANCE }, "every fact is vigilance")
        assertEquals(SentinelSpec.DEFAULT_SIGNAL_TRIALS, run.trialFacts.count { it.isSignal }, "signal count")
        assertEquals(SentinelSpec.DEFAULT_NOISE_TRIALS, run.trialFacts.count { !it.isSignal }, "noise count")
        assertEquals("foc2-e01-centinela", run.descriptor.exerciseId)
    }

    @Test
    fun a_measurement_run_is_eligible_while_the_warm_ups_are_neither_eligible_nor_measurable() {
        assertEquals(
            ProfileEligibilityDisposition.Eligible,
            SentinelRunPlanner.prepare(SentinelRunKind.MEASUREMENT, "s-m", fixedSeed).descriptor.evidenceDisposition(),
        )
        listOf(SentinelRunKind.TUTORIAL, SentinelRunKind.FAMILIARIZATION).forEach { kind ->
            val run = SentinelRunPlanner.prepare(kind, "s-${kind.name}", fixedSeed)
            assertEquals(
                ProfileEligibilityDisposition.NotEligibleByPurpose(kind.purpose),
                run.descriptor.evidenceDisposition(),
                "${kind.name} must never be evidence by purpose",
            )
            assertTrue(kind.belowMeasurementFloor, "${kind.name} must also be unmeasurable by construction")
        }
        assertFalse(SentinelRunKind.MEASUREMENT.belowMeasurementFloor, "the discriminating half: the real block IS measurable")
    }

    /**
     * The one place the go-only paradigm departs from the flanker's classification, pinned so it cannot regress:
     * silence on a NON-target is a correct rejection, and silence on a target is a miss — never an omission.
     * An omission would invent an error out of the paradigm's own normal behaviour (not responding is expected).
     */
    @Test
    fun the_spec_classifies_silence_as_a_correct_rejection_not_an_omission() {
        val spec = SentinelTask.SPEC
        val slowEnough = 400L // comfortably past the 150 ms anticipation floor
        assertEquals(TrialOutcome.HIT, spec.outcomeOf(isSignal = true, Response.TAP, slowEnough))
        assertEquals(TrialOutcome.MISS, spec.outcomeOf(isSignal = true, Response.NONE, null))
        assertEquals(TrialOutcome.FALSE_ALARM, spec.outcomeOf(isSignal = false, Response.TAP, slowEnough))
        assertEquals(TrialOutcome.CORRECT_REJECTION, spec.outcomeOf(isSignal = false, Response.NONE, null))
        // Never an omission anywhere in go-only vigilance, and a too-fast tap is an anticipation, not a hit.
        assertEquals(TrialOutcome.ANTICIPATION, spec.outcomeOf(isSignal = true, Response.TAP, rtMs = 10L))
    }
}
