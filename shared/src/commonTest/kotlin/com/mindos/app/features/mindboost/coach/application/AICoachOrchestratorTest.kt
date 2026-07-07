@file:Suppress("MaxLineLength", "MagicNumber", "LongParameterList") // Fixtures + dense DI wiring read best whole.

package com.mindos.app.features.mindboost.coach.application

import com.mindos.app.core.analytics.MindBoostAnalyticsEvent
import com.mindos.app.core.analytics.MindBoostAnalyticsSink
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.coach.analytics.AICoachAnalytics
import com.mindos.app.features.mindboost.coach.analytics.AICoachEvents
import com.mindos.app.features.mindboost.coach.domain.AICoachConsentPolicy
import com.mindos.app.features.mindboost.coach.domain.AICoachConsentStatus
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
import com.mindos.app.features.mindboost.coach.domain.InMemoryAICoachQuota
import com.mindos.app.features.mindboost.coach.domain.PlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplyOutcome
import com.mindos.app.features.mindboost.coach.prompt.AICoachProvider
import com.mindos.app.features.mindboost.coach.prompt.CoachPrompt
import com.mindos.app.features.mindboost.coach.prompt.PendingAICoachProvider
import com.mindos.app.features.mindboost.coach.safety.AICoachSafetyPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AICoachOrchestratorTest {

    private class FakeSource(private val contribution: CoachContribution?) : CoachContributionSource {
        override val blockId: String = "TEST"
        override suspend fun contribute(userId: String): CoachContribution? = contribution
    }
    private class FakeElite(private val t: EliteTier) : EliteAccessPolicy { override fun tier(userId: String): EliteTier = t }
    private class FakeConsent(private val s: AICoachConsentStatus) : AICoachConsentPolicy { override fun status(userId: String): AICoachConsentStatus = s }
    private class EchoProvider(private val text: String) : AICoachProvider { override suspend fun generate(prompt: CoachPrompt): String = text }
    private class RecordingSink : MindBoostAnalyticsSink { val events = mutableListOf<MindBoostAnalyticsEvent>(); override fun log(event: MindBoostAnalyticsEvent) { events += event } }
    private class FakeApplier(private val outcome: PlanApplyOutcome) : PlanApplier {
        var lastConfirmed: Boolean? = null
        override suspend fun apply(userId: String, suggestion: CoachPlanSuggestion, confirmed: Boolean): PlanApplyOutcome {
            lastConfirmed = confirmed
            return if (confirmed) outcome else PlanApplyOutcome.REJECTED
        }
    }

    private val clock = EpochClock { 1_000L }
    private fun established() = CoachContribution("EMOCIONES", CoachCalibrationState.ESTABLISHED, CoachConfidenceLevel.HIGH, patterns = listOf("atencion"))

    private fun orchestrator(
        source: CoachContributionSource,
        tier: EliteTier = EliteTier.ELITE,
        consent: AICoachConsentStatus = AICoachConsentStatus.GRANTED,
        quota: InMemoryAICoachQuota = InMemoryAICoachQuota(),
        provider: AICoachProvider = PendingAICoachProvider,
        sink: RecordingSink = RecordingSink(),
    ) = AICoachOrchestrator(listOf(source), FakeConsent(consent), clock, FakeElite(tier), quota, provider, null, AICoachAnalytics(sink))

    @Test
    fun a_crisis_message_is_answered_safely_even_for_a_free_user_no_paywall() = runTest {
        val r = orchestrator(FakeSource(established()), tier = EliteTier.FREE).ask("u", "a veces quiero hacerme daño")
        assertEquals(CoachResponseKind.CRISIS, r.kind, "crisis is never paywalled or sent to a model")
    }

    @Test
    fun a_diagnosis_request_is_refused_without_a_model() = runTest {
        val provider = object : AICoachProvider { override suspend fun generate(prompt: CoachPrompt): String = error("model must not be called for a diagnosis ask") }
        val r = orchestrator(FakeSource(established()), provider = provider).ask("u", "¿Tengo depresión? Diagnostícame.")
        assertEquals(CoachResponseKind.SAFE_REFUSAL, r.kind)
    }

    @Test
    fun a_free_user_is_blocked_from_the_coach() = runTest {
        assertEquals(CoachResponseKind.BLOCKED_FREE, orchestrator(FakeSource(established()), tier = EliteTier.FREE).ask("u", "¿qué practico hoy?").kind)
    }

    @Test
    fun an_elite_user_without_consent_is_blocked() = runTest {
        assertEquals(CoachResponseKind.BLOCKED_NO_CONSENT, orchestrator(FakeSource(established()), consent = AICoachConsentStatus.UNKNOWN).ask("u", "¿qué practico hoy?").kind)
    }

    @Test
    fun the_daily_quota_blocks_the_coach_but_emits_limit_reached() = runTest {
        val sink = RecordingSink()
        val orch = orchestrator(FakeSource(established()), quota = InMemoryAICoachQuota(AICoachUsageLimits(dailyQuota = 0)), sink = sink)
        assertEquals(CoachResponseKind.BLOCKED_QUOTA, orch.ask("u", "¿qué practico hoy?").kind)
        assertTrue(sink.events.any { it.name == AICoachEvents.LIMIT_REACHED })
    }

    @Test
    fun an_empty_summary_never_invents_data() = runTest {
        assertEquals(CoachResponseKind.NOT_ENOUGH_DATA, orchestrator(FakeSource(null)).ask("u", "analiza mi progreso").kind)
    }

    @Test
    fun a_normal_request_with_no_real_provider_is_unavailable_not_faked() = runTest {
        assertEquals(CoachResponseKind.COACH_UNAVAILABLE, orchestrator(FakeSource(established())).ask("u", "¿qué practico hoy?").kind)
    }

    @Test
    fun an_unsafe_model_output_is_guarded_before_reaching_the_user() = runTest {
        val r = orchestrator(FakeSource(established()), provider = EchoProvider("Tienes TDAH y te garantizo que curará tu ansiedad")).ask("u", "¿cómo voy?")
        assertEquals(CoachResponseKind.ANSWER, r.kind)
        assertEquals(AICoachSafetyPolicy.SAFE_FALLBACK, r.text, "an unsafe model output is replaced by the safe fallback")
    }

    @Test
    fun the_quota_only_decrements_on_a_served_answer() = runTest {
        val quota = InMemoryAICoachQuota(AICoachUsageLimits(dailyQuota = 2))
        val orch = orchestrator(FakeSource(established()), quota = quota, provider = EchoProvider("Tu rendimiento es consistente."))
        orch.ask("u", "¿cómo voy?")
        assertEquals(1, quota.remaining("u"), "a served answer consumes one quota unit")
    }

    @Test
    fun an_unavailable_provider_never_consumes_quota() = runTest {
        val quota = InMemoryAICoachQuota(AICoachUsageLimits(dailyQuota = 2))
        val r = orchestrator(FakeSource(established()), quota = quota, provider = PendingAICoachProvider).ask("u", "¿cómo voy?")
        assertEquals(CoachResponseKind.COACH_UNAVAILABLE, r.kind)
        assertEquals(2, quota.remaining("u"), "an unavailable provider (or an error) must not spend a message")
    }

    @Test
    fun a_crisis_neither_consumes_quota_nor_logs_a_bare_refusal() = runTest {
        val sink = RecordingSink()
        val quota = InMemoryAICoachQuota(AICoachUsageLimits(dailyQuota = 2))
        orchestrator(FakeSource(established()), quota = quota, sink = sink).ask("u", "a veces quiero hacerme daño")
        assertEquals(2, quota.remaining("u"), "a crisis is answered safely and never spends a message")
        assertTrue(sink.events.any { it.name == AICoachEvents.CRISIS_RESPONSE_SHOWN }, "a crisis logs a distinct crisis event")
    }

    @Test
    fun a_recommendation_is_applied_only_after_explicit_confirmation() = runTest {
        val sink = RecordingSink()
        val orch = orchestrator(FakeSource(established()), sink = sink)
        val suggestion = CoachPlanSuggestion(CoachRecommendation(CoachRecommendationType.PRACTICE_MORE, "más práctica", "EMOCIONES"), "porque estás calibrando")
        val applier = FakeApplier(PlanApplyOutcome.APPLIED)

        assertEquals(PlanApplyOutcome.REJECTED, orch.applyRecommendation("u", suggestion, confirmed = false, applier = applier))
        assertEquals(false, applier.lastConfirmed)
        assertTrue(sink.events.none { it.name == AICoachEvents.RECOMMENDATION_APPLIED }, "no applied event without confirmation")

        assertEquals(PlanApplyOutcome.APPLIED, orch.applyRecommendation("u", suggestion, confirmed = true, applier = applier))
        assertTrue(sink.events.any { it.name == AICoachEvents.RECOMMENDATION_APPLIED }, "applied event only after confirmation")
    }
}
