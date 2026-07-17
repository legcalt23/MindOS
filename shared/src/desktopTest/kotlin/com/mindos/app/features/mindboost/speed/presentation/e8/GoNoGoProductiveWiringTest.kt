@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.InMemorySessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.DefaultCognitiveSessionHistoryRepository
import com.mindos.app.features.mindboost.core.session.cognitive.DeleteResult
import com.mindos.app.features.mindboost.core.session.cognitive.InMemoryCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.ReadResult
import com.mindos.app.features.mindboost.core.session.cognitive.ReconcileResult
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import com.mindos.app.features.mindboost.core.session.cognitive.StoreFailureReason
import com.mindos.app.features.mindboost.core.session.payload.InMemoryPayloadJournalStore
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4B · A completed E8 run is reachable by a REAL, testable OFFICIAL consumer
 * ([DefaultCognitiveSessionHistoryRepository]); its wiring to a history UI is a later slice (5+). Every [SaveResult]
 * maps to an honest [CognitiveRunSaveState] (a failure/conflict never fakes saved progress).
 */
class GoNoGoProductiveWiringTest {

    private val fixedClock = EpochClock { 1_000L }

    /** A clock that advances on each read — used to reproduce overlapping runs where run 2 starts AFTER run 1 ends. */
    private fun incrementingClock(start: Long = 1_000L, step: Long = 1_000L): EpochClock {
        var t = start
        return EpochClock { val v = t; t += step; v }
    }

    /** A store whose `save` always returns a fixed result — to exercise the honest save-result state mapping. */
    private class FixedSaveStore(private val result: SaveResult) : CognitiveSessionRunStore {
        override suspend fun save(run: CognitiveSessionRun): SaveResult = result
        override suspend fun get(runId: String): ReadResult<CognitiveSessionRun> = ReadResult.Missing
        override suspend fun listRecent(limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun listByBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionRun> = emptyList()
        override suspend fun deleteAll(): DeleteResult = DeleteResult.Cleared(0)
        override suspend fun reconcileIndices(): ReconcileResult = ReconcileResult.Reconciled(0)
    }

    /** Plays a full 16-trial run on an existing holder WITHOUT advancing time (the seal coroutine stays pending). */
    private fun playTrials(h: GoNoGoSessionStateHolder) {
        h.startSession()
        repeat(16) {
            h.onStimulusShown()
            val type = h.state.value.currentStimulus!!.trialType
            if (type == GoNoGoTrialType.GO) h.onStimulusTap(300L) else h.onResponseWindowElapsed()
            h.beginNextTrial()
        }
    }

    private fun playToFinish(
        scope: CoroutineScope,
        runs: CognitiveSessionRunStore,
        records: SessionRecordStore = InMemorySessionRecordStore(),
    ): GoNoGoSessionStateHolder {
        val h = GoNoGoSessionStateHolder(
            scope = scope,
            sessionRecordStore = records,
            cognitiveRunStore = runs,
            config = GoNoGoConfig(),
            clock = fixedClock,
            trialCount = 16,
        )
        playTrials(h)
        return h
    }

    @Test
    fun a_completed_E8_run_is_visible_through_the_history_repository() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = playToFinish(this, runs)
        advanceUntilIdle()
        val sessionId = h.state.value.sessionId!!
        val repo = DefaultCognitiveSessionHistoryRepository(runs)

        assertTrue(repo.recent(10).any { it.sessionId == sessionId }, "the saved E8 run appears in recent history")
        assertEquals(listOf(sessionId), repo.byBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
        coroutineContext.cancelChildren()
    }

    @Test
    fun the_E8_history_item_is_visible_but_marked_not_counted() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = playToFinish(this, runs)
        advanceUntilIdle()
        val sessionId = h.state.value.sessionId!!
        val item = DefaultCognitiveSessionHistoryRepository(runs).recent(10).single { it.sessionId == sessionId }

        assertFalse(item.mayCountTowardProfile)
        assertFalse(item.mayCountTowardCalibration)
        assertFalse(item.mayCountTowardRanking)
        assertTrue("score_preliminary" in item.officialUseDeniedReasons)
        assertTrue("timebase_not_authoritative" in item.officialUseDeniedReasons)
        assertTrue("anti_gaming_coverage_missing" in item.officialUseDeniedReasons)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_saved_index_repair_needed_result_is_surfaced_as_repair_pending_not_a_plain_save() = runTest {
        val h = playToFinish(this, FixedSaveStore(SaveResult.SavedIndexRepairNeeded))
        advanceUntilIdle()
        assertEquals(CognitiveRunSaveState.SavedIndexRepairNeeded, h.state.value.saveState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun an_idempotent_duplicate_save_is_a_success_state() = runTest {
        val h = playToFinish(this, FixedSaveStore(SaveResult.IdempotentDuplicate))
        advanceUntilIdle()
        assertEquals(CognitiveRunSaveState.IdempotentDuplicate, h.state.value.saveState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_failed_save_never_shows_saved_progress() = runTest {
        val h = playToFinish(this, FixedSaveStore(SaveResult.Failed(StoreFailureReason.BACKEND_ERROR)))
        advanceUntilIdle()
        val saveState = h.state.value.saveState
        assertTrue(saveState is CognitiveRunSaveState.Failed, "a backend failure must NOT read as saved")
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_duplicate_conflict_never_shows_saved_progress() = runTest {
        val h = playToFinish(this, FixedSaveStore(SaveResult.DuplicateRunConflict("e8-1000")))
        advanceUntilIdle()
        val saveState = h.state.value.saveState
        assertTrue(saveState is CognitiveRunSaveState.DuplicateConflict, "a conflict must NOT read as saved")
        coroutineContext.cancelChildren()
    }

    /**
     * Regression (diff-auditor F1): a fast "Repetir bloque" starts a NEW run before the previous run's NonCancellable
     * seal coroutine has run. With an advancing clock, run 2 starts AFTER run 1 completes — so if the seal read the
     * holder's MUTABLE `startedAtEpochMs` (instead of the run's captured value) it would build run 1 with
     * `startedAt(run 2) > completedAt(run 1)`, breaking CognitiveSessionRun's `completedAt >= startedAt` invariant and
     * throwing inside a launched coroutine. The per-run [RunLifecycle] capture makes both runs seal consistently.
     */
    @Test
    fun repeating_a_block_before_the_previous_save_settles_saves_both_runs_without_crashing() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = GoNoGoSessionStateHolder(
            scope = this,
            sessionRecordStore = InMemorySessionRecordStore(),
            cognitiveRunStore = runs,
            config = GoNoGoConfig(),
            clock = incrementingClock(),
            trialCount = 16,
        )
        playTrials(h) // run 1: finished, seal launched but NOT advanced
        val firstId = h.state.value.sessionId!!
        playTrials(h) // run 2 starts immediately, before run 1's seal coroutine executes
        val secondId = h.state.value.sessionId!!
        advanceUntilIdle() // both seals run now; a clobbered start time would throw here and fail the test

        assertNotEquals(firstId, secondId, "each run has its own sessionId")
        val saved = DefaultCognitiveSessionHistoryRepository(runs).recent(10).map { it.sessionId }
        assertTrue(firstId in saved, "run 1 is still saved despite an immediate repeat")
        assertTrue(secondId in saved, "run 2 is saved")
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 1: a TERMINAL non-success (the subject was already erased — [StoreFailureReason.SUBJECT_TOMBSTONED])
     * must never read as saved, must get its OWN distinct terminal state (never the retryable [CognitiveRunSaveState
     * .Failed]), and must be DROPPED from the write-ahead journal — retaining it would retry forever a save that can
     * never succeed.
     */
    @Test
    fun a_tombstoned_subject_failure_is_a_distinct_terminal_state_and_is_dropped_never_retried() = runTest {
        val journal = InMemoryPayloadJournalStore()
        val h = GoNoGoSessionStateHolder(
            scope = this,
            sessionRecordStore = InMemorySessionRecordStore(),
            cognitiveRunStore = FixedSaveStore(SaveResult.Failed(StoreFailureReason.SUBJECT_TOMBSTONED)),
            config = GoNoGoConfig(),
            clock = fixedClock,
            trialCount = 16,
            payloadJournal = journal,
        )
        playTrials(h)
        advanceUntilIdle()

        val saveState = h.state.value.saveState
        assertTrue(
            saveState is CognitiveRunSaveState.Unrecoverable,
            "a tombstoned subject must be a distinct terminal state, never Saved and never the retryable Failed",
        )
        assertTrue(
            journal.pending().isEmpty(),
            "a terminal failure must be dropped from the journal, never retried forever",
        )
        coroutineContext.cancelChildren()
    }
}
