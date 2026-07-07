@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Millis per day; the quota resets when the epoch-day derived from "now" changes. */
private const val MILLIS_PER_DAY = 86_400_000L

/** Pure day bucket for the daily quota. UTC-day granularity (a local-timezone refinement is a documented follow-up). */
fun coachQuotaDayOf(nowMillis: Long): Long = nowMillis / MILLIS_PER_DAY

/**
 * MC8u-G — the durable daily coach-message quota record. Persisted in the local encrypted store so the counter SURVIVES
 * a restart and RESETS on a new day. Holds only a day bucket + a small integer count — never content or PII.
 */
@kotlinx.serialization.Serializable
data class AICoachQuotaRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val dayEpoch: Long,
    val used: Int,
    val version: Int = 1,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Durable per-user daily quota. [usedToday] returns 0 when the stored bucket is a previous day (auto-reset); [record]
 * persists the current used count for today. Delete supports account deletion (erased with the `mindboost_coach` namespace).
 */
interface AICoachQuotaStore {
    suspend fun usedToday(userId: String, todayEpoch: Long): Int
    suspend fun record(userId: String, todayEpoch: Long, used: Int)
    suspend fun delete(userId: String)
}

private val QuotaJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val QUOTA_USER_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

/** Local encrypted quota store. `userId` is validated so it can't corrupt the key; reads quarantine a future schema. */
class SecureAICoachQuotaStore(private val store: SecureKeyValueStore) : AICoachQuotaStore {

    override suspend fun usedToday(userId: String, todayEpoch: Long): Int {
        requireUserId(userId)
        val record = store.readVersioned(
            key = key(userId),
            currentVersion = AICoachQuotaRecord.SCHEMA_VERSION,
            parser = { QuotaJson.decodeFromString<AICoachQuotaRecord>(it) },
            versionOf = { it.schemaVersion },
        ) ?: return 0
        return if (record.dayEpoch == todayEpoch) record.used else 0
    }

    override suspend fun record(userId: String, todayEpoch: Long, used: Int) {
        requireUserId(userId)
        store.put(key(userId), QuotaJson.encodeToString(AICoachQuotaRecord(dayEpoch = todayEpoch, used = used.coerceAtLeast(0))))
    }

    override suspend fun delete(userId: String) {
        requireUserId(userId)
        store.remove(key(userId))
    }

    private fun key(userId: String) = "quota:$userId"
    private fun requireUserId(userId: String) = require(QUOTA_USER_ID_REGEX.matches(userId)) { "userId must match [A-Za-z0-9_-]{1,128}." }
}
