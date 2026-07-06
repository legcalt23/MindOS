@file:Suppress("MagicNumber") // Counts/timestamps are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSchema
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalEmotionRepositoryTest {

    private fun summary(startedAt: Long, version: Int = EmotionSchema.SESSION_SUMMARY) = EmotionSessionSummaryDto(
        schemaVersion = version,
        exerciseId = "EM1_PAUSA",
        startedAtMillis = startedAt,
        durationMs = 1000,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        // MC8m: writes require a valid non-null sessionId; distinct per startedAt so rows don't collapse.
        sessionId = "sess_$startedAt",
    )

    @Test
    fun corruptBlobs_areQuarantined_readsAreNullOrEmpty_neverCrash() = runTest {
        // A malformed aggregate/summary blob must load as null / be skipped — never crash, never fabricate.
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        store.put("aggregate:u1", "{ not valid emotion json") // keys mirror the repo's conventions
        store.put("session:u1:5", "{ garbage summary")
        assertNull(repo.readAggregate("u1"))
        assertNull(repo.snapshot("u1"))
        assertTrue(repo.recentSummaries("u1").isEmpty(), "a corrupt session summary is skipped, not fatal")
    }

    @Test
    fun save_and_list_round_trip_sorted_desc() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.saveSessionSummary("u1", summary(1))
        repo.saveSessionSummary("u1", summary(3))
        repo.saveSessionSummary("u1", summary(2))
        val list = repo.recentSummaries("u1")
        assertEquals(listOf(3L, 2L, 1L), list.map { it.startedAtMillis })
        assertEquals(EmotionSchema.SESSION_SUMMARY, list.first().schemaVersion)
    }

    @Test
    fun prune_keeps_only_the_last_100_on_write() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        (1..105L).forEach { repo.saveSessionSummary("u1", summary(it)) }
        val list = repo.recentSummaries("u1")
        assertEquals(100, list.size)
        assertEquals(6L, list.minOf { it.startedAtMillis })
    }

    @Test
    fun delete_all_clears_summaries_and_aggregate() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.saveSessionSummary("u1", summary(1))
        repo.saveAggregate("u1", aggregate())
        repo.deleteAll("u1")
        assertTrue(repo.recentSummaries("u1").isEmpty())
        assertNull(repo.readAggregate("u1"))
    }

    @Test
    fun user_id_with_separator_is_rejected() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        assertFailsWith<IllegalArgumentException> { repo.saveSessionSummary("a:b", summary(1)) }
    }

    @Test
    fun future_schema_version_is_quarantined_not_returned() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("session:u1:1", EmotionJson.encodeToString(summary(1, version = 99)))
        val repo = LocalEmotionRepository(store)
        assertTrue(repo.recentSummaries("u1").isEmpty(), "a future-version summary must be quarantined, not returned")
    }

    @Test
    fun snapshot_maps_aggregate_to_read_only_view() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.saveAggregate("u1", aggregate())
        val snapshot = assertNotNull(repo.snapshot("u1"))
        assertEquals(ProfileTier.INITIAL_TREND, snapshot.tier)
        assertEquals(0.5, snapshot.confidence, 1e-9)
    }

    private fun aggregate() = EmotionAggregateDto(
        sessionsCompleted = 4,
        tier = "INITIAL_TREND",
        dataSufficiency = 0.5,
        safetyLoad = 0.0,
        transferConfidence = "NOT_DEMONSTRATED",
        confidence = 0.5,
        trend = "IMPROVING",
        updatedAtMillis = 1,
    )
}
