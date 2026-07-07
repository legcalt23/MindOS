@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values are the data under test.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MC8u-D §2/§7 — release-blocking persistence contract: no progress loss, no duplicated sessions on retry, and the
 * exposure history round-trips + caps. Uses the real [LocalEmotionRepository] over the in-memory secure store (the
 * same store that backs desktop/iOS actuals), so this exercises the production persistence path, not a mock.
 */
class EmotionPersistenceReleaseContractTest {

    private fun summary(sessionId: String, started: Long = 1_000L) = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = started,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("CATEGORY_AMBIGUITY", 0.5, 2),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = sessionId,
        endReason = "COMPLETED",
    )

    @Test
    fun retrying_the_same_session_does_not_duplicate_it() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val s = summary("sess_dup1")
        repo.saveSessionSummary("u", s)
        repo.saveSessionSummary("u", s) // idempotent retry: same sessionId → same key → overwrite, never a duplicate
        assertEquals(1, repo.recentSummaries("u").size, "the same sessionId must never create a second record")
    }

    @Test
    fun a_completed_session_survives_recreating_the_repository_over_the_same_store() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalEmotionRepository(store).saveSessionSummary("u", summary("sess_persist1"))
        // Simulate a fresh holder/repo (e.g. app relaunch) over the same durable store: progress must not be lost.
        val recovered = LocalEmotionRepository(store).recentSummaries("u")
        assertEquals(1, recovered.size)
        assertEquals("sess_persist1", recovered.first().sessionId, "no progress loss across repository recreation")
    }

    @Test
    fun exposure_history_round_trips_most_recent_first_and_caps_to_the_recency_window() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.recordExposedPhrases("u", "EM3_SEPARADOR", listOf("p1", "p2", "p3"))
        assertEquals(listOf("p1", "p2", "p3"), repo.recentExposedPhrases("u", "EM3_SEPARADOR"))

        repo.recordExposedPhrases("u", "EM3_SEPARADOR", (1..30).map { "q$it" })
        val recent = repo.recentExposedPhrases("u", "EM3_SEPARADOR")
        assertEquals(EmotionExposureStore.EXPOSURE_CAP, recent.size, "the recency window is capped")
        assertEquals("q1", recent.first(), "the most-recent batch is at the front")
    }

    @Test
    fun exposure_history_survives_repository_recreation() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalEmotionRepository(store).recordExposedPhrases("u", "EM10_SEP_PRESION", listOf("x", "y"))
        assertEquals(listOf("x", "y"), LocalEmotionRepository(store).recentExposedPhrases("u", "EM10_SEP_PRESION"))
    }
}
