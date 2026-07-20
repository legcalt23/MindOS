package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionEligibilityPolicy
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegrityStatus
import com.mindos.app.features.mindboost.core.session.cognitive.WriteAheadAnomalies
import com.mindos.app.features.mindboost.core.session.cognitive.WriteAheadVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4 (+ CEO kill-switch micro-patch) · Eligibility is decided by the ONE authority
 * (SessionEligibilityPolicy) and HARD-GATED by measurement quality. Because E8 is preliminary today (§15 gate
 * open), on a non-authoritative timebase, and has no adequate anti-gaming coverage, EVERY E8 run is denied the
 * official lanes — while the GENERIC lifecycle gates (write-ahead / duration / purpose / integrity / data
 * quality) stay individually visible in [CognitiveSessionRun.eligibilityTrace], never masked.
 */
class GoNoGoCognitiveSessionAdapterEligibilityTest {

    @Test
    fun `a clean 120s run passes every GENERIC lifecycle gate but is denied the official lanes while preliminary`() {
        val run = GoNoGoAdapterFixtures.run()
        val t = run.eligibilityTrace
        // GENERIC lifecycle gates all pass — the denial is attributable to measurement quality, not a masked
        // lifecycle failure.
        assertTrue(t.writeAheadEligible)
        assertTrue(t.durationEligible)
        assertTrue(t.integrityEligible)
        assertTrue(t.dataQualityEligible)
        // The PURPOSE gate is the exception, and it is now honestly false: E8 seals no measurement purpose and
        // carries no versioned lineage, so ADR-FOCUS-PURPOSE denies it the profile lane. Before S1 this read
        // `true` for a TRAINING_FREE run with no lineage at all — the over-permissive claim the ADR removed.
        assertFalse(t.purposeEligible, "E8 has no lineage, so the profile lane is denied on purpose grounds")
        assertTrue(SessionEligibilityPolicy.REASON_PURPOSE in t.rejectionReasons)
        // ...but the measurement kill-switch denies every official lane (E8 is preliminary today).
        assertFalse(t.officialUseEligible)
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        assertFalse(run.eligibility.dataQualityCompromised)
        assertTrue(SessionEligibilityPolicy.REASON_PRELIMINARY in t.rejectionReasons)
    }

    @Test
    fun `today's E8 run is denied for preliminary, non-authoritative timebase, and missing anti-gaming coverage`() {
        val reasons = GoNoGoAdapterFixtures.run().eligibilityTrace.rejectionReasons
        assertTrue(SessionEligibilityPolicy.REASON_PRELIMINARY in reasons)
        assertTrue(SessionEligibilityPolicy.REASON_TIMEBASE in reasons)
        assertTrue(SessionEligibilityPolicy.REASON_ANTI_GAMING in reasons)
    }

    @Test
    fun `a preliminary run cannot be consumed as official even by a forgetful ingestor - the run denies it`() {
        val run = GoNoGoAdapterFixtures.run()
        assertTrue(run.score.preliminary, "E8 is informative, not authoritative, while §15 is open")
        // An ingestor that keys ONLY on *Eligible still gets a hard denial from the sealed run itself.
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        // The reason is both structured (trace) and copied into anomalyFlags for at-a-glance scanning.
        assertTrue(SessionEligibilityPolicy.REASON_PRELIMINARY in run.eligibilityTrace.rejectionReasons)
        assertTrue(SessionEligibilityPolicy.REASON_PRELIMINARY in run.anomalyFlags)
    }

    @Test
    fun `an ineligible write-ahead denies every lane, fails its trace gate, and surfaces both flags`() {
        val run = GoNoGoAdapterFixtures.run(
            writeAhead = WriteAheadVerdict(eligible = false, anomalyFlags = listOf(WriteAheadAnomalies.MISSING)),
        )
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        assertFalse(run.eligibilityTrace.writeAheadEligible)
        assertTrue(WriteAheadAnomalies.MISSING in run.anomalyFlags)
        assertTrue(SessionEligibilityPolicy.REASON_WRITE_AHEAD in run.eligibilityTrace.rejectionReasons)
    }

    @Test
    fun `an abandoned write-ahead cannot claim an eligible official result`() {
        val run = GoNoGoAdapterFixtures.run(
            writeAhead = WriteAheadVerdict(eligible = false, anomalyFlags = listOf(WriteAheadAnomalies.ABANDONED)),
        )
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
    }

    @Test
    fun `an ABANDONED completion status is never eligible and is flagged not-completed`() {
        val run = GoNoGoAdapterFixtures.run(completionStatus = SessionCompletionStatus.ABANDONED)
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_NOT_COMPLETED in run.eligibilityTrace.rejectionReasons)
    }

    @Test
    fun `a short fixed-count run fails the GENERIC duration gate, recorded in the trace`() {
        val run = GoNoGoAdapterFixtures.run(durationActualMs = 60_000L)
        val t = run.eligibilityTrace
        assertFalse(t.durationEligible, "under 120s with no approved protocol fails the calibration/ranking gate")
        assertTrue(SessionEligibilityPolicy.REASON_DURATION in t.rejectionReasons)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
    }

    @Test
    fun `compromised timing yields DEVICE_UNRELIABLE, publishes no number, and denies every lane`() {
        val artifacts =
            GoNoGoAdapterFixtures.artifacts(GoNoGoAdapterFixtures.sufficientCleanOutcomes(), missingOnsetTaps = 5)
        val run = GoNoGoAdapterFixtures.run(artifacts)
        assertEquals(ScoreState.DEVICE_UNRELIABLE, run.score.category)
        assertNull(run.score.normalized, "a non-VALID (device-unreliable) run publishes no number — renders '—'")
        assertTrue(run.eligibility.dataQualityCompromised)
        assertEquals(TimingIntegrityStatus.COMPROMISED, run.eligibility.timingIntegrity.integrityStatus)
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        assertFalse(run.eligibilityTrace.integrityEligible)
        assertFalse(run.eligibilityTrace.dataQualityEligible)
    }

    @Test
    fun `a non-VALID score state never publishes a normalized number`() {
        val deviceUnreliable = GoNoGoAdapterFixtures.run(
            GoNoGoAdapterFixtures.artifacts(GoNoGoAdapterFixtures.sufficientCleanOutcomes(), missingOnsetTaps = 5),
        )
        assertEquals(ScoreState.DEVICE_UNRELIABLE, deviceUnreliable.score.category)
        assertNull(deviceUnreliable.score.normalized)

        val calibrating =
            GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goHit(0))))
        assertEquals(ScoreState.CALIBRATING, calibrating.score.category)
        assertNull(calibrating.score.normalized)
    }

    @Test
    fun `a PRACTICE purpose is structurally excluded and flagged`() {
        val run = GoNoGoAdapterFixtures.run(sessionPurpose = SessionPurpose.PRACTICE)
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        assertFalse(run.eligibilityTrace.purposeEligible)
        assertTrue(SessionEligibilityPolicy.REASON_PURPOSE in run.eligibilityTrace.rejectionReasons)
    }

    @Test
    fun `an insufficient run is CALIBRATING, publishes no number, and is denied the official lanes`() {
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goHit(0))))
        assertEquals(ScoreState.CALIBRATING, run.score.category)
        assertNull(run.score.normalized, "no computable index ⇒ '—', never a fabricated 0")
        assertFalse(run.eligibility.profileEligible)
        assertTrue(SessionEligibilityPolicy.REASON_NOT_VALID in run.eligibilityTrace.rejectionReasons)
    }
}
