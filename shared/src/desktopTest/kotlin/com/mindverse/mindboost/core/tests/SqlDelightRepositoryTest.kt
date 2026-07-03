package com.mindverse.mindboost.core.tests

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import com.mindverse.mindboost.core.storage.SqlDelightDiaryRepository
import com.mindverse.mindboost.core.storage.SqlDelightSessionRepository
import com.mindverse.mindboost.core.storage.db.MindBoostDecisionDatabase
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class SqlDelightRepositoryTest {
    @Test
    fun sessionRepositorySavesAndReadsDecisionSession() = withTestStore { store ->
        val session = decisionSession(sessionId = "session-1", timestampMs = 1_000L)

        store.sessionRepository.saveSession(session)

        assertEquals(listOf(session), store.sessionRepository.getRecentSessions(limit = 10))
    }

    @Test
    fun sessionRepositoryReturnsRecentSessionsByTimestampDescending() = withTestStore { store ->
        val older = decisionSession(sessionId = "session-old", timestampMs = 1_000L)
        val newest = decisionSession(sessionId = "session-new", timestampMs = 3_000L)
        val middle = decisionSession(sessionId = "session-mid", timestampMs = 2_000L)

        store.sessionRepository.saveSession(older)
        store.sessionRepository.saveSession(newest)
        store.sessionRepository.saveSession(middle)

        assertEquals(
            listOf(newest, middle, older),
            store.sessionRepository.getRecentSessions(limit = 10),
        )
    }

    @Test
    fun sessionRepositoryRespectsRecentSessionsLimit() = withTestStore { store ->
        val older = decisionSession(sessionId = "session-old", timestampMs = 1_000L)
        val newest = decisionSession(sessionId = "session-new", timestampMs = 3_000L)
        val middle = decisionSession(sessionId = "session-mid", timestampMs = 2_000L)

        store.sessionRepository.saveSession(older)
        store.sessionRepository.saveSession(newest)
        store.sessionRepository.saveSession(middle)

        assertEquals(
            listOf(newest, middle),
            store.sessionRepository.getRecentSessions(limit = 2),
        )
    }

    @Test
    fun sessionRepositoryReturnsEmptyListForZeroLimit() = withTestStore { store ->
        store.sessionRepository.saveSession(decisionSession(sessionId = "session-1", timestampMs = 1_000L))

        assertEquals(emptyList(), store.sessionRepository.getRecentSessions(limit = 0))
    }

    @Test
    fun sessionRepositoryUpsertsExistingSessionId() = withTestStore { store ->
        val initial = decisionSession(sessionId = "session-1", timestampMs = 1_000L)
        val updated = initial.copy(timestampMs = 2_000L, icd = 91.0, fatigueScore = 0.40)

        store.sessionRepository.saveSession(initial)
        store.sessionRepository.saveSession(updated)

        assertEquals(listOf(updated), store.sessionRepository.getRecentSessions(limit = 10))
    }

    @Test
    fun diaryRepositorySavesAndReadsEntry() = withTestStore { store ->
        val entry = DiaryEntry(
            id = "diary-1",
            timestampMs = 10L,
            encryptedContent = byteArrayOf(1, 2, 3),
            safetyFlag = true,
        )

        store.diaryRepository.saveDiaryEntry(entry)
        val stored = store.diaryRepository.getDiaryEntry("diary-1")
        require(stored != null)

        assertEquals(entry.id, stored.id)
        assertEquals(entry.timestampMs, stored.timestampMs)
        assertEquals(entry.safetyFlag, stored.safetyFlag)
        assertContentEquals(entry.encryptedContent, stored.encryptedContent)
    }

    @Test
    fun diaryRepositoryDefensivelyCopiesByteArrayPayload() = withTestStore { store ->
        val content = byteArrayOf(1, 2, 3)
        val entry = DiaryEntry(
            id = "diary-copy",
            timestampMs = 20L,
            encryptedContent = content,
            safetyFlag = false,
        )

        store.diaryRepository.saveDiaryEntry(entry)
        content[0] = 9

        val stored = store.diaryRepository.getDiaryEntry("diary-copy")
        require(stored != null)
        stored.encryptedContent[1] = 8

        val reloaded = store.diaryRepository.getDiaryEntry("diary-copy")
        require(reloaded != null)

        assertContentEquals(byteArrayOf(1, 2, 3), reloaded.encryptedContent)
        assertNotSame(stored.encryptedContent, reloaded.encryptedContent)
    }

    @Test
    fun wipeDiaryDataDeletesDiaryEntries() = withTestStore { store ->
        store.diaryRepository.saveDiaryEntry(
            DiaryEntry(
                id = "diary-a",
                timestampMs = 1L,
                encryptedContent = byteArrayOf(1),
                safetyFlag = false,
            ),
        )
        store.diaryRepository.saveDiaryEntry(
            DiaryEntry(
                id = "diary-b",
                timestampMs = 2L,
                encryptedContent = byteArrayOf(2),
                safetyFlag = true,
            ),
        )

        store.diaryRepository.wipeDiaryData()

        assertNull(store.diaryRepository.getDiaryEntry("diary-a"))
        assertNull(store.diaryRepository.getDiaryEntry("diary-b"))
    }

    @Test
    fun wipeDiaryDataDoesNotDeleteDecisionSessions() = withTestStore { store ->
        val session = decisionSession(sessionId = "session-1", timestampMs = 1_000L)
        store.sessionRepository.saveSession(session)
        store.diaryRepository.saveDiaryEntry(
            DiaryEntry(
                id = "diary-a",
                timestampMs = 1L,
                encryptedContent = byteArrayOf(1),
                safetyFlag = false,
            ),
        )

        store.diaryRepository.wipeDiaryData()

        assertEquals(listOf(session), store.sessionRepository.getRecentSessions(limit = 10))
        assertNull(store.diaryRepository.getDiaryEntry("diary-a"))
    }

    private fun withTestStore(block: (SqlDelightTestStore) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MindBoostDecisionDatabase.Schema.create(driver)

        try {
            val database = MindBoostDecisionDatabase(driver)
            block(
                SqlDelightTestStore(
                    sessionRepository = SqlDelightSessionRepository(database.decisionSessionQueries),
                    diaryRepository = SqlDelightDiaryRepository(database.diaryEntryQueries),
                ),
            )
        } finally {
            driver.close()
        }
    }

    private fun decisionSession(
        sessionId: String,
        timestampMs: Long,
    ): DecisionSession = DecisionSession(
        sessionId = sessionId,
        timestampMs = timestampMs,
        icd = 80.0,
        ied = 70.0,
        cde = 60.0,
        fatigueScore = 0.20,
        gamingScore = 0.10,
    )

    private data class SqlDelightTestStore(
        val sessionRepository: SqlDelightSessionRepository,
        val diaryRepository: SqlDelightDiaryRepository,
    )
}
