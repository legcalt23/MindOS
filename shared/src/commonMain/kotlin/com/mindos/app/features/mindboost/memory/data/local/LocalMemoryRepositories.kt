package com.mindos.app.features.mindboost.memory.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecallItemsDto
import com.mindos.app.features.mindboost.memory.data.dto.MemoryProfileStateDto
import com.mindos.app.features.mindboost.memory.data.dto.MemorySessionSummaryDto
import com.mindos.app.features.mindboost.memory.data.dto.RecentItemRegistryDto
import com.mindos.app.features.mindboost.memory.data.mapper.toDomain
import com.mindos.app.features.mindboost.memory.data.mapper.toDto
import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredRecallRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemoryProfileRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.domain.repository.RecentItemRegistryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local-first, encrypted persistence for MindBoost · Memoria, backed by the shared
 * [SecureKeyValueStore] (AES-256-GCM via Android Keystore; in-memory on desktop/iOS).
 * Reusing that proven store avoids re-inventing crypto (project learning 2026-05-28).
 * Corrupt payloads decode to null (never crash). Only summaries/state are stored —
 * never raw trials (learning 2026-06-05).
 */
private object MemoryKeys {
    const val PROFILE = "profile:"
    const val SESSION = "session:"
    const val DEFERRED = "deferred:"
    const val RECENT = "recent:"
}

/** Encrypted store of the evolving [MemoryProfileState]. */
class LocalMemoryProfileRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : MemoryProfileRepository {

    override suspend fun load(userId: String): MemoryProfileState? =
        store.get(MemoryKeys.PROFILE + userId)?.let { raw ->
            runCatching { json.decodeFromString<MemoryProfileStateDto>(raw).toDomain() }.getOrNull()
        }

    override suspend fun save(state: MemoryProfileState) {
        store.put(MemoryKeys.PROFILE + state.userId, json.encodeToString(state.toDto()))
    }
}

/** Encrypted store of session SUMMARIES, one entry per session, user-scoped keys. */
class LocalMemorySessionHistoryRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : MemorySessionHistoryRepository {

    override suspend fun append(summary: MemorySessionSummary) {
        store.put(MemoryKeys.SESSION + summary.sessionId, json.encodeToString(summary.toDto()))
    }

    override suspend fun recent(userId: String, limit: Int): List<MemorySessionSummary> {
        require(limit > 0) { "limit must be > 0." }
        return store.keys(MemoryKeys.SESSION + "$userId:")
            .mapNotNull { key -> store.get(key)?.let { decodeSummaryOrNull(json, it) } }
            .sortedByDescending { it.endedAtEpoch }
            .take(limit)
    }
}

/** Encrypted store of the user's pending delayed-recall items. */
class LocalDeferredRecallRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : DeferredRecallRepository {

    override suspend fun load(userId: String): List<DeferredRecallItem> =
        store.get(MemoryKeys.DEFERRED + userId)?.let { raw ->
            runCatching { json.decodeFromString<DeferredRecallItemsDto>(raw).toDomain() }.getOrNull()
        } ?: emptyList()

    override suspend fun save(userId: String, items: List<DeferredRecallItem>) {
        store.put(MemoryKeys.DEFERRED + userId, json.encodeToString(items.toDto()))
    }
}

/** Encrypted store of the per-exercise recent-item registry (no-repetition). */
class LocalRecentItemRegistryRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : RecentItemRegistryRepository {

    override suspend fun load(userId: String, exerciseId: MemoryExerciseId): RecentItemRegistry? =
        store.get(key(userId, exerciseId))?.let { raw ->
            runCatching { json.decodeFromString<RecentItemRegistryDto>(raw).toDomain() }.getOrNull()
        }

    override suspend fun save(userId: String, registry: RecentItemRegistry) {
        store.put(key(userId, registry.exerciseId), json.encodeToString(registry.toDto()))
    }

    private fun key(userId: String, exerciseId: MemoryExerciseId): String =
        "${MemoryKeys.RECENT}$userId:${exerciseId.name}"
}

/** Decodes a persisted session summary; corrupt entries become null. */
private fun decodeSummaryOrNull(json: Json, raw: String): MemorySessionSummary? =
    runCatching { json.decodeFromString<MemorySessionSummaryDto>(raw).toDomain() }.getOrNull()
