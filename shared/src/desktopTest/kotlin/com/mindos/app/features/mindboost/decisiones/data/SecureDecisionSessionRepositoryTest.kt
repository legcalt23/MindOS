package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindverse.mindboost.core.domain.DecisionSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecureDecisionSessionRepositoryTest {
    @Test
    fun savesAndReadsRecentSessionsNewestFirst() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())
        repo.save(session("old", 1_000L))
        repo.save(session("new", 3_000L))
        repo.save(session("mid", 2_000L))

        val recent = repo.recent(limit = 10)

        assertEquals(listOf("new", "mid", "old"), recent.map { it.sessionId })
    }

    @Test
    fun respectsRecentLimit() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())
        repo.save(session("old", 1_000L))
        repo.save(session("new", 3_000L))
        repo.save(session("mid", 2_000L))

        assertEquals(listOf("new", "mid"), repo.recent(limit = 2).map { it.sessionId })
    }

    @Test
    fun zeroLimitReturnsEmpty() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())
        repo.save(session("a", 1_000L))

        assertEquals(emptyList(), repo.recent(limit = 0))
    }

    @Test
    fun saveUpsertsExistingSessionId() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())
        repo.save(session("a", 1_000L))
        repo.save(session("a", 5_000L).copy(icd = 91.0))

        val recent = repo.recent(limit = 10)
        assertEquals(1, recent.size)
        assertEquals(5_000L, recent[0].timestampMs)
        assertEquals(91.0, recent[0].icd, 0.0001)
    }

    @Test
    fun roundTripPreservesAllFields() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())
        val original = session("a", 2_000L)
        repo.save(original)

        assertEquals(original, repo.recent(limit = 1).single())
    }

    @Test
    fun blankSessionIdIsRejected() = runTest {
        val repo = SecureDecisionSessionRepository(InMemorySecureKeyValueStore())

        assertFailsWith<IllegalArgumentException> { repo.save(session("   ", 1_000L)) }
    }

    @Test
    fun corruptEntryIsSkipped() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("decision_session:bad", "not-valid-json")
        val repo = SecureDecisionSessionRepository(store)
        repo.save(session("good", 1_000L))

        val recent = repo.recent(limit = 10)
        assertEquals(listOf("good"), recent.map { it.sessionId })
        assertTrue(recent.size == 1)
    }

    private fun session(id: String, timestampMs: Long) = DecisionSession(
        sessionId = id,
        timestampMs = timestampMs,
        icd = 80.0,
        ied = 70.0,
        cde = 60.0,
        fatigueScore = 0.2,
        gamingScore = 0.1,
    )
}
