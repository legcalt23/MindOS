@file:Suppress("LongParameterList")

package com.mindverse.mindboost.core.storage

import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import com.mindverse.mindboost.core.storage.db.DecisionSessionQueries
import com.mindverse.mindboost.core.storage.db.DiaryEntryQueries

private const val MIN_LIMIT = 0
private const val SAFETY_FLAG_TRUE = 1L
private const val SAFETY_FLAG_FALSE = 0L

class SqlDelightSessionRepository(
    private val queries: DecisionSessionQueries,
) : SessionRepository {
    override fun saveSession(session: DecisionSession) {
        require(session.sessionId.isNotBlank()) { "sessionId must not be blank." }

        queries.upsertDecisionSession(
            session.sessionId,
            session.timestampMs,
            session.icd,
            session.ied,
            session.cde,
            session.fatigueScore,
            session.gamingScore,
        )
    }

    override fun getRecentSessions(limit: Int): List<DecisionSession> {
        require(limit >= MIN_LIMIT) { "limit must be >= 0." }

        return queries
            .selectRecentDecisionSessions(limit.toLong(), ::decisionSessionFromDatabase)
            .executeAsList()
    }
}

class SqlDelightDiaryRepository(
    private val queries: DiaryEntryQueries,
) : DiaryRepository {
    override fun saveDiaryEntry(entry: DiaryEntry) {
        require(entry.id.isNotBlank()) { "diary entry id must not be blank." }

        queries.upsertDiaryEntry(
            entry.id,
            entry.timestampMs,
            // v0.2 persists opaque bytes; encryption is provided by a future storage layer.
            entry.encryptedContent.copyOf(),
            entry.safetyFlag.toStorageFlag(),
        )
    }

    override fun getDiaryEntry(id: String): DiaryEntry? =
        queries
            .selectDiaryEntryById(id, ::diaryEntryFromDatabase)
            .executeAsOneOrNull()

    override fun wipeDiaryData() {
        queries.deleteDiaryEntries()
    }
}

private fun decisionSessionFromDatabase(
    sessionId: String,
    timestampMs: Long,
    icd: Double,
    ied: Double,
    cde: Double,
    fatigueScore: Double,
    gamingScore: Double,
): DecisionSession = DecisionSession(
    sessionId = sessionId,
    timestampMs = timestampMs,
    icd = icd,
    ied = ied,
    cde = cde,
    fatigueScore = fatigueScore,
    gamingScore = gamingScore,
)

private fun diaryEntryFromDatabase(
    id: String,
    timestampMs: Long,
    content: ByteArray,
    safetyFlag: Long,
): DiaryEntry = DiaryEntry(
    id = id,
    timestampMs = timestampMs,
    encryptedContent = content.copyOf(),
    safetyFlag = safetyFlag == SAFETY_FLAG_TRUE,
)

private fun Boolean.toStorageFlag(): Long =
    if (this) SAFETY_FLAG_TRUE else SAFETY_FLAG_FALSE
