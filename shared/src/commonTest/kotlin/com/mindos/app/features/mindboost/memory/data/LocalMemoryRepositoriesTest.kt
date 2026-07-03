package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.local.LocalDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.domain.personalization.MemoryArchetype
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMemoryRepositoriesTest {

    @Test
    fun profile_survivesThroughTheStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalMemoryProfileRepository(store).save(
            MemoryProfileState.initial("u1").copy(archetype = MemoryArchetype.FAST_FORGETTER),
        )
        // A fresh repo over the SAME store reads it back (true persistence path).
        val loaded = LocalMemoryProfileRepository(store).load("u1")
        assertEquals(MemoryArchetype.FAST_FORGETTER, loaded?.archetype)
    }

    @Test
    fun profile_missingIsNull() = runTest {
        assertNull(LocalMemoryProfileRepository(InMemorySecureKeyValueStore()).load("ghost"))
    }

    @Test
    fun sessionHistory_isNewestFirstAndLimited() = runTest {
        val repo = LocalMemorySessionHistoryRepository(InMemorySecureKeyValueStore())
        listOf(100L, 300L, 200L).forEach { t ->
            repo.append(
                MemorySessionSummary("u1:$t", "u1", MemoryExerciseId.EX3_LISTA_ESTRATEGIA, t, 0.7, 0.6, 0.65),
            )
        }
        val recent = repo.recent("u1", limit = 2)
        assertEquals(listOf(300L, 200L), recent.map { it.endedAtEpoch })
    }

    @Test
    fun sessionHistory_scopedByUser() = runTest {
        val repo = LocalMemorySessionHistoryRepository(InMemorySecureKeyValueStore())
        repo.append(MemorySessionSummary("u1:1", "u1", MemoryExerciseId.EX1_SPAN_COMPLEJO, 1, null, null, 0.5))
        repo.append(MemorySessionSummary("u2:1", "u2", MemoryExerciseId.EX1_SPAN_COMPLEJO, 1, null, null, 0.5))
        assertEquals(1, repo.recent("u1", 10).size)
    }

    @Test
    fun deferredItems_roundTripThroughStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        val items = listOf(
            DeferredRecallItem(
                id = "d1",
                exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                language = Language.SPANISH_ES,
                studiedWordIds = listOf(WordId("w1")),
                studiedAtEpoch = 10,
            ),
        )
        LocalDeferredRecallRepository(store).save("u1", items)
        assertEquals(items, LocalDeferredRecallRepository(store).load("u1"))
    }

    @Test
    fun deferred_missingIsEmpty() = runTest {
        assertTrue(LocalDeferredRecallRepository(InMemorySecureKeyValueStore()).load("ghost").isEmpty())
    }

    @Test
    fun recentRegistry_roundTripThroughStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        val reg = RecentItemRegistry(
            exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
            wordUsage = mapOf(WordId("w1") to 5L),
        )
        LocalRecentItemRegistryRepository(store).save("u1", reg)
        val loaded = LocalRecentItemRegistryRepository(store)
            .load("u1", MemoryExerciseId.EX4_PARES_ASOCIADOS)
        assertEquals(reg, loaded)
    }

    @Test
    fun corruptPayload_decodesToNull() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("profile:u1", "{not valid json")
        assertNull(LocalMemoryProfileRepository(store).load("u1"))
    }

    @Test
    fun storedPayloadCarriesSchemaVersion() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalMemoryProfileRepository(store).save(MemoryProfileState.initial("u1"))
        val raw = (
            store.read("profile:u1") as?
                com.mindos.app.features.mindboost.focus.data.local.SecureReadResult.Value
            )?.value
        assertTrue(raw != null && raw.contains("schemaVersion"))
    }
}
