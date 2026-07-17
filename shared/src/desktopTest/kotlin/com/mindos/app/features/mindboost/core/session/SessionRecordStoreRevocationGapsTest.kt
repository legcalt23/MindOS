package com.mindos.app.features.mindboost.core.session

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.ControllableReadStore
import com.mindos.app.features.mindboost.core.session.cognitive.CountingSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.PutFailingSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.ReadThrowingSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.RemoveFailingSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (P2-01 ENMIENDA 2026-07-14) · the TRIADIC revocation contract for [SecureSessionRecordStore]
 * via the shared [SubjectRevocationAuthority] over a DEDICATED revocation backing. The SessionRecord store's
 * CHANNELLED read ([findEvidence] → [SessionRecordRead.ReadFailed]) surfaces STATUS_UNAVAILABLE honestly; its
 * channel-less reads ([find]/[all]) breadcrumb + return the conservative default (they feed recovery/write-ahead,
 * not the profile). [upsert] returns a TYPED outcome, never a silent success. Replaces the prior fail-OPEN read
 * backstop. Test ids map to the ENMIENDA §5 matrix.
 */
class SessionRecordStoreRevocationGapsTest {

    private fun record(
        sessionId: String = "s1",
        status: SessionStatus = SessionStatus.STARTED,
        updatedAtMs: Long = 1_000L,
    ) = SessionRecord(
        sessionId = sessionId,
        block = ScoreModuleId.FOCUS,
        status = status,
        startedAtMs = 1_000L,
        updatedAtMs = updatedAtMs,
        completedTrials = 0,
        plannedTrials = 20,
        activeDurationMs = 0L,
    )

    // T1/T2 — STATUS_UNAVAILABLE: findEvidence (channelled) → ReadFailed; find/all (channel-less) → null/empty; the
    // payload row is never read in any of them.
    @Test
    fun t1_statusUnavailableRead_channelledSurfacesReadFailed_channelLessConservative_payloadUntouched() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = ControllableReadStore()
        val store = SecureSessionRecordStore(payload, revocation, subject = "alice")
        store.upsert(record("s1")) // seeded while ACTIVE
        payload.reads.clear()

        revocation.throwOnRead = true
        assertEquals(SessionRecordRead.ReadFailed, store.findEvidence("s1"), "channelled read surfaces the fault honestly")
        assertNull(store.find("s1"), "channel-less find returns null (conservative), never the row")
        assertTrue(store.all().isEmpty(), "channel-less all returns empty (conservative)")
        assertTrue(
            payload.reads.none { it.startsWith("session_record:") },
            "STATUS_UNAVAILABLE must never read the payload row",
        )
    }

    // T3 — ACTIVE but the payload read faults: findEvidence surfaces ReadFailed (null != failure), never Missing.
    @Test
    fun t3_activeSubject_payloadReadFault_findEvidenceIsReadFailed_notMissing() = runTest {
        val payload = ReadThrowingSecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore() // ACTIVE
        val store = SecureSessionRecordStore(payload, revocation, subject = "alice")

        assertEquals(
            SessionRecordRead.ReadFailed,
            store.findEvidence("s1"),
            "a real payload backend fault is ReadFailed, never a fabricated Missing (null != failure)",
        )
    }

    // T4 — a POSITIVE tombstone with a physically surviving row (a Failed deleteAll left it): every read is gated.
    @Test
    fun t4_revokedSubject_survivingRow_isNeverReadable() = runTest {
        val payload = RemoveFailingSecureKeyValueStore(failOn = { true }) // remove faults; markRevoked's put lands
        val store = SecureSessionRecordStore(payload, InMemorySecureKeyValueStore(), subject = "alice")
        store.upsert(record("s1"))

        assertEquals(SessionRecordDeleteResult.Failed, store.deleteAll()) // tombstoned, but the row survives on disk

        assertNull(store.find("s1"), "find is revocation-gated, not deletion-dependent")
        assertTrue(store.all().isEmpty(), "all is revocation-gated")
        assertEquals(SessionRecordRead.Missing, store.findEvidence("s1"), "findEvidence maps REVOKED → Missing")
    }

    // T8 — STATUS_UNAVAILABLE on upsert: typed RetryableFailure, no write.
    @Test
    fun t8_statusUnavailableUpsert_isRetryableFailure_withoutWriting() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = ControllableReadStore(throwOnRead = true)
        val store = SecureSessionRecordStore(payload, revocation, subject = "alice")

        val outcome = store.upsert(record("s1"))

        assertIs<SessionRecordUpsertOutcome.RetryableFailure>(outcome)
        assertTrue(payload.reads.isEmpty(), "the write is never attempted under STATUS_UNAVAILABLE")
    }

    // REVOKED upsert: terminal RejectedRevoked (no resurrection).
    @Test
    fun revokedUpsert_isRejectedRevoked() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(payload, revocation, subject = "alice")
        SubjectRevocationAuthority(revocation).markRevoked("alice")

        assertEquals(SessionRecordUpsertOutcome.RejectedRevoked, store.upsert(record("s1")))
        assertNull(store.find("s1"))
    }

    // upsert returns a TYPED outcome on a genuine backend put fault — never a silent success, never a crash.
    @Test
    fun upsert_backendPutFault_isRetryableFailure_notCrashNotSilentSuccess() = runTest {
        val payload = PutFailingSecureKeyValueStore(failPrefix = "session_record:alice:")
        val store = SecureSessionRecordStore(payload, InMemorySecureKeyValueStore(), subject = "alice")

        val outcome = store.upsert(record("s1")) // must NOT throw (LaunchedEffect caller) and must NOT report Stored

        assertIs<SessionRecordUpsertOutcome.RetryableFailure>(outcome)
        assertNull(store.find("s1"), "the faulted write persisted nothing")
    }

    // T9 — recover() counts only Stored: an orphan whose ABANDONED upsert fails is NOT reported as recovered.
    @Test
    fun t9_recover_doesNotReportSuccessWhenTheAbandonedUpsertFails() = runTest {
        val payload = PutFailingSecureKeyValueStore(failPrefix = "session_record:alice:")
        payload.active = false // seed the orphan with a working write
        val store = SecureSessionRecordStore(payload, InMemorySecureKeyValueStore(), subject = "alice")
        store.upsert(record("s1", status = SessionStatus.STARTED, updatedAtMs = 0L)) // aged orphan
        payload.active = true // now every write for this subject faults

        val scanner = SessionRecoveryScanner(store, EpochClock { 10_000_000L })
        val recovered = scanner.recover()

        assertEquals(0, recovered, "a recovery whose ABANDONED write did not persist must NOT be counted as recovered")
    }

    // T10 — CancellationException during the status read is rethrown, never a false outcome.
    @Test
    fun t10_cancellationDuringStatusRead_isRethrown() = runTest {
        val store = SecureSessionRecordStore(
            InMemorySecureKeyValueStore(),
            ControllableReadStore(cancelOnRead = true),
            subject = "alice",
        )
        assertFailsWith<CancellationException> { store.upsert(record("s1")) }
        assertFailsWith<CancellationException> { store.findEvidence("s1") }
    }

    // T6 — a fresh instance sharing the authority sees a prior instance's durable tombstone and refuses.
    @Test
    fun t6_aFreshInstance_sharingTheAuthority_seesThePriorTombstone_andRefusesUpsert() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore() // SHARED
        val instanceA = SecureSessionRecordStore(payload, revocation, subject = "alice")
        instanceA.upsert(record("s1"))
        assertEquals(SessionRecordDeleteResult.Cleared(1), instanceA.deleteAll())

        val instanceB = SecureSessionRecordStore(payload, revocation, subject = "alice")
        assertEquals(SessionRecordUpsertOutcome.RejectedRevoked, instanceB.upsert(record("s2")))
        assertNull(instanceB.find("s2"))
        assertTrue(instanceB.all().isEmpty())
    }

    // T7 — process-restart simulation: only the durable revocation backing survives; the tombstone still gates.
    @Test
    fun t7_processRestart_durableTombstoneSurvives_andResumedDeleteAllStaysIdempotent() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val before = SecureSessionRecordStore(payload, revocation, subject = "alice")
        before.upsert(record("s1"))
        assertEquals(SessionRecordDeleteResult.Cleared(1), before.deleteAll())

        val after = SecureSessionRecordStore(payload, revocation, subject = "alice")
        assertEquals(SessionRecordUpsertOutcome.RejectedRevoked, after.upsert(record("s2")))
        assertEquals(SessionRecordDeleteResult.Cleared(0), after.deleteAll(), "a resumed erasure stays idempotent")
    }

    // T11 — hold-through-erase does not deadlock: a concurrent same-subject upsert and deleteAll both complete.
    @Test
    fun t11_deletionAndConcurrentUpsert_completeWithoutDeadlock() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val writer = SecureSessionRecordStore(payload, revocation, subject = "alice")
        val eraser = SecureSessionRecordStore(payload, revocation, subject = "alice")
        writer.upsert(record("s0"))

        val d = async { eraser.deleteAll() }
        val u = async { writer.upsert(record("s1")) }
        assertIs<SessionRecordDeleteResult.Cleared>(d.await())
        u.await()
        assertNull(writer.find("s1"), "no row resurrects after revocation")
    }

    // T12 — account A/B isolation over the shared authority: revoking B leaves A fully ACTIVE and readable.
    @Test
    fun t12_revokingSubjectB_leavesSubjectAActive() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore() // SHARED
        val a = SecureSessionRecordStore(payload, revocation, subject = "alice")
        val b = SecureSessionRecordStore(payload, revocation, subject = "bob")
        a.upsert(record("a1"))
        b.upsert(record("b1"))

        assertEquals(SessionRecordDeleteResult.Cleared(1), b.deleteAll())

        assertNull(b.find("b1"), "B is revoked + erased")
        assertEquals("a1", a.find("a1")?.sessionId, "A is untouched — still ACTIVE and readable")
        assertEquals(SessionRecordUpsertOutcome.Stored, a.upsert(record("a2")), "A can still write (ACTIVE)")
    }
}
