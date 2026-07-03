package com.mindverse.mindboost.core.storage

import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import kotlin.random.Random

interface SecureKeyProvider {
    fun getDatabasePassphrase(): ByteArray
}

class DebugEphemeralKeyProvider(
    random: Random = Random.Default,
    keySizeBytes: Int = DEFAULT_KEY_SIZE_BYTES,
) : SecureKeyProvider {
    private val key: ByteArray = random.nextBytes(ByteArray(validatedKeySize(keySizeBytes)))

    override fun getDatabasePassphrase(): ByteArray = key.copyOf()

    private companion object {
        const val DEFAULT_KEY_SIZE_BYTES = 32

        fun validatedKeySize(size: Int): Int {
            require(size > 0) { "keySizeBytes must be > 0." }
            return size
        }
    }
}

interface SessionRepository {
    fun saveSession(session: DecisionSession)
    fun getRecentSessions(limit: Int): List<DecisionSession>
}

interface DiaryRepository {
    fun saveDiaryEntry(entry: DiaryEntry)
    fun getDiaryEntry(id: String): DiaryEntry?
    fun wipeDiaryData()
}

class InMemorySessionRepository : SessionRepository {
    private val sessionsById = mutableMapOf<String, DecisionSession>()

    override fun saveSession(session: DecisionSession) {
        require(session.sessionId.isNotBlank()) { "sessionId must not be blank." }
        sessionsById[session.sessionId] = session
    }

    override fun getRecentSessions(limit: Int): List<DecisionSession> {
        require(limit >= 0) { "limit must be >= 0." }

        return sessionsById.values
            .sortedWith(compareByDescending<DecisionSession> { it.timestampMs }.thenBy { it.sessionId })
            .take(limit)
    }
}

class InMemoryDiaryRepository : DiaryRepository {
    private val entriesById = mutableMapOf<String, DiaryEntry>()

    override fun saveDiaryEntry(entry: DiaryEntry) {
        require(entry.id.isNotBlank()) { "diary entry id must not be blank." }
        entriesById[entry.id] = entry.copy(encryptedContent = entry.encryptedContent.copyOf())
    }

    override fun getDiaryEntry(id: String): DiaryEntry? =
        entriesById[id]?.let { entry -> entry.copy(encryptedContent = entry.encryptedContent.copyOf()) }

    override fun wipeDiaryData() {
        entriesById.clear()
    }
}
