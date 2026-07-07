@file:Suppress("MaxLineLength") // User-facing strings + wiring read best whole.

package com.mindos.app.features.mindboost.coach.application

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.coach.analytics.AICoachAnalytics
import com.mindos.app.features.mindboost.coach.analytics.AICoachEvents
import com.mindos.app.features.mindboost.coach.data.AICoachMemory
import com.mindos.app.features.mindboost.coach.data.AICoachMemoryFactory
import com.mindos.app.features.mindboost.coach.data.AICoachMemoryStore
import com.mindos.app.features.mindboost.coach.domain.AICoachAccessDecision
import com.mindos.app.features.mindboost.coach.domain.AICoachAccessGate
import com.mindos.app.features.mindboost.coach.domain.AICoachConsentPolicy
import com.mindos.app.features.mindboost.coach.domain.AICoachConsentStatus
import com.mindos.app.features.mindboost.coach.domain.AICoachQuota
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachContributionSource
import com.mindos.app.features.mindboost.coach.domain.CoachPlanSuggestion
import com.mindos.app.features.mindboost.coach.domain.CoachSummary
import com.mindos.app.features.mindboost.coach.domain.EliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.FreeEliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.InMemoryAICoachQuota
import com.mindos.app.features.mindboost.coach.domain.PlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplyOutcome
import com.mindos.app.features.mindboost.coach.domain.PendingPlanApplier
import com.mindos.app.features.mindboost.coach.prompt.AICoachProvider
import com.mindos.app.features.mindboost.coach.prompt.AICoachPromptBuilder
import com.mindos.app.features.mindboost.coach.prompt.PendingAICoachProvider
import com.mindos.app.features.mindboost.coach.safety.AICoachSafetyPolicy
import com.mindos.app.features.mindboost.coach.safety.CoachRequestCategory
import com.mindos.app.features.mindboost.coach.safety.OutputVerdict
import kotlin.coroutines.cancellation.CancellationException

enum class CoachResponseKind { CRISIS, SAFE_REFUSAL, REDIRECT, BLOCKED_FREE, BLOCKED_NO_CONSENT, BLOCKED_QUOTA, NOT_ENOUGH_DATA, COACH_UNAVAILABLE, ANSWER }

data class CoachResponse(val kind: CoachResponseKind, val text: String)

/** In-memory consent (tests/dev). A durable, encrypted consent store is a small follow-up. */
class InMemoryAICoachConsentStore(private var status: AICoachConsentStatus = AICoachConsentStatus.UNKNOWN) : AICoachConsentPolicy {
    override fun status(userId: String): AICoachConsentStatus = status
    fun set(newStatus: AICoachConsentStatus) { status = newStatus }
}

/**
 * MC8u-F — the AI Coach vertical slice. Order is: SAFETY FIRST (a crisis/diagnosis/medication ask is answered by a
 * fixed safe response WITHOUT a model and WITHOUT paywalling), then Elite+consent+quota gating (coach-only; TRAINING is
 * never blocked here), then a STRUCTURED prompt over the PII-free summary → the (pending) provider → the OUTPUT guard.
 * It never invents data (empty summary ⇒ NOT_ENOUGH_DATA) and never mutates the plan without confirmation.
 */
@Suppress("LongParameterList") // DI root: 8 injectable collaborators, all with safe defaults (mirrors the data-class graphs).
class AICoachOrchestrator(
    private val sources: List<CoachContributionSource>,
    private val consent: AICoachConsentPolicy,
    private val clock: EpochClock,
    private val elite: EliteAccessPolicy = FreeEliteAccessPolicy,
    private val quota: AICoachQuota = InMemoryAICoachQuota(),
    private val provider: AICoachProvider = PendingAICoachProvider,
    private val memoryStore: AICoachMemoryStore? = null,
    private val analytics: AICoachAnalytics = AICoachAnalytics(),
) {
    /** Emits `ia_coach_opened`, requests consent if unknown, and returns the current access decision for the UI. */
    fun open(userId: String): AICoachAccessDecision {
        analytics.log(AICoachEvents.OPENED, mapOf("tier" to elite.tier(userId).name.lowercase()))
        if (consent.status(userId) == AICoachConsentStatus.UNKNOWN) analytics.log(AICoachEvents.CONSENT_REQUESTED)
        return AICoachAccessGate.evaluate(elite.tier(userId), consent.status(userId), quota.remaining(userId))
    }

    suspend fun ask(userId: String, request: String): CoachResponse {
        analytics.log(AICoachEvents.MESSAGE_SENT, mapOf("tier" to elite.tier(userId).name.lowercase()))
        val category = AICoachSafetyPolicy.classifyRequest(request)
        return if (category != CoachRequestCategory.NORMAL_TRAINING) safeNonNormal(category) else answerNormal(userId, request)
    }

    private fun safeNonNormal(category: CoachRequestCategory): CoachResponse {
        val kind = when (category) {
            CoachRequestCategory.CRISIS_SELF_HARM -> CoachResponseKind.CRISIS
            CoachRequestCategory.EMOTIONAL_STATE, CoachRequestCategory.OUT_OF_SCOPE -> CoachResponseKind.REDIRECT
            else -> CoachResponseKind.SAFE_REFUSAL
        }
        if (kind != CoachResponseKind.REDIRECT) analytics.log(AICoachEvents.SAFETY_REFUSAL_SHOWN, mapOf("safety_category" to category.name.lowercase()))
        return CoachResponse(kind, AICoachSafetyPolicy.responseFor(category))
    }

    private suspend fun answerNormal(userId: String, request: String): CoachResponse {
        val decision = AICoachAccessGate.evaluate(elite.tier(userId), consent.status(userId), quota.remaining(userId))
        return if (decision != AICoachAccessDecision.ALLOWED) blocked(decision) else generate(userId, request)
    }

    private fun blocked(decision: AICoachAccessDecision): CoachResponse {
        if (decision == AICoachAccessDecision.BLOCKED_QUOTA) analytics.log(AICoachEvents.LIMIT_REACHED, mapOf("quota_state" to "reached"))
        return when (decision) {
            AICoachAccessDecision.BLOCKED_FREE -> CoachResponse(CoachResponseKind.BLOCKED_FREE, FREE_MESSAGE)
            AICoachAccessDecision.BLOCKED_NO_CONSENT -> CoachResponse(CoachResponseKind.BLOCKED_NO_CONSENT, NO_CONSENT_MESSAGE)
            AICoachAccessDecision.BLOCKED_QUOTA -> CoachResponse(CoachResponseKind.BLOCKED_QUOTA, QUOTA_MESSAGE)
            AICoachAccessDecision.ALLOWED -> CoachResponse(CoachResponseKind.ANSWER, "")
        }
    }

    private suspend fun generate(userId: String, request: String): CoachResponse {
        val summary = buildSummary(userId)
        return if (summary.isEmpty) {
            CoachResponse(CoachResponseKind.NOT_ENOUGH_DATA, AICoachSafetyPolicy.NOT_ENOUGH_DATA)
        } else {
            quota.consume(userId)
            val prompt = AICoachPromptBuilder.build(summary, request, currentPlan = null, candidates = emptyList())
            val raw = tryGenerate(prompt)
            if (raw == null) {
                CoachResponse(CoachResponseKind.COACH_UNAVAILABLE, COACH_UNAVAILABLE_MESSAGE)
            } else {
                val guarded = AICoachSafetyPolicy.guardOutput(raw)
                analytics.log(AICoachEvents.RESPONSE_GENERATED, mapOf("guarded" to (guarded.verdict == OutputVerdict.BLOCK).toString()))
                CoachResponse(CoachResponseKind.ANSWER, guarded.safeText)
            }
        }
    }

    /** Calls the (pending) provider, isolating an ordinary failure to null; a real cancellation still propagates. */
    private suspend fun tryGenerate(prompt: com.mindos.app.features.mindboost.coach.prompt.CoachPrompt): String? {
        val result = runCatching { provider.generate(prompt) }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result.getOrNull()
    }

    /** Aggregates each block's PII-free contribution. Null-returning sources (uncalibrated) are skipped. */
    suspend fun buildSummary(userId: String): CoachSummary {
        val contributions = sources.mapNotNull { it.contribute(userId) }
        return CoachSummary(contributions, overallConfidenceOf(contributions))
    }

    /** Persists a minimized memory from the current summary (if a store is wired). Never raw/clinical/PII. */
    suspend fun saveMemory(userId: String, currentPlanMd: String, version: Int): AICoachMemory? {
        val store = memoryStore ?: return null
        val memory = AICoachMemoryFactory.fromSummary(buildSummary(userId), currentPlanMd, clock.nowMillis(), version)
        store.save(userId, memory)
        return memory
    }

    /** A recommendation is applied ONLY when [confirmed]; `ia_recommendation_applied` fires only on a real apply. */
    suspend fun applyRecommendation(userId: String, suggestion: CoachPlanSuggestion, confirmed: Boolean, applier: PlanApplier = PendingPlanApplier): PlanApplyOutcome {
        analytics.log(AICoachEvents.RECOMMENDATION_SHOWN, mapOf("recommendation_type" to suggestion.recommendation.type.name.lowercase()))
        val outcome = applier.apply(userId, suggestion, confirmed)
        if (outcome == PlanApplyOutcome.APPLIED) analytics.log(AICoachEvents.RECOMMENDATION_APPLIED, mapOf("recommendation_type" to suggestion.recommendation.type.name.lowercase()))
        return outcome
    }

    private companion object {
        const val FREE_MESSAGE = "El coach con IA es parte de Elite. Puedes seguir entrenando con normalidad sin él."
        const val NO_CONSENT_MESSAGE = "Para usar el coach necesito tu permiso para analizar tu progreso. Puedes darlo o retirarlo cuando quieras."
        const val QUOTA_MESSAGE = "Has alcanzado el límite de mensajes del coach por hoy. Tu entrenamiento sigue disponible; vuelve mañana."
        const val COACH_UNAVAILABLE_MESSAGE = "El coach todavía no está disponible en esta versión. Tu entrenamiento funciona con normalidad."
    }
}

private fun overallConfidenceOf(contributions: List<CoachContribution>): CoachConfidenceLevel = when {
    contributions.isEmpty() -> CoachConfidenceLevel.LOW
    contributions.any { it.confidence == CoachConfidenceLevel.LOW } -> CoachConfidenceLevel.LOW
    contributions.any { it.confidence == CoachConfidenceLevel.MEDIUM } -> CoachConfidenceLevel.MEDIUM
    else -> CoachConfidenceLevel.HIGH
}
