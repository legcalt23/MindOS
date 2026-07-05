package com.mindos.app.features.mindboost.core.session

/**
 * The canonical local log of training sessions. Total and suspend-based (matches the encrypted store).
 * Writes are write-ahead upserts keyed by [SessionRecord.sessionId], so STARTED -> CHECKPOINT ->
 * FINALIZED/ABANDONED update the same row in place; a retry or a later transition never duplicates a session.
 */
interface SessionRecordStore {
    /** Insert or replace the row for [record]'s session id. */
    suspend fun upsert(record: SessionRecord)

    /** The record for [sessionId], or null if none was recorded. */
    suspend fun find(sessionId: String): SessionRecord?

    /** Every recorded session, newest first (by startedAtMs, then sessionId). */
    suspend fun all(): List<SessionRecord>

    /** Remove the row for [sessionId] (no-op if absent). */
    suspend fun delete(sessionId: String)
}

/** In-memory [SessionRecordStore] for tests and non-persistent contexts. */
class InMemorySessionRecordStore : SessionRecordStore {
    private val rows = mutableMapOf<String, SessionRecord>()

    override suspend fun upsert(record: SessionRecord) {
        require(record.sessionId.isNotBlank()) { "sessionId must not be blank." }
        rows[record.sessionId] = record
    }

    override suspend fun find(sessionId: String): SessionRecord? = rows[sessionId]

    override suspend fun all(): List<SessionRecord> =
        rows.values.sortedWith(compareByDescending<SessionRecord> { it.startedAtMs }.thenBy { it.sessionId })

    override suspend fun delete(sessionId: String) {
        rows.remove(sessionId)
    }
}
