package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.local.LocalDeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecordLog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** EX3 deferred recall (P3) · the encrypted delayed-recall log: round-trip, cap, user-scope, corruption. */
class LocalDeferredResultStoreTest {

    private fun outcome(hits: Int) =
        DeferredRecallOutcome(studiedCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0)

    @Test
    fun record_survivesThroughTheStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalDeferredResultStore(store).record("u1", outcome(6))
        val loaded = LocalDeferredResultStore(store).load("u1") // fresh instance, same store = true persistence
        assertEquals(1, loaded.outcomes.size)
        assertEquals(6, loaded.outcomes.single().hits)
    }

    @Test
    fun missing_isEmptyLog() = runTest {
        assertEquals(0, LocalDeferredResultStore(InMemorySecureKeyValueStore()).load("ghost").outcomes.size)
    }

    @Test
    fun record_accumulatesAndCapsToRetention() = runTest {
        val repo = LocalDeferredResultStore(InMemorySecureKeyValueStore())
        var log = DeferredRecordLog()
        repeat(DeferredRecordLog.MAX_RETAINED_OUTCOMES + 5) { log = repo.record("u1", outcome(3)) }
        assertEquals(DeferredRecordLog.MAX_RETAINED_OUTCOMES, log.outcomes.size)
    }

    @Test
    fun record_isScopedByUser() = runTest {
        val repo = LocalDeferredResultStore(InMemorySecureKeyValueStore())
        repo.record("u1", outcome(6))
        repo.record("u2", outcome(2))
        assertEquals(1, repo.load("u1").outcomes.size)
        assertEquals(2, repo.load("u2").outcomes.single().hits)
    }

    @Test
    fun corruptPayload_decodesToEmptyLog() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("delayed:u1", "{ not valid json")
        assertEquals(0, LocalDeferredResultStore(store).load("u1").outcomes.size)
    }
}
