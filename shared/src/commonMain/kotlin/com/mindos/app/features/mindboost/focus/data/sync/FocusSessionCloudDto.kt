package com.mindos.app.features.mindboost.focus.data.sync

import kotlinx.serialization.Serializable

/** Schema constants for the Enfoque (focus) cloud-session pilot. */
object FocusSessionCloudSchema {
    /** Bump when the cloud shape changes; read alongside [FocusSessionCloudDto.schemaVersion]. */
    const val CURRENT = 1

    /** Stable module id carried in the fingerprint and the cloud document (the Enfoque motor). */
    const val MODULE_ID = "focus"

    /**
     * Label of the scoring semantics that produced [FocusSessionCloudDto.normalizedScore]. Bump ONLY when the
     * scoring output changes meaning — this pilot does not compute a score (it leaves the slot null), so the
     * label just travels with the document for future continuity.
     */
    const val SCORING_VERSION = "focus-scoring-1"
}

/**
 * Cloud-safe summary of ONE Enfoque session, written to `users/{userId}/sessions/{sessionId}`.
 *
 * Minimization by design — this is NOT a raw copy of the local `SessionResult`. It carries only allowlisted
 * continuity fields plus the privacy-preserving [fingerprint].
 *
 * EXPLICITLY EXCLUDED (never uploaded in Phase 1): raw trials, the per-trial reaction-time timeline, the
 * per-metric `sessionMetrics` map, the `subjectiveState` self-report (sleep / caffeine / mood — health-adjacent),
 * email / displayName / country / ageRange, any medical or diagnostic field, AI prompts or summaries, and
 * invasive device / advertising identifiers.
 *
 * [normalizedScore] is nullable to preserve `null != 0` (a calibrating session has no score; 0.0 is a real
 * score). [validForCalibration] is nullable = "unknown" — the pilot leaves both null; a future integration
 * fills them from an already-sealed authoritative score without touching scoring here.
 */
@Serializable
data class FocusSessionCloudDto(
    val userId: String,
    val sessionId: String,
    val moduleId: String = FocusSessionCloudSchema.MODULE_ID,
    val exerciseId: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val validForCalibration: Boolean? = null,
    val normalizedScore: Double? = null,
    val scoringVersion: String,
    val appVersion: String? = null,
    val sourcePlatform: String? = null,
    val fingerprint: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val schemaVersion: Int = FocusSessionCloudSchema.CURRENT,
)
