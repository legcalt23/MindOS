@file:Suppress("MaxLineLength") // Event declarations + KDoc read best on one line.

package com.mindos.app.features.mindboost.emotions.analytics

/**
 * Local analytics contract for the Emociones block (MC8u-B+ §I; extended MC8u-D §3). There is NO shared analytics
 * infrastructure in MindBoost yet, so this is an explicit, testable local interface: the session holder emits typed,
 * PII-FREE events and a sink (production = [NoOpEmotionAnalytics] until a real backend is wired) consumes them.
 * Emission is best-effort and fail-safe — analytics can never break, freeze, or alter a training session.
 *
 * PRIVACY: events carry ONLY non-identifying, non-content fields — exercise/recommendation ids (enum names), coarse
 * counts, coarse flags, and safety-event type. NEVER self-report values, stimulus/response content, free text, a user
 * id, or a wall-clock timestamp. A guard test scans every event's rendered fields.
 */
interface EmotionAnalytics {
    fun log(event: EmotionAnalyticsEvent)
}

/** No-op production sink (no analytics backend wired yet — see §I debt). */
object NoOpEmotionAnalytics : EmotionAnalytics {
    override fun log(event: EmotionAnalyticsEvent) = Unit
}

/**
 * MC8u-C §5 / MC8u-D §3: day-1 observability guard. A RELEASE build must NOT ship with the no-op sink — Emociones
 * needs analytics on release. The app calls [requireRealSinkInRelease] at startup with its real build flag; it fails
 * fast (require) if a release build still has [NoOpEmotionAnalytics]. A debug build may keep the no-op. This is the
 * enforceable mechanism; wiring a real backend sink (e.g. Firebase Analytics via a multiplatform wrapper mapping each
 * [EmotionAnalyticsEvent.name] to a logEvent call) + passing the release flag is the remaining app-level integration.
 */
object EmotionAnalyticsPolicy {
    fun requireRealSinkInRelease(analytics: EmotionAnalytics, isReleaseBuild: Boolean) {
        require(!(isReleaseBuild && analytics === NoOpEmotionAnalytics)) {
            "Emociones analytics: a release build must wire a real analytics sink, not NoOpEmotionAnalytics."
        }
    }
}

/**
 * The typed, PII-free emotion analytics events. Every event names its `exerciseId` (enum name), carries a stable
 * snake_case [name] (the telemetry event name a real sink logs), and only coarse, non-identifying metadata.
 *
 * `emotion_score_updated` is intentionally ABSENT — Emociones has NO per-session cognitive/emotional score by charter;
 * progression is reported via [CalibrationProgressUpdated]/[CalibrationCompleted]. Re-introducing a per-session score
 * event would resurrect the removed "loaded gun"; a guard test fails if a `*Score*` event ever appears.
 */
sealed interface EmotionAnalyticsEvent {
    val exerciseId: String

    /** Stable telemetry name a real sink logs (never a raw enum name, never PII). */
    val name: String

    data class ExerciseStarted(override val exerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_exercise_started"
    }

    /** The exercise run reached a non-abandoned terminal (COMPLETED or SAFETY_STOP), independent of whether it counted. */
    data class ExerciseCompleted(override val exerciseId: String, val stoppedForSafety: Boolean) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_exercise_completed"
    }

    data class SessionAbandoned(override val exerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_exercise_abandoned"
    }

    data class SessionCompleted(override val exerciseId: String, val counted: Boolean, val validSessions: Int) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_session_completed"
    }

    /** A completed session that did NOT count toward calibration (too short, safety-stopped, low-confidence, anomalous). */
    data class SessionNotCounted(override val exerciseId: String, val reason: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_session_not_counted"
    }

    data class LowConfidenceResultShown(override val exerciseId: String, val anomalyFlags: List<String>) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_low_confidence_result_shown"
    }

    /** One or more quality anomaly flags fired for this session (coarse flag names only, e.g. `impossible_timing`). */
    data class AnomalyFlagDetected(override val exerciseId: String, val anomalyFlags: List<String>) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_anomaly_flag_detected"
    }

    data class SafetyEventTriggered(override val exerciseId: String, val type: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_safety_event_triggered"
    }

    data class CalibrationProgressUpdated(override val exerciseId: String, val validSessions: Int, val target: Int) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_calibration_progress_updated"
    }

    data class CalibrationCompleted(override val exerciseId: String, val validSessions: Int) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_calibration_completed"
    }

    data class RecommendationShown(override val exerciseId: String, val recommendedExerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_recommendation_shown"
    }

    /** The user opened the history/progress surface. No content, only the block id. */
    data class HistoryViewed(override val exerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_history_viewed"
    }

    /** The terminal could not be made durable (neither summary nor pending marker persisted) — observability for loss. */
    data class SessionSaveFailed(override val exerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_session_save_failed"
    }

    /** A cloud-sync attempt failed. Emitted by the (future) sync layer; declared here so day-1 telemetry is complete. */
    data class SyncFailed(override val exerciseId: String) : EmotionAnalyticsEvent {
        override val name: String get() = "emotion_sync_failed"
    }

    companion object {
        /**
         * MC8u-D §3: the day-1 telemetry names Emociones must be able to emit for release observability. A coverage
         * test asserts every name here is produced by some [EmotionAnalyticsEvent]. `emotion_score_updated` is
         * deliberately NOT here (charter: no per-session score event).
         */
        val REQUIRED_DAY1_EVENTS: Set<String> = setOf(
            "emotion_exercise_started",
            "emotion_exercise_completed",
            "emotion_exercise_abandoned",
            "emotion_session_completed",
            "emotion_session_not_counted",
            "emotion_low_confidence_result_shown",
            "emotion_anomaly_flag_detected",
            "emotion_safety_event_triggered",
            "emotion_calibration_progress_updated",
            "emotion_calibration_completed",
            "emotion_recommendation_shown",
            "emotion_history_viewed",
            "emotion_session_save_failed",
            "emotion_sync_failed",
        )
    }
}
