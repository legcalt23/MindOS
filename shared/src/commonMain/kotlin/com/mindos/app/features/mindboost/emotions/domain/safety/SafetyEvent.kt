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

    /**
     * MC8u-B §3/§8 — RESERVED, designed-only. No session flow emits this today (the current safety path REDUCES
     * intensity / widens the window and STOPS, it never shortens the block), and no UI state renders it as a
     * user-facing label. `EmotionSafetyUxTest` fails the build if it is ever emitted by the holder or shown without
     * being wired to a real, tested shortening behavior — so it can never become a silent dead-but-visible path.
     */
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
