package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionPendingTerminalDto
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
import kotlin.coroutines.cancellation.CancellationException

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
        summary.sessionId?.let { requireSessionId(it) } // MC8j: reject an id that would break the keyspace
        // MC8h P2: `sessionId` is the primary identity of a session, so two sessions that share a `startedAtMillis`
        // never overwrite each other; legacy rows without a sessionId keep their startedAt key (still read below).
        val key = sessionStorageKey(userId, summary)
        store.put(key, EmotionJson.encodeToString(summary))
        // MC8h P1 / MC8i P0: the canonical write above is CRITICAL; everything below is best-effort maintenance and
        // must NEVER turn a successful summary save into a false failure. Migrate away a legacy startedAt-keyed row
        // for THIS same session (so it can't be double-counted), then prune. MC8j: a CancellationException is NOT
        // swallowed here — cooperative cancellation must still propagate even from best-effort maintenance.
        runCatching {
            migrateLegacyKey(userId, summary, key)
            pruneToLast(userId, MAX_SESSIONS)
        }.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    /**
     * Dedupe by canonical session identity (opaque sessionId, else legacy startedAt): a session that exists under
     * BOTH a legacy startedAt key and a new sessionId key (MC8g→MC8h transition) is returned exactly ONCE, so it
     * cannot be double-counted in scoring/calibration. On a tie, the deterministic winner is the sessionId-keyed
     * row, then a row with a non-null `endReason`, then the larger `durationMs`.
     */
    override suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto> {
        requireUserId(userId)
        val keyed = store.keys(sessionPrefix(userId)).mapNotNull { key -> readSummary(key)?.let { key to it } }
        return keyed.groupBy { (_, summary) -> EmotionTerminalSemantics.identityOf(summary) }
            .map { (_, rows) -> rows.maxWith(rowPreference(userId)).second }
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

    override suspend fun deleteAggregate(userId: String) {
        requireUserId(userId)
        store.remove(aggregateKey(userId))
    }

    override suspend fun savePendingTerminal(userId: String, pending: EmotionPendingTerminalDto) {
        requireUserId(userId)
        val sessionId = requireNotNull(pending.summary.sessionId) { "pending terminal requires a sessionId." }
        requireSessionId(sessionId)
        store.put(pendingKey(userId, sessionId), EmotionJson.encodeToString(pending))
    }

    override suspend fun listPendingTerminals(userId: String): List<EmotionPendingTerminalDto> {
        requireUserId(userId)
        return store.keys(pendingPrefix(userId)).mapNotNull { key -> readPending(key) }
    }

    override suspend fun deletePendingTerminal(userId: String, sessionId: String) {
        requireUserId(userId)
        store.remove(pendingKey(userId, sessionId))
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

    private suspend fun readPending(key: String): EmotionPendingTerminalDto? = store.readVersioned(
        key = key,
        currentVersion = EmotionSchema.PENDING_TERMINAL,
        parser = { EmotionJson.decodeFromString<EmotionPendingTerminalDto>(it) },
        versionOf = { it.schemaVersion },
    )

    private suspend fun pruneToLast(userId: String, max: Int) {
        val keys = store.keys(sessionPrefix(userId))
        if (keys.size <= max) return
        // Keys may now be sessionId-based (not parseable as a timestamp), so sort by the DECODED startedAtMillis to
        // keep the newest [max]. Only paid when actually over the cap; a missing/corrupt row sorts oldest.
        keys.map { key -> key to (readSummary(key)?.startedAtMillis ?: Long.MIN_VALUE) }
            .sortedByDescending { it.second }
            .drop(max)
            .forEach { store.remove(it.first) }
    }

    private fun requireUserId(userId: String) {
        require(userId.isNotBlank() && !userId.contains(KEY_SEPARATOR)) {
            "userId must be non-blank and must not contain '$KEY_SEPARATOR'."
        }
    }

    /** MC8j/MC8k: the repository owns its keyspace boundary — an id containing the delimiter would corrupt/collide
     * keys. Writes REQUIRE a valid id (fail-closed); the read/dedupe path only CHECKS (never throws on legacy data). */
    private fun isValidSessionId(sessionId: String): Boolean =
        sessionId.isNotBlank() && !sessionId.contains(KEY_SEPARATOR)

    private fun requireSessionId(sessionId: String) {
        require(isValidSessionId(sessionId)) {
            "sessionId must be non-blank and must not contain '$KEY_SEPARATOR'."
        }
    }

    /** Deterministic winner among rows that share one canonical identity: sessionId-keyed, then endReason-bearing,
     * then longer duration. Higher wins under [maxWith]. */
    private fun rowPreference(userId: String): Comparator<Pair<String, EmotionSessionSummaryDto>> =
        compareBy(
            { (key, summary) -> if (isSessionIdKeyed(userId, key, summary)) 1 else 0 },
            { (_, summary) -> if (summary.endReason != null) 1 else 0 },
            { (_, summary) -> summary.durationMs },
        )

    private fun isSessionIdKeyed(userId: String, key: String, summary: EmotionSessionSummaryDto): Boolean {
        // MC8k: guard with isValidSessionId BEFORE sessionKeyById (which throws) so a legacy/corrupt row with an
        // invalid sessionId can never crash a critical read (recentSummaries → aggregate/reconciler/tier).
        val sessionId = summary.sessionId ?: return false
        return isValidSessionId(sessionId) && key == sessionKeyById(userId, sessionId)
    }

    /** Best-effort: drop a legacy startedAt-keyed row that is genuinely THIS session (same sessionId), so a
     * pre-sessionId row cannot survive as a duplicate. A different session sharing startedAtMillis is left intact. */
    private suspend fun migrateLegacyKey(userId: String, summary: EmotionSessionSummaryDto, newKey: String) {
        val sessionId = summary.sessionId ?: return
        val legacyKey = "${sessionPrefix(userId)}${summary.startedAtMillis}"
        if (legacyKey == newKey) return
        if (readSummary(legacyKey)?.sessionId == sessionId) store.remove(legacyKey)
    }

    private fun sessionPrefix(userId: String) = "session$KEY_SEPARATOR$userId$KEY_SEPARATOR"

    /** Primary key by opaque sessionId when present; legacy fallback by startedAtMillis. Both share [sessionPrefix]
     * so [recentSummaries] (prefix scan) reads new and legacy rows alike. */
    private fun sessionStorageKey(userId: String, summary: EmotionSessionSummaryDto): String =
        "${sessionPrefix(userId)}${summary.sessionId ?: summary.startedAtMillis}"
    private fun sessionKeyById(userId: String, sessionId: String): String {
        requireSessionId(sessionId)
        return "${sessionPrefix(userId)}$sessionId"
    }
    private fun aggregateKey(userId: String) = "aggregate$KEY_SEPARATOR$userId"
    private fun pendingPrefix(userId: String) = "pending$KEY_SEPARATOR$userId$KEY_SEPARATOR"
    private fun pendingKey(userId: String, sessionId: String): String {
        requireSessionId(sessionId)
        return "${pendingPrefix(userId)}$sessionId"
    }

    companion object {
        const val MAX_SESSIONS = 100
        private const val KEY_SEPARATOR = ":"
    }
}
