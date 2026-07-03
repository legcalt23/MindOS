package com.mindos.app.features.mindboost.decisiones.presentation.ui

import com.mindos.app.features.mindboost.decisiones.data.SecureDecisionSessionRepository
import com.mindos.app.features.mindboost.decisiones.data.SecureDiaryRepository
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * B1 regression: sealing a Decisiones session used to run `onSealed = { _, _ -> onExit() }`, silently
 * discarding both the [DecisionSession] and the reflective [DiaryEntry]. The persist-on-seal orchestration
 * is now the pure [handleSealed], so we can assert it persists BEFORE leaving the feature — and that a
 * real repo-backed persist survives to be read back after "reopening" (a fresh repo over the same store).
 */
class HandleSealedFinishTest {

    private fun session(id: String) = DecisionSession(
        sessionId = id,
        timestampMs = 10L,
        icd = 1.0,
        ied = 2.0,
        cde = 3.0,
        fatigueScore = 0.1,
        gamingScore = 0.2,
    )

    private fun entry(id: String, content: ByteArray) = DiaryEntry(
        id = id,
        timestampMs = 10L,
        encryptedContent = content,
        safetyFlag = false,
    )

    @Test
    fun sealed_persistsSessionAndEntry_thenExits() {
        val calls = mutableListOf<String>()
        var persistedSession: DecisionSession? = null
        var persistedEntry: DiaryEntry? = null
        handleSealed(
            session = session("s1"),
            entry = entry("s1", "reflexión".encodeToByteArray()),
            persist = { s, e -> calls += "persist"; persistedSession = s; persistedEntry = e },
            exit = { calls += "exit" },
        )
        assertEquals(listOf("persist", "exit"), calls, "persist must run before leaving the feature")
        assertEquals("s1", assertNotNull(persistedSession).sessionId, "the sealed session is handed to persist")
        assertEquals("s1", assertNotNull(persistedEntry).id, "the diary entry is handed to persist")
    }

    @Test
    fun sealed_repoBackedPersist_survivesReopen() = runTest {
        val store = InMemorySecureKeyValueStore()
        val diaryText = "lo que aprendí hoy".encodeToByteArray()
        var exited = false

        handleSealed(
            session = session("sX"),
            entry = entry("sX", diaryText),
            persist = { s, e ->
                launch {
                    SecureDecisionSessionRepository(store).save(s)
                    SecureDiaryRepository(store).save(e)
                }
            },
            exit = { exited = true },
        )
        advanceUntilIdle()

        // "Reopen the app": fresh repo views over the SAME encrypted store must see both.
        val reopenedSession = SecureDecisionSessionRepository(store).recent(10).firstOrNull()
        val reopenedDiary = SecureDiaryRepository(store).get("sX")

        assertEquals("sX", assertNotNull(reopenedSession, "session must survive to be visible after reopen").sessionId)
        assertContentEquals(
            diaryText,
            assertNotNull(reopenedDiary, "diary must survive to be visible after reopen").encryptedContent,
        )
        assertTrue(exited, "the feature is left after persistence, not instead of it")
    }
}
