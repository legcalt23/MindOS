package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.SessionRecordUpsertOutcome

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.InMemorySessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 3 · The resolver reads the EXISTING lifecycle store (no new draft/scanner) and keeps absent /
 * unreadable / backend-failed distinct, exactly as the evaluator's rules require.
 */
class WriteAheadResolverTest {

    private fun record(id: String, status: SessionStatus, started: Long = 1_000L, updated: Long = 5_000L) =
        SessionRecord(
            sessionId = id,
            block = ScoreModuleId.SPEED,
            status = status,
            startedAtMs = started,
            updatedAtMs = updated,
            completedTrials = 20,
            plannedTrials = 20,
            activeDurationMs = 120_000L,
        )

    @Test
    fun `a finalized record resolves to present evidence with matching id and timestamps`() = runTest {
        val store = InMemorySessionRecordStore()
        store.upsert(record("sess_a", SessionStatus.FINALIZED))
        val e = resolveWriteAheadEvidence(store, "sess_a")
        assertTrue(e.present)
        assertEquals(SessionStatus.FINALIZED, e.lifecycleStatus)
        assertTrue(e.sessionIdMatches)
        assertEquals(1_000L, e.startedAtEpochMs)
        assertEquals(5_000L, e.finalizedAtEpochMs)
        assertNull(e.failureReason)
    }

    @Test
    fun `a non-finalized record exposes no finalized timestamp`() = runTest {
        val store = InMemorySessionRecordStore()
        store.upsert(record("sess_a", SessionStatus.CHECKPOINT))
        val e = resolveWriteAheadEvidence(store, "sess_a")
        assertEquals(SessionStatus.CHECKPOINT, e.lifecycleStatus)
        assertNull(e.finalizedAtEpochMs)
    }

    @Test
    fun `an absent record resolves to missing`() = runTest {
        val e = resolveWriteAheadEvidence(InMemorySessionRecordStore(), "sess_x")
        assertFalse(e.present)
        assertNull(e.failureReason)
    }

    @Test
    fun `a backend read fault resolves to READ_FAILED, never crashing`() = runTest {
        val e = resolveWriteAheadEvidence(ThrowingFindStore(), "sess_x")
        assertFalse(e.present)
        assertEquals(WriteAheadFailureReason.READ_FAILED, e.failureReason)
    }

    @Test
    fun `a corrupt durable record resolves to UNREADABLE, distinct from missing`() = runTest {
        val kv = InMemorySecureKeyValueStore()
        kv.put("session_record:u1:sess_z", "{ not valid json")
        val store = testSessionRecordStore(kv, subject = "u1")
        val e = resolveWriteAheadEvidence(store, "sess_z")
        assertFalse(e.present)
        assertEquals(WriteAheadFailureReason.UNREADABLE, e.failureReason)
    }

    @Test
    fun `a stored record whose id differs is a mismatch`() = runTest {
        val store = FixedFoundStore(record("OTHER", SessionStatus.FINALIZED))
        val e = resolveWriteAheadEvidence(store, "sess_a")
        assertTrue(e.present)
        assertFalse(e.sessionIdMatches)
    }
}

/** A store whose `find` throws — exercises the default `findEvidence` fault path (→ ReadFailed). */
private class ThrowingFindStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = SessionRecordUpsertOutcome.Stored
    override suspend fun find(sessionId: String): SessionRecord? = throw IllegalStateException("simulated fault")
    override suspend fun all(): List<SessionRecord> = emptyList()
    override suspend fun delete(sessionId: String) = Unit
}

/** A store that always finds the same record — exercises the sessionId-mismatch path. */
private class FixedFoundStore(private val fixed: SessionRecord) : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = SessionRecordUpsertOutcome.Stored
    override suspend fun find(sessionId: String): SessionRecord? = fixed
    override suspend fun all(): List<SessionRecord> = listOf(fixed)
    override suspend fun delete(sessionId: String) = Unit
}
