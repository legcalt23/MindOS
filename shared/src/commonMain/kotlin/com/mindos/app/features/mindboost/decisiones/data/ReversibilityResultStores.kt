package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.decisiones.domain.repository.ReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityRecordLog
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encrypted, local-first store of the per-user E2 reversibility-card log, backed by the shared
 * [SecureKeyValueStore]. Mirrors the memory module's result stores: the DTO is versioned, a corrupt
 * payload decodes to an empty log (never crashes), and only compact scored facts are persisted (no RT /
 * lock / raw behavior). Keyed under the `reversibility:` prefix.
 *
 * Namespace note (GDPR): in production the MindBoost hub constructs this on
 * `MindBoostStorageNamespaces.Decisiones`, which is registered in `AccountBound` and the AppGraph
 * account-deletion eraser — so reversibility data is erased on account deletion. Any other caller MUST
 * likewise pass a store opened on an AccountBound-registered namespace.
 */
class LocalReversibilityResultStore(
    private val store: SecureKeyValueStore,
    private val json: Json = defaultJson,
) : ReversibilityResultStore {

    override suspend fun load(userId: String): ReversibilityRecordLog =
        store.readVersioned(
            key = KEY + userId,
            currentVersion = ReversibilityRecordLogDto.SCHEMA_VERSION,
            parser = json::decodeFromString,
            versionOf = ReversibilityRecordLogDto::schemaVersion,
        )?.toDomain() ?: ReversibilityRecordLog()

    override suspend fun record(userId: String, trials: List<ReversibilityTrial>): ReversibilityRecordLog {
        val updated = load(userId).append(trials)
        store.put(KEY + userId, json.encodeToString(updated.toDto()))
        return updated
    }

    private companion object {
        const val KEY = "reversibility:"
        val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/** In-memory store for tests and non-durable platforms; per-user, durable only within a process run. */
class InMemoryReversibilityResultStore : ReversibilityResultStore {
    private val byUser = mutableMapOf<String, ReversibilityRecordLog>()

    override suspend fun load(userId: String): ReversibilityRecordLog =
        byUser[userId] ?: ReversibilityRecordLog()

    override suspend fun record(userId: String, trials: List<ReversibilityTrial>): ReversibilityRecordLog {
        val updated = load(userId).append(trials)
        byUser[userId] = updated
        return updated
    }
}
