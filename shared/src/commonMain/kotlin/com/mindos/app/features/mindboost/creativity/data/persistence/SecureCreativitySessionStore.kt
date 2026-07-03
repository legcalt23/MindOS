package com.mindos.app.features.mindboost.creativity.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Durable, encrypted-at-rest implementation of [CreativitySessionStore] over the same
 * [SecureKeyValueStore] (AES-256-GCM + Android Keystore) already used by MindBoost. No new
 * crypto is invented (project rule, CLAUDE.md 2026-06-05). Local-only: nothing leaves the device.
 *
 * Key layout within the namespace: `sess:<userId>::<sessionId>` -> [PersistedCreativitySessionDto].
 * The `::` separator disambiguates `userId` from `sessionId`. The [Mutex] makes save and the
 * multi-key per-user deletion atomic.
 */
class SecureCreativitySessionStore(
    private val store: SecureKeyValueStore,
    private val json: Json = CreativityJson.instance,
) : CreativitySessionStore {

    private val mutex = Mutex()

    override suspend fun save(session: PersistedCreativitySessionDto) = mutex.withLock {
        store.put(key(session.session.userId, session.session.sessionId), json.encodeToString(session))
    }

    override suspend fun read(sessionId: String): PersistedCreativitySessionDto? {
        val storedKey = store.keys(PREFIX).firstOrNull { it.substringAfter(SEP) == sessionId } ?: return null
        return readDto(storedKey)
    }

    override suspend fun historyForUser(userId: String): List<PersistedCreativitySessionDto> =
        store.keys(userPrefix(userId))
            .mapNotNull { readDto(it) }
            .sortedByDescending { it.session.createdAtMs }

    override suspend fun deleteUserData(userId: String): Int = mutex.withLock {
        val keys = store.keys(userPrefix(userId))
        for (storedKey in keys) {
            store.remove(storedKey)
        }
        keys.size
    }

    private suspend fun readDto(key: String): PersistedCreativitySessionDto? =
        store.readVersioned(
            key = key,
            currentVersion = PersistedCreativitySessionDto.CURRENT_SCHEMA_VERSION,
            parser = { raw -> json.decodeFromString<PersistedCreativitySessionDto>(raw) },
            versionOf = PersistedCreativitySessionDto::schemaVersion,
        )

    private fun userPrefix(userId: String): String = "$PREFIX$userId$SEP"
    private fun key(userId: String, sessionId: String): String = "$PREFIX$userId$SEP$sessionId"

    private companion object {
        const val PREFIX = "sess:"
        const val SEP = "::"
    }
}
