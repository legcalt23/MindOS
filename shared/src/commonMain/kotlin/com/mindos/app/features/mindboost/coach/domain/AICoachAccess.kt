@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.domain

/**
 * MC8u-F — AI Coach access control: Elite tier + IA consent + daily quota. The coach is an Elite feature; TRAINING is
 * never gated by any of this (this layer only ever affects the coach). Billing is not built yet, so [EliteAccessPolicy]
 * defaults to FREE with a release guard: a release build must wire real billing. Consent gates whether progress data
 * is even sent. The quota caps daily coach use but never blocks training.
 */
enum class EliteTier { FREE, ELITE }

enum class AICoachConsentStatus { UNKNOWN, GRANTED, REVOKED }

enum class AICoachAccessDecision { ALLOWED, BLOCKED_FREE, BLOCKED_NO_CONSENT, BLOCKED_QUOTA }

/** Resolves the user's subscription tier. Real implementation is billing-backed (foreign/not-built). */
interface EliteAccessPolicy {
    fun tier(userId: String): EliteTier
}

/** Charter default until billing is wired: everyone is FREE (the coach is blocked, training is not). */
object FreeEliteAccessPolicy : EliteAccessPolicy {
    override fun tier(userId: String): EliteTier = EliteTier.FREE
}

/** MC8u-F release guard: a release that ships the Elite coach must wire real billing, not the FREE stub. */
object EliteAccessPolicyGuard {
    fun requireRealBillingForEliteRelease(policy: EliteAccessPolicy, isReleaseBuild: Boolean) {
        require(!(isReleaseBuild && policy === FreeEliteAccessPolicy)) {
            "AI Coach: shipping the Elite coach in a release build requires real billing, not FreeEliteAccessPolicy."
        }
    }
}

/** Resolves IA consent. No progress data is sent to the coach without GRANTED consent. */
interface AICoachConsentPolicy {
    fun status(userId: String): AICoachConsentStatus
}

data class AICoachUsageLimits(val dailyQuota: Int = DEFAULT_DAILY_QUOTA) {
    companion object {
        const val DEFAULT_DAILY_QUOTA = 20
    }
}

/** Per-user daily quota. Never throws; a depleted quota blocks the coach only (training is unaffected). */
interface AICoachQuota {
    fun remaining(userId: String): Int
    fun consume(userId: String)
}

/** In-memory quota for tests/dev (a durable per-day store is a small follow-up). */
class InMemoryAICoachQuota(private val limits: AICoachUsageLimits = AICoachUsageLimits()) : AICoachQuota {
    private val used = mutableMapOf<String, Int>()
    override fun remaining(userId: String): Int = (limits.dailyQuota - (used[userId] ?: 0)).coerceAtLeast(0)
    override fun consume(userId: String) { used[userId] = (used[userId] ?: 0) + 1 }
}

/** Pure access decision: Elite AND consented AND quota-remaining ⇒ ALLOWED. Order fixes the block reason. */
object AICoachAccessGate {
    fun evaluate(tier: EliteTier, consent: AICoachConsentStatus, quotaRemaining: Int): AICoachAccessDecision = when {
        tier != EliteTier.ELITE -> AICoachAccessDecision.BLOCKED_FREE
        consent != AICoachConsentStatus.GRANTED -> AICoachAccessDecision.BLOCKED_NO_CONSENT
        quotaRemaining <= 0 -> AICoachAccessDecision.BLOCKED_QUOTA
        else -> AICoachAccessDecision.ALLOWED
    }
}
