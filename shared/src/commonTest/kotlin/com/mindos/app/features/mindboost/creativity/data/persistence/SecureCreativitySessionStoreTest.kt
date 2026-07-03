package com.mindos.app.features.mindboost.creativity.data.persistence

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureCreativitySessionStoreTest {

    private fun store() = SecureCreativitySessionStore(InMemorySecureKeyValueStore())

    private fun dto(userId: String, sessionId: String, createdAtMs: Long) =
        PersistedCreativitySessionDto(
            session = CreativitySessionRecord(
                sessionId = sessionId,
                userId = userId,
                exerciseId = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
                createdAtMs = createdAtMs,
                updatedAtMs = createdAtMs,
            ),
        )

    @Test
    fun savesAndReadsBackASession() = runTest {
        val s = store()
        s.save(dto("u1", "s1", 100))
        assertEquals("u1", s.read("s1")?.session?.userId)
    }

    @Test
    fun readReturnsNullForUnknownSession() = runTest {
        assertNull(store().read("missing"))
    }

    @Test
    fun historyForUserIsMostRecentFirst() = runTest {
        val s = store()
        s.save(dto("u1", "s1", 100))
        s.save(dto("u1", "s2", 300))
        s.save(dto("u1", "s3", 200))
        assertEquals(listOf("s2", "s3", "s1"), s.historyForUser("u1").map { it.session.sessionId })
    }

    @Test
    fun deleteUserDataRemovesOnlyThatUser() = runTest {
        val s = store()
        s.save(dto("u1", "s1", 100))
        s.save(dto("u1", "s2", 200))
        s.save(dto("u2", "s3", 100))
        val removed = s.deleteUserData("u1")
        assertEquals(2, removed)
        assertTrue(s.historyForUser("u1").isEmpty())
        assertEquals(listOf("s3"), s.historyForUser("u2").map { it.session.sessionId })
    }

    @Test
    fun saveReplacesExistingSession() = runTest {
        val s = store()
        s.save(dto("u1", "s1", 100))
        s.save(dto("u1", "s1", 999))
        val history = s.historyForUser("u1")
        assertEquals(1, history.size)
        assertEquals(999, history.first().session.createdAtMs)
    }
}
