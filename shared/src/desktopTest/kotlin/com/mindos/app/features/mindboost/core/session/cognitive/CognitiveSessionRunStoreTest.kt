package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.testCognitiveRunStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 2 · Behavior of [CognitiveSessionRunStore]: roundtrip, immutable idempotency/conflict, ordered
 * listing via metadata indices, limit validation, index read-efficiency, resilience to orphan indices / corrupt
 * primaries / partial writes, full erasure, the no-userId-in-payload / no-sync-state guards, and (SYSTEM-1
 * slice 6-R3, P0 fix 6R3-P0C-01) cross-subject isolation over a SHARED physical [SecureKeyValueStore].
 */
class CognitiveSessionRunStoreTest {

    private companion object {
        /** A stable, non-blank, colon-free subject used by every test that doesn't itself test isolation. */
        const val TEST_SUBJECT = "test-subject-a"
        const val OTHER_SUBJECT = "test-subject-b"
    }

    private fun run(id: String, block: ScoreModuleId = ScoreModuleId.SPEED, completedAt: Long? = 121_000L, startedAt: Long = 1_000L) =
        CognitiveSessionRunFixtures.validRun(
            sessionId = id,
            blockId = block,
            startedAtEpochMs = startedAt,
            completedAtEpochMs = completedAt,
        )

    // 1
    @Test
    fun `save then get roundtrips the run`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        val r = run("sess_a")
        assertEquals(SaveResult.Saved, store.save(r))
        val found = assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get("sess_a"))
        assertEquals(r, found.value)
    }

    // 2
    @Test
    fun `re-saving the identical run is an idempotent success`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        val r = run("sess_a")
        store.save(r)
        assertEquals(SaveResult.IdempotentDuplicate, store.save(r))
        assertEquals(r, assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get("sess_a")).value)
    }

    // 3
    @Test
    fun `same runId with a different payload conflicts and never overwrites`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        val original = run("sess_a")
        store.save(original)
        val altered = original.copy(durationActualMs = original.durationActualMs + 1)
        assertIs<SaveResult.DuplicateRunConflict>(store.save(altered))
        assertEquals(original, assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get("sess_a")).value)
    }

    // 4
    @Test
    fun `listRecent returns most recent first by completedAt`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        store.save(run("sess_a", completedAt = 5_000L))
        store.save(run("sess_b", completedAt = 9_000L))
        store.save(run("sess_c", completedAt = 7_000L))
        assertEquals(listOf("sess_b", "sess_c", "sess_a"), store.listRecent(10).map { it.sessionId })
    }

    // 4b
    @Test
    fun `listRecent falls back to startedAt when completedAt is null`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        store.save(run("sess_a", completedAt = 5_000L, startedAt = 1_000L))
        store.save(run("sess_b", completedAt = 9_000L, startedAt = 1_000L))
        store.save(
            CognitiveSessionRunFixtures.validRun(
                sessionId = "sess_d",
                completionStatus = SessionCompletionStatus.INVALID_LIFECYCLE,
                completedAtEpochMs = null,
                startedAtEpochMs = 8_000L,
                anomalyFlags = listOf("recovery_corrupt"),
            ),
        )
        assertEquals(listOf("sess_b", "sess_d", "sess_a"), store.listRecent(10).map { it.sessionId })
    }

    // 5
    @Test
    fun `listByBlock returns only that block, ordered`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        store.save(run("sess_s1", block = ScoreModuleId.SPEED, completedAt = 121_000L))
        store.save(run("sess_s2", block = ScoreModuleId.SPEED, completedAt = 130_000L))
        store.save(run("sess_m1", block = ScoreModuleId.MEMORY))
        assertEquals(listOf("sess_s2", "sess_s1"), store.listByBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
        assertEquals(listOf("sess_m1"), store.listByBlock(ScoreModuleId.MEMORY, 10).map { it.sessionId })
    }

    // 6
    @Test
    fun `limit is validated`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        store.save(run("sess_a"))
        assertTrue(store.listRecent(0).isEmpty())
        assertTrue(store.listRecent(-5).isEmpty())
        assertEquals(1, store.listRecent(1_000_000).size) // excessive limit is capped, returns all available
    }

    // 7
    @Test
    fun `listByBlock reads only the block index and its primaries`() = runTest {
        val kv = CountingSecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_s1", block = ScoreModuleId.SPEED))
        store.save(run("sess_m1", block = ScoreModuleId.MEMORY))
        kv.reads.clear()
        store.listByBlock(ScoreModuleId.SPEED, 10)
        assertTrue(kv.reads.contains("crun_idx_block:$TEST_SUBJECT:SPEED"))
        assertTrue(kv.reads.contains("crun:$TEST_SUBJECT:sess_s1"))
        assertFalse(kv.reads.contains("crun:$TEST_SUBJECT:sess_m1")) // never touches the other block's primary
    }

    // 8
    @Test
    fun `an orphan index entry does not crash a list`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_x"))
        kv.remove("crun:$TEST_SUBJECT:sess_x") // primary gone, index still points at it
        assertTrue(store.listRecent(10).isEmpty())
        assertEquals(ReadResult.Missing, store.get("sess_x"))
    }

    // 9
    @Test
    fun `a primary written without its index reports repair-needed and reconcile fixes it`() = runTest {
        val kv = PutFailingSecureKeyValueStore(failPrefix = "crun_idx")
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        assertEquals(SaveResult.SavedIndexRepairNeeded, store.save(run("sess_y"))) // primary durable; index failed
        assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get("sess_y"))
        assertTrue(store.listRecent(10).isEmpty()) // not indexed yet
        kv.active = false
        assertEquals(ReconcileResult.Reconciled(1), store.reconcileIndices())
        assertEquals(listOf("sess_y"), store.listRecent(10).map { it.sessionId })
    }

    // 10
    @Test
    fun `a corrupt primary does not crash get or list`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_z"))
        kv.put("crun:$TEST_SUBJECT:sess_z", "{ not valid json")
        assertEquals(ReadResult.Corrupt("sess_z"), store.get("sess_z"))
        assertTrue(store.listRecent(10).isEmpty()) // index entry present, primary corrupt -> skipped
    }

    // 11
    @Test
    fun `deleteAll clears primaries and indices`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_a"))
        store.save(run("sess_b", block = ScoreModuleId.MEMORY))
        assertEquals(DeleteResult.Cleared(2), store.deleteAll())
        // SYSTEM-1 slice 6-R3 (§18 C1): deleteAll's own legacy-purge gate (its first statement) leaves the
        // reserved, device-wide `legacy_purge_marker:v1` key behind — it is infra, not a subject's own row, and
        // must survive account deletion (see LegacyNamespacePurge / SecureNamespaceUserDataEraser).
        // SYSTEM-1 slice 6-R3 (P2-01): the PERMANENT per-subject revocation tombstone now lives in the DEDICATED
        // `mindboost_revocation` namespace (SubjectRevocationAuthority), NOT in this data namespace — so after a
        // deleteAll NO row bearing this subject remains in the data backing at all.
        val survivingSubjectKeys = kv.keys("").filter { it.contains(TEST_SUBJECT) }
        assertTrue(survivingSubjectKeys.isEmpty(), "no data row of the deleted subject remains in this namespace")
        assertTrue(store.listRecent(10).isEmpty())
    }

    // 12
    @Test
    fun `no serialized field carries a userId (only the key is subject-scoped)`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_a"))
        val allKeys = kv.keys("")
        // The KEY legitimately carries the subject (that IS the P0 fix, 6R3-P0C-01) — but never the literal
        // "userId" and never the "::" convention used by OTHER repos' key formats.
        assertTrue(allKeys.none { it.contains("userId") })
        assertTrue(allKeys.none { it.contains("::") })
        assertTrue(allKeys.all { it.contains(TEST_SUBJECT) }, "every key must be scoped to the bound subject")
        assertFalse(encodeRun(cognitiveRunJson, run("sess_a")).contains("userId"))
    }

    // 12b (6R3-P0C-01 regression): two subjects sharing the SAME physical backend never see each other's rows.
    @Test
    fun `two subjects sharing the same backend cannot read each other's runs`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(kv, TEST_SUBJECT)
        val storeB = testCognitiveRunStore(kv, OTHER_SUBJECT)

        storeA.save(run("sess_a1", block = ScoreModuleId.SPEED))
        storeB.save(run("sess_b1", block = ScoreModuleId.SPEED))

        assertEquals(listOf("sess_a1"), storeA.listRecent(10).map { it.sessionId })
        assertEquals(listOf("sess_b1"), storeB.listRecent(10).map { it.sessionId })
        assertEquals(listOf("sess_a1"), storeA.listByBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
        assertEquals(listOf("sess_b1"), storeB.listByBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
        assertEquals(ReadResult.Missing, storeA.get("sess_b1"), "A must not be able to read B's run by id")
        assertEquals(ReadResult.Missing, storeB.get("sess_a1"), "B must not be able to read A's run by id")
    }

    // 12c (6R3-P0C-01 regression): the SAME runId can be used independently by two subjects without collision.
    @Test
    fun `two subjects may independently save a run under the same runId without conflict`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(kv, TEST_SUBJECT)
        val storeB = testCognitiveRunStore(kv, OTHER_SUBJECT)
        val runA = run("shared_id")
        val runB = run("shared_id").copy(durationActualMs = runA.durationActualMs + 1)

        assertEquals(SaveResult.Saved, storeA.save(runA))
        assertEquals(SaveResult.Saved, storeB.save(runB), "same runId under a DIFFERENT subject is not a conflict")
        assertEquals(runA, assertIs<ReadResult.Found<CognitiveSessionRun>>(storeA.get("shared_id")).value)
        assertEquals(runB, assertIs<ReadResult.Found<CognitiveSessionRun>>(storeB.get("shared_id")).value)
    }

    // 12d (6R3-P0C-01 regression — the account-deletion collateral-wipe half of the finding): deleteAll on one
    // subject's store must never destroy a DIFFERENT subject's rows sharing the same physical backend.
    @Test
    fun `deleteAll erases only the bound subject's rows, never another subject sharing the backend`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(kv, TEST_SUBJECT)
        val storeB = testCognitiveRunStore(kv, OTHER_SUBJECT)
        storeA.save(run("sess_a1", block = ScoreModuleId.SPEED))
        storeA.save(run("sess_a2", block = ScoreModuleId.MEMORY))
        storeB.save(run("sess_b1", block = ScoreModuleId.SPEED))

        val result = storeA.deleteAll()

        assertEquals(DeleteResult.Cleared(2), result, "deleteAll reports only ITS OWN subject's primary count")
        assertTrue(storeA.listRecent(10).isEmpty(), "the deleting subject's own rows are gone")
        assertEquals(
            listOf("sess_b1"),
            storeB.listRecent(10).map { it.sessionId },
            "a DIFFERENT subject's rows survive the other subject's deleteAll — conservation, never collateral wipe",
        )
        assertEquals(listOf("sess_b1"), storeB.listByBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
    }

    // 11b (P2-01, GDPR fail-closed): deleteAll must attempt EVERY key even when one remove faults mid-loop —
    // never abort after the first failure and leave the rest of the deleted subject's rows behind. It reports
    // Failed, but only the single faulting key survives; every other primary + index was still erased.
    @Test
    fun `deleteAll attempts every key even when one remove faults, then reports Failed`() = runTest {
        // Fault on a PRIMARY key — it precedes the two block indices AND the recent index in the erasure order, so
        // the OLD abort-on-first-throw `forEach` would have left ALL of those behind. Asserting the faulting key is
        // the SOLE survivor therefore genuinely discriminates the best-effort-total fix from the early-abort bug.
        val faultKey = "crun:$TEST_SUBJECT:sess_a"
        val kv = RemoveFailingSecureKeyValueStore(failOn = { it == faultKey })
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_a", block = ScoreModuleId.SPEED))
        store.save(run("sess_b", block = ScoreModuleId.MEMORY))

        assertEquals(DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR), store.deleteAll())

        // SYSTEM-1 slice 6-R3 (P2-01): the revocation tombstone now lives in the dedicated `mindboost_revocation`
        // namespace, so the SOLE survivor in this data namespace is the single faulting key.
        val survivors = kv.keys("").filter { it.contains(TEST_SUBJECT) }
        assertEquals(
            setOf(faultKey),
            survivors.toSet(),
            "only the faulting key remains; every other row of the deleted subject was still erased (no early abort)",
        )
    }

    // 11c (P2-01): a structured CancellationException raised mid-delete must PROPAGATE, never be swallowed into a
    // typed Failed (which would report a clean-ish outcome for a coroutine that was actually cancelled).
    @Test
    fun `deleteAll propagates CancellationException instead of swallowing it to Failed`() = runTest {
        val kv = RemoveFailingSecureKeyValueStore(failOn = { it.startsWith("crun:$TEST_SUBJECT:") }, cancel = true)
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_a"))
        assertFailsWith<CancellationException> { store.deleteAll() }
    }

    // 11d (P2-01): a runId that itself contains the key delimiter round-trips, lists, and deletes correctly. The
    // subject is delimiter-free (validated), so the FIRST colon after the subject unambiguously ends the subject
    // segment; any colons in the runId are just the trailing segment — isolation never relies on counting colons.
    @Test
    fun `a runId containing the key delimiter round-trips, lists, and fully deletes`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        val weirdId = "sess:with:colons"
        store.save(run(weirdId))
        assertEquals(weirdId, assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get(weirdId)).value.sessionId)
        assertEquals(listOf(weirdId), store.listRecent(10).map { it.sessionId })
        assertEquals(DeleteResult.Cleared(1), store.deleteAll())
        assertEquals(ReadResult.Missing, store.get(weirdId))
    }

    // 11e (P2-01): a non-ASCII (Unicode) subject is a valid scope — non-blank and colon-free — and stays isolated
    // from an ASCII subject sharing the same physical backend.
    @Test
    fun `a Unicode subject is accepted, scoped, and isolated from an ASCII subject`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val storeU = testCognitiveRunStore(kv, "用户-Ω-🧠") // 用户-Ω-🧠
        val storeA = testCognitiveRunStore(kv, TEST_SUBJECT)
        storeU.save(run("sess_u"))
        storeA.save(run("sess_a"))
        assertEquals(listOf("sess_u"), storeU.listRecent(10).map { it.sessionId })
        assertEquals(ReadResult.Missing, storeA.get("sess_u"), "an ASCII subject cannot read the Unicode subject's run")
        assertEquals(DeleteResult.Cleared(1), storeU.deleteAll())
        assertEquals(listOf("sess_a"), storeA.listRecent(10).map { it.sessionId }, "the ASCII subject's row survives")
    }

    // 12e: a blank subject is rejected fail-closed (mirrors SubjectScopedSessionBatch.of's contract).
    @Test
    fun `a blank subject is rejected at construction`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            testCognitiveRunStore(InMemorySecureKeyValueStore(), "   ")
        }
    }

    // 12f: a subject containing the key-prefix delimiter is rejected (would let one subject's prefix match
    // another's keys, e.g. subject "abc" vs "abc:def" — see SecureCognitiveSessionRunStore's KDoc).
    @Test
    fun `a subject containing the key delimiter is rejected at construction`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            testCognitiveRunStore(InMemorySecureKeyValueStore(), "abc:def")
        }
    }

    // 13
    @Test
    fun `the serialized run carries no sync or outbox state`() {
        val raw = encodeRun(cognitiveRunJson, CognitiveSessionRunFixtures.validRun())
        for (forbidden in listOf("syncStatus", "retryCount", "pending", "lastErrorCode", "idempotencyKey", "userId")) {
            assertFalse(raw.contains(forbidden), "run JSON must not contain '$forbidden'")
        }
    }

    // 14
    @Test
    fun `InMemory and Secure stores behave identically`() = runTest {
        val inMem = InMemoryCognitiveSessionRunStore()
        val secure = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        val runs = listOf(run("sess_a", completedAt = 5_000L), run("sess_b", completedAt = 9_000L))
        for (store in listOf<CognitiveSessionRunStore>(inMem, secure)) {
            runs.forEach { store.save(it) }
        }
        assertEquals(inMem.listRecent(10).map { it.sessionId }, secure.listRecent(10).map { it.sessionId })
        assertEquals(inMem.save(runs[0]), secure.save(runs[0])) // both IdempotentDuplicate
    }

    // 15
    @Test
    fun `serialization roundtrips a full run losslessly`() {
        val full = CognitiveSessionRunFixtures.validRun(
            exerciseMetrics = CognitiveSessionRunFixtures.exercisePayload(),
            calibrationEffect = CalibrationEffect(countedTowardCalibration = true, validSessionIndexAtCompletion = 3, calibrationRuleVersion = "cal-1"),
            platform = PlatformSnapshot(osName = "Android", osVersion = "34", appVersion = "1.0", deviceTier = "mid"),
            anomalyFlags = listOf("write_ahead_missing"),
            score = CognitiveSessionRunFixtures.validScore().copy(normalized = null, category = com.mindos.app.features.mindboost.core.scoring.ScoreState.CALIBRATING),
            metrics = CognitiveSessionRunFixtures.validMetrics().copy(accuracy = SessionMetric.Unavailable(MetricUnavailableReason.INSUFFICIENT_SAMPLE)),
        )
        assertEquals(full, decodeRun(cognitiveRunJson, encodeRun(cognitiveRunJson, full)))
    }

    // extra: primary backend write failure surfaces as Failed
    @Test
    fun `a primary write failure yields SaveResult Failed`() = runTest {
        val kv = PutFailingSecureKeyValueStore(failPrefix = "crun:")
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        assertEquals(SaveResult.Failed(StoreFailureReason.BACKEND_ERROR), store.save(run("sess_a")))
    }

    // extra: a corrupt existing primary conflicts and is not overwritten
    @Test
    fun `saving over a corrupt existing primary conflicts without overwriting`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        kv.put("crun:$TEST_SUBJECT:sess_c", "garbage")
        assertIs<SaveResult.DuplicateRunConflict>(store.save(run("sess_c")))
        val raw = kv.read("crun:$TEST_SUBJECT:sess_c")
        assertTrue(raw is SecureReadResult.Value && raw.value == "garbage")
    }

    // micro-patch: a KV-layer read Failure (existing but unreadable row) maps to Corrupt, not Failed
    @Test
    fun `a KV-layer read failure maps to Corrupt`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_x"))
        kv.simulateReadFailure("crun:$TEST_SUBJECT:sess_x", SecureReadResult.Corrupt)
        assertEquals(ReadResult.Corrupt("sess_x"), store.get("sess_x"))
    }

    // micro-patch: a thrown backend read is a transient fault -> Failed, never conflated with corruption
    @Test
    fun `a backend read fault maps to Failed`() = runTest {
        val store = testCognitiveRunStore(ReadThrowingSecureKeyValueStore(), TEST_SUBJECT)
        assertEquals(ReadResult.Failed(StoreFailureReason.BACKEND_ERROR), store.get("sess_x"))
    }

    // micro-patch: an unknown enum name is fail-closed -> Corrupt (not silently accepted)
    @Test
    fun `an unknown enum value decodes as Corrupt`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        val tampered = encodeRun(cognitiveRunJson, run("sess_x"))
            .replace("\"blockId\":\"SPEED\"", "\"blockId\":\"BOGUS\"")
        kv.put("crun:$TEST_SUBJECT:sess_x", tampered)
        assertEquals(ReadResult.Corrupt("sess_x"), store.get("sess_x"))
    }

    // micro-patch: reconcile never throws from the repair path; a backend fault yields a typed Failed
    @Test
    fun `reconcile returns Failed without throwing when the backend fails`() = runTest {
        val kv = PutFailingSecureKeyValueStore(failPrefix = "crun_idx")
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_z")) // primary ok, index write fails -> SavedIndexRepairNeeded
        assertEquals(ReconcileResult.Failed(StoreFailureReason.BACKEND_ERROR), store.reconcileIndices())
    }

    // ---- SYSTEM-1 slice 6-R3, P2-01 (erasure race: permanent per-subject tombstone + per-subject lock) ----

    // 16a: a subject's own tombstone never blocks a DIFFERENT subject sharing the same physical backend.
    @Test
    fun `a subject's tombstone never blocks a DIFFERENT subject sharing the same backend`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(kv, TEST_SUBJECT)
        val storeB = testCognitiveRunStore(kv, OTHER_SUBJECT)
        storeA.save(run("sess_a"))
        storeA.deleteAll() // tombstones ONLY TEST_SUBJECT

        assertEquals(SaveResult.Saved, storeB.save(run("sess_b")), "B's subject was never revoked")
        assertEquals(listOf("sess_b"), storeB.listRecent(10).map { it.sessionId })
    }

    // 16b: a save for a subject already erased via its OWN deleteAll is refused, never resurrecting a row.
    @Test
    fun `save is refused with SUBJECT_TOMBSTONED after the subject's own deleteAll`() = runTest {
        val store = testCognitiveRunStore(InMemorySecureKeyValueStore(), TEST_SUBJECT)
        store.save(run("sess_a"))
        assertEquals(DeleteResult.Cleared(1), store.deleteAll())

        assertEquals(SaveResult.Failed(StoreFailureReason.SUBJECT_TOMBSTONED), store.save(run("sess_new")))
        assertEquals(ReadResult.Missing, store.get("sess_new"))
    }

    // 16c (THE CORE P0 REGRESSION, R3-AUD-P0-01): deleteAll must never run ANY part of its own body while an
    // in-flight save for the SAME subject is still mid-write, holding the per-subject lock. This test FAILS
    // against pre-guard code: without a shared lock, deleteAll's `keys(primaryPrefix)` scan would run to
    // completion WHILE save's write is still gated/paused (seeing nothing yet, reporting Cleared(0)); releasing
    // the gate would then let save's write land afterward -- resurrecting "sess_new" after erasure already
    // reported success. With the guard, deleteAll cannot even begin until save's lock fully releases, so it
    // always sees (and erases) whatever save actually persisted.
    @Test
    fun `deleteAll cannot interleave partway through an in-flight save for the same subject`() = runTest {
        val kv = GatedPutSecureKeyValueStore(gateOnKeyPrefix = "crun:$TEST_SUBJECT:")
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)

        val saveJob = async { store.save(run("sess_new")) }
        kv.awaitPutEntered() // save() is now suspended mid-write, still holding the per-subject lock
        val deleteJob = async { store.deleteAll() }
        kv.releasePut() // only NOW can save() finish -- and only then can deleteAll's own body even begin

        assertEquals(SaveResult.Saved, saveJob.await())
        assertEquals(
            DeleteResult.Cleared(1),
            deleteJob.await(),
            "deleteAll only runs after save fully released the lock, so it sees (and erases) the just-saved row",
        )
        assertEquals(ReadResult.Missing, store.get("sess_new"), "no row resurrects after the subject's own deleteAll")
    }

    // 16d: the tombstone write itself is the FIRST durable action inside deleteAll's lock -- if it fails, the
    // erasure aborts with Failed and never proceeds to erase anything unmarked.
    @Test
    fun `deleteAll aborts with Failed, never erasing unmarked, when the tombstone write itself fails`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        // The tombstone lives in the dedicated revocation backing now: fail ITS put so markRevoked fails.
        val store = testCognitiveRunStore(
            kv,
            TEST_SUBJECT,
            revocationStore = PutFailingSecureKeyValueStore(failPrefix = "revocation:"),
        )
        store.save(run("sess_a"))

        assertEquals(DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR), store.deleteAll())
        assertIs<ReadResult.Found<CognitiveSessionRun>>(
            store.get("sess_a"),
            "no erasure happened -- the tombstone write failed first, before the removal loop ran",
        )
    }

    // 16e (P2-01 triadic): a faulting revocation-status read is STATUS_UNAVAILABLE — the write is refused
    // RETRYABLE and never persisted, NEVER treated as a positive revocation (that would be the fail-open/closed
    // conflation the triadic contract removes).
    @Test
    fun `save is refused STATUS_UNAVAILABLE when the tombstone read faults -- retryable, never written`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(kv, TEST_SUBJECT, revocationStore = ControllableReadStore(throwOnRead = true))

        assertEquals(SaveResult.Failed(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE), store.save(run("sess_a")))
        assertTrue(kv.keys("crun:$TEST_SUBJECT:").isEmpty(), "the write is refused (retryable); nothing persisted")
    }

    // 16f (R3-AUD-P1-02): a backend whose remove() call returns without throwing yet does not durably delete the
    // key must still be caught by the post-delete physical re-scan -- never a false Cleared.
    @Test
    fun `deleteAll downgrades to Failed when a post-delete rescan still finds a physical row`() = runTest {
        val ghostKey = "crun:$TEST_SUBJECT:sess_ghost"
        val kv = GhostRemoveSecureKeyValueStore(ghostKey)
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_ghost"))

        assertEquals(DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR), store.deleteAll())
    }

    // 16g (R3-AUD-P1-02 / P2-01 review F1): the singleton recent-index key is one of the THREE key categories
    // deleteAllRuns removes, but -- unlike primaries/block-indices -- it is a single, non-prefix-scanned key. A
    // ghost-remove there must ALSO be caught by the post-delete re-scan (exact read), never a false Cleared while
    // that per-subject metadata row (runId/blockId/timestamp) physically survives. DISCRIMINATES the fix: with only
    // the recent-index key ghosted, primaries + block indices are truly gone, so the OLD two-prefix-only re-scan
    // returned Cleared; the exact-read fix returns Failed.
    @Test
    fun `deleteAll downgrades to Failed when only the recent-index row ghost-survives the delete`() = runTest {
        val recentIndexKey = "crun_idx_recent:$TEST_SUBJECT"
        val kv = GhostRemoveSecureKeyValueStore(recentIndexKey)
        val store = testCognitiveRunStore(kv, TEST_SUBJECT)
        store.save(run("sess_a"))

        assertEquals(DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR), store.deleteAll())
    }

    // 16h (P2-01 triadic): a genuinely-THROWING revocation-status read (the iOS KeyValueBackend contract throws on
    // a real fault, rather than returning SecureReadResult.Failure) must be caught by statusOf and mapped to
    // STATUS_UNAVAILABLE — save refused retryable, NEVER an uncaught exception escaping into the NonCancellable seal.
    @Test
    fun `save is refused STATUS_UNAVAILABLE when the tombstone read throws a genuine backend fault`() = runTest {
        val store = testCognitiveRunStore(
            InMemorySecureKeyValueStore(),
            TEST_SUBJECT,
            revocationStore = ReadThrowingSecureKeyValueStore(),
        )

        assertEquals(SaveResult.Failed(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE), store.save(run("sess_a")))
    }
}
