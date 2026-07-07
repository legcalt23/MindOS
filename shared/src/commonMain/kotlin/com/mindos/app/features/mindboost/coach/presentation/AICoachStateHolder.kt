@file:Suppress("MaxLineLength", "TooManyFunctions", "LongParameterList") // UI state machine: one small function per intent + a DI root with safe defaults read best whole.

package com.mindos.app.features.mindboost.coach.presentation

import androidx.compose.runtime.mutableStateOf
import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.coach.analytics.AICoachAnalytics
import com.mindos.app.features.mindboost.coach.analytics.AICoachEvents
import com.mindos.app.features.mindboost.coach.application.AICoachOrchestrator
import com.mindos.app.features.mindboost.coach.application.CoachResponse
import com.mindos.app.features.mindboost.coach.application.CoachResponseKind
import com.mindos.app.features.mindboost.coach.application.InMemoryAICoachConsentStore
import com.mindos.app.features.mindboost.coach.data.AICoachConsentPolicy
import com.mindos.app.features.mindboost.coach.data.AICoachConsentStore
import com.mindos.app.features.mindboost.coach.data.AICoachMemory
import com.mindos.app.features.mindboost.coach.data.AICoachMemoryStore
import com.mindos.app.features.mindboost.coach.data.AICoachQuotaStore
import com.mindos.app.features.mindboost.coach.data.coachQuotaDayOf
import com.mindos.app.features.mindboost.coach.data.toStatus
import com.mindos.app.features.mindboost.coach.domain.AICoachConsentStatus
import com.mindos.app.features.mindboost.coach.domain.AICoachUsageLimits
import com.mindos.app.features.mindboost.coach.domain.CoachContributionSource
import com.mindos.app.features.mindboost.coach.domain.CoachPlanSuggestion
import com.mindos.app.features.mindboost.coach.domain.CoachRecommendation
import com.mindos.app.features.mindboost.coach.domain.EliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.EliteTier
import com.mindos.app.features.mindboost.coach.domain.FreeEliteAccessPolicy
import com.mindos.app.features.mindboost.coach.domain.InMemoryAICoachQuota
import com.mindos.app.features.mindboost.coach.domain.PendingPlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplier
import com.mindos.app.features.mindboost.coach.domain.PlanApplyOutcome
import com.mindos.app.features.mindboost.coach.prompt.AICoachProvider
import com.mindos.app.features.mindboost.coach.prompt.PendingAICoachProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * MC8u-G — the AI Coach product state holder: the single, testable orchestration of the usable coach flow
 * (consent → question → orchestrator → response → memory/analytics). It is a plain class (not a composable) so it is
 * fully unit-testable on the desktop JVM.
 *
 * Durability: IA consent and the daily quota are persisted in the local encrypted [AICoachConsentStore] /
 * [AICoachQuotaStore] and hydrated into the synchronous in-memory adapters the [AICoachOrchestrator] uses (a
 * write-through cache), so consent and the quota SURVIVE a restart and the orchestrator's committed contract is
 * unchanged. Safety-first: a crisis/diagnosis/medication question is answered by the orchestrator WITHOUT a model and
 * WITHOUT paywalling. Training is NEVER gated here. Applying a recommendation requires explicit confirmation.
 */
class AICoachStateHolder(
    private val userId: String,
    private val scope: CoroutineScope,
    sources: List<CoachContributionSource>,
    private val consentStore: AICoachConsentStore,
    private val quotaStore: AICoachQuotaStore,
    private val memoryStore: AICoachMemoryStore,
    private val clock: EpochClock,
    private val elite: EliteAccessPolicy = FreeEliteAccessPolicy,
    private val limits: AICoachUsageLimits = AICoachUsageLimits(),
    provider: AICoachProvider = PendingAICoachProvider,
    private val analytics: AICoachAnalytics = AICoachAnalytics(),
    private val planApplier: PlanApplier = PendingPlanApplier,
    private val policyVersion: Int = AICoachConsentPolicy.CURRENT_POLICY_VERSION,
) {
    private val consentAdapter = InMemoryAICoachConsentStore(AICoachConsentStatus.UNKNOWN)
    private val quota = InMemoryAICoachQuota(limits)
    private val orchestrator = AICoachOrchestrator(sources, consentAdapter, clock, elite, quota, provider, memoryStore, analytics)

    private val backing = mutableStateOf(AICoachUiState())
    val state: AICoachUiState get() = backing.value

    private fun update(block: (AICoachUiState) -> AICoachUiState) { backing.value = block(backing.value) }
    private fun now(): Long = clock.nowMillis()
    private fun today(): Long = coachQuotaDayOf(now())

    /** Loads durable consent + quota + memory and computes the entry state. Emits `ia_coach_opened`. */
    fun open() {
        analytics.log(AICoachEvents.OPENED, mapOf("tier" to elite.tier(userId).name.lowercase()))
        scope.launch {
            runIsolating {
                val status = consentStore.load(userId).toStatus(policyVersion)
                consentAdapter.set(status)
                if (status == AICoachConsentStatus.UNKNOWN) analytics.log(AICoachEvents.CONSENT_REQUESTED)
                quota.restore(userId, quotaStore.usedToday(userId, today()))
                val memory = loadMemoryUi()
                update { it.copy(access = accessState(), remainingQuota = quota.remaining(userId), memory = memory) }
            }
        }
    }

    /** 4. Shows the consent explanation. Emits `ia_consent_viewed`. */
    fun showConsentExplanation() {
        analytics.log(AICoachEvents.CONSENT_VIEWED)
        update { it.copy(access = CoachAccessState.CONSENT_EXPLANATION) }
    }

    fun dismissConsentExplanation() = update { it.copy(access = accessState()) }

    /** Grants durable IA consent. No progress data is ever sent before this. Emits `ia_consent_granted`. */
    fun grantConsent() {
        scope.launch {
            runIsolating {
                consentStore.grant(userId, now(), policyVersion)
                consentAdapter.set(AICoachConsentStatus.GRANTED)
                analytics.log(AICoachEvents.CONSENT_GRANTED)
                update { it.copy(access = accessState(), remainingQuota = quota.remaining(userId)) }
            }
        }
    }

    /** Revokes consent. Blocks new analysis; NEVER deletes memory (that is a separate action). Emits `ia_consent_revoked`. */
    fun revokeConsent() {
        scope.launch {
            runIsolating {
                consentStore.revoke(userId, now())
                consentAdapter.set(AICoachConsentStatus.REVOKED)
                analytics.log(AICoachEvents.CONSENT_REVOKED)
                update { it.copy(access = accessState()) }
            }
        }
    }

    /** Sends a question to the orchestrator (safety-first, gated, PII-free), then persists the quota counter. */
    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || state.isSending) return
        update { it.copy(messages = it.messages + CoachMessageUi(fromUser = true, kind = null, text = text), isSending = true) }
        scope.launch {
            try {
                val response: CoachResponse = orchestrator.ask(userId, text)
                quotaStore.record(userId, today(), quota.used(userId))
                update {
                    it.copy(
                        messages = it.messages + CoachMessageUi(fromUser = false, kind = response.kind, text = response.text),
                        remainingQuota = quota.remaining(userId),
                        access = accessState(),
                    )
                }
            } finally {
                update { it.copy(isSending = false) }
            }
        }
    }

    /** Loads and shows the saved memory. Emits `ia_memory_viewed`. */
    fun viewMemory() {
        analytics.log(AICoachEvents.MEMORY_VIEWED)
        scope.launch { runIsolating { val memory = loadMemoryUi(); update { it.copy(memory = memory) } } }
    }

    /** 15a. Asks for confirmation before deleting local memory. */
    fun requestDeleteMemory() = update { it.copy(confirming = CoachConfirm.DELETE_MEMORY) }

    /** 15b. Deletes local memory after confirmation. Emits `ia_memory_deleted`. */
    fun confirmDeleteMemory() {
        scope.launch {
            runIsolating {
                memoryStore.delete(userId)
                analytics.log(AICoachEvents.MEMORY_DELETED)
                update { it.copy(memory = CoachMemoryUi.Deleted, confirming = null) }
            }
        }
    }

    fun cancelConfirmation() = update { it.copy(confirming = null) }

    /** 16. Surfaces the first recommendation from the PII-free summary. Emits `ia_recommendation_shown`. */
    fun showRecommendation() {
        scope.launch {
            runIsolating {
                val recommendation = firstRecommendation() ?: return@runIsolating
                analytics.log(AICoachEvents.RECOMMENDATION_SHOWN, mapOf("recommendation_type" to recommendation.type.name.lowercase()))
                update { it.copy(recommendation = CoachRecommendationUi.Shown(CoachPlanSuggestion(recommendation, recommendation.reason))) }
            }
        }
    }

    /** 17. Applies the shown suggestion — ONLY after explicit confirmation. A pending applier never mutates the plan. */
    fun confirmApplyRecommendation() {
        val shown = state.recommendation as? CoachRecommendationUi.Shown ?: return
        scope.launch {
            runIsolating {
                val outcome = planApplier.apply(userId, shown.suggestion, confirmed = true)
                val next = when (outcome) {
                    PlanApplyOutcome.APPLIED -> { analytics.log(AICoachEvents.RECOMMENDATION_APPLIED, mapOf("recommendation_type" to shown.suggestion.recommendation.type.name.lowercase())); CoachRecommendationUi.Applied }
                    PlanApplyOutcome.PENDING_NOT_WIRED -> CoachRecommendationUi.PendingNotWired
                    PlanApplyOutcome.REJECTED -> CoachRecommendationUi.Rejected
                }
                update { it.copy(recommendation = next) }
            }
        }
    }

    /** 18. Rejects the suggestion — the applier is NEVER called. */
    fun rejectRecommendation() = update { it.copy(recommendation = CoachRecommendationUi.Rejected) }

    private fun accessState(): CoachAccessState = when {
        elite.tier(userId) != EliteTier.ELITE -> CoachAccessState.FREE_LOCKED
        consentAdapter.status(userId) != AICoachConsentStatus.GRANTED -> CoachAccessState.CONSENT_REQUIRED
        quota.remaining(userId) <= 0 -> CoachAccessState.QUOTA_REACHED
        else -> CoachAccessState.READY
    }

    private suspend fun firstRecommendation(): CoachRecommendation? =
        orchestrator.buildSummary(userId).contributions.flatMap { it.recommendations }.firstOrNull()

    private suspend fun loadMemoryUi(): CoachMemoryUi = memoryStore.load(userId)?.let { it.toUi() } ?: CoachMemoryUi.Empty

    /** Isolates an ordinary failure so a store/analytics error never crashes the coach; a real cancellation propagates. */
    private inline fun runIsolating(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Contained resilience: a durable-store/analytics failure leaves the UI in its prior state (never a crash),
            // and is surfaced PII-free (exception TYPE only, never a message) rather than swallowed silently.
            ObservabilityHub.breadcrumb("ai_coach_op_failed:${e::class.simpleName}")
        }
    }
}

private fun AICoachMemory.toUi(): CoachMemoryUi = CoachMemoryUi.Loaded(
    summary = summaryMd,
    strengths = strengthsMd,
    weaknesses = weaknessesMd,
    recommendations = recommendationsMd,
    cloudActive = false,
)
