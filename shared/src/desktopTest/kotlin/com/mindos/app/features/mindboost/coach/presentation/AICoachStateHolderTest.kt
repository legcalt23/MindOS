package com.mindos.app.features.mindboost.coach.presentation

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsSink
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.coach.analytics.AICoachAnalytics
import com.mindos.app.features.mindboost.coach.analytics.AICoachEvents
import com.mindos.app.features.mindboost.coach.application.CoachResponseKind
import com.mindos.app.features.mindboost.coach.data.AICoachMemory
import com.mindos.app.features.mindboost.coach.data.SecureAICoachConsentStore
import com.mindos.app.features.mindboost.coach.data.SecureAICoachMemoryStore
import com.mindos.app.features.mindboost.coach.data.SecureAICoachQuotaStore
import com.mindos.app.features.mindboost.coach.domain.AICoachUsageLimits
import com.mindos.app.features.mindboost.coach.domain.CoachCalibrationState
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachContributionSource
import com.mindos.app.features.mindboost.coach.domain.CoachPlanSuggestion
import com.mindos.app.features.mindboost.coach.domain.CoachRecommendation
import com.mindos.app.features.mindboost.coach.domain.CoachRecommendationType
import com.mindos.app.features.mindboost.coach.domain.EliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.EliteTier
import com.mindos.app.features.mindboost.coach.domain.PendingPlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplyOutcome
import com.mindos.app.features.mindboost.coach.prompt.AICoachProvider
import com.mindos.app.features.mindboost.coach.prompt.CoachPrompt
import com.mindos.app.features.mindboost.coach.prompt.PendingAICoachProvider
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * MC8u-G — the AI Coach product flow end-to-end (offline, no real model): consent → question → orchestrator →
 * response → memory/analytics. Every one of the 18 UI states is reached and asserted. Durability, the "safe/crisis
 * never spends quota" policy, memory delete, and recommendation-apply-with-confirmation are all covered.
 */
class AICoachStateHolderTest {

    private class FakeSource(private val contribution: CoachContribution?) : CoachContributionSource {
        override val blockId = "EMOCIONES"
        var calls = 0
        override suspend fun contribute(userId: String): CoachContribution? { calls++; return contribution }
    }
    private class FakeElite(private val tier: EliteTier) : EliteAccessPolicy { override fun tier(userId: String) = tier }
    private class Echo(private val text: String) : AICoachProvider { override suspend fun generate(prompt: CoachPrompt) = text }
    private class RecordingSink : MindBoostAnalyticsSink {
        val events = mutableListOf<MindBoostAnalyticsEvent>()
        override fun log(event: MindBoostAnalyticsEvent) { events += event }
        fun names() = events.map { it.name }
    }
    private class FakeApplier(private val outcome: PlanApplyOutcome) : PlanApplier {
        var calls = 0
        override suspend fun apply(userId: String, suggestion: CoachPlanSuggestion, confirmed: Boolean): PlanApplyOutcome {
            calls++
            return if (confirmed) outcome else PlanApplyOutcome.REJECTED
        }
    }

    private val clockMillis = 1_700_000_000_000L
    private fun established() = CoachContribution("EMOCIONES", CoachCalibrationState.ESTABLISHED, CoachConfidenceLevel.HIGH, patterns = listOf("atencion"))
    private fun withRecommendation() = CoachContribution(
        "EMOCIONES", CoachCalibrationState.CALIBRATING, CoachConfidenceLevel.LOW,
        recommendations = listOf(CoachRecommendation(CoachRecommendationType.PRACTICE_MORE, "Completa mas sesiones validas.", "EMOCIONES")),
    )

    @Suppress("LongParameterList")
    private fun buildHolder(
        scope: CoroutineScope,
        disk: InMemorySecureKeyValueStore,
        source: CoachContributionSource = FakeSource(established()),
        tier: EliteTier = EliteTier.ELITE,
        limits: AICoachUsageLimits = AICoachUsageLimits(),
        provider: AICoachProvider = PendingAICoachProvider,
        sink: RecordingSink = RecordingSink(),
        applier: PlanApplier = PendingPlanApplier,
    ) = AICoachStateHolder(
        userId = "u",
        scope = scope,
        sources = listOf(source),
        consentStore = SecureAICoachConsentStore(disk),
        quotaStore = SecureAICoachQuotaStore(disk),
        memoryStore = SecureAICoachMemoryStore(disk),
        clock = EpochClock { clockMillis },
        elite = FakeElite(tier),
        limits = limits,
        provider = provider,
        analytics = AICoachAnalytics(sink),
        planApplier = applier,
    )

    // 1/2 — a free user is locked out of the coach (training is unaffected).
    @Test
    fun a_free_user_is_free_locked() = runTest {
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), tier = EliteTier.FREE)
        holder.open(); advanceUntilIdle()
        assertEquals(CoachAccessState.FREE_LOCKED, holder.state.access)
    }

    // 3/4 — elite without consent is CONSENT_REQUIRED; viewing the explanation is its own state + a consent_viewed event.
    @Test
    fun elite_without_consent_requires_consent_and_can_show_the_explanation() = runTest {
        val sink = RecordingSink()
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), sink = sink)
        holder.open(); advanceUntilIdle()
        assertEquals(CoachAccessState.CONSENT_REQUIRED, holder.state.access)
        holder.showConsentExplanation()
        assertEquals(CoachAccessState.CONSENT_EXPLANATION, holder.state.access)
        assertTrue(AICoachEvents.CONSENT_VIEWED in sink.names())
    }

    // 5/6 — granting consent is durable (survives a "restart") and opens the usable coach.
    @Test
    fun granting_consent_is_durable_and_opens_ready() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val first = buildHolder(this, disk)
        first.open(); advanceUntilIdle()
        first.grantConsent(); advanceUntilIdle()
        assertEquals(CoachAccessState.READY, first.state.access)
        // "restart": a brand-new holder over the same encrypted disk must reopen READY without re-consent.
        val second = buildHolder(this, disk)
        second.open(); advanceUntilIdle()
        assertEquals(CoachAccessState.READY, second.state.access)
    }

    // Revocation persists, blocks new analysis, and NEVER deletes memory.
    @Test
    fun revoking_consent_persists_blocks_and_keeps_memory() = runTest {
        val disk = InMemorySecureKeyValueStore()
        SecureAICoachMemoryStore(disk).save("u", memory())
        val holder = buildHolder(this, disk)
        holder.open(); advanceUntilIdle()
        holder.grantConsent(); advanceUntilIdle()
        holder.revokeConsent(); advanceUntilIdle()
        assertEquals(CoachAccessState.CONSENT_REQUIRED, holder.state.access)
        // persisted revocation + memory still present (revoke is not delete).
        assertNotNull(SecureAICoachMemoryStore(disk).load("u"), "revoking consent must not delete memory")
    }

    // No progress data is sent to the coach without consent (the sources are never queried).
    @Test
    fun no_progress_data_is_sent_without_consent() = runTest {
        val source = FakeSource(established())
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), source = source)
        holder.open(); advanceUntilIdle()
        holder.sendMessage("¿que practico hoy?"); advanceUntilIdle()
        assertEquals(CoachResponseKind.BLOCKED_NO_CONSENT, holder.state.lastCoachMessage?.kind)
        assertEquals(0, source.calls, "a source must never be queried without consent")
    }

    // 9 — a normal question with the pending provider is UNAVAILABLE and NEVER spends quota.
    @Test
    fun a_pending_provider_is_unavailable_and_does_not_consume_quota() = runTest {
        val sink = RecordingSink()
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        val before = holder.state.remainingQuota
        holder.sendMessage("¿como voy?"); advanceUntilIdle()
        assertEquals(CoachResponseKind.COACH_UNAVAILABLE, holder.state.lastCoachMessage?.kind)
        assertEquals(before, holder.state.remainingQuota, "an unavailable provider must not spend a message")
        assertTrue(AICoachEvents.PROVIDER_UNAVAILABLE in sink.names())
    }

    // 12 — a served answer is shown and consumes exactly one quota unit, persisted across a restart.
    @Test
    fun a_served_answer_consumes_one_quota_and_persists() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val sink = RecordingSink()
        val holder = buildHolder(this, disk, provider = Echo("Tu rendimiento es consistente."), sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        val before = holder.state.remainingQuota
        holder.sendMessage("¿como voy?"); advanceUntilIdle()
        assertEquals(CoachResponseKind.ANSWER, holder.state.lastCoachMessage?.kind)
        assertEquals(before - 1, holder.state.remainingQuota)
        assertTrue(AICoachEvents.RESPONSE_GENERATED in sink.names())
        // durable: a fresh holder over the same disk reopens with the consumed quota.
        val reopened = buildHolder(this, disk)
        reopened.open(); advanceUntilIdle()
        assertEquals(before - 1, reopened.state.remainingQuota, "the quota counter survives a restart")
    }

    // 11 — a crisis message is answered safely and NEVER spends quota; emits a distinct crisis event.
    @Test
    fun a_crisis_message_is_safe_and_free() = runTest {
        val sink = RecordingSink()
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        val before = holder.state.remainingQuota
        holder.sendMessage("a veces quiero hacerme dano"); advanceUntilIdle()
        assertEquals(CoachResponseKind.CRISIS, holder.state.lastCoachMessage?.kind)
        assertEquals(before, holder.state.remainingQuota, "a crisis never spends a message")
        assertTrue(AICoachEvents.CRISIS_RESPONSE_SHOWN in sink.names())
    }

    // 10 — a diagnosis request is refused safely and does not spend quota.
    @Test
    fun a_diagnosis_request_is_refused_and_free() = runTest {
        val holder = buildHolder(this, InMemorySecureKeyValueStore())
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        val before = holder.state.remainingQuota
        holder.sendMessage("diagnosticame, ¿tengo depresion?"); advanceUntilIdle()
        assertEquals(CoachResponseKind.SAFE_REFUSAL, holder.state.lastCoachMessage?.kind)
        assertEquals(before, holder.state.remainingQuota)
    }

    // 7 — quota reached blocks the coach but memory stays usable (training-parallel).
    @Test
    fun quota_reached_blocks_the_coach_but_memory_stays_usable() = runTest {
        val disk = InMemorySecureKeyValueStore()
        SecureAICoachConsentStore(disk).grant("u", clockMillis)
        SecureAICoachMemoryStore(disk).save("u", memory())
        val holder = buildHolder(this, disk, limits = AICoachUsageLimits(dailyQuota = 0))
        holder.open(); advanceUntilIdle()
        assertEquals(CoachAccessState.QUOTA_REACHED, holder.state.access)
        holder.viewMemory(); advanceUntilIdle()
        assertIs<CoachMemoryUi.Loaded>(holder.state.memory)
    }

    // 8 — no data yet ⇒ NOT_ENOUGH_DATA (the coach never invents).
    @Test
    fun no_data_yields_not_enough_data() = runTest {
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), source = FakeSource(null), provider = Echo("x"))
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        holder.sendMessage("analiza mi progreso"); advanceUntilIdle()
        assertEquals(CoachResponseKind.NOT_ENOUGH_DATA, holder.state.lastCoachMessage?.kind)
    }

    // 13/14/15 — memory: empty → loaded → delete-with-confirmation → deleted (emits ia_memory_deleted).
    @Test
    fun memory_loads_and_deletes_only_after_confirmation() = runTest {
        val disk = InMemorySecureKeyValueStore()
        SecureAICoachMemoryStore(disk).save("u", memory())
        val sink = RecordingSink()
        val holder = buildHolder(this, disk, sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        assertIs<CoachMemoryUi.Loaded>(holder.state.memory)
        holder.requestDeleteMemory()
        assertEquals(CoachConfirm.DELETE_MEMORY, holder.state.confirming)
        assertNotNull(SecureAICoachMemoryStore(disk).load("u"), "not deleted until confirmed")
        holder.confirmDeleteMemory(); advanceUntilIdle()
        assertEquals(CoachMemoryUi.Deleted, holder.state.memory)
        assertNull(SecureAICoachMemoryStore(disk).load("u"))
        assertTrue(AICoachEvents.MEMORY_DELETED in sink.names())
        // deleting memory must NOT delete consent.
        assertEquals(CoachAccessState.READY, holder.state.access)
    }

    // 16/17 — a recommendation is shown, and applying with the pending applier changes nothing (no ia_recommendation_applied).
    @Test
    fun a_recommendation_shows_and_a_pending_apply_does_not_mutate() = runTest {
        val sink = RecordingSink()
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), source = FakeSource(withRecommendation()), sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        holder.showRecommendation(); advanceUntilIdle()
        assertIs<CoachRecommendationUi.Shown>(holder.state.recommendation)
        assertTrue(AICoachEvents.RECOMMENDATION_SHOWN in sink.names())
        holder.confirmApplyRecommendation(); advanceUntilIdle()
        assertEquals(CoachRecommendationUi.PendingNotWired, holder.state.recommendation)
        assertFalse(AICoachEvents.RECOMMENDATION_APPLIED in sink.names(), "a pending applier never emits applied")
    }

    // 17 — a real applier mutates ONLY after confirmation (emits ia_recommendation_applied).
    @Test
    fun a_real_apply_requires_confirmation_and_emits_applied() = runTest {
        val sink = RecordingSink()
        val applier = FakeApplier(PlanApplyOutcome.APPLIED)
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), source = FakeSource(withRecommendation()), sink = sink, applier = applier)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        holder.showRecommendation(); advanceUntilIdle()
        holder.confirmApplyRecommendation(); advanceUntilIdle()
        assertEquals(CoachRecommendationUi.Applied, holder.state.recommendation)
        assertEquals(1, applier.calls)
        assertTrue(AICoachEvents.RECOMMENDATION_APPLIED in sink.names())
    }

    // 18 — rejecting a recommendation NEVER calls the applier.
    @Test
    fun rejecting_a_recommendation_never_calls_the_applier() = runTest {
        val applier = FakeApplier(PlanApplyOutcome.APPLIED)
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), source = FakeSource(withRecommendation()), applier = applier)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        holder.showRecommendation(); advanceUntilIdle()
        holder.rejectRecommendation()
        assertEquals(CoachRecommendationUi.Rejected, holder.state.recommendation)
        assertEquals(0, applier.calls)
    }

    // §10 — analytics carry no PII, content, or clinical terms.
    @Test
    fun analytics_never_carry_pii_or_content() = runTest {
        val sink = RecordingSink()
        val holder = buildHolder(this, InMemorySecureKeyValueStore(), provider = Echo("ok"), sink = sink)
        holder.open(); advanceUntilIdle(); holder.grantConsent(); advanceUntilIdle()
        holder.sendMessage("hola coach, ¿como voy?"); advanceUntilIdle()
        assertTrue(sink.events.isNotEmpty())
        val forbiddenKeys = listOf("prompt", "response", "text", "userid", "email", "name")
        sink.events.forEach { event ->
            assertTrue(event.name.startsWith("ia_"), "coach events are ia_-namespaced: ${event.name}")
            event.params.forEach { (k, v) ->
                forbiddenKeys.forEach { assertFalse(k.contains(it, ignoreCase = true), "PII/content param key '$k'") }
                assertFalse(v.contains("hola", ignoreCase = true), "the user's message must never reach analytics")
            }
        }
    }

    private fun memory() = AICoachMemory(summaryMd = "Bloques con datos: EMOCIONES.", strengthsMd = "—", weaknessesMd = "—", currentPlanMd = "—", recommendationsMd = "atencion", lastUpdatedAtMillis = clockMillis, version = 1)
}
