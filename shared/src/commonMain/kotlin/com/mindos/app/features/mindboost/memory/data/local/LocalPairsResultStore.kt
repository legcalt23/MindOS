package com.mindos.app.features.mindboost.memory.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import com.mindos.app.features.mindboost.memory.data.dto.PairsRecordLogDto
import com.mindos.app.features.mindboost.memory.data.mapper.toDomain
import com.mindos.app.features.mindboost.memory.data.mapper.toDto
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encrypted, local-first store of the per-user EX4 paired-associates log, backed by the shared
 * [SecureKeyValueStore] (AES-256-GCM via Android Keystore; in-memory on desktop/iOS). Lives in the SAME
 * `mindboost_memory` namespace as the other Memoria repositories, so account deletion already erases it.
 * Corrupt payloads decode to an empty log (never crash). Only compact per-session summaries are stored —
 * never raw trials (project learning 2026-06-05).
 */
class LocalPairsResultStore(
    private val store: SecureKeyValueStore,
    private val json: Json = MemoryJson.instance,
) : PairsResultStore {

    override suspend fun load(userId: String): PairsRecordLog =
        store.readVersioned(
            key = KEY + userId,
            currentVersion = PairsRecordLogDto.SCHEMA_VERSION,
            parser = json::decodeFromString,
            versionOf = PairsRecordLogDto::schemaVersion,
        )?.toDomain() ?: PairsRecordLog()

    override suspend fun record(userId: String, trial: PairsTrial): PairsRecordLog {
        val updated = load(userId).append(trial)
        store.put(KEY + userId, json.encodeToString(updated.toDto()))
        return updated
    }

    private companion object {
        const val KEY = "pairs:"
    }
}
