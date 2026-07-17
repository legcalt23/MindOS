package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.SessionFingerprint
import com.mindos.app.features.mindboost.core.sync.SyncSessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult

/**
 * Builds the cloud-safe [FocusSessionCloudDto] (and its [SessionFingerprint]) from a sealed [SessionResult].
 *
 * Fail-closed: [toCloudDtoOrNull] returns null — a session is NEVER written partially or unsafely — when the
 * authenticated user id is blank, when it does not own the session id, or when the session id is not a valid
 * Firestore document id. The DTO carries NO raw trials by construction.
 */
object FocusSessionCloudMapper {

    /** The '<userId>:<endedAtMs>' focus session id encodes its owner as the prefix before this separator. */
    private const val OWNER_SEPARATOR = ':'

    /** A valid [FocusSessionCloudDto.normalizedScore] is null (no score) or a real value in this closed range. */
    private const val MIN_NORMALIZED_SCORE = 0.0
    private const val MAX_NORMALIZED_SCORE = 1.0

    /**
     * @param nowMs injected wall clock for [FocusSessionCloudDto.createdAtMs] / `updatedAtMs`.
     * @param normalizedScore an already-sealed score in 0.0..1.0, or null (calibrating / not supplied). The
     *   pilot passes null and never computes it here — computing a score would touch scoring.
     * @param validForCalibration a sealed calibration verdict, or null = unknown (the pilot passes null).
     */
    @Suppress("LongParameterList") // Mapper: the trailing optional fields are caller-supplied cloud metadata.
    fun toCloudDtoOrNull(
        result: SessionResult,
        userId: String,
        nowMs: Long,
        scoringVersion: String = FocusSessionCloudSchema.SCORING_VERSION,
        sourcePlatform: String? = null,
        appVersion: String? = null,
        validForCalibration: Boolean? = null,
        normalizedScore: Double? = null,
    ): FocusSessionCloudDto? {
        // Fail-closed: a valid, owned session id AND a temporally coherent session AND a valid score, or null.
        val sessionId = validatedSessionId(result, userId)
            ?.takeIf { isTemporallyCoherent(result) && isValidScore(normalizedScore) }
            ?: return null
        val draft = FocusSessionCloudDto(
            userId = userId,
            sessionId = sessionId.value,
            moduleId = FocusSessionCloudSchema.MODULE_ID,
            exerciseId = result.exerciseFocus.name,
            startedAtMs = result.startedAtMs,
            endedAtMs = result.endedAtMs,
            durationMs = result.durationMs,
            completed = result.endedAtMs >= result.startedAtMs,
            validForCalibration = validForCalibration,
            normalizedScore = normalizedScore,
            scoringVersion = scoringVersion,
            appVersion = appVersion,
            sourcePlatform = sourcePlatform,
            fingerprint = "",
            createdAtMs = nowMs,
            updatedAtMs = nowMs,
        )
        // fingerprintOf ignores the fingerprint field, so hashing the draft yields the final value.
        return draft.copy(fingerprint = fingerprintOf(draft).fingerprint())
    }

    /** The [SessionFingerprint] for a built DTO — used as the `expected` value in sync classification. */
    fun fingerprintOf(dto: FocusSessionCloudDto): SessionFingerprint = SessionFingerprint(
        userId = dto.userId,
        sessionId = dto.sessionId,
        moduleId = dto.moduleId,
        exerciseId = dto.exerciseId,
        startedAtMs = dto.startedAtMs,
        completedAtMs = dto.endedAtMs,
        durationMs = dto.durationMs,
        scoringVersion = dto.scoringVersion,
        normalizedScore = dto.normalizedScore,
    )

    /**
     * A valid, owned [SyncSessionId] in the CANONICAL Enfoque format `<userId>:<endedAtMs>`, or null. The id is
     * accepted ONLY when it parses as a Firestore document id AND the owner prefix equals a non-blank [userId]
     * that itself contains no separator (so the owner/suffix split is unambiguous) AND the suffix is the numeric
     * `endedAtMs` of this session. Anything malformed fails closed (null) — the mapper never regenerates or
     * repairs an id, and does no silent fallback. ReturnCount stays <= 2; the split checks are named to keep the
     * final condition below Detekt's ComplexCondition threshold.
     */
    private fun validatedSessionId(result: SessionResult, userId: String): SyncSessionId? {
        val raw = result.sessionId.value
        val parsed = SyncSessionId.parseOrNull(raw) ?: return null
        val owner = raw.substringBefore(OWNER_SEPARATOR)
        val endedAtText = raw.substringAfter(OWNER_SEPARATOR, missingDelimiterValue = "")
        val ownerValid = userId.isNotBlank() && OWNER_SEPARATOR !in userId && owner == userId
        val suffixValid = raw.indexOf(OWNER_SEPARATOR) > 0 && endedAtText.toLongOrNull() == result.endedAtMs
        return if (ownerValid && suffixValid) parsed else null
    }

    /**
     * A temporally coherent session: non-negative start, an end at or after the start, and a non-negative
     * duration. Note `SessionResult.durationMs` is DERIVED as `(endedAtMs - startedAtMs).coerceAtLeast(0)`,
     * so for an accepted session (`endedAtMs >= startedAtMs`) it equals `endedAtMs - startedAtMs` by
     * construction; the `>= 0` check is defensive and we do not re-enforce that exact equality (the domain
     * getter guarantees it, and a genuinely negative duration is unreachable through the model).
     */
    private fun isTemporallyCoherent(result: SessionResult): Boolean =
        result.startedAtMs >= 0 &&
            result.endedAtMs >= result.startedAtMs &&
            result.durationMs >= 0

    /**
     * A valid score is null (calibrating / not supplied — preserving `null != 0`) or a finite value in
     * [MIN_NORMALIZED_SCORE]..[MAX_NORMALIZED_SCORE]. NaN and infinities are rejected; no clamping, no rounding.
     */
    private fun isValidScore(score: Double?): Boolean =
        score == null ||
            (!score.isNaN() && !score.isInfinite() && score in MIN_NORMALIZED_SCORE..MAX_NORMALIZED_SCORE)
}
