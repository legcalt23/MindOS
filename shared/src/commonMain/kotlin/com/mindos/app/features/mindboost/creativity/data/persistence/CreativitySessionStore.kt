package com.mindos.app.features.mindboost.creativity.data.persistence

/**
 * Local-only store for creativity practice sessions. One [PersistedCreativitySessionDto]
 * blob per session, persisted offline (no network, no AI). Mirrors the Velocidad Mental
 * persistence shape but with a single, simpler aggregate per session.
 *
 * The durable implementation ([SecureCreativitySessionStore]) is backed by the encrypted
 * [com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore]; tests back it
 * with `InMemorySecureKeyValueStore`, so no device is required.
 */
interface CreativitySessionStore {

    /** Persist (insert or replace) one practice session blob. */
    suspend fun save(session: PersistedCreativitySessionDto)

    /** Read one session by id, or null if absent / unreadable. */
    suspend fun read(sessionId: String): PersistedCreativitySessionDto?

    /** All sessions for [userId], most recent first. */
    suspend fun historyForUser(userId: String): List<PersistedCreativitySessionDto>

    /** Atomically delete every session for [userId]; returns the number removed. */
    suspend fun deleteUserData(userId: String): Int
}
