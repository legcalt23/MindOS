@file:Suppress("MaxLineLength") // Contract KDoc + mapping expressions read best whole.

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.coach.domain.AICoachConsentStatus
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MC8u-G — the durable IA consent record. Persisted in the local encrypted [SecureKeyValueStore] so consent (and its
 * withdrawal) SURVIVE a restart. No progress data is ever sent to the coach without a GRANTED status derived from this
 * record. Revoking sets [revokedAtMillis] but NEVER deletes the coach memory — "borrar memoria" is a separate action.
 * A bump of [policyVersion] forces re-consent (an old grant no longer maps to GRANTED). `userId` is the account id, not
 * PII; the record is account-bound and fully erased on account deletion (namespace `mindboost_coach`).
 */
@kotlinx.serialization.Serializable
data class AICoachConsentRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val userId: String,
    val granted: Boolean,
    val grantedAtMillis: Long? = null,
    val revokedAtMillis: Long? = null,
    val version: Int = 1,
    val policyVersion: Int = AICoachConsentPolicy.CURRENT_POLICY_VERSION,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** The consent-policy version. Bumping it means the disclosure changed and prior grants require a fresh consent. */
object AICoachConsentPolicy {
    const val CURRENT_POLICY_VERSION = 1
}

/**
 * Maps a persisted record to the runtime [AICoachConsentStatus] the access gate uses. FAIL-CLOSED: only a live grant
 * (granted, not revoked, same policy version) is GRANTED; an out-of-date grant degrades to UNKNOWN so the UI asks again.
 */
fun AICoachConsentRecord?.toStatus(currentPolicyVersion: Int = AICoachConsentPolicy.CURRENT_POLICY_VERSION): AICoachConsentStatus = when {
    this == null -> AICoachConsentStatus.UNKNOWN
    revokedAtMillis != null && (grantedAtMillis == null || revokedAtMillis >= grantedAtMillis) -> AICoachConsentStatus.REVOKED
    granted && policyVersion == currentPolicyVersion -> AICoachConsentStatus.GRANTED
    else -> AICoachConsentStatus.UNKNOWN
}

/** Durable consent storage. Delete supports account deletion (the record is erased with the whole `mindboost_coach` namespace). */
interface AICoachConsentStore {
    suspend fun load(userId: String): AICoachConsentRecord?
    suspend fun grant(userId: String, nowMillis: Long, policyVersion: Int = AICoachConsentPolicy.CURRENT_POLICY_VERSION)
    suspend fun revoke(userId: String, nowMillis: Long)
    suspend fun delete(userId: String)
}

private val ConsentJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val CONSENT_USER_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

/** Local encrypted consent store. `userId` is validated so it can't corrupt the key; reads quarantine a future schema. */
class SecureAICoachConsentStore(private val store: SecureKeyValueStore) : AICoachConsentStore {

    override suspend fun load(userId: String): AICoachConsentRecord? {
        requireUserId(userId)
        return store.readVersioned(
            key = key(userId),
            currentVersion = AICoachConsentRecord.SCHEMA_VERSION,
            parser = { ConsentJson.decodeFromString<AICoachConsentRecord>(it) },
            versionOf = { it.schemaVersion },
        )
    }

    override suspend fun grant(userId: String, nowMillis: Long, policyVersion: Int) {
        requireUserId(userId)
        val prior = load(userId)
        val record = AICoachConsentRecord(
            userId = userId,
            granted = true,
            grantedAtMillis = nowMillis,
            revokedAtMillis = null,
            version = (prior?.version ?: 0) + 1,
            policyVersion = policyVersion,
        )
        store.put(key(userId), ConsentJson.encodeToString(record))
    }

    override suspend fun revoke(userId: String, nowMillis: Long) {
        requireUserId(userId)
        val prior = load(userId)
        val record = AICoachConsentRecord(
            userId = userId,
            granted = false,
            grantedAtMillis = prior?.grantedAtMillis,
            revokedAtMillis = nowMillis,
            version = (prior?.version ?: 0) + 1,
            policyVersion = prior?.policyVersion ?: AICoachConsentPolicy.CURRENT_POLICY_VERSION,
        )
        store.put(key(userId), ConsentJson.encodeToString(record))
    }

    override suspend fun delete(userId: String) {
        requireUserId(userId)
        store.remove(key(userId))
    }

    private fun key(userId: String) = "consent:$userId"
    private fun requireUserId(userId: String) = require(CONSENT_USER_ID_REGEX.matches(userId)) { "userId must match [A-Za-z0-9_-]{1,128}." }
}
