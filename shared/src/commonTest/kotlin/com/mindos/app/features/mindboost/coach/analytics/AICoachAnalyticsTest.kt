@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.coach.analytics

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-F — AI Coach telemetry: declared day-1 event set, PII-free low-cardinality params, fail-safe forwarding. */
class AICoachAnalyticsTest {

    private class Recording : MindBoostAnalyticsSink {
        val events = mutableListOf<MindBoostAnalyticsEvent>()
        override fun log(event: MindBoostAnalyticsEvent) { events += event }
    }

    @Test
    fun the_event_set_is_declared() {
        assertEquals(15, AICoachEvents.ALL.size)
        assertTrue(AICoachEvents.OPENED in AICoachEvents.ALL)
        assertTrue(AICoachEvents.SAFETY_REFUSAL_SHOWN in AICoachEvents.ALL)
        assertTrue(AICoachEvents.RECOMMENDATION_APPLIED in AICoachEvents.ALL)
    }

    @Test
    fun the_full_mc8u_g_ui_event_matrix_is_present() {
        // Every UI-facing event the MC8u-G spec §10 requires must be declared and namespaced.
        listOf(
            "ia_coach_opened", "ia_consent_viewed", "ia_consent_granted", "ia_consent_revoked", "ia_message_sent",
            "ia_response_generated", "ia_provider_unavailable", "ia_limit_reached", "ia_memory_viewed", "ia_memory_deleted",
            "ia_recommendation_shown", "ia_recommendation_applied", "ia_safety_refusal_shown", "ia_crisis_response_shown",
        ).forEach { assertTrue(it in AICoachEvents.ALL, "missing required UI event '$it'") }
    }

    @Test
    fun no_event_name_carries_content_or_a_clinical_lexeme() {
        // Event names may describe an action (e.g. "response_generated"); they must NOT embed content or clinical terms.
        AICoachEvents.ALL.forEach { name ->
            listOf("prompt_text", "response_text", "message_text", "email", "diagnos", "medicac", "trastorno", "ansiedad").forEach {
                assertFalse(name.contains(it, ignoreCase = true), "event name '$name' must not embed content/clinical token '$it'")
            }
            assertTrue(name.startsWith("ia_"), "coach events are namespaced 'ia_': $name")
        }
    }

    @Test
    fun forwards_low_cardinality_pii_free_params() {
        val sink = Recording()
        AICoachAnalytics(sink).log(AICoachEvents.MESSAGE_SENT, mapOf("tier" to "elite"))
        assertEquals(1, sink.events.size)
        val forbiddenKeys = listOf("prompt", "response", "text", "userId", "email", "name")
        sink.events.first().params.keys.forEach { k -> forbiddenKeys.forEach { assertFalse(k.contains(it, ignoreCase = true), "PII/content param key '$k'") } }
    }

    @Test
    fun a_throwing_sink_never_breaks_the_coach() {
        AICoachAnalytics(object : MindBoostAnalyticsSink { override fun log(event: MindBoostAnalyticsEvent) = error("down") }).log(AICoachEvents.OPENED)
    }
}
