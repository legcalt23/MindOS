@file:Suppress("MaxLineLength") // Event declarations + KDoc read best on one line.

package com.mindos.app.features.mindboost.emotions.analytics

/**
 * Local analytics contract for the Emociones block (MC8u-B+ §I). There is NO shared analytics infrastructure in
 * MindBoost yet, so this is an explicit, testable local interface: the session holder emits typed, PII-FREE events and
 * a sink (production = [NoOpEmotionAnalytics] until a real backend exists) consumes them. Emission is best-effort and
 * fail-safe — analytics can never break, freeze, or alter a training session.
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
 * The typed, PII-free emotion analytics events. Every event names its `exerciseId` (enum name) and only coarse,
 * non-identifying metadata. `score_updated` is intentionally ABSENT — Emociones has no per-session cognitive score by
 * charter; calibration progress is reported via [CalibrationProgressUpdated]/[CalibrationCompleted] instead.
 */
sealed interface EmotionAnalyticsEvent {
    val exerciseId: String

    data class ExerciseStarted(override val exerciseId: String) : EmotionAnalyticsEvent
    data class SessionCompleted(override val exerciseId: String, val counted: Boolean, val validSessions: Int) : EmotionAnalyticsEvent
    data class SessionAbandoned(override val exerciseId: String) : EmotionAnalyticsEvent
    data class SafetyEventTriggered(override val exerciseId: String, val type: String) : EmotionAnalyticsEvent
    data class LowConfidenceResultShown(override val exerciseId: String, val anomalyFlags: List<String>) : EmotionAnalyticsEvent
    data class CalibrationProgressUpdated(override val exerciseId: String, val validSessions: Int, val target: Int) : EmotionAnalyticsEvent
    data class CalibrationCompleted(override val exerciseId: String, val validSessions: Int) : EmotionAnalyticsEvent
    data class RecommendationShown(override val exerciseId: String, val recommendedExerciseId: String) : EmotionAnalyticsEvent
}
