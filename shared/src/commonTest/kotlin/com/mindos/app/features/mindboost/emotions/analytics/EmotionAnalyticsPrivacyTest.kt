@file:Suppress("MaxLineLength") // Event construction lines read best whole.

package com.mindos.app.features.mindboost.emotions.analytics

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-B+ §I / MC8u-D §3 privacy + completeness: emotion analytics events must be PII-FREE — no self-report values, no
 * stimulus/response content, no user id, no free text, no wall-clock timestamp — and the day-1 event set required for
 * release observability must be fully covered by typed events.
 */
class EmotionAnalyticsPrivacyTest {

    private fun allEvents(): List<EmotionAnalyticsEvent> {
        val id = EmotionExerciseId.EM1_PAUSA.name
        return listOf(
            EmotionAnalyticsEvent.ExerciseStarted(id),
            EmotionAnalyticsEvent.ExerciseCompleted(id, stoppedForSafety = false),
            EmotionAnalyticsEvent.SessionAbandoned(id),
            EmotionAnalyticsEvent.SessionCompleted(id, counted = true, validSessions = 3),
            EmotionAnalyticsEvent.SessionNotCounted(id, reason = "low_confidence"),
            EmotionAnalyticsEvent.LowConfidenceResultShown(id, anomalyFlags = listOf("low_confidence")),
            EmotionAnalyticsEvent.AnomalyFlagDetected(id, anomalyFlags = listOf("impossible_timing")),
            EmotionAnalyticsEvent.SafetyEventTriggered(id, type = "PAUSE"),
            EmotionAnalyticsEvent.CalibrationProgressUpdated(id, validSessions = 3, target = 5),
            EmotionAnalyticsEvent.CalibrationCompleted(id, validSessions = 5),
            EmotionAnalyticsEvent.RecommendationShown(id, recommendedExerciseId = EmotionExerciseId.EM3_SEPARADOR.name),
            EmotionAnalyticsEvent.HistoryViewed(id),
            EmotionAnalyticsEvent.SessionSaveFailed(id),
            EmotionAnalyticsEvent.SyncFailed(id),
        )
    }

    @Test
    fun every_event_exposes_only_an_exercise_id_no_user_or_content() {
        val forbidden = listOf("valence", "arousal", "selfReport", "phrase", "userId", "Siento", "@", "\"label\"")
        allEvents().forEach { event ->
            assertTrue(event.exerciseId == EmotionExerciseId.EM1_PAUSA.name)
            val rendered = event.toString()
            forbidden.forEach { bad -> assertFalse(rendered.contains(bad, ignoreCase = true), "PII/content token '$bad' in analytics event: $rendered") }
        }
    }

    @Test
    fun day_one_required_events_are_all_covered_by_typed_events() {
        val emitted = allEvents().map { it.name }.toSet()
        EmotionAnalyticsEvent.REQUIRED_DAY1_EVENTS.forEach { required ->
            assertTrue(required in emitted, "required day-1 event '$required' has no typed EmotionAnalyticsEvent")
        }
        // And no event carries a name outside the declared required set (no rogue/undeclared telemetry).
        emitted.forEach { name -> assertTrue(name in EmotionAnalyticsEvent.REQUIRED_DAY1_EVENTS, "undeclared analytics event name '$name'") }
        assertEquals(EmotionAnalyticsEvent.REQUIRED_DAY1_EVENTS, emitted, "typed events and the required day-1 set must match exactly")
    }

    @Test
    fun release_build_must_not_ship_with_noop_analytics() {
        assertFailsWith<IllegalArgumentException> {
            EmotionAnalyticsPolicy.requireRealSinkInRelease(NoOpEmotionAnalytics, isReleaseBuild = true)
        }
        // A debug build may keep the no-op; a release build with a real sink is fine.
        EmotionAnalyticsPolicy.requireRealSinkInRelease(NoOpEmotionAnalytics, isReleaseBuild = false)
        val realSink = object : EmotionAnalytics { override fun log(event: EmotionAnalyticsEvent) = Unit }
        EmotionAnalyticsPolicy.requireRealSinkInRelease(realSink, isReleaseBuild = true)
    }

    @Test
    fun there_is_no_score_event_by_charter() {
        // Emociones has no per-session cognitive score; progression is calibration-count only. If a "Score" event is
        // ever added, this test (and the required-set match above) forces a deliberate charter review.
        val names = allEvents().map { it::class.simpleName }
        assertFalse(names.any { it?.contains("Score") == true }, "Emociones must not emit a per-session score event")
        assertFalse(EmotionAnalyticsEvent.REQUIRED_DAY1_EVENTS.any { it.contains("score") }, "no score telemetry name by charter")
    }
}
