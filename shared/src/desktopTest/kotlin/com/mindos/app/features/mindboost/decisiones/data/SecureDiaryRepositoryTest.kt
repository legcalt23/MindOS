package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindverse.mindboost.core.domain.DiaryEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SecureDiaryRepositoryTest {
    @Test
    fun savesAndReadsEntry() = runTest {
        val repo = SecureDiaryRepository(InMemorySecureKeyValueStore())
        val entry = entry("d1", "reflexión".encodeToByteArray(), safetyFlag = true)

        repo.save(entry)
        val stored = repo.get("d1")

        requireNotNull(stored)
        assertEquals("d1", stored.id)
        assertEquals(10L, stored.timestampMs)
        assertEquals(true, stored.safetyFlag)
        assertContentEquals("reflexión".encodeToByteArray(), stored.encryptedContent)
    }

    @Test
    fun getMissingReturnsNull() = runTest {
        val repo = SecureDiaryRepository(InMemorySecureKeyValueStore())

        assertNull(repo.get("nope"))
    }

    @Test
    fun saveUpsertsExistingId() = runTest {
        val repo = SecureDiaryRepository(InMemorySecureKeyValueStore())
        repo.save(entry("d1", "antes".encodeToByteArray(), safetyFlag = false))
        repo.save(entry("d1", "después".encodeToByteArray(), safetyFlag = true))

        val stored = repo.get("d1")
        requireNotNull(stored)
        assertContentEquals("después".encodeToByteArray(), stored.encryptedContent)
        assertEquals(true, stored.safetyFlag)
    }

    @Test
    fun wipeRemovesAllDiaryEntries() = runTest {
        val repo = SecureDiaryRepository(InMemorySecureKeyValueStore())
        repo.save(entry("d1", byteArrayOf(1), safetyFlag = false))
        repo.save(entry("d2", byteArrayOf(2), safetyFlag = false))

        repo.wipe()

        assertNull(repo.get("d1"))
        assertNull(repo.get("d2"))
    }

    @Test
    fun blankIdIsRejected() = runTest {
        val repo = SecureDiaryRepository(InMemorySecureKeyValueStore())

        assertFailsWith<IllegalArgumentException> { repo.save(entry("  ", byteArrayOf(1), safetyFlag = false)) }
    }

    @Test
    fun corruptEntryReadsAsNull() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("decision_diary:d1", "not-valid-json")
        val repo = SecureDiaryRepository(store)

        assertNull(repo.get("d1"))
    }

    private fun entry(id: String, content: ByteArray, safetyFlag: Boolean) = DiaryEntry(
        id = id,
        timestampMs = 10L,
        encryptedContent = content,
        safetyFlag = safetyFlag,
    )
}
