package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.DeleteResult
import com.mindos.app.features.mindboost.core.session.cognitive.InMemoryCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.ReadResult
import com.mindos.app.features.mindboost.core.session.cognitive.ReconcileResult
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import com.mindos.app.features.mindboost.core.session.cognitive.StoreFailureReason
import com.mindos.app.features.mindboost.core.session.payload.JournaledPayload
import com.mindos.app.features.mindboost.core.session.payload.PayloadKind
import com.mindos.app.features.mindboost.core.session.payload.ReplayOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S3 E8 replay handler: a journaled run is re-saved on replay, and re-applying the SAME entry is idempotent (the
 * run store dedups by runId). An undecodable payload is dropped so replay never loops.
 */
class E8RunJournalReplayTest {

    private fun entry(payload: String) =
        JournaledPayload(entryId = "sess_abc", kind = PayloadKind.E8_COGNITIVE_RUN, payload = payload, enqueuedAtMs = 1L)

    @Test
    fun handler_savesRun_thenIsIdempotentOnReplay() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = CognitiveSessionRunFixtures.validRun(sessionId = "sess_abc")
        val handler = e8RunReplayHandler(store)
        val journaled = entry(encodeCognitiveRun(run))

        assertEquals(ReplayOutcome.CONFIRMED, handler(journaled))
        assertEquals(1, store.listRecent(10).size, "the journaled run is saved on replay")

        // Replaying the SAME entry again is a no-op at the sink (dedup by runId) — still one run.
        assertEquals(ReplayOutcome.CONFIRMED, handler(journaled))
        assertEquals(1, store.listRecent(10).size, "an idempotent replay never duplicates the run")
    }

    @Test
    fun handler_dropsUndecodablePayload_withoutSaving() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        assertEquals(
            ReplayOutcome.CONFIRMED,
            e8RunReplayHandler(store)(entry("{ garbage")),
            "an undecodable payload is dropped (CONFIRMED), never looping the replay",
        )
        assertTrue(store.listRecent(10).isEmpty())
    }

    /** A store whose `save` always returns a fixed result — to exercise the [SaveOutcomeClass] classification. */
    private class FixedSaveStore(private val result: SaveResult) : CognitiveSessionRunStore {
        override suspend fun save(run: CognitiveSessionRun): SaveResult = result
        override suspend fun get(runId: String): ReadResult<CognitiveSessionRun> = ReadResult.Missing
        override suspend fun listRecent(limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun listByBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun deleteAll(): DeleteResult = DeleteResult.Cleared(0)
        override suspend fun reconcileIndices(): ReconcileResult = ReconcileResult.Reconciled(0)
    }

    /**
     * P2-01 item 1: a permanently-refused (tombstoned-subject) result can NEVER succeed on retry — the replay
     * handler must drop it (CONFIRMED, dead-lettered), never RETAIN it forever like a transient backend fault.
     */
    @Test
    fun handler_dropsATombstonedSubjectFailure_neverRetryingForever() = runTest {
        val store = FixedSaveStore(SaveResult.Failed(StoreFailureReason.SUBJECT_TOMBSTONED))
        val run = CognitiveSessionRunFixtures.validRun(sessionId = "sess_abc")
        assertEquals(
            ReplayOutcome.CONFIRMED,
            e8RunReplayHandler(store)(entry(encodeCognitiveRun(run))),
            "a permanently-tombstoned subject must be dropped, never retried forever",
        )
    }

    /** A transient backend fault must still RETAIN (unlike a terminal failure), so a later replay can retry. */
    @Test
    fun handler_retainsATransientBackendFailure_forTheNextReplay() = runTest {
        val store = FixedSaveStore(SaveResult.Failed(StoreFailureReason.BACKEND_ERROR))
        val run = CognitiveSessionRunFixtures.validRun(sessionId = "sess_abc")
        assertEquals(
            ReplayOutcome.RETAIN,
            e8RunReplayHandler(store)(entry(encodeCognitiveRun(run))),
            "a transient backend fault must be retained for the next replay, not dropped",
        )
    }
}
