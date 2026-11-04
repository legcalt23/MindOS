package com.mindos.app.features.mindboost.focus2.domain.evidence

import com.mindos.app.features.mindboost.core.timing.ClockDomain
import com.mindos.app.features.mindboost.core.timing.ProbeCapability
import com.mindos.app.features.mindboost.core.timing.TemporalClass
import com.mindos.app.features.mindboost.core.timing.TemporalWindowVerdict
import com.mindos.app.features.mindboost.core.timing.WindowExclusionReason
import com.mindos.app.features.mindboost.focus2.domain.runtime.ConditionTally
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeInput
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeSnapshot
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionReducer
import com.mindos.app.features.mindboost.focus2.domain.runtime.TemporalCellTally
import com.mindos.app.features.mindboost.focus2.domain.runtime.VectorFilterTask
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SdtCell
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §9 [R2-HIST] — the 0/0 trap and the shadow verdicts. Snapshots are built by
 * DRIVING the reducer (never hand-assembled), so every evaluated state is one the engine can produce.
 */
class FocusTemporalEligibilityTest {

    private val spec = VectorFilterTask.SPEC

    private fun sessionWith(
        verdicts: List<TemporalWindowVerdict?>,
        framesPerWindow: Int = 60,
        response: Response = Response.LEFT,
        rtNanos: Long = RT_NANOS,
    ): FocusRuntimeSnapshot {
        var s = FocusSessionReducer.reduce(
            FocusRuntimeSnapshot.initial("s5-elig"), FocusRuntimeInput.StartSession(0L), spec,
        ).snapshot
        var at = 1_000L
        verdicts.forEach { verdict ->
            s = FocusSessionReducer.reduce(
                s, FocusRuntimeInput.PresentTrial(at, WINDOW_NANOS, PLANNED, ConditionId.CONGRUENT), spec,
            ).snapshot
            s = FocusSessionReducer.reduce(
                s,
                FocusRuntimeInput.Respond(
                    at + rtNanos, response, ConditionId.CONGRUENT, isSignal = true,
                    temporalVerdict = verdict,
                    temporalSampledFrames = if (verdict != null) framesPerWindow else 0,
                ),
                spec,
            ).snapshot
            at += STEP_NANOS
        }
        return s
    }

    // --- The 0/0 trap: absence is NotCaptured, never Eligible ("0 bad") nor Excluded ("0 good") ---

    @Test
    fun emptyCells_isNotCaptured_neverEligibleNorExcluded() {
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = listOf(null, null)),
        )
        assertIs<FocusTemporalEligibility.Verdict.NotCaptured>(evaluation.rtShadowVerdict)
        assertEquals(TemporalClass.T0_NON_AUTHORITATIVE, evaluation.sessionClass)
        assertEquals(null, evaluation.excludedFraction)
    }

    @Test
    fun nullCapability_v1Record_isNotCaptured() {
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = null,
            clockDomain = ClockDomain.UNKNOWN,
            snapshot = sessionWith(verdicts = List(3) { TemporalWindowVerdict.Clean }),
        )
        assertIs<FocusTemporalEligibility.Verdict.NotCaptured>(evaluation.rtShadowVerdict)
    }

    // --- The eligible half (discriminant: an honest session must SURVIVE) ---

    @Test
    fun cleanCoveredSession_atT1_isEligible() {
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = List(4) { TemporalWindowVerdict.Clean }),
        )
        assertEquals(TemporalClass.T1_FRAME_OBSERVED, evaluation.sessionClass)
        assertTrue(evaluation.coverageComplete)
        assertEquals(0.0, evaluation.excludedFraction)
        assertIs<FocusTemporalEligibility.Verdict.Eligible>(evaluation.rtShadowVerdict)
    }

    // --- Each cause, in isolation ---

    @Test
    fun fallbackCapability_isExcluded_nonAuthoritativeClass() {
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.NONE_MONOTONIC_FALLBACK,
            clockDomain = ClockDomain.NONE_MONOTONIC_FALLBACK,
            snapshot = sessionWith(verdicts = List(4) { TemporalWindowVerdict.Clean }),
        )
        val verdict = assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertTrue(FocusTemporalEligibility.ExclusionCause.NON_AUTHORITATIVE_CLASS in verdict.causes)
    }

    @Test
    fun partialCoverage_isExcluded_coverageIncomplete() {
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = listOf(TemporalWindowVerdict.Clean, TemporalWindowVerdict.Clean, null)),
        )
        val verdict = assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertTrue(FocusTemporalEligibility.ExclusionCause.COVERAGE_INCOMPLETE in verdict.causes)
        assertEquals(2, evaluation.windowsClassified)
    }

    @Test
    fun majorityExcluded_trippsTheSessionBackstop() {
        val excluded = TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = listOf(TemporalWindowVerdict.Clean, excluded, excluded, excluded)),
        )
        val verdict = assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertTrue(FocusTemporalEligibility.ExclusionCause.SESSION_BACKSTOP_EXCEEDED in verdict.causes)
        assertEquals(0.75, evaluation.excludedFraction)
    }

    @Test
    fun attributedDiscards_countIntoTheExcludedFraction() {
        var s = sessionWith(verdicts = listOf(TemporalWindowVerdict.Clean))
        // Open a second window and pause: an attributed PAUSE_DISCARD, never resolved.
        s = FocusSessionReducer.reduce(
            s, FocusRuntimeInput.PresentTrial(90_000_000_000L, WINDOW_NANOS, PLANNED, ConditionId.CONGRUENT), spec,
        ).snapshot
        s = FocusSessionReducer.reduce(s, FocusRuntimeInput.Pause(90_000_100_000L), spec).snapshot
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = s,
        )
        assertEquals(0.5, evaluation.excludedFraction, "1 discard over (1 classified + 1 discard)")
    }

    // --- R3.1 contract properties, on the real Kotlin code (the JS harness proves them at contract level) ---

    @Test
    fun outcomesAndRt_doNotChangeTemporalEligibility_nonInterference() {
        // P1 (D-R3-01, T-R3-NONINTERFERENCE): holding the temporal metadata fixed, an arbitrary outcome
        // (a HIT vs a MISS) and an arbitrary RT must not move E. evaluate() reads only temporal cells/frames.
        val verdicts = listOf(
            TemporalWindowVerdict.Clean,
            TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK),
            TemporalWindowVerdict.Clean,
        )
        val hit = FocusTemporalEligibility.evaluate(
            ProbeCapability.ANDROID_CHOREOGRAPHER, ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            sessionWith(verdicts = verdicts, response = Response.LEFT, rtNanos = 200_000_000L),
        )
        val miss = FocusTemporalEligibility.evaluate(
            ProbeCapability.ANDROID_CHOREOGRAPHER, ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            sessionWith(verdicts = verdicts, response = Response.RIGHT, rtNanos = 900_000_000L),
        )
        assertEquals(hit.sessionClass, miss.sessionClass)
        assertEquals(hit.windowsClassified, miss.windowsClassified)
        assertEquals(hit.coverageComplete, miss.coverageComplete)
        assertEquals(hit.excludedFraction, miss.excludedFraction)
        assertEquals(hit.rtShadowVerdict, miss.rtShadowVerdict, "outcome/RT changed the temporal verdict")
    }

    @Test
    fun addingExcludedWindows_isMonotone_neverReopensEligibility() {
        // P2 (Lemma 2 one-sided adders, T-R3-MONOTONICITY): more technically-invalid capture never moves
        // Excluded -> Eligible, and the excluded fraction is non-decreasing as exclusion grows. Threshold-free.
        val excluded = TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)
        var lastFraction = -1.0
        var sawExcluded = false
        listOf(0, 1, 2, 3, 4).forEach { nExcluded ->
            val verdicts = List(4) { if (it < nExcluded) excluded else TemporalWindowVerdict.Clean }
            val e = FocusTemporalEligibility.evaluate(
                ProbeCapability.ANDROID_CHOREOGRAPHER, ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
                sessionWith(verdicts = verdicts),
            )
            val fraction = e.excludedFraction ?: 1.0
            assertTrue(fraction >= lastFraction, "excludedFraction fell: $fraction after $lastFraction")
            lastFraction = fraction
            if (e.rtShadowVerdict is FocusTemporalEligibility.Verdict.Excluded) sawExcluded = true
            if (sawExcluded) {
                assertTrue(
                    e.rtShadowVerdict !is FocusTemporalEligibility.Verdict.Eligible,
                    "more exclusion re-opened eligibility at nExcluded=$nExcluded",
                )
            }
        }
    }

    // --- C4-01: a session that could classify NOTHING must never read as impeccable ------------------

    @Test
    fun allWindowsUnobserved_isExcluded_neverEligible() {
        // This is the iOS path in S5, reachable today: the actual declares `expectedCadenceHz = null`, so
        // `cadenceResolved` fails and EVERY window is NoObservation — while `temporalSampledFrames` keeps
        // accumulating (deltas are counted whether or not they are classifiable), so the session floor is
        // met and the class is authoritative. Counting an unobserved window as "classified" then made
        // coverage complete and the excluded fraction 0.0, and the session came out Eligible: absence
        // fabricating the maximum, which is the direction of `null != 0` that costs the most.
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = List(4) { TemporalWindowVerdict.NoObservation }),
        )
        assertEquals(
            TemporalClass.T1_FRAME_OBSERVED,
            evaluation.sessionClass,
            "the class must stay authoritative, or this test would pass for the wrong reason",
        )
        assertEquals(0, evaluation.windowsClassified, "a NoObservation window classified nothing")
        assertEquals(4, evaluation.windowsUnobserved, "the four windows are unobserved, not absent")
        assertFalse(evaluation.coverageComplete, "4 resolved trials, 0 classified — coverage cannot be complete")
        assertNull(evaluation.excludedFraction, "with nothing classifiable there is no fraction to derive")
        val verdict = assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertEquals(
            setOf(
                FocusTemporalEligibility.ExclusionCause.COVERAGE_INCOMPLETE,
                FocusTemporalEligibility.ExclusionCause.SESSION_BACKSTOP_EXCEEDED,
            ),
            verdict.causes,
        )
    }

    @Test
    fun someWindowsUnobserved_lowerCoverage_withoutInflatingTheExcludedFraction() {
        // The discriminating half. "Unobserved" must not silently become "excluded": coverage falls
        // because we could not classify everything, while the fraction still answers only "of what we DID
        // classify, how much was bad". They are orthogonal axes, and collapsing one into the other would
        // make an unobservable platform look like a degraded one.
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(
                verdicts = listOf(
                    TemporalWindowVerdict.Clean,
                    TemporalWindowVerdict.Clean,
                    TemporalWindowVerdict.Clean,
                    TemporalWindowVerdict.NoObservation,
                ),
            ),
        )
        assertEquals(3, evaluation.windowsClassified)
        assertEquals(1, evaluation.windowsUnobserved)
        assertFalse(evaluation.coverageComplete)
        assertEquals(0.0, evaluation.excludedFraction, "none of the three classified windows was excluded")
        val verdict = assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertEquals(setOf(FocusTemporalEligibility.ExclusionCause.COVERAGE_INCOMPLETE), verdict.causes)
    }

    @Test
    fun degradedUsableIsIndistinguishableFromClean_andThatIsThePinnedS5State() {
        // `MB-FOCUS-0076`. A DegradedUsable window IS classified — its drop fraction is known, it just
        // sits above the clean ceiling and below the exclude floor — so it counts toward coverage and
        // contributes 0 to the excluded fraction.
        //
        // The earlier version of this test asserted exactly those numbers and stopped, which proves
        // nothing: an all-Clean session produces the same ones. What the finding claims is a DISTINCTION
        // WITHOUT CONSEQUENCE, so the assertion has to be the EQUALITY. `TemporalEvaluation` is a data
        // class, so comparing the two whole evaluations covers every field including the verdict, and it
        // fails the day anyone gives this bucket a consequence — which is precisely when the KDoc on
        // `TemporalWindowVerdict.DegradedUsable` has to be rewritten with it. The live contract (R3-11,
        // `D-R3-02` CLOSED) keeps every temporal diagnostic in SHADOW and states none of them produces
        // measurement evidence in S5, so the equality is the deliberate S5 state, not a bug. (The
        // softening tier the R2 sketched is not part of the R3 contract; the R2 sections of that ADR are
        // history and explicitly NOT authoritative.)
        fun evalOf(verdict: TemporalWindowVerdict) = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = sessionWith(verdicts = List(4) { verdict }),
        )

        val degraded = evalOf(TemporalWindowVerdict.DegradedUsable)
        assertEquals(evalOf(TemporalWindowVerdict.Clean), degraded)

        // Spelled out too, so a failure says WHICH fact moved instead of only that two objects differ.
        assertEquals(4, degraded.windowsClassified)
        assertEquals(0, degraded.windowsUnobserved)
        assertTrue(degraded.coverageComplete)
        assertEquals(0.0, degraded.excludedFraction)
        assertIs<FocusTemporalEligibility.Verdict.Eligible>(degraded.rtShadowVerdict)

        // Discriminant half, and the reason the KDoc no longer lumps these two into one clause: the
        // unobserved window is NOT a classification, so it does constrain — coverage, and through it
        // the shadow lane. If this ever passes, the two have been re-coupled and the doc is wrong again.
        assertNotEquals(degraded, evalOf(TemporalWindowVerdict.NoObservation))
    }

    @Test
    fun aBucketThisBuildCannotRead_isNotCountedAsClassified() {
        // The only test in this file that hand-assembles instead of driving the reducer, and it has to:
        // the scenario IS a record written by a build that is not this one. Under the N-1 downgrade window
        // `ADR-FOCUS-MIGRATION` allows, a newer schema's bucket name arrives through the fail-closed
        // `fromWire` lookup as `null`. A denylist (`!= NO_OBSERVATION`) would have counted it as a clean
        // classified window — the same shape as the defect above, one layer down.
        val fromTheFuture = FocusRuntimeSnapshot.initial("s5-elig").copy(
            tallies = listOf(ConditionTally(ConditionId.CONGRUENT.name, SdtCell.EMPTY + TrialOutcome.HIT)),
            temporalCells = listOf(
                TemporalCellTally(
                    conditionName = ConditionId.CONGRUENT.name,
                    bucketName = "A_BUCKET_FROM_A_NEWER_SCHEMA",
                    cell = SdtCell.EMPTY + TrialOutcome.HIT,
                ),
            ),
            temporalSampledFrames = 600,
        )
        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            snapshot = fromTheFuture,
        )
        assertEquals(0, evaluation.windowsClassified, "an unreadable bucket is not a classified window")
        assertEquals(0, evaluation.windowsUnobserved, "nor is it an unobserved one — it is unreadable")
        assertFalse(evaluation.coverageComplete)
        assertIs<FocusTemporalEligibility.Verdict.Excluded>(evaluation.rtShadowVerdict)
    }

    private companion object {
        const val WINDOW_NANOS = 1_200_000_000L
        const val PLANNED = 50
        const val RT_NANOS = 400_000_000L
        const val STEP_NANOS = 2_000_000_000L
    }
}
