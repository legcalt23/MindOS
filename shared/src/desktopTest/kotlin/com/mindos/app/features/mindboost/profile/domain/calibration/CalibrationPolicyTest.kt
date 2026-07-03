package com.mindos.app.features.mindboost.profile.domain.calibration

import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.SessionEvidence
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fase 1.8 (DC-3) — the upstream calibration authority. Session-count maturity clause: 0/1 valid session is
 * never eligible; ≥2 valid cross-session administrations may reach EMERGING; ESTABLISHED additionally needs
 * ≥5 sessions across ≥3 distinct days. Duplicated / invalid / same-day sessions do not inflate eligibility.
 */
class CalibrationPolicyTest {

    private fun s(id: String, day: Long, valid: Boolean = true) = CalibrationSession(id, day, valid)

    // --- summarize: valid filter + de-dup by id + distinct days ---

    @Test
    fun summarize_dropsInvalidSessions() {
        val e = CalibrationPolicy.summarize(listOf(s("a", 1), s("b", 2, valid = false), s("c", 3)))
        assertEquals(SessionEvidence(validSessions = 2, distinctDays = 2), e)
    }

    @Test
    fun summarize_deDupesBySessionId_soADuplicateIsNotCrossSession() {
        // The same session id appearing twice (a re-read / duplicate) counts once, on one day.
        val e = CalibrationPolicy.summarize(listOf(s("a", 1), s("a", 1), s("a", 2)))
        assertEquals(SessionEvidence(validSessions = 1, distinctDays = 1), e)
    }

    @Test
    fun summarize_countsDistinctDays() {
        val e = CalibrationPolicy.summarize(listOf(s("a", 5), s("b", 5), s("c", 9)))
        assertEquals(SessionEvidence(validSessions = 3, distinctDays = 2), e)
    }

    // --- eligibleCeiling: DC-3 thresholds ---

    @Test
    fun zeroSessions_isNotEligible() {
        assertEquals(DimensionStatus.CALIBRATING, CalibrationPolicy.eligibleCeiling(SessionEvidence(0, 0)))
    }

    @Test
    fun oneSession_isNotEligible() {
        assertEquals(DimensionStatus.CALIBRATING, CalibrationPolicy.eligibleCeiling(SessionEvidence(1, 1)))
    }

    @Test
    fun twoValidCrossSession_mayReachEmerging_butNotEstablished() {
        assertEquals(DimensionStatus.EMERGING, CalibrationPolicy.eligibleCeiling(SessionEvidence(2, 2)))
    }

    @Test
    fun fiveSessionsButTooFewDistinctDays_capsAtEmerging() {
        // ≥5 sessions but only 2 distinct days → not enough temporal spread for ESTABLISHED.
        assertEquals(DimensionStatus.EMERGING, CalibrationPolicy.eligibleCeiling(SessionEvidence(5, 2)))
    }

    @Test
    fun fiveSessionsAcrossThreeDays_mayReachEstablished() {
        assertEquals(DimensionStatus.ESTABLISHED, CalibrationPolicy.eligibleCeiling(SessionEvidence(5, 3)))
    }

    // --- applyCeiling: caps DOWN, never raises ---

    @Test
    fun applyCeiling_capsAConfidentCandidateDownToWhatEvidenceAllows() {
        // Confidence says ESTABLISHED, but a single session forbids any displayable status.
        assertEquals(
            DimensionStatus.CALIBRATING,
            CalibrationPolicy.applyCeiling(DimensionStatus.ESTABLISHED, SessionEvidence(1, 1)),
        )
        // ≥2 sessions but 1 day: ceiling EMERGING caps an ESTABLISHED candidate to EMERGING.
        assertEquals(
            DimensionStatus.EMERGING,
            CalibrationPolicy.applyCeiling(DimensionStatus.ESTABLISHED, SessionEvidence(3, 1)),
        )
    }

    @Test
    fun applyCeiling_neverRaisesAWeakCandidate() {
        // Evidence permits ESTABLISHED, but confidence only says EMERGING → stays EMERGING (no upgrade).
        assertEquals(
            DimensionStatus.EMERGING,
            CalibrationPolicy.applyCeiling(DimensionStatus.EMERGING, SessionEvidence(5, 3)),
        )
    }
}
