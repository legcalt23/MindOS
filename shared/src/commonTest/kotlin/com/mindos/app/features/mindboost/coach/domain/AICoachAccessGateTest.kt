@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.coach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** MC8u-F — coach access: Elite + consent + quota. Training is never gated by this layer. */
class AICoachAccessGateTest {

    @Test
    fun free_user_is_blocked() {
        assertEquals(AICoachAccessDecision.BLOCKED_FREE, AICoachAccessGate.evaluate(EliteTier.FREE, AICoachConsentStatus.GRANTED, 5))
    }

    @Test
    fun elite_without_consent_is_blocked() {
        assertEquals(AICoachAccessDecision.BLOCKED_NO_CONSENT, AICoachAccessGate.evaluate(EliteTier.ELITE, AICoachConsentStatus.UNKNOWN, 5))
        assertEquals(AICoachAccessDecision.BLOCKED_NO_CONSENT, AICoachAccessGate.evaluate(EliteTier.ELITE, AICoachConsentStatus.REVOKED, 5))
    }

    @Test
    fun elite_with_consent_and_quota_is_allowed() {
        assertEquals(AICoachAccessDecision.ALLOWED, AICoachAccessGate.evaluate(EliteTier.ELITE, AICoachConsentStatus.GRANTED, 5))
    }

    @Test
    fun depleted_quota_blocks() {
        assertEquals(AICoachAccessDecision.BLOCKED_QUOTA, AICoachAccessGate.evaluate(EliteTier.ELITE, AICoachConsentStatus.GRANTED, 0))
    }

    @Test
    fun quota_decrements_and_never_goes_negative() {
        val q = InMemoryAICoachQuota(AICoachUsageLimits(dailyQuota = 2))
        assertEquals(2, q.remaining("u"))
        q.consume("u"); assertEquals(1, q.remaining("u"))
        q.consume("u"); q.consume("u"); assertEquals(0, q.remaining("u"))
    }

    @Test
    fun release_must_wire_real_billing_for_the_elite_coach() {
        assertFailsWith<IllegalArgumentException> { EliteAccessPolicyGuard.requireRealBillingForEliteRelease(FreeEliteAccessPolicy, isReleaseBuild = true) }
        EliteAccessPolicyGuard.requireRealBillingForEliteRelease(FreeEliteAccessPolicy, isReleaseBuild = false)
    }
}
