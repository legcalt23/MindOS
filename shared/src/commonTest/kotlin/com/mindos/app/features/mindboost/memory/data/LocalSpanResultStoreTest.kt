package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.local.LocalSpanResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** P1 EX1 (P4) · the encrypted span log round-trips, accumulates, is user-scoped, and tolerates corruption. */
class LocalSpanResultStoreTest {

    private fun trial(items: Int) = SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = items)

    @Test
    fun record_survivesThroughTheStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalSpanResultStore(store).record("u1", trial(4), listOf(1, 0))
        // A fresh store-backed instance reads it back (true persistence path).
        val loaded = LocalSpanResultStore(store).load("u1")
        assertEquals(1, loaded.trials.size)
        assertEquals(4, loaded.trials.single().serialCorrectItems)
        assertEquals(listOf(1, 0), loaded.processingResponses)
    }

    @Test
    fun missing_isEmptyLog() = runTest {
        val loaded = LocalSpanResultStore(InMemorySecureKeyValueStore()).load("ghost")
        assertEquals(0, loaded.trials.size)
        assertEquals(0, loaded.processingResponses.size)
    }

    @Test
    fun record_accumulatesAcrossCalls() = runTest {
        val repo = LocalSpanResultStore(InMemorySecureKeyValueStore())
        repo.record("u1", trial(3), listOf(1))
        val after = repo.record("u1", trial(5), listOf(0))
        assertEquals(2, after.trials.size)
        assertEquals(listOf(1, 0), after.processingResponses)
    }

    @Test
    fun record_isScopedByUser() = runTest {
        val repo = LocalSpanResultStore(InMemorySecureKeyValueStore())
        repo.record("u1", trial(4), listOf(1))
        repo.record("u2", trial(4), listOf(0))
        assertEquals(1, repo.load("u1").trials.size)
        assertEquals(1, repo.load("u2").trials.size)
    }

    @Test
    fun corruptPayload_decodesToEmptyLog() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("span:u1", "{ not valid json")
        assertEquals(0, LocalSpanResultStore(store).load("u1").trials.size)
    }
}
