package com.mindos.app.features.mindboost.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SessionFingerprintTest {

    private fun fp(
        sessionId: String = "enfoque-1-abc",
        durationMs: Long = 120L,
        normalizedScore: Double? = 0.63,
    ) = SessionFingerprint(
        userId = "u1",
        sessionId = sessionId,
        moduleId = "ENFOQUE",
        exerciseId = "E1",
        startedAtMs = 100L,
        completedAtMs = 220L,
        durationMs = durationMs,
        scoringVersion = "v1",
        normalizedScore = normalizedScore,
    )

    @Test
    fun sameSession_sameFingerprint() {
        assertEquals(fp().fingerprint(), fp().fingerprint())
        assertEquals(fp().canonical(), fp().canonical())
        assertEquals(fp(), fp()) // data-class equality is the "same session" test
    }

    @Test
    fun differentSessionId_differentFingerprint() {
        assertNotEquals(fp(sessionId = "enfoque-2-xyz").fingerprint(), fp().fingerprint())
    }

    @Test
    fun differentDuration_differentFingerprint() {
        assertNotEquals(fp(durationMs = 999L).fingerprint(), fp().fingerprint())
    }

    @Test
    fun differentScore_differentFingerprint() {
        assertNotEquals(fp(normalizedScore = 0.90).fingerprint(), fp().fingerprint())
    }

    @Test
    fun nullScore_differsFromZeroScore() {
        // null != 0 preserved: a calibrating session (null) must not fingerprint like a zero score.
        assertNotEquals(fp(normalizedScore = null).fingerprint(), fp(normalizedScore = 0.0).fingerprint())
        assertNotEquals(fp(normalizedScore = null).canonical(), fp(normalizedScore = 0.0).canonical())
    }

    @Test
    fun fingerprint_isStableAcrossCalls() {
        val f = fp()
        assertEquals(f.fingerprint(), f.fingerprint())
    }
}
