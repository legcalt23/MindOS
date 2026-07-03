package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityRecordLog
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** E2 reversibility (P2) · the encrypted reversibility-card log: round-trip, cap, user-scope, corruption. */
class LocalReversibilityResultStoreTest {

    private fun trial(actual: Boolean, selected: Boolean) = ReversibilityTrial(actual, selected)

    @Test
    fun recordThenLoad_roundTripsTheTrials() = runTest {
        val store = LocalReversibilityResultStore(InMemorySecureKeyValueStore())
        store.record("u1", listOf(trial(true, true), trial(false, false)))
        val log = store.load("u1")
        assertEquals(
            listOf(true to true, false to false),
            log.trials.map { it.actuallyReversible to it.selectedReversible },
        )
    }

    @Test
    fun record_capsToTheRecentWindow() = runTest {
        val store = LocalReversibilityResultStore(InMemorySecureKeyValueStore())
        val many = List(ReversibilityRecordLog.MAX_RETAINED_TRIALS + 10) { trial(true, true) }
        val log = store.record("u1", many)
        assertEquals(ReversibilityRecordLog.MAX_RETAINED_TRIALS, log.trials.size)
        assertEquals(ReversibilityRecordLog.MAX_RETAINED_TRIALS, store.load("u1").trials.size)
    }

    @Test
    fun corruptPayload_decodesToEmptyLog_neverThrows() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put("reversibility:u1", "{ not valid json")
        val log = LocalReversibilityResultStore(backing).load("u1")
        assertTrue(log.trials.isEmpty(), "a corrupt entry yields an empty log, never a crash")
    }

    @Test
    fun futureSchemaVersion_decodesToEmptyLog_neverDowngrades() = runTest {
        // A payload written by a NEWER schema must NOT be silently mis-read as v1 → quarantined → empty.
        val backing = InMemorySecureKeyValueStore()
        // A non-empty trials array that WOULD load if the version were accepted — proving the version gate
        // (not emptiness) is what yields the empty log.
        val payload = """{"schemaVersion":999,"trials":[{"actuallyReversible":true,"selectedReversible":true}]}"""
        backing.put("reversibility:u1", payload)
        val log = LocalReversibilityResultStore(backing).load("u1")
        assertTrue(log.trials.isEmpty(), "an unsupported future version yields an empty log, never a downgrade")
    }

    @Test
    fun loadsAreScopedPerUser() = runTest {
        val store = LocalReversibilityResultStore(InMemorySecureKeyValueStore())
        store.record("u1", listOf(trial(true, true)))
        assertTrue(store.load("u2").trials.isEmpty(), "user u1's log is not visible to u2")
    }
}
