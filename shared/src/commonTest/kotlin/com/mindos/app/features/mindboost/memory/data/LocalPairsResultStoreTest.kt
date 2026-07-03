package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.local.LocalPairsResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** P1 EX4 (P2b) · the encrypted pairs log round-trips, accumulates, is user-scoped, and tolerates corruption. */
class LocalPairsResultStoreTest {

    private fun trial(hits: Int) = PairsTrial(targetCount = 5, hits = hits, distractorCount = 6, falseAlarms = 0)

    @Test
    fun record_survivesThroughTheStore() = runTest {
        val store = InMemorySecureKeyValueStore()
        LocalPairsResultStore(store).record("u1", trial(4))
        val loaded = LocalPairsResultStore(store).load("u1") // fresh instance, same store = true persistence
        assertEquals(1, loaded.trials.size)
        assertEquals(4, loaded.trials.single().hits)
    }

    @Test
    fun missing_isEmptyLog() = runTest {
        assertEquals(0, LocalPairsResultStore(InMemorySecureKeyValueStore()).load("ghost").trials.size)
    }

    @Test
    fun record_accumulatesAndCapsToRetention() = runTest {
        val repo = LocalPairsResultStore(InMemorySecureKeyValueStore())
        var log = PairsRecordLog()
        repeat(PairsRecordLog.MAX_RETAINED_TRIALS + 5) { log = repo.record("u1", trial(3)) }
        assertEquals(PairsRecordLog.MAX_RETAINED_TRIALS, log.trials.size)
    }

    @Test
    fun record_isScopedByUser() = runTest {
        val repo = LocalPairsResultStore(InMemorySecureKeyValueStore())
        repo.record("u1", trial(4))
        repo.record("u2", trial(2))
        assertEquals(1, repo.load("u1").trials.size)
        assertEquals(2, repo.load("u2").trials.single().hits)
    }

    @Test
    fun corruptPayload_decodesToEmptyLog() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("pairs:u1", "{ not valid json")
        assertEquals(0, LocalPairsResultStore(store).load("u1").trials.size)
    }
}
