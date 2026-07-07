@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.coach.domain.AICoachConsentStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/** MC8u-G §4 — IA consent is durable (survives a restart), revocation persists, and a policy-version bump forces re-consent. */
class AICoachConsentStoreTest {

    @Test
    fun a_grant_persists_across_a_restart() = runTest {
        val disk = InMemorySecureKeyValueStore()
        SecureAICoachConsentStore(disk).grant("u", nowMillis = 1_000L)
        // "restart": a fresh store instance over the same encrypted disk.
        assertEquals(AICoachConsentStatus.GRANTED, SecureAICoachConsentStore(disk).load("u").toStatus())
    }

    @Test
    fun a_revocation_persists_across_a_restart() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val store = SecureAICoachConsentStore(disk)
        store.grant("u", 1_000L)
        store.revoke("u", 2_000L)
        assertEquals(AICoachConsentStatus.REVOKED, SecureAICoachConsentStore(disk).load("u").toStatus())
    }

    @Test
    fun a_policy_version_bump_requires_reconsent() = runTest {
        val disk = InMemorySecureKeyValueStore()
        SecureAICoachConsentStore(disk).grant("u", 1_000L, policyVersion = 1)
        // The disclosure changed (current policy is now 2): the old grant no longer maps to GRANTED.
        assertEquals(AICoachConsentStatus.UNKNOWN, SecureAICoachConsentStore(disk).load("u").toStatus(currentPolicyVersion = 2))
        // Same policy version still counts.
        assertEquals(AICoachConsentStatus.GRANTED, SecureAICoachConsentStore(disk).load("u").toStatus(currentPolicyVersion = 1))
    }

    @Test
    fun absent_consent_is_unknown() = runTest {
        assertEquals(AICoachConsentStatus.UNKNOWN, SecureAICoachConsentStore(InMemorySecureKeyValueStore()).load("u").toStatus())
    }

    @Test
    fun revoke_keeps_the_prior_grant_timestamp_but_reads_revoked() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val store = SecureAICoachConsentStore(disk)
        store.grant("u", 1_000L)
        store.revoke("u", 2_000L)
        val record = assertNotNull(store.load("u"))
        assertEquals(1_000L, record.grantedAtMillis)
        assertEquals(2_000L, record.revokedAtMillis)
        assertEquals(AICoachConsentStatus.REVOKED, record.toStatus())
    }

    @Test
    fun an_invalid_user_id_is_rejected() = runTest {
        assertFailsWith<IllegalArgumentException> { SecureAICoachConsentStore(InMemorySecureKeyValueStore()).grant("bad:id", 1L) }
    }
}
