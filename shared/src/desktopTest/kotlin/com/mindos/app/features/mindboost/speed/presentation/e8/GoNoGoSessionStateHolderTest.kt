@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.InMemorySessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.InMemoryCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import com.mindos.app.features.mindboost.core.session.cognitive.TrialOutcomeKind
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoCategory
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedbackKind
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import com.mindos.app.features.mindboost.speed.domain.score.TimingIntegrityLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4B · E8 — the event-driven session state machine now persists the OFFICIAL `CognitiveSessionRun`
 * (the single cognitive-history source) and wires the write-ahead `SessionRecord` lifecycle
 * (STARTED → FINALIZED / ABANDONED). No telemetry summary is written anymore.
 */
class GoNoGoSessionStateHolderTest {

    private val fixedClock = EpochClock { 1_000L }

    // scheduleBlock 8 + minNoGo 2, trialCount 16 ⇒ deterministic counts: exactly 12 GO + 4 NO-GO.
    private fun holder(
        scope: CoroutineScope,
        records: InMemorySessionRecordStore = InMemorySessionRecordStore(),
        runs: InMemoryCognitiveSessionRunStore = InMemoryCognitiveSessionRunStore(),
        trialCount: Int = 16,
    ) = GoNoGoSessionStateHolder(
        scope = scope,
        sessionRecordStore = records,
        cognitiveRunStore = runs,
        config = GoNoGoConfig(),
        clock = fixedClock,
        trialCount = trialCount,
    )

    /** Drives one trial: reveal the stimulus, then respond per [respond] (true = tap), then advance. */
    private fun playTrial(h: GoNoGoSessionStateHolder, respond: (GoNoGoTrialType) -> Boolean) {
        h.onStimulusShown()
        val type = h.state.value.currentStimulus!!.trialType
        if (respond(type)) h.onStimulusTap(300L) else h.onResponseWindowElapsed()
        h.beginNextTrial()
    }

    @Test
    fun a_perfectly_played_run_finalizes_the_session_record_and_saves_one_cognitive_run() = runTest {
        val records = InMemorySessionRecordStore()
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, records, runs)
        h.startSession()
        val sessionId = assertNotNull(h.state.value.sessionId)
        // The write-ahead STARTED row exists as soon as the session begins.
        advanceUntilIdle()
        assertEquals(SessionStatus.STARTED, records.find(sessionId)?.status, "start writes SessionRecord STARTED")

        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()

        val state = h.state.value
        assertEquals(GoNoGoPhase.Finished, state.phase)
        val score = assertNotNull(state.score)
        assertTrue(score.index != null && score.index!! > 0.9, "perfect play ⇒ high index: ${score.index}")
        assertEquals(GoNoGoCategory.STRONG_CONTROL, score.category)
        assertEquals(GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED, state.feedback)

        // Lifecycle FINALIZED + exactly one saved CognitiveSessionRun with the SAME sessionId.
        assertEquals(SessionStatus.FINALIZED, records.find(sessionId)?.status)
        val savedRuns = runs.listRecent(10)
        assertEquals(1, savedRuns.size, "E8 finish builds exactly one CognitiveSessionRun")
        val run = savedRuns.first()
        assertEquals(sessionId, run.sessionId, "CognitiveSessionRun.sessionId == SessionRecord.sessionId")
        assertEquals(records.find(sessionId)?.sessionId, run.sessionId)
        assertEquals(ScoreModuleId.SPEED, run.blockId)
        assertEquals(CognitiveRunSaveState.Saved, state.saveState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_preliminary_run_is_saved_to_history_but_may_not_count_officially() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, runs = runs)
        h.startSession()
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()

        val run = runs.listRecent(10).single()
        assertTrue(run.score.preliminary, "E8 stays preliminary (§7)")
        assertFalse(run.eligibility.profileEligible)
        assertFalse(run.eligibility.calibrationEligible)
        assertFalse(run.eligibility.rankingEligible)
        val reasons = run.eligibilityTrace.rejectionReasons
        assertTrue("score_preliminary" in reasons)
        assertTrue("timebase_not_authoritative" in reasons)
        assertTrue("anti_gaming_coverage_missing" in reasons)
        // The write-ahead was FINALIZED cleanly, so it is NOT a denial reason.
        assertFalse("write_ahead_ineligible" in reasons)
        coroutineContext.cancelChildren()
    }

    @Test
    fun aborting_mid_run_returns_to_idle_marks_abandoned_and_saves_no_run() = runTest {
        val records = InMemorySessionRecordStore()
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, records, runs)
        h.startSession()
        val sessionId = assertNotNull(h.state.value.sessionId)
        h.onStimulusShown()
        h.onStimulusTap(300L)
        h.abort()
        advanceUntilIdle()

        assertEquals(GoNoGoPhase.Idle, h.state.value.phase)
        assertEquals(SessionStatus.ABANDONED, records.find(sessionId)?.status, "an aborted session is ABANDONED")
        assertTrue(runs.listRecent(10).isEmpty(), "an incomplete (aborted) run is NOT saved to history")
        coroutineContext.cancelChildren()
    }

    @Test
    fun tapping_every_trial_scores_low_and_flags_commissions() = runTest {
        val h = holder(this)
        h.startSession()
        repeat(16) { playTrial(h) { true } } // tap-all: GO correct, but every NO-GO is a commission
        advanceUntilIdle()

        val score = assertNotNull(h.state.value.score)
        assertTrue(score.index != null && score.index!! < 0.35, "tap-all must score low: ${score.index}")
        assertEquals(GoNoGoCategory.IMPULSIVE_COMMISSION, score.category)
        assertEquals(GoNoGoFeedbackKind.HIGH_COMMISSION, h.state.value.feedback)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_foreperiod_tap_resolves_the_trial_as_an_anticipation() = runTest {
        val h = holder(this)
        h.startSession()
        h.onPrematureTap()
        val outcome = assertNotNull(h.state.value.lastOutcome)
        assertTrue(outcome.anticipation)
        assertEquals(GoNoGoPhase.Feedback, h.state.value.phase)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_double_tap_does_not_produce_a_second_response() = runTest {
        val h = holder(this)
        h.startSession()
        h.onStimulusShown()
        h.onStimulusTap(300L)
        val afterFirst = assertNotNull(h.state.value.lastOutcome)
        assertEquals(GoNoGoPhase.Feedback, h.state.value.phase)
        h.onStimulusTap(300L) // second tap: phase is Feedback, not Stimulus ⇒ ignored
        assertEquals(afterFirst, h.state.value.lastOutcome, "a double tap must not change the resolved outcome")
        assertEquals(GoNoGoPhase.Feedback, h.state.value.phase)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_late_go_tap_is_captured_as_late_through_the_holder() = runTest {
        val h = holder(this)
        h.startSession()
        var sawLate = false
        repeat(16) {
            h.onStimulusShown()
            val stim = h.state.value.currentStimulus!!
            if (stim.trialType == GoNoGoTrialType.GO) {
                h.onStimulusTap(stim.deadlineMs + 100L) // beyond the deadline, within the grace window
                if (h.state.value.lastOutcome!!.lateResponse) sawLate = true
            } else {
                h.onResponseWindowElapsed()
            }
            h.beginNextTrial()
        }
        advanceUntilIdle()
        assertTrue(sawLate, "a GO tap past the deadline resolves as lateResponse through the holder")
        coroutineContext.cancelChildren()
    }

    @Test
    fun the_finished_state_exposes_a_non_official_in_memory_run_view() = runTest {
        val h = holder(this)
        h.startSession()
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()
        val run = assertNotNull(h.state.value.resultView)
        assertEquals(16, run.totalTrials)
        assertEquals(16, run.plannedTrials)
        assertEquals(16, run.completedTrials)
        assertEquals(assertNotNull(h.state.value.sessionId), run.telemetrySummarySessionId)
        assertTrue(!run.contributesToOfficialProfile, "the in-memory view is NON-official; only the CognitiveSessionRun persists")
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_tap_without_onset_is_a_lost_input_counted_in_the_saved_run_never_fabricated() = runTest {
        // MC9-D-R3 live path: a tap with no captured onset is routed as onStimulusTap(null). It must NOT fabricate
        // an rt=0 / resolve the trial; it must be counted, surface in the saved run's payload + anomaly flags, and
        // downgrade timing integrity to MINOR (1/16 ≤ 0.10) — never a fabricated CORRECT_INHIBITION trial.
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, runs = runs)
        h.startSession()
        h.onStimulusShown()
        h.onStimulusTap(null)
        assertEquals(GoNoGoPhase.Stimulus, h.state.value.phase, "a null-onset tap does NOT resolve the trial")
        h.onResponseWindowElapsed()
        h.beginNextTrial()
        repeat(15) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()

        val score = assertNotNull(h.state.value.score)
        assertEquals(TimingIntegrityLevel.MINOR_ANOMALIES, score.timingIntegrity)
        val run = runs.listRecent(10).single()
        assertTrue("missingOnsetTaps" in run.anomalyFlags, "the lost input is flagged in the saved run, not hidden")
        // The lost tap is NOT a completed trial and is never turned into a fabricated inhibition/RT.
        assertTrue(run.trials.outcomes.none { it.kind == TrialOutcomeKind.LOST_INPUT }, "no LOST_INPUT partition trial")
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_correct_inhibition_run_saves_correct_inhibition_with_no_fabricated_reaction_time() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, runs = runs)
        h.startSession()
        // Respond to GO, withhold on NO-GO ⇒ the NO-GO trials are correct inhibitions.
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()

        val run = runs.listRecent(10).single()
        val inhibition = run.trials.outcomes.firstOrNull { it.kind == TrialOutcomeKind.CORRECT_INHIBITION }
        assertNotNull(inhibition, "a correct inhibition is recorded")
        assertTrue(inhibition.count > 0)
        coroutineContext.cancelChildren()
    }

    @Test
    fun the_shipped_production_default_config_runs_mixed_difficulty_end_to_end() = runTest {
        // No config override ⇒ the SHIPPED default GoNoGoConfig.mixedDifficulty() (3 levels). 45 trials ⇒ the run
        // saves with 45 completed trials and a computable index, proving the real production config is exercised.
        val runs = InMemoryCognitiveSessionRunStore()
        val h = GoNoGoSessionStateHolder(
            scope = this,
            sessionRecordStore = InMemorySessionRecordStore(),
            cognitiveRunStore = runs,
            clock = fixedClock,
            trialCount = 45,
        )
        h.startSession()
        repeat(45) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()
        val run = runs.listRecent(10).single()
        assertEquals(45, run.trials.completed)
        assertEquals(ScoreState.VALID, run.score.category, "a full clean run scores VALID (though preliminary)")
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 3: a "Volver" tap on the Finished screen (calling `abort()`) right after `finishSession()` claimed
     * the run for finalization must be a TOTAL no-op — it must not wipe the just-computed result and must not mark
     * the write-ahead record ABANDONED (never both a finalize AND an abandon for the same run).
     */
    @Test
    fun aborting_immediately_after_finishing_never_wipes_the_finished_result_or_abandons_the_record() = runTest {
        val records = InMemorySessionRecordStore()
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, records, runs)
        h.startSession()
        val sessionId = assertNotNull(h.state.value.sessionId)
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } } // the last trial triggers finishSession()
        assertEquals(GoNoGoPhase.Finished, h.state.value.phase, "finishing synchronously reaches Finished")
        val finishedScore = assertNotNull(h.state.value.score)

        h.abort() // simulates an immediate "Volver" tap, before the async seal below settles

        assertEquals(
            GoNoGoPhase.Finished,
            h.state.value.phase,
            "an abort on an already-finished (claimed) run must not wipe the result",
        )
        assertEquals(finishedScore, h.state.value.score)

        advanceUntilIdle()
        assertEquals(
            SessionStatus.FINALIZED,
            records.find(sessionId)?.status,
            "the record must end FINALIZED, never flipped to ABANDONED by the immediate back-tap",
        )
        assertEquals(1, runs.listRecent(10).size, "the completed run is still saved; abort never abandons it")
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 4: starting a new session while the previous one is still INCOMPLETE (never reached
     * `finishSession()`) must abandon the previous one exactly once — never orphan its write-ahead STARTED record.
     */
    @Test
    fun starting_a_new_session_while_the_previous_one_is_still_incomplete_abandons_it_exactly_once() = runTest {
        val records = InMemorySessionRecordStore()
        val h = holder(this, records)
        h.startSession()
        val firstId = assertNotNull(h.state.value.sessionId)
        h.onStimulusShown()
        h.onStimulusTap(300L) // one trial answered; the run is still INCOMPLETE (trialIndex 0 of 16)

        h.startSession() // starting again mid-play must abandon the incomplete previous run
        val secondId = assertNotNull(h.state.value.sessionId)
        assertNotEquals(firstId, secondId, "each start produces its own sessionId")
        advanceUntilIdle()

        assertEquals(
            SessionStatus.ABANDONED,
            records.find(firstId)?.status,
            "an incomplete run superseded by a new start must be abandoned, never left orphaned as STARTED",
        )
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 2: run A finishes (its seal is launched but not yet settled); run B starts immediately. When run
     * A's seal finally resolves, it must NEVER overwrite run B's (still in-progress) visible `saveState` — the
     * update is guarded by sessionId.
     */
    @Test
    fun completing_run_As_seal_after_run_B_has_started_never_leaks_As_saveState_onto_Bs_ui() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val h = holder(this, runs = runs)
        h.startSession()
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } } // run A finishes; seal launched, not advanced
        val runAId = assertNotNull(h.state.value.sessionId)
        assertEquals(GoNoGoPhase.Finished, h.state.value.phase)

        h.startSession() // run B starts immediately, before run A's async save has settled
        val runBId = assertNotNull(h.state.value.sessionId)
        assertNotEquals(runAId, runBId)
        assertEquals(GoNoGoPhase.Foreperiod, h.state.value.phase)

        advanceUntilIdle() // run A's seal now completes; a leaking update would overwrite run B's CURRENT saveState

        assertEquals(runBId, h.state.value.sessionId, "run B's session must still be the one visible")
        assertEquals(
            CognitiveRunSaveState.NotAttempted,
            h.state.value.saveState,
            "run A's completed save must never leak onto run B's still-in-progress UI",
        )
        assertTrue(
            runs.listRecent(10).any { it.sessionId == runAId },
            "run A must still be saved to history despite no longer being the visible session",
        )
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 5: a completed run's durable seal must not depend on the UI scope's coroutine ever getting a
     * chance to START — a plain `launch` can be cancelled before its body runs if the caller's scope (the
     * screen's `rememberCoroutineScope`) is torn down in the same tick `finishSession()` returns.
     */
    @Test
    fun a_completed_run_is_captured_even_if_the_ui_scope_is_cancelled_immediately_after_finishing() = runTest {
        val records = InMemorySessionRecordStore()
        val runs = InMemoryCognitiveSessionRunStore()
        val holderJob = Job(coroutineContext[Job])
        val holderScope = CoroutineScope(coroutineContext + holderJob)
        val h = holder(holderScope, records, runs)
        h.startSession()
        val sessionId = assertNotNull(h.state.value.sessionId)
        advanceUntilIdle() // let the write-ahead STARTED row settle before simulating the dispose race
        assertEquals(SessionStatus.STARTED, records.find(sessionId)?.status)

        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } } // the last trial launches the seal
        holderJob.cancel() // simulate the screen's scope being disposed immediately, before the seal coroutine runs
        advanceUntilIdle()

        assertEquals(
            SessionStatus.FINALIZED,
            records.find(sessionId)?.status,
            "a completed run must still be finalized even if its UI scope is torn down immediately",
        )
        assertEquals(1, runs.listRecent(10).size, "the completed run must still be saved despite the cancellation")
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 (kmp-architect follow-up): the write-ahead STARTED seed must land durably even if the UI scope is torn
     * down in the SAME tick `startSession()` returns — BEFORE its `onStart` coroutine ever runs. A plain `launch`
     * would be cancelled before its body executes (leaving no STARTED row, so a later `abort()` could write
     * ABANDONED over a never-written STARTED); `ATOMIC` + `NonCancellable` guarantees the STARTED write still
     * persists. Discriminates the fix: without it, `records.find(sessionId)` is null here; with it, it is STARTED.
     */
    @Test
    fun the_write_ahead_started_seed_persists_even_if_the_ui_scope_is_torn_down_before_it_runs() = runTest {
        val records = InMemorySessionRecordStore()
        val runs = InMemoryCognitiveSessionRunStore()
        val holderJob = Job(coroutineContext[Job])
        val holderScope = CoroutineScope(coroutineContext + holderJob)
        val h = holder(holderScope, records, runs)
        h.startSession()
        val sessionId = assertNotNull(h.state.value.sessionId)
        holderJob.cancel() // dispose the scope BEFORE advanceUntilIdle lets the onStart write-ahead coroutine run
        advanceUntilIdle()

        assertEquals(
            SessionStatus.STARTED,
            records.find(sessionId)?.status,
            "the write-ahead STARTED seed must persist even when the UI scope is cancelled before onStart runs",
        )
        coroutineContext.cancelChildren()
    }

    /**
     * P2-01 item 6: the UI must never claim "saved" before the durable `cognitiveRunStore.save()` actually
     * resolves — it stays in the pending [CognitiveRunSaveState.NotAttempted] default until then.
     */
    @Test
    fun the_ui_never_shows_saved_before_the_durable_store_save_completes() = runTest {
        val backing = InMemoryCognitiveSessionRunStore()
        val slowRuns = object : CognitiveSessionRunStore by backing {
            override suspend fun save(run: CognitiveSessionRun): SaveResult {
                delay(50)
                return backing.save(run)
            }
        }
        val h = GoNoGoSessionStateHolder(
            scope = this,
            sessionRecordStore = InMemorySessionRecordStore(),
            cognitiveRunStore = slowRuns,
            config = GoNoGoConfig(),
            clock = fixedClock,
            trialCount = 16,
        )
        h.startSession()
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        assertEquals(
            CognitiveRunSaveState.NotAttempted,
            h.state.value.saveState,
            "the UI must not show Saved before the durable store.save() resolves",
        )
        advanceUntilIdle()
        assertEquals(CognitiveRunSaveState.Saved, h.state.value.saveState)
        coroutineContext.cancelChildren()
    }
}
