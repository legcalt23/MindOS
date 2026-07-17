package com.mindos.app.features.mindboost.core.session.payload

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Durable [SecurePayloadJournalStore]: round-trip, upsert-by-id, subject isolation, survival across a fresh store
 * view (process restart), corrupt-entry skipping, and fail-closed subject validation.
 */
class PayloadJournalStoreTest {

    private val clock = EpochClock { 5_000L }

    private fun store(backing: InMemorySecureKeyValueStore, subject: String) =
        SecurePayloadJournalStore(backing, subject = subject, clock = clock)

    @Test
    fun enqueue_thenPending_roundTrips() = runTest {
        val journal = store(InMemorySecureKeyValueStore(), "user-1")
        journal.enqueue("user-1:100", PayloadKind.REVERSIBILITY_LOG, "payload-A")

        val entry = journal.pending().single()
        assertEquals("user-1:100", entry.entryId)
        assertEquals(PayloadKind.REVERSIBILITY_LOG, entry.kind)
        assertEquals("payload-A", entry.payload)
        assertEquals(5_000L, entry.enqueuedAtMs, "enqueuedAtMs comes from the injected clock, never a 0L sentinel")
    }

    @Test
    fun enqueue_isUpsertByEntryId_neverDuplicates() = runTest {
        val journal = store(InMemorySecureKeyValueStore(), "user-1")
        journal.enqueue("s1", PayloadKind.REVERSIBILITY_LOG, "v1")
        journal.enqueue("s1", PayloadKind.REVERSIBILITY_LOG, "v2")

        val pending = journal.pending()
        assertEquals(1, pending.size, "the same entryId upserts in place, never a second entry")
        assertEquals("v2", pending.single().payload)
    }

    @Test
    fun confirm_drainsTheEntry() = runTest {
        val journal = store(InMemorySecureKeyValueStore(), "user-1")
        journal.enqueue("s1", PayloadKind.E8_COGNITIVE_RUN, "run")
        journal.confirm("s1")
        assertTrue(journal.pending().isEmpty(), "the journal is empty after confirmation")
    }

    @Test
    fun pending_isSubjectScoped_andConfirmNeverTouchesAnotherSubject() = runTest {
        val backing = InMemorySecureKeyValueStore() // ONE physical namespace shared by both accounts
        val a = store(backing, "user-A")
        val b = store(backing, "user-B")
        a.enqueue("sA", PayloadKind.REVERSIBILITY_LOG, "A")
        b.enqueue("sB", PayloadKind.REVERSIBILITY_LOG, "B")

        assertEquals(listOf("sA"), a.pending().map { it.entryId }, "A sees only its own entry")
        assertEquals(listOf("sB"), b.pending().map { it.entryId }, "B sees only its own entry")

        a.confirm("sB") // A cannot remove B's entry (different subject prefix)
        assertEquals(listOf("sB"), b.pending().map { it.entryId }, "A's confirm never removes B's entry")
    }

    @Test
    fun pendingEntry_survivesAFreshStoreView_simulatingProcessRestart() = runTest {
        val backing = InMemorySecureKeyValueStore() // the durable device-wide file
        store(backing, "user-1").enqueue("user-1:100", PayloadKind.REVERSIBILITY_LOG, "payload")

        // "Process restart": a brand-new store over the SAME backing must still see the durable pending entry.
        assertEquals("payload", store(backing, "user-1").pending().single().payload)
    }

    @Test
    fun corruptEntry_isSkipped_notMisread() = runTest {
        val backing = InMemorySecureKeyValueStore()
        // A garbage blob under a well-formed journal key for this subject.
        backing.put("payload_journal:user-1:bad", "{ not valid json")
        assertTrue(store(backing, "user-1").pending().isEmpty(), "an undecodable entry is skipped, never crashes")
    }

    @Test
    fun blankSubject_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            SecurePayloadJournalStore(InMemorySecureKeyValueStore(), subject = "", clock = clock)
        }
    }

    @Test
    fun subjectContainingTheDelimiter_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            SecurePayloadJournalStore(InMemorySecureKeyValueStore(), subject = "a:b", clock = clock)
        }
    }
}
