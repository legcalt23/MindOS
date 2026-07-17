package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SubjectRevocationAuthority
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (P2-01 ENMIENDA 2026-07-14) · the TRIADIC revocation contract (ACTIVE / REVOKED /
 * STATUS_UNAVAILABLE) for [SecureCognitiveSessionRunStore], via the shared [SubjectRevocationAuthority] over a
 * DEDICATED revocation backing (separate from the payload backing). That separation is what makes the tests below
 * discriminating: a fake that failed ALL reads could not tell a revocation-status read fault from a payload read
 * fault. Replaces the prior fail-OPEN read backstop (an exposure when the tombstone could not be read). Test ids
 * map to the ENMIENDA §5 matrix (T1–T12).
 */
class CognitiveSessionRunStoreRevocationGapsTest {

    private fun run(id: String, block: ScoreModuleId = ScoreModuleId.SPEED) =
        CognitiveSessionRunFixtures.validRun(sessionId = id, blockId = block)

    // T1/T2 — the revocation STATUS read faults while the payload row exists: STATUS_UNAVAILABLE, payload untouched.
    @Test
    fun t1_statusUnavailableRead_returnsFailedRetryable_andNeverReadsThePayloadRow() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = ControllableReadStore()
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        assertEquals(SaveResult.Saved, store.save(run("r1"))) // seeded while ACTIVE
        payload.reads.clear()

        revocation.throwOnRead = true // status can no longer be determined
        val result = store.get("r1")

        assertIs<ReadResult.Failed>(result)
        assertEquals(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE, result.reason)
        assertTrue(
            payload.reads.none { it.startsWith("crun") },
            "STATUS_UNAVAILABLE must NEVER touch the payload row (no crun read); it is not Found and not Missing",
        )
    }

    // T2 — a NON-throwing revocation-status Failure (corrupt tombstone) is STATUS_UNAVAILABLE too (distinct from a
    // throw): the write/read is refused, payload untouched — exercises statusOf's `is Failure` branch.
    @Test
    fun t2_corruptRevocationStatus_isStatusUnavailable_payloadUntouched() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = ControllableReadStore(failureOnRead = true)
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")

        val result = store.get("r1")

        assertIs<ReadResult.Failed>(result)
        assertEquals(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE, result.reason)
        assertTrue(payload.reads.none { it.startsWith("crun") }, "a corrupt tombstone status never reads payload")
    }

    // T3 — ACTIVE but the PAYLOAD read faults: a real backend error surfaces honestly (null != failure), NOT Missing.
    @Test
    fun t3_activeSubject_payloadReadFault_mapsToBackendError_notMissing() = runTest {
        val payload = ReadThrowingSecureKeyValueStore() // payload read throws
        val revocation = InMemorySecureKeyValueStore() // ACTIVE (empty)
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")

        val result = store.get("r1")

        assertIs<ReadResult.Failed>(result)
        assertEquals(StoreFailureReason.BACKEND_ERROR, result.reason)
    }

    // T4 — a POSITIVE tombstone with a physically surviving payload row: Missing (revoked), payload never read.
    @Test
    fun t4_revokedSubject_survivingPayload_returnsMissing_withoutReadingIt() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        assertEquals(SaveResult.Saved, store.save(run("r1")))
        SubjectRevocationAuthority(revocation).markRevoked("alice") // durable positive tombstone
        payload.reads.clear()

        assertEquals(ReadResult.Missing, store.get("r1"))
        assertTrue(payload.reads.none { it.startsWith("crun") }, "a revoked subject's payload row is never read")
    }

    // Channel-less list reads: REVOKED → empty, and STATUS_UNAVAILABLE → empty (breadcrumbed), never a leak.
    @Test
    fun listReads_areEmptyUnderRevokedAndStatusUnavailable() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = ControllableReadStore()
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        store.save(run("r1"))

        SubjectRevocationAuthority(revocation).markRevoked("alice")
        assertTrue(store.listRecent(10).isEmpty(), "REVOKED → empty list")
        assertTrue(store.listByBlock(ScoreModuleId.SPEED, 10).isEmpty())

        revocation.throwOnRead = true // now even the status is unavailable
        assertTrue(store.listRecent(10).isEmpty(), "STATUS_UNAVAILABLE → empty list, never a leak")
    }

    // T8 — STATUS_UNAVAILABLE on a WRITE: refused retryable, and the write block is never invoked (no put).
    @Test
    fun t8_statusUnavailableWrite_refusesRetryable_withoutWriting() = runTest {
        val payload = CountingSecureKeyValueStore()
        val revocation = ControllableReadStore(throwOnRead = true)
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")

        val result = store.save(run("r1"))

        assertIs<SaveResult.Failed>(result)
        assertEquals(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE, result.reason)
        assertTrue(payload.reads.isEmpty(), "the write block (saveRun) is never invoked under STATUS_UNAVAILABLE")
    }

    // REVOKED write: terminal SUBJECT_TOMBSTONED refusal (no resurrection).
    @Test
    fun revokedWrite_isTerminallyRefused() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val store = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        SubjectRevocationAuthority(revocation).markRevoked("alice")

        val result = store.save(run("r1"))

        assertIs<SaveResult.Failed>(result)
        assertEquals(StoreFailureReason.SUBJECT_TOMBSTONED, result.reason)
        assertEquals(ReadResult.Missing, store.get("r1"))
    }

    // T10 — CancellationException during the status read is RETHROWN, never mapped to a false status/result.
    @Test
    fun t10_cancellationDuringStatusRead_isRethrown_notSwallowed() = runTest {
        val store = SecureCognitiveSessionRunStore(
            InMemorySecureKeyValueStore(),
            ControllableReadStore(cancelOnRead = true),
            subject = "alice",
        )
        assertFailsWith<CancellationException> { store.get("r1") }
        assertFailsWith<CancellationException> { store.save(run("r1")) }
    }

    // reconcileIndices under STATUS_UNAVAILABLE must NOT report a false "reconciled" — retryable Failed, honest.
    @Test
    fun reconcileIndices_underStatusUnavailable_isRetryableFailed_notFalseReconciled() = runTest {
        val store = SecureCognitiveSessionRunStore(
            InMemorySecureKeyValueStore(),
            ControllableReadStore(throwOnRead = true),
            subject = "alice",
        )

        val result = store.reconcileIndices()

        assertIs<ReconcileResult.Failed>(result)
        assertEquals(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE, result.reason)
    }

    // T6 — the SAME authority (shared revocation backing) is consulted by a DIFFERENT store instance: a fresh
    // instance sees a prior instance's tombstone and refuses. This is the real holder-vs-eraser shape.
    @Test
    fun t6_aFreshInstance_sharingTheAuthority_seesThePriorTombstone_andRefuses() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore() // SHARED authority backing
        val instanceA = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        assertEquals(SaveResult.Saved, instanceA.save(run("r1")))
        assertIs<DeleteResult.Cleared>(instanceA.deleteAll())

        val instanceB = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        assertIs<SaveResult.Failed>(instanceB.save(run("r2")))
        assertEquals(ReadResult.Missing, instanceB.get("r2"))
    }

    // T7 — process-restart simulation: discard every store/authority instance, reconstruct over ONLY the durable
    // revocation backing. The tombstone must survive with NO dependency on the in-memory SubjectLocks/instance.
    @Test
    fun t7_processRestart_durableTombstoneSurvives_withoutInMemoryLockDependency() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val before = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        before.save(run("r1"))
        assertIs<DeleteResult.Cleared>(before.deleteAll())
        // `before` is discarded; ONLY `payload` + `revocation` (durable) persist a real restart.

        val after = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        assertIs<SaveResult.Failed>(after.save(run("r2")))
        assertEquals(ReadResult.Missing, after.get("r2"))
    }

    // T11 — hold-through-erase must not deadlock (non-reentrant Mutex): a concurrent same-subject guarded op and a
    // deleteAll both complete (runTest fails on an uncompleted coroutine); the survivor sees the tombstone.
    @Test
    fun t11_deletionAndConcurrentGuardedOp_completeWithoutDeadlock() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val eraser = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        val writer = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        eraser.save(run("r0"))

        val d = async { eraser.deleteAll() }
        val s = async { writer.save(run("r1")) }
        assertIs<DeleteResult.Cleared>(d.await())
        s.await() // the concurrent save either ran before revocation or was refused after — never a deadlock.
        assertEquals(ReadResult.Missing, writer.get("r1"), "no row resurrects after revocation")
    }

    // T12 — account A/B isolation over the SAME shared authority backing: revoking B leaves A fully ACTIVE.
    @Test
    fun t12_revokingSubjectB_doesNotRevokeOrReadOrDeleteSubjectA() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore() // SHARED
        val a = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        val b = SecureCognitiveSessionRunStore(payload, revocation, subject = "bob")
        assertEquals(SaveResult.Saved, a.save(run("a1")))
        assertEquals(SaveResult.Saved, b.save(run("b1")))

        assertIs<DeleteResult.Cleared>(b.deleteAll())

        assertEquals(ReadResult.Missing, b.get("b1"), "B is revoked + erased")
        assertIs<ReadResult.Found<*>>(a.get("a1")) // A is untouched — still ACTIVE, data intact
        assertEquals(SaveResult.IdempotentDuplicate, a.save(run("a1")), "A can still write (ACTIVE) with identical data")
    }

    // Two concurrent deleteAll() for the same subject stay idempotent; a different subject's row survives.
    @Test
    fun twoConcurrentDeleteAlls_areIdempotent_andDoNotTouchAnotherSubject() = runTest {
        val payload = InMemorySecureKeyValueStore()
        val revocation = InMemorySecureKeyValueStore()
        val a1 = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        val a2 = SecureCognitiveSessionRunStore(payload, revocation, subject = "alice")
        val bob = SecureCognitiveSessionRunStore(payload, revocation, subject = "bob")
        a1.save(run("a1"))
        bob.save(run("b1"))

        val r1 = async { a1.deleteAll() }
        val r2 = async { a2.deleteAll() }
        val cleared = listOf(r1.await(), r2.await()).filterIsInstance<DeleteResult.Cleared>()
        assertEquals(2, cleared.size, "both concurrent deleteAlls report Cleared (idempotent)")

        assertIs<ReadResult.Found<*>>(bob.get("b1"))
    }
}
