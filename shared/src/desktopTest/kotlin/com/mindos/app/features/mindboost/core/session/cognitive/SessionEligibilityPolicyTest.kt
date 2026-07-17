package com.mindos.app.features.mindboost.core.session.cognitive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 1 · The pure eligibility policy (P0-1 purpose taxonomy + P0-2 120s/protocol gate). Confirms
 * that official free-mode training is NOT blocked when valid, PRACTICE/DEBUG never count, INITIAL_ASSESSMENT is
 * provisional-only, and calibration honors the 120s wall-clock OR an approved equivalent protocol.
 */
class SessionEligibilityPolicyTest {

    private fun evaluate(inputs: EligibilityInputs) = SessionEligibilityPolicy.evaluate(inputs).eligibility

    private fun trace(inputs: EligibilityInputs) = SessionEligibilityPolicy.evaluate(inputs).trace

    @Test
    fun `valid free-mode training over 120s counts for profile calibration and ranking`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(purpose = SessionPurpose.TRAINING_FREE))
        assertTrue(result.profileEligible)
        assertTrue(result.calibrationEligible)
        assertTrue(result.rankingEligible)
    }

    @Test
    fun `valid recommended training over 120s counts`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(purpose = SessionPurpose.TRAINING_RECOMMENDED))
        assertTrue(result.calibrationEligible)
        assertTrue(result.rankingEligible)
    }

    @Test
    fun `PRACTICE never counts even when clean and over 120s`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(purpose = SessionPurpose.PRACTICE))
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    @Test
    fun `DEBUG never counts`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(purpose = SessionPurpose.DEBUG))
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    @Test
    fun `INITIAL_ASSESSMENT feeds a provisional profile but not calibration or ranking`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(purpose = SessionPurpose.INITIAL_ASSESSMENT))
        assertTrue(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    @Test
    fun `under 120s without a protocol does not count for calibration`() {
        val result = evaluate(
            CognitiveSessionRunFixtures.validInputs(
                purpose = SessionPurpose.TRAINING_FREE,
                durationActualMs = 90_000L,
                approvedProtocol = null,
            ),
        )
        assertTrue(result.profileEligible) // provisional profile does not require 120s
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    @Test
    fun `under 120s with a met approved protocol counts for calibration`() {
        val result = evaluate(
            CognitiveSessionRunFixtures.validInputs(
                purpose = SessionPurpose.TRAINING_FREE,
                durationActualMs = 60_000L,
                completedTrials = 40,
                stimuliShown = 40,
                approvedProtocol = ApprovedProtocol(
                    minTrials = 30,
                    minStimuli = 30,
                    minEffectiveDurationMs = 45_000L,
                    scoringValidated = true,
                ),
            ),
        )
        assertTrue(result.calibrationEligible)
        assertTrue(result.rankingEligible)
    }

    @Test
    fun `an unvalidated protocol does not unlock calibration`() {
        val result = evaluate(
            CognitiveSessionRunFixtures.validInputs(
                durationActualMs = 60_000L,
                completedTrials = 40,
                stimuliShown = 40,
                approvedProtocol = ApprovedProtocol(
                    minTrials = 30,
                    minStimuli = 30,
                    minEffectiveDurationMs = 45_000L,
                    scoringValidated = false,
                ),
            ),
        )
        assertFalse(result.calibrationEligible)
    }

    @Test
    fun `an abandoned session counts for nothing`() {
        val result = evaluate(
            CognitiveSessionRunFixtures.validInputs(completionStatus = SessionCompletionStatus.ABANDONED),
        )
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
    }

    @Test
    fun `compromised data quality counts for nothing`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(dataQualityCompromised = true))
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
    }

    @Test
    fun `compromised timing counts for nothing`() {
        val compromised = CognitiveSessionRunFixtures.cleanTiming().copy(
            integrityStatus = TimingIntegrityStatus.COMPROMISED,
        )
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(timing = compromised))
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
    }

    @Test
    fun `a missing write-ahead draft makes the session ineligible`() {
        val result = evaluate(CognitiveSessionRunFixtures.validInputs(writeAheadPresent = false))
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    // ---- measurement-quality KILL-SWITCH (CEO micro-patch): each condition denies EVERY official lane ----

    @Test
    fun `a preliminary score denies profile, calibration and ranking with an explicit reason`() {
        val inputs = CognitiveSessionRunFixtures.validInputs(measurementPreliminary = true)
        val result = evaluate(inputs)
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_PRELIMINARY in trace(inputs).rejectionReasons)
    }

    @Test
    fun `a non-VALID score category denies every official lane`() {
        val inputs = CognitiveSessionRunFixtures.validInputs(scoreCategoryValid = false)
        val result = evaluate(inputs)
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_NOT_VALID in trace(inputs).rejectionReasons)
    }

    @Test
    fun `a low-confidence score denies every official lane`() {
        val inputs = CognitiveSessionRunFixtures.validInputs(lowConfidence = true)
        val result = evaluate(inputs)
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_LOW_CONFIDENCE in trace(inputs).rejectionReasons)
    }

    @Test
    fun `a non-authoritative timebase denies every official lane`() {
        val inputs = CognitiveSessionRunFixtures.validInputs(authoritativeTimebaseSatisfied = false)
        val result = evaluate(inputs)
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_TIMEBASE in trace(inputs).rejectionReasons)
    }

    @Test
    fun `missing anti-gaming coverage denies every official lane`() {
        val inputs = CognitiveSessionRunFixtures.validInputs(antiGamingCoverageSatisfied = false)
        val result = evaluate(inputs)
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
        assertTrue(SessionEligibilityPolicy.REASON_ANTI_GAMING in trace(inputs).rejectionReasons)
    }

    @Test
    fun `when the kill-switch fires the generic lifecycle gates stay visible in the trace and are NOT masked`() {
        // A preliminary run that is otherwise perfectly clean: write-ahead / duration / purpose / integrity /
        // data-quality all pass in the trace, and ONLY officialUseEligible is false — proving the denial is
        // attributable to the measurement kill-switch, not a masked lifecycle failure.
        val inputs = CognitiveSessionRunFixtures.validInputs(measurementPreliminary = true)
        val t = trace(inputs)
        assertTrue(t.writeAheadEligible)
        assertTrue(t.durationEligible)
        assertTrue(t.purposeEligible)
        assertTrue(t.integrityEligible)
        assertTrue(t.dataQualityEligible)
        assertFalse(t.officialUseEligible)
        assertEquals(listOf(SessionEligibilityPolicy.REASON_PRELIMINARY), t.rejectionReasons)
    }

    @Test
    fun `a fully official-ready run passes every lane and records no rejection reason`() {
        val inputs = CognitiveSessionRunFixtures.validInputs()
        val result = evaluate(inputs)
        assertTrue(result.profileEligible)
        assertTrue(result.calibrationEligible)
        assertTrue(result.rankingEligible)
        assertTrue(trace(inputs).officialUseEligible)
        assertTrue(trace(inputs).rejectionReasons.isEmpty())
    }

    @Test
    fun `the kill-switch reason strings are the canonical mandated wire names`() {
        assertEquals("score_preliminary", SessionEligibilityPolicy.REASON_PRELIMINARY)
        assertEquals("score_not_valid", SessionEligibilityPolicy.REASON_NOT_VALID)
        assertEquals("score_low_confidence", SessionEligibilityPolicy.REASON_LOW_CONFIDENCE)
        assertEquals("timebase_not_authoritative", SessionEligibilityPolicy.REASON_TIMEBASE)
        assertEquals("anti_gaming_coverage_missing", SessionEligibilityPolicy.REASON_ANTI_GAMING)
    }
}
