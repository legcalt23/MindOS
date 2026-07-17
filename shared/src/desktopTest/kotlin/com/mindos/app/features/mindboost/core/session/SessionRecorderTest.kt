package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionRecorderTest {

    private class RecordingDiagnostics : SessionRecorderDiagnostics {
        val ops = mutableListOf<String>()
        override fun onRecorderError(operation: String) {
            ops += operation
        }
    }

    private class ThrowingStore(private val error: Throwable) : SessionRecordStore {
        override suspend fun upsert(record: SessionRecord) = throw error
        override suspend fun find(sessionId: String): SessionRecord? = throw error
        override suspend fun all(): List<SessionRecord> = throw error
        override suspend fun delete(sessionId: String) = throw error
    }

    private fun recorder(
        store: SessionRecordStore,
        now: Long = 9_000L,
        diag: SessionRecorderDiagnostics = NoOpSessionRecorderDiagnostics,
    ) = SecureSessionRecorder(store, EpochClock { now }, diag)

    @Test
    fun onStart_createsStarted() = runTest {
        val store = InMemorySessionRecordStore()
        recorder(store).onStart("s1", ScoreModuleId.FOCUS, startedAtMs = 1_000L, plannedTrials = 20)
        val r = store.find("s1")!!
        assertEquals(SessionStatus.STARTED, r.status)
        assertEquals(ScoreModuleId.FOCUS, r.block)
        assertEquals(1_000L, r.startedAtMs)
        assertEquals(20, r.plannedTrials)
    }

    @Test
    fun lifecycle_updatesSameRowInPlace_noDuplicates() = runTest {
        val store = InMemorySessionRecordStore()
        val rec = recorder(store, now = 5_000L)
        rec.onStart("s1", ScoreModuleId.MEMORY, startedAtMs = 1_000L, plannedTrials = 8)
        rec.onCheckpoint("s1", completedTrials = 4, activeDurationMs = 30_000L)
        rec.onFinalize("s1", completedTrials = 8, activeDurationMs = 60_000L)
        val all = store.all()
        assertEquals(1, all.size)
        val r = all.single()
        assertEquals(SessionStatus.FINALIZED, r.status)
        assertEquals(8, r.completedTrials)
        assertEquals(60_000L, r.activeDurationMs)
        // preserved from STARTED:
        assertEquals(ScoreModuleId.MEMORY, r.block)
        assertEquals(1_000L, r.startedAtMs)
        assertEquals(8, r.plannedTrials)
        assertEquals(5_000L, r.updatedAtMs)
    }

    @Test
    fun onAbandon_marksAbandoned() = runTest {
        val store = InMemorySessionRecordStore()
        val rec = recorder(store, now = 7_000L)
        rec.onStart("s1", ScoreModuleId.LOGIC, startedAtMs = 1_000L, plannedTrials = 5)
        rec.onAbandon("s1")
        assertEquals(SessionStatus.ABANDONED, store.find("s1")!!.status)
        assertEquals(7_000L, store.find("s1")!!.updatedAtMs)
    }

    @Test
    fun checkpointOnMissingRow_isDiagnosed_notCreated() = runTest {
        val store = InMemorySessionRecordStore()
        val diag = RecordingDiagnostics()
        recorder(store, diag = diag).onCheckpoint("ghost", completedTrials = 1, activeDurationMs = 1L)
        assertEquals(listOf("onCheckpoint.missingRecord"), diag.ops)
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun finalizeOnMissingRow_isDiagnosed_notCreated() = runTest {
        val store = InMemorySessionRecordStore()
        val diag = RecordingDiagnostics()
        recorder(store, diag = diag).onFinalize("ghost", completedTrials = 1, activeDurationMs = 1L)
        assertEquals(listOf("onFinalize.missingRecord"), diag.ops)
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun abandonOnMissingRow_isDiagnosed_notCreated() = runTest {
        val store = InMemorySessionRecordStore()
        val diag = RecordingDiagnostics()
        recorder(store, diag = diag).onAbandon("ghost")
        assertEquals(listOf("onAbandon.missingRecord"), diag.ops)
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun storageError_isDiagnosed_notThrown() = runTest {
        val diag = RecordingDiagnostics()
        val rec = recorder(ThrowingStore(IllegalStateException("disk full")), diag = diag)
        rec.onStart("s1", ScoreModuleId.SPEED, startedAtMs = 1L, plannedTrials = 1) // must NOT throw
        assertEquals(listOf("onStart"), diag.ops)
    }

    @Test
    fun cancellationException_isNotSwallowed() = runTest {
        val rec = recorder(ThrowingStore(CancellationException("cancelled")))
        assertFailsWith<CancellationException> {
            rec.onStart("s1", ScoreModuleId.SPEED, startedAtMs = 1L, plannedTrials = 1)
        }
    }

    @Test
    fun recorderOutput_staysMinimized_noRawTrialsOrFreeText() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val rec = SecureSessionRecorder(testSessionRecordStore(backing, subject = "u1"), EpochClock { 9_000L })
        rec.onStart("s1", ScoreModuleId.FOCUS, startedAtMs = 1_000L, plannedTrials = 20)
        val key = backing.keys("").single()
        val raw = (backing.read(key) as SecureReadResult.Value).value
        val keys = Json.parseToJsonElement(raw).jsonObject.keys
        assertEquals(
            setOf(
                "schemaVersion", "sessionId", "block", "status", "startedAtMs",
                "updatedAtMs", "completedTrials", "plannedTrials", "activeDurationMs",
                // ENG-01, schema 2. See SessionRecordStoreTest's canary for the privacy rationale.
                "reproducibleIdentity",
            ),
            keys,
        )
    }

    /**
     * A recorder that opts out of identity records NO identity — the field is absent from the row rather than
     * present-and-empty. A block that never adopted the engine route stays honestly non-reproducible.
     */
    @Test
    fun onStart_withoutAnIdentity_writesNoIdentity() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testSessionRecordStore(backing, subject = "u1")
        val rec = SecureSessionRecorder(store, EpochClock { 2_000L })
        rec.onStart("s2", ScoreModuleId.FOCUS, startedAtMs = 1_000L, plannedTrials = 20)
        assertEquals(null, store.find("s2")?.reproducibleIdentity)
    }
}
