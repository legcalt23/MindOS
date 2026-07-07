@file:Suppress("MaxLineLength") // Event constants read best whole.

package com.mindos.app.features.mindboost.coach.analytics

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsHub
import com.mindos.app.core.analytics.MindBoostAnalyticsSink

/** MC8u-F/MC8u-G — the AI Coach telemetry names. PII-free, low-cardinality (never content, user id, or clinical terms). */
object AICoachEvents {
    const val OPENED = "ia_coach_opened"
    const val MESSAGE_SENT = "ia_message_sent"
    const val RESPONSE_GENERATED = "ia_response_generated"
    const val LIMIT_REACHED = "ia_limit_reached"
    const val CONSENT_REQUESTED = "ia_consent_requested"
    const val CONSENT_VIEWED = "ia_consent_viewed"
    const val CONSENT_GRANTED = "ia_consent_granted"
    const val CONSENT_REVOKED = "ia_consent_revoked"
    const val RECOMMENDATION_SHOWN = "ia_recommendation_shown"
    const val RECOMMENDATION_APPLIED = "ia_recommendation_applied"
    const val SAFETY_REFUSAL_SHOWN = "ia_safety_refusal_shown"
    const val CRISIS_RESPONSE_SHOWN = "ia_crisis_response_shown"
    const val PROVIDER_UNAVAILABLE = "ia_provider_unavailable"
    const val MEMORY_VIEWED = "ia_memory_viewed"
    const val MEMORY_DELETED = "ia_memory_deleted"

    val ALL: Set<String> = setOf(
        OPENED, MESSAGE_SENT, RESPONSE_GENERATED, LIMIT_REACHED, CONSENT_REQUESTED, CONSENT_VIEWED, CONSENT_GRANTED,
        CONSENT_REVOKED, RECOMMENDATION_SHOWN, RECOMMENDATION_APPLIED, SAFETY_REFUSAL_SHOWN, CRISIS_RESPONSE_SHOWN,
        PROVIDER_UNAVAILABLE, MEMORY_VIEWED, MEMORY_DELETED,
    )
}

/**
 * Forwards AI Coach events to the app-wide [MindBoostAnalyticsSink]. Params are LOW-CARDINALITY strings only (tier,
 * block id, calibrated flag, safety category, quota state, recommendation type) — NEVER prompt/response text, user id,
 * email/name, raw cognitive results, or clinical terms. Fail-safe: analytics can never break the coach.
 */
class AICoachAnalytics(private val sink: MindBoostAnalyticsSink = MindBoostAnalyticsHub) {
    fun log(name: String, params: Map<String, String> = emptyMap()) {
        runCatching { sink.log(MindBoostAnalyticsEvent(name, params)) }.exceptionOrNull()?.let { if (it is Error) throw it }
    }
}
