package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 2.1 SessionRecord persistence: round-trip parity (in-memory vs secure), write-ahead upsert semantics,
 * ordering, deletion, the minimization canary (serialized shape has ONLY allowlisted keys), and fail-closed
 * reads (future schemaVersion / unknown enum quarantined; unknown extra field tolerated).
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
        val store = SecureSessionRecordStore(InMemorySecureKeyValueStore())
        val r = record(block = ScoreModuleId.EPISODIC_MEMORY, status = SessionStatus.FINALIZED, completedTrials = 12)
        store.upsert(r)
        assertEquals(r, store.find("s1"))
        assertEquals(listOf(r), store.all())
    }

    @Test
    fun upsert_isWriteAhead_sameSessionUpdatesInPlace() = runTest {
        val store = SecureSessionRecordStore(InMemorySecureKeyValueStore())
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
        val store = SecureSessionRecordStore(InMemorySecureKeyValueStore())
        store.upsert(record(sessionId = "a", startedAtMs = 100L))
        store.upsert(record(sessionId = "b", startedAtMs = 300L))
        store.upsert(record(sessionId = "c", startedAtMs = 300L))
        assertEquals(listOf("b", "c", "a"), store.all().map { it.sessionId })
    }

    @Test
    fun delete_removesRow() = runTest {
        val store = SecureSessionRecordStore(InMemorySecureKeyValueStore())
        store.upsert(record())
        store.delete("s1")
        assertNull(store.find("s1"))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun serializedForm_hasSchemaVersion_andOnlyAllowlistedKeys() = runTest {
        val backing = InMemorySecureKeyValueStore()
        SecureSessionRecordStore(backing).upsert(record(status = SessionStatus.FINALIZED))
        val key = backing.keys("").single()
        val raw = (backing.read(key) as SecureReadResult.Value).value
        val keys = Json.parseToJsonElement(raw).jsonObject.keys
        assertEquals(
            setOf(
                "schemaVersion", "sessionId", "block", "status",
                "startedAtMs", "updatedAtMs", "completedTrials", "plannedTrials", "activeDurationMs",
            ),
            keys,
            "SessionRecord serialized shape must stay minimized (no raw trials / free text); a new field " +
                "requires a privacy review.",
        )
    }

    @Test
    fun futureSchemaVersion_isQuarantinedNotMisread() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:s1",
            """{"schemaVersion":999,"sessionId":"s1","block":"FOCUS","status":"STARTED","startedAtMs":1,"updatedAtMs":1,"completedTrials":0,"plannedTrials":20,"activeDurationMs":0}""",
        )
        assertNull(SecureSessionRecordStore(backing).find("s1"))
    }

    @Test
    fun unknownBlock_isSkipped() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:s1",
            """{"schemaVersion":1,"sessionId":"s1","block":"NOT_A_BLOCK","status":"STARTED","startedAtMs":1,"updatedAtMs":1,"completedTrials":0,"plannedTrials":20,"activeDurationMs":0}""",
        )
        assertNull(SecureSessionRecordStore(backing).find("s1"))
    }

    @Test
    fun unknownExtraField_isIgnored() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put(
            "session_record:s1",
            """{"schemaVersion":1,"sessionId":"s1","block":"LOGIC","status":"FINALIZED","startedAtMs":5,"updatedAtMs":6,"completedTrials":8,"plannedTrials":8,"activeDurationMs":120,"future":42}""",
        )
        val found = SecureSessionRecordStore(backing).find("s1")
        assertEquals(ScoreModuleId.LOGIC, found?.block)
        assertEquals(SessionStatus.FINALIZED, found?.status)
    }
}
