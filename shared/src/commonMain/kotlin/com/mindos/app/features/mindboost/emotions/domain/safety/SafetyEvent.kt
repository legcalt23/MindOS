package com.mindos.app.features.mindboost.emotions.domain.safety

/**
 * The minimal, persisted safety-event taxonomy. Only these coarse types are stored (no narrative, no
 * free text, no full proxy list — at most a single dominant proxy).
 */
enum class SafetyEventType {
    PAUSE,
    REDUCE_INTENSITY,
    HIGH_INTENSITY_REPORTED,
    SAFETY_STOP,
    ABANDONMENT,
    MID_SESSION_CHECK_IN,
    SESSION_SHORTENED,
}

/**
 * A single safety event during a session. [atOffsetMs] is relative to session start (not a wall clock
 * timestamp) to avoid storing stable time identifiers. [reportedIntensity] is set only for check-ins;
 * [dominantProxy] records at most ONE proxy (the full fired-proxy list stays in memory, never persisted).
 */
data class SafetyEvent(
    val type: SafetyEventType,
    val atOffsetMs: Long,
    val reportedIntensity: Int? = null,
    val dominantProxy: OverloadProxy? = null,
)
