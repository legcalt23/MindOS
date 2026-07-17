package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.GatedPutSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.GhostRemoveSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.PutFailingSecureKeyValueStore
import com.mindos.app.features.mindboost.core.session.cognitive.RemoveFailingSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import com.mindos.app.features.mindboost.core.session.cognitive.ControllableReadStore
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 2.1 SessionRecord persistence: round-trip parity (in-memory vs secure), write-ahead upsert semantics,
 * ordering, deletion, the minimization canary (serialized shape has ONLY allowlisted keys), and fail-closed
 * reads (future schemaVersion / unknown enum quarantined; unknown extra field tolerated).
 *
 * SYSTEM-1 slice 6-R3 (§14 fold-in): [SecureSessionRecordStore] now takes a mandatory `subject`. Tests below use
 * a fixed test subject ("u1") for the pre-existing single-subject coverage, plus a dedicated cross-account
 * section proving two different subjects sharing the SAME backing store can never read/list/delete/deleteAll
 * one another's rows.
 */
class SessionRecordStoreTest {

    private fun record(
        sessionId: String = "s1",
        block: ScoreModuleId = ScoreModuleId.FOCUS,
        status: SessionStatus = SessionStatus.STARTED,
        startedAtMs: Long = 1_000L,
        updatedAtMs: Long = 1_000L,
        completedTrials: Int = 0,
        plannedTrials: Int = 20,
        activeDurationMs: Long = 0L,
    ) = SessionRecord(
        sessionId, block, status, startedAtMs, updatedAtMs, completedTrials, plannedTrials, activeDurationMs,
    )

    @Test
    fun inMemory_roundTrip() = runTest {
        val store = InMemorySessionRecordStore()
        val r = record(status = SessionStatus.FINALIZED, completedTrials = 20, activeDurationMs = 90_000L)
        store.upsert(r)
        assertEquals(r, store.find("s1"))
        assertEquals(listOf(r), store.all())
    }

    @Test
    fun secure_roundTrip() = runTest {
        val store = testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "u1")
        val r = record(block = ScoreModuleId.EPISODIC_MEMORY, status = SessionStatus.FINALIZED, completedTrials = 12)
        store.upsert(r)
        assertEquals(r, store.find("s1"))
        assertEquals(listOf(r), store.all())
    }

    @Test
    fun upsert_isWriteAhead_sameSessionUpdatesInPlace() = runTest {
        val store = testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "u1")
        store.upsert(record(status = SessionStatus.STARTED, updatedAtMs = 1_000L))
        store.upsert(record(status = SessionStatus.CHECKPOINT, updatedAtMs = 2_000L, completedTrials = 10))
        store.upsert(record(status = SessionStatus.FINALIZED, updatedAtMs = 3_000L, completedTrials = 20))
        val all = store.all()
        assertEquals(1, all.size)
        assertEquals(SessionStatus.FINALIZED, all.single().status)
        assertEquals(20, all.single().completedTrials)
    }

    @Test
    fun all_ordersByStartedAtDescThenId() = runTest {
        val store = testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "u1")
        store.upsert(record(sessionId = "a", startedAtMs = 100L))
        store.upsert(record(sessionId = "b", startedAtMs = 300L))
        store.upsert(record(sessionId = "c", startedAtMs = 300L))
        assertEquals(listOf("b", "c", "a"), store.all().map { it.sessionId })
    }

    @Test
    fun delete_removesRow() = runTest {
        val store = testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "u1")
        store.upsert(record())
        store.delete("s1")
        assertNull(store.find("s1"))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun serializedForm_hasSchemaVersion_andOnlyAllowlistedKeys() = runTest {
        val backing = InMemorySecureKeyValueStore()
        testSessionRecordStore(backing, subject = "u1").upsert(record(status = SessionStatus.FINALIZED))
        val key = backing.keys("").single()
        val raw = (backing.read(key) as SecureReadResult.Value).value
        val keys = Json.parseToJsonElement(raw).jsonObject.keys
        assertEquals(
            setOf(
                "schemaVersion", "sessionId", "block", "status",
                "startedAtMs", "updatedAtMs", "completedTrials", "plannedTrials", "activeDurationMs",
                // ENG-01, schema 2 — a DELIBERATE addition to the minimization allowlist, admitted only after
                // stating what it carries: versions, a seed, and the effective difficulty. It carries no
                // responses, no trials, no timings, no device data and no free text, so it cannot describe what
                // the user did — only what they were shown. That is the point: without it, the form of every
                // session ever run is unrecoverable, and nothing about what a user was presented can be audited.
                // It rides the same per-user, account-bound, encrypted namespace and is erased with the account.
                "reproducibleIdentity",
            ),
            keys,
            "SessionRecord serialized shape must stay minimized (no raw trials / free text); a new field " +
                "requires a privacy review.",
        )
    }

    /**
     * The canary above guards ONE level: it admits `reproducibleIdentity` as a single opaque key and would not
     * notice a field added INSIDE it. A privacy review flagged exactly that gap — a device id or a free-text
     * note could be added to [ReproducibleSessionIdentity] and the outer canary would stay green.
     *
     * The identity now nests one level deeper again (`descriptor`), so BOTH levels are pinned. A canary that
     * stops at the first object it does not recognise is a canary that stops working the moment the model grows,
     * which is exactly when it is needed.
     */
    @Test
    fun reproducibleIdentity_serializedShape_hasOnlyAllowlistedKeys() = runTest {
        val backing = InMemorySecureKeyValueStore()
        testSessionRecordStore(backing, subject = "u1").upsert(
            record(status = SessionStatus.STARTED).copy(reproducibleIdentity = testIdentity()),
        )
        val raw = (backing.read(backing.keys("").single()) as SecureReadResult.Value).value
        val identity = Json.parseToJsonElement(raw).jsonObject["reproducibleIdentity"]!!.jsonObject
        assertEquals(
            setOf(
                "replaySchemaVersion", "canonicalizerVersionId", "digestAlgorithmId", "descriptor",
                "formDefinitionId", "generatedPlanDigest",
            ),
            identity.keys,
            "ReproducibleSessionIdentity must carry ONLY form-reproduction inputs and the digests derived from " +
                "them. A new field here requires a privacy review exactly as one on SessionRecord does.",
        )
        assertEquals(
            setOf(
                "seedHex", "rngVersionId", "engineContractVersion", "generatorVersion", "exerciseId",
                "exerciseVersion", "difficultyMappingVersion", "resolvedDifficulty", "sessionTemplateVersion",
                "plannedTrialCount", "resolvedTimingPolicy", "responseMappingVersion", "contentVersion",
            ),
            identity["descriptor"]!!.jsonObject.keys,
            "FormDescriptorV1 must carry ONLY what reproduces the form: no responses, no trials, no timings of " +
                "what the user DID, no device data, no free text. resolvedDifficulty is admitted deliberately " +
                "and is the one field that infers anything about the user.",
        )
    }

    /** A valid identity: the digest is computed, never supplied, so this cannot be an incoherent fixture. */
    private fun testIdentity(): ReproducibleSessionIdentity = ReproducibleSessionIdentity.of(
        descriptor = FormDescriptorV1(
            seedHex = "00000000000000ff",
            rngVersionId = "splitmix64-v1",
            engineContractVersion = 1,
            generatorVersion = 2,
            exerciseId = "E2_FILTRO",
            exerciseVersion = 1,
            difficultyMappingVersion = 1,
            resolvedDifficulty = listOf(AxisValueV1("INTERFERENCE", 0.0)),
            sessionTemplateVersion = 1,
            plannedTrialCount = 40,
            resolvedTimingPolicy = listOf(TimingValueV1("isiMs", 700)),
            responseMappingVersion = 1,
            contentVersion = 1,
        ),
        generatedPlanDigest = "0".repeat(64),
    )

    @Test
    fun futureSchemaVersion_isQuarantinedNotMisread() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:u1:s1",
            """{"schemaVersion":999,"sessionId":"s1","block":"FOCUS","status":"STARTED","startedAtMs":1,"updatedAtMs":1,"completedTrials":0,"plannedTrials":20,"activeDurationMs":0}""",
        )
        assertNull(testSessionRecordStore(backing, subject = "u1").find("s1"))
    }

    @Test
    fun unknownBlock_isSkipped() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:u1:s1",
            """{"schemaVersion":1,"sessionId":"s1","block":"NOT_A_BLOCK","status":"STARTED","startedAtMs":1,"updatedAtMs":1,"completedTrials":0,"plannedTrials":20,"activeDurationMs":0}""",
        )
        assertNull(testSessionRecordStore(backing, subject = "u1").find("s1"))
    }

    @Test
    fun unknownExtraField_isIgnored() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:u1:s1",
            """{"schemaVersion":1,"sessionId":"s1","block":"LOGIC","status":"FINALIZED","startedAtMs":5,"updatedAtMs":6,"completedTrials":8,"plannedTrials":8,"activeDurationMs":120,"future":42}""",
        )
        val found = testSessionRecordStore(backing, subject = "u1").find("s1")
        assertEquals(ScoreModuleId.LOGIC, found?.block)
        assertEquals(SessionStatus.FINALIZED, found?.status)
    }

    // ---- subject validation (SYSTEM-1 slice 6-R3, §14 fold-in) ----

    @Test
    fun blankSubject_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "")
        }
    }

    @Test
    fun subjectContainingDelimiter_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            testSessionRecordStore(InMemorySecureKeyValueStore(), subject = "abc:def")
        }
    }

    // ---- cross-account isolation (SYSTEM-1 slice 6-R3, §14 fold-in — the P1 this slice fixes) ----

    @Test
    fun differentSubjects_sharingBackingStore_cannotReadEachOthersRecords() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record(sessionId = "s1", block = ScoreModuleId.FOCUS))

        assertEquals(SessionStatus.STARTED, alice.find("s1")?.status)
        assertNull(bob.find("s1"), "bob must not be able to read alice's session by id")
    }

    @Test
    fun differentSubjects_sharingBackingStore_allIsScopedToOwnSubjectOnly() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record(sessionId = "a1"))
        alice.upsert(record(sessionId = "a2"))
        bob.upsert(record(sessionId = "b1"))

        assertEquals(setOf("a1", "a2"), alice.all().map { it.sessionId }.toSet())
        assertEquals(setOf("b1"), bob.all().map { it.sessionId }.toSet())
    }

    @Test
    fun differentSubjects_sharingBackingStore_deleteNeverTouchesTheOtherSubject() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record(sessionId = "s1"))
        bob.upsert(record(sessionId = "s1")) // same sessionId, DIFFERENT subject — must not collide

        alice.delete("s1")
        assertNull(alice.find("s1"))
        assertEquals(SessionStatus.STARTED, bob.find("s1")?.status, "bob's row with the SAME sessionId must survive")
    }

    @Test
    fun deleteAll_removesOnlyTheOwningSubjectsRows() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record(sessionId = "a1"))
        alice.upsert(record(sessionId = "a2"))
        bob.upsert(record(sessionId = "b1"))

        val result = alice.deleteAll()
        assertEquals(SessionRecordDeleteResult.Cleared(2), result)
        assertTrue(alice.all().isEmpty(), "alice's own rows must be gone")
        assertEquals(setOf("b1"), bob.all().map { it.sessionId }.toSet(), "bob's rows must be UNTOUCHED")
    }

    @Test
    fun subjectPrefixCannotCollideWithASimilarlyNamedSubject() = runTest {
        // "abc" vs "abcd" would collide under naive prefix matching without the trailing delimiter.
        val backing = InMemorySecureKeyValueStore()
        val abc = testSessionRecordStore(backing, subject = "abc")
        val abcd = testSessionRecordStore(backing, subject = "abcd")
        abc.upsert(record(sessionId = "s1"))
        abcd.upsert(record(sessionId = "s2"))

        assertEquals(listOf("s1"), abc.all().map { it.sessionId })
        assertEquals(listOf("s2"), abcd.all().map { it.sessionId })
    }

    // ---- SYSTEM-1 slice 6-R3, P2-01 (erasure race: permanent per-subject tombstone + per-subject lock) ----

    @Test
    fun aSubjectsTombstone_neverBlocksADifferentSubject_sharingTheSameBackend() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record(sessionId = "a1"))
        alice.deleteAll() // tombstones ONLY alice

        bob.upsert(record(sessionId = "b1"))
        assertEquals(SessionStatus.STARTED, bob.find("b1")?.status, "bob's subject was never revoked")
    }

    @Test
    fun upsert_isSilentlyRefused_afterTheSubjectsOwnDeleteAll() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        alice.upsert(record(sessionId = "a1"))
        assertEquals(SessionRecordDeleteResult.Cleared(1), alice.deleteAll())

        alice.upsert(record(sessionId = "a2")) // must never resurrect a row for an already-erased subject

        assertNull(alice.find("a2"), "a write for an already-tombstoned subject must never persist")
        assertTrue(alice.all().isEmpty())
    }

    // THE CORE P0 REGRESSION (R3-AUD-P0-01), mirrored for SessionRecordStore: deleteAll must never run ANY part
    // of its own body while an in-flight upsert for the SAME subject is still mid-write, holding the per-subject
    // lock. Fails against pre-guard code for the same reason as the cognitive-store equivalent: without a shared
    // lock, deleteAll's own keys() scan could complete (seeing nothing yet) WHILE upsert's write is still gated,
    // then have that write land afterward -- a resurrected row surviving "erasure".
    @Test
    fun deleteAll_cannotInterleavePartwayThroughAnInFlightUpsert_forTheSameSubject() = runTest {
        val backing = GatedPutSecureKeyValueStore(gateOnKeyPrefix = "session_record:alice:")
        val store = testSessionRecordStore(backing, subject = "alice")

        val upsertJob = async { store.upsert(record(sessionId = "s1")) }
        backing.awaitPutEntered() // upsert() is now suspended mid-write, still holding the per-subject lock
        val deleteJob = async { store.deleteAll() }
        backing.releasePut() // only NOW can upsert() finish -- and only then can deleteAll's own body even begin

        upsertJob.await()
        assertEquals(
            SessionRecordDeleteResult.Cleared(1),
            deleteJob.await(),
            "deleteAll only runs after upsert fully released the lock, so it sees (and erases) the just-written row",
        )
        assertNull(store.find("s1"), "no row resurrects: deleteAll's view is always consistent with upsert's finish")
    }

    @Test
    fun deleteAll_abortsWithFailed_neverErasingUnmarked_whenTheTombstoneWriteItselfFails() = runTest {
        val backing = InMemorySecureKeyValueStore()
        // The tombstone lives in the dedicated revocation backing now: fail ITS put so markRevoked fails.
        val store = testSessionRecordStore(
            backing,
            subject = "alice",
            revocationStore = PutFailingSecureKeyValueStore(failPrefix = "revocation:"),
        )
        store.upsert(record(sessionId = "a1"))

        assertEquals(SessionRecordDeleteResult.Failed, store.deleteAll())
        assertEquals(
            SessionStatus.STARTED,
            store.find("a1")?.status,
            "no erasure happened -- the tombstone write failed first, before the removal loop ran",
        )
    }

    // P2-01 triadic: a faulting revocation-status read is STATUS_UNAVAILABLE — upsert returns a RETRYABLE outcome
    // and never writes, never treated as a positive revocation.
    @Test
    fun upsert_isRefused_retryable_whenTheRevocationStatusIsUnavailable() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testSessionRecordStore(
            backing,
            subject = "alice",
            revocationStore = ControllableReadStore(throwOnRead = true),
        )

        val outcome = store.upsert(record(sessionId = "a1"))

        assertIs<SessionRecordUpsertOutcome.RetryableFailure>(outcome)
        assertTrue(backing.keys("session_record:alice:").isEmpty(), "the write is refused (retryable); nothing persisted")
    }

    // R3-AUD-P1-02: a backend whose remove() call returns without throwing yet does not durably delete the key
    // must still be caught by the post-delete physical re-scan -- never a false Cleared.
    @Test
    fun deleteAll_downgradesToFailed_whenAPostDeleteRescanStillFindsAPhysicalRow() = runTest {
        val ghostKey = "session_record:alice:s1"
        val backing = GhostRemoveSecureKeyValueStore(ghostKey)
        val store = testSessionRecordStore(backing, subject = "alice")
        store.upsert(record(sessionId = "s1"))

        assertEquals(SessionRecordDeleteResult.Failed, store.deleteAll())
    }

    // Contract §8 / P0 regression: deleteAll must attempt EVERY key even when the FIRST-iterated one faults --
    // never abort early via `forEach { remove }` and abandon the rest of the deleted subject's rows. This test
    // FAILS against the old `forEach { store.remove(key) }` code (which would leave "a2" behind too).
    @Test
    fun deleteAll_attemptsEveryKey_evenWhenTheFirstIteratedRemoveFaults_thenReportsFailed() = runTest {
        val faultKey = "session_record:alice:a1"
        val backing = RemoveFailingSecureKeyValueStore(failOn = { it == faultKey })
        val store = testSessionRecordStore(backing, subject = "alice")
        store.upsert(record(sessionId = "a1"))
        store.upsert(record(sessionId = "a2"))

        assertEquals(SessionRecordDeleteResult.Failed, store.deleteAll())

        assertEquals(
            setOf(faultKey),
            backing.keys("session_record:alice:"),
            "only the faulting key remains; a2 was still erased (no early abort)",
        )
    }
}
