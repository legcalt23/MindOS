package com.mindos.app.features.mindboost.memory.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecordLogDto
import com.mindos.app.features.mindboost.memory.data.mapper.toDomain
import com.mindos.app.features.mindboost.memory.data.mapper.toDto
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecordLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encrypted, local-first store of the per-user EX3 delayed-recall outcome log, backed by the shared
 * [SecureKeyValueStore]. Lives in the SAME `mindboost_memory` namespace as the other Memoria repositories
 * (account deletion already erases it), under the `delayed:` key prefix — DISTINCT from the deferred-item
 * registry's `deferred:` keys. Corrupt payloads decode to an empty log (never crash). Only compact scored
 * summaries are stored — never raw trials (project learning 2026-06-05).
 */
class LocalDeferredResultStore(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : DeferredResultStore {

    override suspend fun load(userId: String): DeferredRecordLog =
        store.readVersioned(
            key = KEY + userId,
            currentVersion = DeferredRecordLogDto.SCHEMA_VERSION,
            parser = json::decodeFromString,
            versionOf = DeferredRecordLogDto::schemaVersion,
        )?.toDomain() ?: DeferredRecordLog()

    override suspend fun record(userId: String, outcome: DeferredRecallOutcome): DeferredRecordLog {
        val updated = load(userId).append(outcome)
        store.put(KEY + userId, json.encodeToString(updated.toDto()))
        return updated
    }

    private companion object {
        const val KEY = "delayed:"
    }
}
