package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.speed.domain.cognitive.GoNoGoAdapterFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 5 · The history read-model over [CognitiveSessionRunStore]. Verifies HISTORY ≠ PROFILE (an
 * ineligible/preliminary run is VISIBLE but honestly marked not-counted), that measurement honesty is preserved
 * (`null`/Unavailable never coerced to 0), that the eligibility trace + anomaly flags project in full, that
 * ordering/limits/corrupt-handling are inherited from the store, and that a real E8 adapter run flows through
 * without any productive wiring, profile mutation, or sync.
 */
class CognitiveSessionHistoryRepositoryTest {

    /** A store stub that returns a fixed read for [get] — to exercise the corrupt/missing/failed detail paths. */
    private class FakeReadStore(
        private val getResult: ReadResult<CognitiveSessionRun>,
    ) : CognitiveSessionRunStore {
        override suspend fun save(run: CognitiveSessionRun): SaveResult = SaveResult.Saved
        override suspend fun get(runId: String): ReadResult<CognitiveSessionRun> = getResult
        override suspend fun listRecent(limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun listByBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun deleteAll(): DeleteResult = DeleteResult.Cleared(0)
        override suspend fun reconcileIndices(): ReconcileResult = ReconcileResult.Reconciled(0)
    }

    @Test
    fun `recent returns sessions ordered most-recent-first`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_old", completedAtEpochMs = 100_000L))
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_new", completedAtEpochMs = 300_000L))
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_mid", completedAtEpochMs = 200_000L))
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        assertEquals(listOf("sess_new", "sess_mid", "sess_old"), repo.recent(limit = 10).map { it.sessionId })
    }

    @Test
    fun `byBlock filters by ScoreModuleId`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_speed", blockId = ScoreModuleId.SPEED))
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_mem", blockId = ScoreModuleId.MEMORY))
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val speed = repo.byBlock(ScoreModuleId.SPEED, limit = 10)
        assertEquals(listOf("sess_speed"), speed.map { it.sessionId })
        assertTrue(speed.all { it.blockId == ScoreModuleId.SPEED })
    }

    @Test
    fun `a preliminary E8 run is visible in history but marked not counted`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertTrue(item.preliminary)
        assertFalse(item.mayCountTowardProfile)
        assertFalse(item.mayCountTowardCalibration)
        assertFalse(item.mayCountTowardRanking)
        assertTrue("score_preliminary" in item.officialUseDeniedReasons)
    }

    @Test
    fun `a VALID but preliminary run publishes its number yet may not count (HISTORY is not PROFILE)`() = runTest {
        // The subtle HISTORY != PROFILE case: a sufficient E8 run scores VALID (a real number is shown in
        // history) but is STILL preliminary, so it may not count toward any official lane.
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertEquals(ScoreState.VALID, item.scoreState)
        assertNotNull(item.scoreNormalized, "a VALID score shows its honest number in history")
        assertTrue(item.preliminary)
        assertFalse(item.mayCountTowardProfile, "preliminary ⇒ may NOT count, even though the number is shown")
        assertTrue("score_preliminary" in item.officialUseDeniedReasons)
    }

    @Test
    fun `durationSelectedMs stays null for fixed-count E8 and is never turned into an anomaly`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertNull(item.durationSelectedMs, "E8 has no user-selected duration; null is honest, not anomalous")
        assertEquals(run.anomalyFlags, item.anomalyFlags, "the read-model derives no anomalies of its own")
    }

    @Test
    fun `the exercise payload is projected as a safe summary, never the raw JsonObject`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val detail = assertNotNull(repo.detail(run.sessionId))
        val payload = assertNotNull(detail.exercisePayload)
        assertEquals("gonogo_inhibition", payload.type)
        assertEquals(1, payload.schemaVersion)
        assertEquals(16, payload.fieldCount, "the safe summary exposes the field count, not the raw values")
    }

    @Test
    fun `a non-VALID score is visible with scoreNormalized null, never a fabricated 0`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(listOf(GoNoGoAdapterFixtures.goHit(0))))
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertEquals(ScoreState.CALIBRATING, item.scoreState)
        assertNull(item.scoreNormalized)
    }

    @Test
    fun `an Unavailable metric is preserved as Unavailable, not coerced to 0`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val reason = MetricUnavailableReason.INSUFFICIENT_SAMPLE
        val metrics = CognitiveSessionMetrics(
            accuracy = SessionMetric.Measured(0.8),
            reactionTimeMedianMs = SessionMetric.Unavailable(reason),
            reactionTimeMeanMs = SessionMetric.Unavailable(reason),
            reactionTimeMinMs = SessionMetric.Unavailable(reason),
            reactionTimeMaxMs = SessionMetric.Unavailable(reason),
            reactionTimeVariabilityCv = SessionMetric.Unavailable(reason),
        )
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_rt", metrics = metrics))
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single()
        assertTrue(item.reactionTimeMedianMs is SessionMetric.Unavailable)
        val detail = assertNotNull(repo.detail("sess_rt"))
        assertTrue(detail.metrics.reactionTimeMedianMs is SessionMetric.Unavailable)
    }

    @Test
    fun `the eligibility trace is projected in full`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertEquals(run.eligibilityTrace, item.eligibilityTrace)
    }

    @Test
    fun `anomalyFlags are preserved verbatim`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val run = GoNoGoAdapterFixtures.run()
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        val item = repo.recent(10).single { it.sessionId == run.sessionId }
        assertTrue(run.anomalyFlags.isNotEmpty(), "the E8 run carries at least the kill-switch reasons")
        assertEquals(run.anomalyFlags, item.anomalyFlags)
    }

    @Test
    fun `detail returns null (never crashes or fabricates) for missing, corrupt or failed reads`() = runTest {
        val found = DefaultCognitiveSessionHistoryRepository(
            FakeReadStore(ReadResult.Found(CognitiveSessionRunFixtures.validRun(sessionId = "sess_ok"))),
        )
        assertNotNull(found.detail("sess_ok"))

        assertNull(DefaultCognitiveSessionHistoryRepository(FakeReadStore(ReadResult.Missing)).detail("x"))
        assertNull(DefaultCognitiveSessionHistoryRepository(FakeReadStore(ReadResult.Corrupt("x"))).detail("x"))
        assertNull(
            DefaultCognitiveSessionHistoryRepository(
                FakeReadStore(ReadResult.Failed(StoreFailureReason.BACKEND_ERROR)),
            ).detail("x"),
        )
    }

    @Test
    fun `recent limit 0, negative and excessive behave exactly like the store`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_1"))
        store.save(CognitiveSessionRunFixtures.validRun(sessionId = "sess_2"))
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        assertEquals(emptyList(), repo.recent(0).map { it.sessionId })
        assertEquals(emptyList(), repo.recent(-5).map { it.sessionId })
        assertEquals(2, repo.recent(1_000).size)
        assertEquals(store.listRecent(1_000).map { it.sessionId }, repo.recent(1_000).map { it.sessionId })
    }

    @Test
    fun `an E8 run appears in history with CORRECT_INHIBITION and no fabricated reaction time`() = runTest {
        val store = InMemoryCognitiveSessionRunStore()
        val outcomes = (0 until 6).map { GoNoGoAdapterFixtures.noGoInhibition(it) } +
            (0 until 6).map { GoNoGoAdapterFixtures.goOmission(100 + it) }
        val run = GoNoGoAdapterFixtures.run(GoNoGoAdapterFixtures.artifacts(outcomes))
        store.save(run)
        val repo = DefaultCognitiveSessionHistoryRepository(store)

        assertTrue(repo.recent(10).any { it.sessionId == run.sessionId }, "E8 run must be visible in recent history")
        assertEquals(listOf(run.sessionId), repo.byBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })

        val detail = assertNotNull(repo.detail(run.sessionId))
        val inhibition = detail.trials.outcomes.firstOrNull { it.kind == TrialOutcomeKind.CORRECT_INHIBITION }
        assertNotNull(inhibition, "a correct inhibition must be recorded")
        assertEquals(6, inhibition.count)
        assertTrue(
            detail.metrics.reactionTimeMedianMs is SessionMetric.Unavailable,
            "no correct-GO response ⇒ RT stays Unavailable, never a fabricated number",
        )
    }

    @Test
    fun `the read-model needs only a run store - no profile, aggregator, sync, outbox or recovery collaborator`() =
        runTest {
            // The constructor takes ONLY a CognitiveSessionRunStore: this compiles and runs end-to-end with an
            // in-memory store alone, structurally proving slice 5 wires no profile/aggregator/sync/outbox/recovery.
            val store = InMemoryCognitiveSessionRunStore()
            store.save(GoNoGoAdapterFixtures.run())
            val repo: CognitiveSessionHistoryRepository = DefaultCognitiveSessionHistoryRepository(store)

            assertEquals(1, repo.recent(10).size)
            assertEquals(1, repo.byBlock(ScoreModuleId.SPEED, 10).size)
            assertNotNull(repo.detail(GoNoGoAdapterFixtures.SESSION_ID))
        }
}
