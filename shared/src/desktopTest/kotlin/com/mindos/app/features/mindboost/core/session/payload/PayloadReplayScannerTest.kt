package com.mindos.app.features.mindboost.core.session.payload

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [PayloadReplayScanner]: apply + confirm (drain), idempotent replay against a dedup sink, RETAIN on transient
 * failure, and leaving entries whose kind has no handler for a sibling scanner.
 */
class PayloadReplayScannerTest {

    @Test
    fun replay_appliesHandler_thenConfirms_drainEmpty() = runTest {
        val journal = InMemoryPayloadJournalStore()
        journal.enqueue("s1", PayloadKind.REVERSIBILITY_LOG, "p1")
        val applied = mutableListOf<String>()
        val handler: suspend (JournaledPayload) -> ReplayOutcome = { entry ->
            applied += entry.payload
            ReplayOutcome.CONFIRMED
        }
        val scanner = PayloadReplayScanner(journal, mapOf(PayloadKind.REVERSIBILITY_LOG to handler))

        assertEquals(1, scanner.replay())
        assertEquals(listOf("p1"), applied)
        assertTrue(journal.pending().isEmpty(), "confirmed entries are drained")
    }

    @Test
    fun replay_isIdempotent_whenSinkDedupsById() = runTest {
        val journal = InMemoryPayloadJournalStore()
        journal.enqueue("s1", PayloadKind.E8_COGNITIVE_RUN, "run")
        val sinkState = mutableSetOf<String>() // an idempotent sink: a re-applied id is a no-op
        val handler: suspend (JournaledPayload) -> ReplayOutcome = { entry ->
            sinkState += entry.entryId
            ReplayOutcome.CONFIRMED
        }
        val scanner = PayloadReplayScanner(journal, mapOf(PayloadKind.E8_COGNITIVE_RUN to handler))

        scanner.replay() // applies + confirms
        // Simulate "persisted but the confirm was lost before it durably removed the entry": re-enqueue + replay.
        journal.enqueue("s1", PayloadKind.E8_COGNITIVE_RUN, "run")
        scanner.replay()

        assertEquals(setOf("s1"), sinkState, "the sink deduped by id — replay never produced a duplicate")
        assertTrue(journal.pending().isEmpty())
    }

    @Test
    fun replay_retain_keepsEntryForNextStartup() = runTest {
        val journal = InMemoryPayloadJournalStore()
        journal.enqueue("s1", PayloadKind.REVERSIBILITY_LOG, "p")
        val handler: suspend (JournaledPayload) -> ReplayOutcome = { ReplayOutcome.RETAIN }
        val scanner = PayloadReplayScanner(journal, mapOf(PayloadKind.REVERSIBILITY_LOG to handler))

        assertEquals(0, scanner.replay(), "a RETAIN is not confirmed")
        assertEquals(1, journal.pending().size, "a transient failure keeps the entry for the next startup replay")
    }

    @Test
    fun replay_leavesEntriesWithNoHandler_forASiblingScanner() = runTest {
        val journal = InMemoryPayloadJournalStore()
        journal.enqueue("s1", PayloadKind.E8_COGNITIVE_RUN, "run")
        val handler: suspend (JournaledPayload) -> ReplayOutcome = { ReplayOutcome.CONFIRMED }
        // Only handles REVERSIBILITY_LOG → the E8 entry is left untouched for the Velocidad scanner.
        val scanner = PayloadReplayScanner(journal, mapOf(PayloadKind.REVERSIBILITY_LOG to handler))

        assertEquals(0, scanner.replay())
        assertEquals(1, journal.pending().size, "an entry whose kind has no handler is never confirmed")
    }
}
