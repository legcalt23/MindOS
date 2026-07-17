package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.InMemoryCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.ReadResult
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4 · An adapter-built E8 run persists to (and reads back from) the SYSTEM-1 store losslessly and
 * idempotently. Uses only the in-memory store — no productive Secure-store wiring, no SyncQueue, no profile.
 */
class GoNoGoCognitiveSessionPersistenceTest {

    @Test
    fun `an adapter-built run saves and reads back losslessly`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()

        assertEquals(SaveResult.Saved, store.save(run))

        val read = store.get(run.sessionId)
        assertTrue(read is ReadResult.Found)
        assertEquals(run, read.value)
    }

    @Test
    fun `re-saving the identical run is an idempotent success, never a second truth`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()

        assertEquals(SaveResult.Saved, store.save(run))
        assertEquals(SaveResult.IdempotentDuplicate, store.save(run))

        assertEquals(1, store.listRecent(limit = 10).size, "the same session yields exactly one stored run")
    }

    @Test
    fun `a saved E8 run is listed under the SPEED block`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)

        val byBlock = store.listByBlock(ScoreModuleId.SPEED, limit = 10)
        assertEquals(listOf(run.sessionId), byBlock.map { it.sessionId })
    }

    @Test
    fun `a preliminary E8 run is savable to history yet its persisted eligibility moves nothing official`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()

        // History: the run persists and reads back losslessly (including the eligibility trace + its reasons).
        assertEquals(SaveResult.Saved, store.save(run))
        val read = store.get(run.sessionId)
        assertTrue(read is ReadResult.Found)
        val stored = read.value

        // ...but the PERSISTED eligibility still denies every official lane — nothing an ingestor could consume.
        assertTrue(stored.score.preliminary)
        assertFalse(stored.eligibility.profileEligible)
        assertFalse(stored.eligibility.calibrationEligible)
        assertFalse(stored.eligibility.rankingEligible)
        assertTrue("score_preliminary" in stored.eligibilityTrace.rejectionReasons)
    }
}
