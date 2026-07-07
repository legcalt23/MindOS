@file:Suppress("MaxLineLength") // Event→param mapping lines read best whole.

package com.mindos.app.features.mindboost.emotions.analytics

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsSink

/**
 * MC8u-E §2 — adapts the emotions-local [EmotionAnalytics] to the app-wide [MindBoostAnalyticsSink], mapping each
 * PII-free [EmotionAnalyticsEvent] to a stable snake_case name + COARSE string params. It is a `data class` so two
 * bridges over the same sink compare equal — this keeps `EmotionGraph`'s structural equality and Compose `remember`
 * stability intact when the production graph injects `EmotionAnalyticsBridge(MindBoostAnalyticsHub)`.
 *
 * PRIVACY: params carry ONLY ids/counts/coarse flags — never self-report values, stimulus/response content, a user
 * id, free text, or a wall-clock timestamp (a guard test scans the rendered params). `exercise_id` is an enum name.
 */
data class EmotionAnalyticsBridge(private val sink: MindBoostAnalyticsSink) : EmotionAnalytics {

    override fun log(event: EmotionAnalyticsEvent) {
        sink.log(MindBoostAnalyticsEvent(event.name, paramsOf(event)))
    }

    private fun paramsOf(event: EmotionAnalyticsEvent): Map<String, String> = buildMap {
        put("exercise_id", event.exerciseId)
        when (event) {
            is EmotionAnalyticsEvent.SessionCompleted -> { put("counted", event.counted.toString()); put("valid_sessions", event.validSessions.toString()) }
            is EmotionAnalyticsEvent.SessionNotCounted -> put("reason", event.reason)
            is EmotionAnalyticsEvent.ExerciseCompleted -> put("stopped_for_safety", event.stoppedForSafety.toString())
            is EmotionAnalyticsEvent.LowConfidenceResultShown -> put("anomaly_flags", event.anomalyFlags.joinToString(","))
            is EmotionAnalyticsEvent.AnomalyFlagDetected -> put("anomaly_flags", event.anomalyFlags.joinToString(","))
            is EmotionAnalyticsEvent.SafetyEventTriggered -> put("type", event.type)
            is EmotionAnalyticsEvent.CalibrationProgressUpdated -> { put("valid_sessions", event.validSessions.toString()); put("target", event.target.toString()) }
            is EmotionAnalyticsEvent.CalibrationCompleted -> put("valid_sessions", event.validSessions.toString())
            is EmotionAnalyticsEvent.RecommendationShown -> put("recommended_exercise_id", event.recommendedExerciseId)
            // id-only events: ExerciseStarted / SessionAbandoned / HistoryViewed / SessionSaveFailed / SyncFailed
            else -> Unit
        }
    }
}
