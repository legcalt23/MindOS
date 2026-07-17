package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusSessionCloudMapperTest {

    private val nowMs = 1_700_000_100_000L

    @Test
    fun mapsSessionToCloudSafeDto_preservingIdentityAndTimes() {
        val session = focusSession(userId = "user-1", endedAtMs = 1_700_000_000_000L, exercise = ExerciseId.E1_VIGIA)
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs)

        assertTrue(dto != null)
        assertEquals("user-1", dto.userId)
        assertEquals("user-1:1700000000000", dto.sessionId)
        assertEquals(FocusSessionCloudSchema.MODULE_ID, dto.moduleId)
        assertEquals(ExerciseId.E1_VIGIA.name, dto.exerciseId)
        assertEquals(session.startedAtMs, dto.startedAtMs)
        assertEquals(session.endedAtMs, dto.endedAtMs)
        assertEquals(session.durationMs, dto.durationMs)
        assertEquals(nowMs, dto.createdAtMs)
        assertEquals(nowMs, dto.updatedAtMs)
        assertEquals(FocusSessionCloudSchema.CURRENT, dto.schemaVersion)
    }

    @Test
    fun reusesExistingSessionId_neverRegenerated() {
        // The '<userId>:<endedAtMs>' id is stable and valid as a Firestore document id; the mapper keeps it.
        val session = focusSession(userId = "abc123", endedAtMs = 1_699_999_999_999L)
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "abc123", nowMs = nowMs)
        assertEquals(session.sessionId.value, dto?.sessionId)
    }

    @Test
    fun defaultsScoreAndCalibrationToNull_preservingNullNotZero() {
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(), userId = "user-1", nowMs = nowMs)
        assertNull(dto?.normalizedScore, "calibrating session has no score; null must not become 0.0")
        assertNull(dto?.validForCalibration)
    }

    @Test
    fun carriesSuppliedScore_whenAlreadySealed() {
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(
            focusSession(), userId = "user-1", nowMs = nowMs,
            validForCalibration = true, normalizedScore = 0.0,
        )
        assertEquals(0.0, dto?.normalizedScore, "0.0 is a real score, distinct from null")
        assertEquals(true, dto?.validForCalibration)
    }

    @Test
    fun rejectsBlankUserId() {
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(), userId = "", nowMs = nowMs))
    }

    @Test
    fun rejectsUserIdThatDoesNotOwnTheSession() {
        // Session id owner is 'user-1'; an authenticated 'intruder' must never write it.
        assertNull(
            FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(), userId = "intruder", nowMs = nowMs),
        )
    }

    @Test
    fun rejectsInvalidFirestoreSessionId() {
        val session = focusSession(userId = "user-1").copy(sessionId = SessionId("user-1:1000/oops"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun fingerprintRoundTripsWithTheDto() {
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(), userId = "user-1", nowMs = nowMs)
        assertTrue(dto != null)
        assertEquals(dto.fingerprint, FocusSessionCloudMapper.fingerprintOf(dto).fingerprint())
    }

    @Test
    fun fingerprintIdentity_matchesTheDtoUserAndSession() {
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(), userId = "user-1", nowMs = nowMs)
        val fp = FocusSessionCloudMapper.fingerprintOf(dto!!)
        assertEquals(dto.userId, fp.userId)
        assertEquals(dto.sessionId, fp.sessionId)
        assertEquals(dto.endedAtMs, fp.completedAtMs)
    }

    // ── normalizedScore validation (Blocker B) ────────────────────────────────

    @Test
    fun rejectsNaNNormalizedScore() {
        assertNull(mapScore(Double.NaN))
    }

    @Test
    fun rejectsInfiniteNormalizedScore() {
        assertNull(mapScore(Double.POSITIVE_INFINITY))
        assertNull(mapScore(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun rejectsNormalizedScoreBelowZero() {
        assertNull(mapScore(-0.1))
    }

    @Test
    fun rejectsNormalizedScoreAboveOne() {
        assertNull(mapScore(1.1))
    }

    @Test
    fun acceptsZeroNormalizedScore_asRealScore() {
        assertEquals(0.0, mapScore(0.0)?.normalizedScore, "0.0 is a real score, distinct from null")
    }

    @Test
    fun acceptsOneNormalizedScore() {
        assertEquals(1.0, mapScore(1.0)?.normalizedScore)
    }

    // ── temporal coherence (Blocker C) ────────────────────────────────────────

    @Test
    fun rejectsNegativeStartedAt() {
        val session = focusSession(startedAtMs = -1L, endedAtMs = 100L)
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsEndedBeforeStarted() {
        val session = focusSession(startedAtMs = 200L, endedAtMs = 100L)
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsNegativeDuration() {
        // durationMs is DERIVED (coerced >= 0), so a negative span manifests as endedAtMs < startedAtMs;
        // the mapper rejects that incoherent temporal window rather than emitting a corrupt session.
        val session = focusSession(startedAtMs = 1_000L, endedAtMs = 999L)
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    // ── canonical sessionId format <userId>:<endedAtMs> (final Blocker) ────────

    @Test
    fun acceptsCanonicalSessionId_withMatchingOwnerAndEndedAt() {
        val session = focusSession(userId = "user-1", endedAtMs = 1_700_000_000_000L)
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs)
        assertEquals("user-1:1700000000000", dto?.sessionId)
    }

    @Test
    fun rejectsSessionIdWithoutOwnerSeparator() {
        val session = focusSession(userId = "user-1").copy(sessionId = SessionId("user-1"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsSessionIdWithBlankEndedAtSuffix() {
        val session = focusSession(userId = "user-1").copy(sessionId = SessionId("user-1:"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsSessionIdWithNonNumericEndedAtSuffix() {
        val session = focusSession(userId = "user-1").copy(sessionId = SessionId("user-1:not-a-time"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsSessionIdWithEndedAtMismatch() {
        val session = focusSession(userId = "user-1", endedAtMs = 1_700_000_000_000L)
            .copy(sessionId = SessionId("user-1:1700000000001"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user-1", nowMs = nowMs))
    }

    @Test
    fun rejectsUserIdContainingOwnerSeparator() {
        // With ':' inside the userId, "<userId>:<endedAtMs>" can no longer be split unambiguously.
        val session = focusSession(userId = "user-1", endedAtMs = 1_700_000_000_000L)
            .copy(sessionId = SessionId("user:1:1700000000000"))
        assertNull(FocusSessionCloudMapper.toCloudDtoOrNull(session, userId = "user:1", nowMs = nowMs))
    }

    private fun mapScore(score: Double?) = FocusSessionCloudMapper.toCloudDtoOrNull(
        focusSession(), userId = "user-1", nowMs = nowMs, normalizedScore = score,
    )
}
