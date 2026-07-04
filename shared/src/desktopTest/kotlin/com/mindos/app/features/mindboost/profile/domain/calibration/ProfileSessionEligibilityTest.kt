package com.mindos.app.features.mindboost.profile.domain.calibration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Fase 1.6 — the session-eligibility gate for PROFILE EVIDENCE (evidence quality, never a psychometric score). */
class ProfileSessionEligibilityTest {

    private val start = 1_000_000L
    private val validEnd = start + ProfileSessionEligibility.MIN_DURATION_MS // exactly the minimum span

    @Test
    fun realSessionWithEvidence_isEligible() {
        assertTrue(ProfileSessionEligibility.isEligible(start, validEnd, hasMeasurableEvidence = true))
    }

    @Test
    fun longEnoughButNoMeasurableEvidence_isIneligible() {
        assertFalse(
            ProfileSessionEligibility.isEligible(start, validEnd, hasMeasurableEvidence = false),
            "duration without evidence is open time, not training",
        )
    }

    @Test
    fun tooShortSession_isIneligible() {
        assertFalse(ProfileSessionEligibility.isEligible(start, validEnd - 1L, hasMeasurableEvidence = true))
    }

    @Test
    fun corruptTimestamps_endedBeforeStarted_isIneligible() {
        assertFalse(
            ProfileSessionEligibility.isEligible(startedAtMs = validEnd, endedAtMs = start, hasMeasurableEvidence = true),
        )
    }

    @Test
    fun absentEndTimestamp_isIneligible_evenIfSpanWouldQualify() {
        assertFalse(
            ProfileSessionEligibility.isEligible(startedAtMs = -120_000L, endedAtMs = 0L, hasMeasurableEvidence = true),
        )
    }
}
