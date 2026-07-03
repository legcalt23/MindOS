package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSchema
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileReadOnly
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileSnapshot
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Encrypted, local-first emotion repository. Persists only summaries + the aggregate; prunes to the
 * last [MAX_SESSIONS] on every write; erases via [SecureKeyValueStore.clearNamespace] (which also
 * purges quarantined entries). Reads go through `readVersioned` so a future schema version is
 * quarantined (not crashed). `userId` is validated to keep it out of the key delimiter.
 */
@Suppress("TooManyFunctions") // Small repository + key helpers; each is a focused one-liner.
class LocalEmotionRepository(private val store: SecureKeyValueStore) : EmotionRepository, EmotionProfileReadOnly {

    override suspend fun saveSessionSummary(userId: String, summary: EmotionSessionSummaryDto) {
        requireUserId(userId)
        store.put(sessionKey(userId, summary.startedAtMillis), EmotionJson.encodeToString(summary))
        pruneToLast(userId, MAX_SESSIONS)
    }

    override suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto> {
        requireUserId(userId)
        return store.keys(sessionPrefix(userId))
            .mapNotNull { key -> readSummary(key) }
            .sortedByDescending { it.startedAtMillis }
    }

    override suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto) {
        requireUserId(userId)
        store.put(aggregateKey(userId), EmotionJson.encodeToString(aggregate))
    }

    override suspend fun readAggregate(userId: String): EmotionAggregateDto? {
        requireUserId(userId)
        return store.readVersioned(
            key = aggregateKey(userId),
            currentVersion = EmotionSchema.AGGREGATE,
            parser = { EmotionJson.decodeFromString<EmotionAggregateDto>(it) },
            versionOf = { it.schemaVersion },
        )
    }

    /**
     * Erases this user's emotion data. Under the single-user-per-device invariant (see
     * `SecureNamespaceUserDataEraser`) the namespace holds ONLY this user's data, so a full
     * [SecureKeyValueStore.clearNamespace] IS the complete per-user erasure; a device shared by two
     * data-bearing accounts is out of scope. [userId] is validated to keep the request well-formed.
     */
    override suspend fun deleteAll(userId: String) {
        requireUserId(userId)
        store.clearNamespace()
    }

    override suspend fun snapshot(userId: String): EmotionProfileSnapshot? {
        val aggregate = readAggregate(userId) ?: return null
        val tier = ProfileTier.entries.firstOrNull { it.name == aggregate.tier }
        return tier?.let {
            EmotionProfileSnapshot(
                tier = it,
                dataSufficiency = aggregate.dataSufficiency,
                confidence = aggregate.confidence,
                trend = trendOf(aggregate.trend),
            )
        }
    }

    private fun trendOf(name: String): ProfileTrend =
        ProfileTrend.entries.firstOrNull { it.name == name } ?: ProfileTrend.UNKNOWN

    private suspend fun readSummary(key: String): EmotionSessionSummaryDto? = store.readVersioned(
        key = key,
        currentVersion = EmotionSchema.SESSION_SUMMARY,
        parser = { EmotionJson.decodeFromString<EmotionSessionSummaryDto>(it) },
        versionOf = { it.schemaVersion },
    )

    private suspend fun pruneToLast(userId: String, max: Int) {
        val keys = store.keys(sessionPrefix(userId))
        if (keys.size <= max) return
        keys.sortedByDescending { it.substringAfterLast(KEY_SEPARATOR).toLongOrNull() ?: 0L }
            .drop(max)
            .forEach { store.remove(it) }
    }

    private fun requireUserId(userId: String) {
        require(userId.isNotBlank() && !userId.contains(KEY_SEPARATOR)) {
            "userId must be non-blank and must not contain '$KEY_SEPARATOR'."
        }
    }

    private fun sessionPrefix(userId: String) = "session$KEY_SEPARATOR$userId$KEY_SEPARATOR"
    private fun sessionKey(userId: String, startedAt: Long) = "${sessionPrefix(userId)}$startedAt"
    private fun aggregateKey(userId: String) = "aggregate$KEY_SEPARATOR$userId"

    companion object {
        const val MAX_SESSIONS = 100
        private const val KEY_SEPARATOR = ":"
    }
}
