package com.mindos.app.features.mindboost.profile.domain.calibration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Fase 1.6 — the session-eligibility gate for PROFILE EVIDENCE (evidence quality, never a psychometric score). */
class ProfileSessionEligibilityTest {

    private val start = 1_000_000L
    private val validEnd = start + ProfileSessionEligibility.MIN_DURATION_MS // exactly the minimum span
    private val now = validEnd + 10_000_000L // comfortably in the "past" relative to validEnd

    @Test
    fun realSessionWithEvidence_isEligible() {
        assertTrue(ProfileSessionEligibility.isEligible(start, validEnd, hasMeasurableEvidence = true, nowMs = now))
    }

    @Test
    fun longEnoughButNoMeasurableEvidence_isIneligible() {
        assertFalse(
            ProfileSessionEligibility.isEligible(start, validEnd, hasMeasurableEvidence = false, nowMs = now),
            "duration without evidence is open time, not training",
        )
    }

    @Test
    fun tooShortSession_isIneligible() {
        val shortEnd = validEnd - 1L
        assertFalse(ProfileSessionEligibility.isEligible(start, shortEnd, hasMeasurableEvidence = true, nowMs = now))
    }

    @Test
    fun corruptTimestamps_endedBeforeStarted_isIneligible() {
        assertFalse(
            ProfileSessionEligibility.isEligible(
                startedAtMs = validEnd,
                endedAtMs = start,
                hasMeasurableEvidence = true,
                nowMs = now,
            ),
        )
    }

    @Test
    fun absentEndTimestamp_isIneligible_evenIfSpanWouldQualify() {
        assertFalse(
            ProfileSessionEligibility.isEligible(
                startedAtMs = -120_000L,
                endedAtMs = 0L,
                hasMeasurableEvidence = true,
                nowMs = now,
            ),
        )
    }

    // --- SYSTEM-1 Slice 6-R3, C2.4 item 4: future-session exclusion ---

    @Test
    fun futureEndTimestamp_isIneligible_evenIfSpanAndEvidenceWouldQualify() {
        val future = now + 1L
        val futureStart = future - ProfileSessionEligibility.MIN_DURATION_MS
        assertFalse(
            ProfileSessionEligibility.isEligible(futureStart, future, hasMeasurableEvidence = true, nowMs = now),
            "a session ending after 'now' must never count as evidence, let alone be treated as the latest one",
        )
    }

    @Test
    fun endTimestampExactlyAtNow_isEligible_boundaryIsInclusive() {
        assertTrue(
            ProfileSessionEligibility.isEligible(start, validEnd, hasMeasurableEvidence = true, nowMs = validEnd),
        )
    }
}
