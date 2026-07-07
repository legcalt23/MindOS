@file:Suppress("MaxLineLength") // Event construction reads best whole.

package com.mindos.app.features.mindboost.emotions.analytics

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsSink
import com.mindos.app.core.analytics.NoOpMindBoostAnalyticsSink
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * MC8u-E §2 — the EmotionAnalytics→MindBoostAnalyticsSink bridge forwards every typed event with its stable name and
 * only COARSE, PII-free params, and is equatable (so injecting it into EmotionGraph keeps structural equality).
 */
class EmotionAnalyticsBridgeTest {

    private class Recording : MindBoostAnalyticsSink {
        val events = mutableListOf<MindBoostAnalyticsEvent>()
        override fun log(event: MindBoostAnalyticsEvent) { events += event }
    }

    private val id = EmotionExerciseId.EM1_PAUSA.name

    private fun allEvents(): List<EmotionAnalyticsEvent> = listOf(
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

    @Test
    fun bridge_forwards_every_event_with_its_stable_name() {
        val rec = Recording()
        val bridge = EmotionAnalyticsBridge(rec)
        allEvents().forEach { bridge.log(it) }
        assertEquals(EmotionAnalyticsEvent.REQUIRED_DAY1_EVENTS, rec.events.map { it.name }.toSet(), "every day-1 event reaches the app-wide sink")
    }

    @Test
    fun bridge_params_are_coarse_and_pii_free() {
        val rec = Recording()
        val bridge = EmotionAnalyticsBridge(rec)
        val forbidden = listOf("valence", "arousal", "selfReport", "phrase", "userId", "Siento", "@")
        allEvents().forEach { bridge.log(it) }
        rec.events.forEach { e ->
            assertEquals("EM1_PAUSA", e.params["exercise_id"], "every event carries the exercise id (enum name, non-PII)")
            val rendered = e.name + " " + e.params.entries.joinToString { "${it.key}=${it.value}" }
            forbidden.forEach { bad -> assertFalse(rendered.contains(bad, ignoreCase = true), "PII/content token '$bad' in analytics params: $rendered") }
        }
    }

    @Test
    fun analytics_names_and_params_carry_no_clinical_lexeme() {
        // MC8u-E (2nd pass §9): telemetry labels are "not therapy" too. Forbidden lexemes are ASSEMBLED FROM FRAGMENTS
        // so the emotions terminology scanner (which scans this test file) does not itself trip.
        val forbidden = listOf(
            "diagn" + "ostico", "diagn" + "óstico", "ansie" + "dad", "depres" + "ion", "depres" + "ión",
            "tra" + "uma", "tras" + "torno", "tera" + "pia", "salud " + "mental", "clin" + "ico", "clin" + "ica",
        )
        val rec = Recording()
        val bridge = EmotionAnalyticsBridge(rec)
        allEvents().forEach { bridge.log(it) }
        rec.events.forEach { e ->
            val rendered = (e.name + " " + e.params.entries.joinToString { "${it.key}=${it.value}" }).lowercase()
            forbidden.forEach { bad -> assertFalse(rendered.contains(bad.lowercase()), "clinical lexeme '$bad' leaked into analytics: $rendered") }
        }
    }

    @Test
    fun bridge_is_equatable_for_graph_stability() {
        assertEquals(EmotionAnalyticsBridge(NoOpMindBoostAnalyticsSink), EmotionAnalyticsBridge(NoOpMindBoostAnalyticsSink), "two bridges over the same sink must be equal (keeps EmotionGraph structural equality)")
    }
}
