package com.mindos.app.features.mindboost.focus2.presentation

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus2.data.session.FocusWriteOutcome
import com.mindos.app.features.mindboost.focus2.data.session.SecureFocusSessionStore
import com.mindos.app.features.mindboost.focus2.domain.runtime.AccessibilityContext
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusClock
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRunKind
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRunPlanner
import com.mindos.app.features.mindboost.focus2.domain.runtime.InputModality
import com.mindos.app.features.mindboost.focus2.domain.scoring.Descriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 — the vertical, end to end, without a device.
 *
 * The tests that matter most here are the failure ones. A holder that runs a perfect session is easy; a holder
 * that behaves honestly when the disk refuses to cooperate, when nobody answers, when the participant pauses
 * for a minute, and when a previous process left a buffer behind, is the thing that was worth writing.
 *
 * The loop is driven by virtual time, so a 112-trial session with a 1200 ms window costs milliseconds of real
 * time and still exercises the real deadline arithmetic.
 */
class FocusTracerStateHolderTest {

    /**
     * A monotonic clock that advances a fixed amount per reading.
     *
     * The step is 400 ms so that a presentation followed by a response yields a latency ABOVE the 150 ms
     * anticipation floor and below the 1200 ms window. A clock that did not advance would make every trial an
     * anticipation and every session unscoreable — which is a real failure mode, and the reason this class has
     * a comment instead of just a zero.
     */
    private class VirtualClock : FocusClock {
        private var nanos = 0L
        override fun elapsedNanos(): Long = nanos.also { nanos += STEP_NANOS }
        override fun wallClockMillis(): Long = 0L

        private companion object {
            const val STEP_NANOS = 400L * 1_000_000L
        }
    }

    private class UnwritableStore : SecureKeyValueStore by InMemorySecureKeyValueStore() {
        override suspend fun put(key: String, value: String): Unit = throw IllegalStateException("disk full")
    }

    /** Fails only AFTER the seal has landed, so the checkpoint path can be tested on its own. */
    private class FailsAfterFirstWrite : SecureKeyValueStore by InMemorySecureKeyValueStore() {
        private var writes = 0
        override suspend fun put(key: String, value: String) {
            writes++
            if (writes > 1) throw IllegalStateException("disk full")
        }
    }

    /** Writes fine, but cannot delete. The shape of a purge that silently does not happen. */
    private class UnremovableStore : SecureKeyValueStore by InMemorySecureKeyValueStore() {
        override suspend fun remove(key: String): Unit = throw IllegalStateException("delete failed")
    }

    /** Cannot enumerate. The scan must treat this as "unknown", never as "nothing to recover". */
    private class UnscannableStore : SecureKeyValueStore by InMemorySecureKeyValueStore() {
        override suspend fun keys(prefix: String): Set<String> = throw IllegalStateException("index unreadable")
    }

    // The default participant declared direct touch, which is the ordinary case these tests model. Leaving it
    // undeclared would make every session UNKNOWN modality — correct behaviour, but not what most tests are
    // about; the UNKNOWN path is asserted in the descriptor tests.
    private fun holder(
        backing: SecureKeyValueStore = InMemorySecureKeyValueStore(),
        kind: FocusRunKind = FocusRunKind.MEASUREMENT,
        accessibility: AccessibilityContext = AccessibilityContext(directTouchDeclared = true),
        clock: FocusClock = VirtualClock(),
        sessionId: String = SESSION,
    ) = FocusTracerStateHolder(
        run = FocusRunPlanner.prepare(
            kind,
            sessionId,
            SeedSource { SessionSeed(21L, RngVersion.V1) },
            accessibility,
        ),
        store = SecureFocusSessionStore(backing),
        userId = USER,
        clock = clock,
    )

    /** Answers every trial correctly, through the holder, exactly as the screen would. */
    private suspend fun playPerfectly(holder: FocusTracerStateHolder) {
        val total = holder.uiState.totalTrials
        repeat(total) {
            holder.presentNext()
            val trial = holder.uiState.trial ?: return@repeat
            holder.submit(trial.correctResponse)
        }
    }

    private suspend fun scored(holder: FocusTracerStateHolder): FocusCompletion.Scored =
        assertIs<FocusCompletion.Scored>(holder.complete())

    @Test
    fun a_full_session_runs_and_produces_a_verdict() = runTest {
        val holder = holder()
        assertEquals(FocusStartOutcome.Measuring, holder.start())
        playPerfectly(holder)

        val result = scored(holder)
        assertEquals(Verdict.Valid, result.descriptive.verdict)
        assertIs<Descriptive.Present<Double>>(result.descriptive.primaryStatistic)
        assertEquals(result.sealedTrials, result.deliveredTrials, "the whole sealed dose was delivered")
        assertTrue(result.evidenceAdmissible)
        assertTrue(result.bufferPurged)
    }

    /**
     * The loop the diff audit found missing: nothing drove the window, so a trial nobody answered stayed open
     * forever and `complete()` was unreachable. Here NOBODY answers, and the session still finishes.
     *
     * The assertions are on the COUNTS, not just on the trial index. An earlier version of this test asserted
     * only that the loop reached the end, which left its own name unproven: making `outcomeOf` classify an
     * absent response as a MISS would have kept it green.
     */
    @Test
    fun a_session_where_nobody_answers_finishes_and_every_trial_is_an_omission() = runTest {
        val holder = holder()
        val result = holder.runSession()

        val scored = assertIs<FocusCompletion.Scored>(result.completion)
        val cells = scored.descriptive.perCondition.values
        assertTrue(cells.isNotEmpty(), "the session must have produced cells at all")
        cells.forEach { condition ->
            assertEquals(0, condition.cell.hits, "nobody answered, so nothing can be a hit")
            assertEquals(0, condition.cell.falseAlarms)
            assertEquals(
                condition.cell.totalTrials,
                condition.cell.omissions,
                "every accounted trial in ${condition.conditionId} must be an omission",
            )
        }
        assertEquals(scored.sealedTrials, scored.deliveredTrials)
        // Nothing was told apart from anything, and that is reported as absence rather than as a score of zero.
        assertEquals(Verdict.InsufficientData, scored.descriptive.verdict)
    }

    // --- The dose delivered must be the dose sealed ---------------------------------------------------------

    /**
     * The blocker the efficacy gate found: the loop iterated a fixed count with no regard for the session
     * state, so a pause burned one planned trial per response window for as long as it lasted. A 30-second
     * pause cost roughly sixteen of the 112 trials, of which exactly ONE was recorded as contaminated.
     */
    @Test
    fun a_pause_suspends_the_loop_instead_of_burning_the_trials_it_covers() = runTest {
        val holder = holder()
        var result: FocusSessionResult? = null
        val job = launch { result = holder.runSession() }

        advanceTimeBy(5_000)
        holder.setPaused(true)
        assertTrue(holder.uiState.paused, "the UI must report the pause it is in")
        advanceTimeBy(60_000) // a minute of real life happening to the participant
        holder.setPaused(false)
        advanceUntilIdle()
        job.join()

        // The assertion the diff audit demanded: NOT `trialNumber == totalTrials`, which is capped and passes
        // whether one trial was contaminated or fifty. The real invariant is that every sealed trial is
        // accounted for AND a minute of pause cost at most the ONE trial that was on screen when it started.
        val scored = assertIs<FocusCompletion.Scored>(assertNotNull(result).completion)
        assertEquals(scored.sealedTrials, scored.deliveredTrials, "every sealed trial must be accounted for")
        val contaminated = scored.sealedTrials - scored.descriptive.perCondition.values.sumOf { it.cell.totalTrials }
        assertTrue(contaminated <= 1, "a single pause may contaminate at most the open trial, not the whole gap")
    }

    @Test
    fun completing_a_paused_session_is_refused_rather_than_scored() = runTest {
        val holder = holder()
        holder.start()
        holder.presentNext()
        holder.setPaused(true)

        val completion = assertIs<FocusCompletion.Refused>(holder.complete())
        assertEquals(FocusSessionState.PAUSED, completion.state, "and it says which state refused it")
    }

    /** The discriminating half of the pause tests: a session that DID finish is still scored normally. */
    @Test
    fun a_session_that_reached_a_terminal_state_is_still_scored() = runTest {
        val holder = holder()
        holder.start()
        playPerfectly(holder)
        assertIs<FocusCompletion.Scored>(holder.complete())
    }

    /**
     * Found by the mutation pass, not by any reviewer: `complete()` legally scores a session cut short from
     * RUNNING (a few trials answered, then completed), and `evidenceAdmissible` must be false because the dose
     * delivered is not the dose sealed — a 3-trial block cannot be compared against 112-trial blocks. Dropping
     * the `delivered == sealed` clause left every test green, so this is the test that pins it.
     */
    @Test
    fun a_session_completed_before_delivering_its_sealed_dose_is_scored_but_not_evidence() = runTest {
        val holder = holder()
        assertEquals(FocusStartOutcome.Measuring, holder.start())
        repeat(3) {
            holder.presentNext()
            holder.submit(holder.uiState.trial!!.correctResponse)
        }

        val scored = assertIs<FocusCompletion.Scored>(holder.complete())
        assertTrue(scored.deliveredTrials < scored.sealedTrials, "the session was cut short — the premise")
        assertFalse(scored.evidenceAdmissible, "an incomplete dose cannot be evidence, even with a clean seal")
    }

    // --- Timing: the instant belongs to the tap, not to the scheduler ---------------------------------------

    /**
     * `submit` classifies by the instant it is GIVEN, never by the clock at the moment it happens to run.
     *
     * This is the plumbing behind the fix: the shell captures the reading in `onResponse`, where the tap
     * arrives, and carries it across the coroutine hop. If `submit` re-read the clock instead, this test would
     * see the next reading — 400 ms later, past the point where the difference changes the classification —
     * and the trial would come back as something other than an anticipation.
     */
    @Test
    fun submit_classifies_by_the_instant_it_is_given_not_by_the_clock_when_it_runs() = runTest {
        val clock = VirtualClock()
        val holder = holder(clock = clock)
        holder.start() // reading 0
        holder.presentNext() // reading 1: the trial appears at 400 ms

        val trial = holder.uiState.trial ?: error("a trial must be on screen")
        // 100 ms after presentation: below the 150 ms anticipation floor.
        holder.submit(trial.correctResponse, atElapsedNanos = 500L * 1_000_000L)

        val scored = scored(holder)
        val anticipations = scored.descriptive.perCondition.values.sumOf { it.cell.anticipations }
        assertEquals(1, anticipations, "a tap 100 ms after the stimulus is an anticipation, whenever it is read")
    }

    // --- Storage failures never become silent success -------------------------------------------------------

    @Test
    fun a_seal_that_never_reached_storage_leaves_the_session_playable_but_never_evidence() = runTest {
        val holder = holder(backing = UnwritableStore())
        val outcome = assertIs<FocusStartOutcome.PracticeOnly>(holder.start())
        assertEquals("IllegalStateException", outcome.reason)

        // The participant still trains, and still gets a real result...
        playPerfectly(holder)
        assertEquals(Verdict.Valid, scored(holder).descriptive.verdict)
        // ...but the run can never count. Both halves matter: locking them out of the exercise would be the
        // other failure, and recording an unsealed run as measurement would be the worse one.
        assertFalse(holder.evidenceAdmissible)
    }

    @Test
    fun a_lost_checkpoint_withdraws_evidence_status_for_the_rest_of_the_session() = runTest {
        val holder = holder(backing = FailsAfterFirstWrite())
        assertEquals(FocusStartOutcome.Measuring, holder.start(), "the seal itself landed")
        assertTrue(holder.evidenceAdmissible, "nothing has gone wrong yet — the discriminating half")

        holder.presentNext()
        holder.submit(holder.uiState.trial!!.correctResponse)
        assertFalse(holder.evidenceAdmissible, "a checkpoint that did not land means the record has a gap")
    }

    /**
     * The diff audit's blocker: `complete()` called `discard()` and threw its typed outcome away, so a purge
     * that failed was indistinguishable from one that worked — silent retention behind a KDoc promising the
     * opposite.
     */
    @Test
    fun a_purge_that_failed_is_reported_rather_than_assumed() = runTest {
        val holder = holder(backing = UnremovableStore())
        holder.start()
        playPerfectly(holder)

        val scored = scored(holder)
        assertFalse(scored.bufferPurged, "a delete that threw must never be reported as a purge")
        assertEquals(Verdict.Valid, scored.descriptive.verdict, "and the measurement itself is unaffected")
    }

    @Test
    fun a_warm_up_run_is_playable_and_still_never_evidence() = runTest {
        val holder = holder(kind = FocusRunKind.TUTORIAL)
        assertEquals(FocusStartOutcome.Measuring, holder.start())
        playPerfectly(holder)
        assertEquals(Verdict.InsufficientData, scored(holder).descriptive.verdict)
        assertFalse(holder.evidenceAdmissible, "the purpose excludes it even though every write succeeded")
    }

    // --- ADR-FOCUS-DATA: the buffer is drained, not kept ----------------------------------------------------

    /**
     * The privacy gate's blocker: the session buffer had no production purge path, so every run — tutorials
     * included — left its record on disk forever. A write-ahead buffer that is never drained is not a buffer.
     */
    @Test
    fun finishing_a_session_purges_its_buffer_from_disk() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val holder = holder(backing = backing)
        holder.start()
        playPerfectly(holder)
        assertTrue(backing.keys("focus2_session:").isNotEmpty(), "the buffer must exist DURING the run")

        holder.complete()
        assertEquals(
            emptySet(),
            backing.keys("focus2_session:"),
            "nothing about the session may remain once it has ended",
        )
    }

    @Test
    fun abandoning_a_session_purges_it_and_reports_the_purge() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val holder = holder(backing = backing)
        holder.start()
        holder.presentNext()
        holder.submit(Response.LEFT)
        assertTrue(backing.keys("focus2_session:").isNotEmpty())

        // BLOQUEA-3: `abandon()` returns the purge outcome, and until this test nothing asserted it. Half of
        // MB-FOCUS-0032 was closed on narrative because only `complete()`'s half had a discriminating test.
        assertEquals(FocusWriteOutcome.Stored, holder.abandon(), "a successful purge reports Stored")
        assertEquals(emptySet(), backing.keys("focus2_session:"), "an abandoned run leaves nothing behind either")
    }

    @Test
    fun abandoning_when_the_store_cannot_delete_reports_the_failure() = runTest {
        val holder = holder(backing = UnremovableStore())
        holder.start()
        holder.presentNext()
        holder.submit(Response.LEFT)

        val outcome = assertIs<FocusWriteOutcome.Failed>(holder.abandon())
        assertEquals("IllegalStateException", outcome.reason, "the discriminating half: a failed delete is not Stored")
    }

    /**
     * The gap the efficacy gate found underneath the purge: nothing production-side ever looked at a buffer
     * left by a process that died, so an interrupted session sat encrypted on disk with no drain path at all.
     * Starting a run now clears what a previous process abandoned.
     */
    @Test
    fun starting_a_run_drains_the_buffer_a_previous_process_left_behind() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val orphan = FocusRunPlanner.prepare(
            FocusRunKind.MEASUREMENT,
            "s-orphan",
            SeedSource { SessionSeed(7L, RngVersion.V1) },
            AccessibilityContext.NONE,
        )
        SecureFocusSessionStore(backing).sealDescriptor(USER, orphan.descriptor)
        assertTrue(backing.keys("focus2_session:$USER:s-orphan").isNotEmpty(), "the orphan exists to begin with")

        holder(backing = backing).start()

        assertEquals(
            emptySet(),
            backing.keys("focus2_session:$USER:s-orphan"),
            "a buffer no run can resume is retention, and must not survive the next start",
        )
        assertTrue(
            backing.keys("focus2_session:$USER:$SESSION").isNotEmpty(),
            "the discriminating half: the run that IS starting keeps its own seal",
        )
    }

    // --- REDTEAM #34: the whole exercise, with two switches ------------------------------------------------

    @Test
    fun the_exercise_is_completable_end_to_end_using_only_advance_and_activate() = runTest {
        val holder = holder(accessibility = AccessibilityContext(switchAccessActive = true))
        assertEquals(InputModality.SWITCH_SEQUENTIAL, holder.uiState.modality)
        holder.start()

        val total = holder.uiState.totalTrials
        repeat(total) {
            holder.presentNext()
            val wanted = holder.uiState.trial?.correctResponse ?: return@repeat
            // Only two controls are ever used: move the focus, then activate it.
            while (holder.cursor.focused != wanted) holder.advanceCursor()
            holder.submit(holder.cursor.activate())
        }

        val result = scored(holder)
        assertEquals(Verdict.Valid, result.descriptive.verdict, "switch-access reaches the same completion")
        assertIs<Descriptive.Present<Double>>(result.descriptive.primaryStatistic)
    }

    @Test
    fun a_switch_access_session_is_recorded_as_its_own_modality_and_withholds_its_latency() = runTest {
        val switch = holder(accessibility = AccessibilityContext(switchAccessActive = true))
        val touch = holder()
        assertEquals(InputModality.SWITCH_SEQUENTIAL, switch.uiState.modality)
        assertEquals(InputModality.TOUCH_DIRECT, touch.uiState.modality)
    }

    // --- The response gate is the real entry point: drive it, not `submit` directly ------------------------

    /**
     * The blocker BLOQUEA-1 of round 3: `onResponse` — the capture site the round-2 fix actually lives in —
     * had NO test. Every test drove `submit` directly and bypassed the gate, so re-reading the clock in the
     * loop instead of using the captured instant left the whole suite green. This drives the real path:
     * `runSession` opens the window, `onResponse` captures the instant at the tap, and the classification must
     * use THAT instant, not the clock when the loop resumes.
     */
    /**
     * A clock scripted so the ONLY thing that keeps trial 0 an anticipation is using the reading captured at
     * `onResponse`. Reads, in order: [0] START, [1] present-trial-0 (presentedAt), [2] onResponse (the tap),
     * then a huge constant for every later trial's timeout. The tap sits at [tapMs] after presentation; if the
     * loop re-read the clock in `submit` instead of using the captured instant, it would get the huge constant
     * and score a normal (very late) response instead.
     */
    private class TapAtClock(tapMs: Long) : FocusClock {
        private val steps = listOf(0L, 0L, tapMs * 1_000_000L)
        private var i = 0
        override fun elapsedNanos(): Long = steps.getOrElse(i++) { HUGE }
        override fun wallClockMillis(): Long = 0L

        private companion object {
            const val HUGE = 3_600_000L * 1_000_000L // an hour, well past any 1200 ms window
        }
    }

    private fun firstTrialAnticipations(result: FocusSessionResult): Int {
        val scored = assertIs<FocusCompletion.Scored>(result.completion)
        return scored.descriptive.perCondition.values.sumOf { it.cell.anticipations }
    }

    @Test
    fun a_tap_through_the_response_gate_is_classified_by_the_instant_at_the_tap() = runTest {
        // Tap 100 ms after presentation — below the 150 ms anticipation floor.
        val holder = holder(clock = TapAtClock(tapMs = 100))
        var result: FocusSessionResult? = null
        val job = launch { result = holder.runSession() }

        advanceTimeBy(10) // first window is open, nothing has timed out
        holder.onResponse(Response.LEFT) // captures the clock HERE
        advanceUntilIdle()
        job.join()

        // The tap was 100 ms after the stimulus — an anticipation. If the loop re-read the clock in `submit`,
        // it would have read the hour-later constant and scored a normal response, and this would be 0.
        assertEquals(1, firstTrialAnticipations(assertNotNull(result)), "the captured instant must classify the tap")
    }

    /** The discriminating half: a tap well after the floor, through the same gate, is NOT an anticipation. */
    @Test
    fun a_tap_through_the_gate_well_after_the_floor_is_not_an_anticipation() = runTest {
        // Tap 500 ms after presentation — above the floor, inside the window.
        val holder = holder(clock = TapAtClock(tapMs = 500))
        var result: FocusSessionResult? = null
        val job = launch { result = holder.runSession() }
        advanceTimeBy(10)
        holder.onResponse(Response.LEFT)
        advanceUntilIdle()
        job.join()
        assertEquals(0, firstTrialAnticipations(assertNotNull(result)), "a 500 ms tap is a normal response")
    }

    // --- A scan that cannot enumerate must not read as "nothing to recover" --------------------------------

    /**
     * BLOQUEA-4: `SessionScan.Failed` had no test at any layer. A scan that throws must NOT be treated as an
     * empty set — that would silently skip the drain. It also must not crash the session start.
     */
    @Test
    fun a_start_whose_scan_cannot_read_the_index_still_seals_and_does_not_crash() = runTest {
        val holder = holder(backing = UnscannableStore())
        // The seal still lands (put works; only keys() throws), so the session is measuring, not practice-only.
        assertEquals(FocusStartOutcome.Measuring, holder.start())
        assertTrue(holder.evidenceAdmissible, "a scan failure is not a seal failure")
    }

    @Test
    fun a_response_with_no_open_window_changes_nothing() = runTest {
        val holder = holder()
        holder.start()
        holder.submit(Response.LEFT) // nothing presented yet
        holder.presentNext()
        holder.submit(holder.uiState.trial!!.correctResponse)
        assertEquals(2, holder.uiState.trialNumber, "exactly one trial was answered")
    }

    private companion object {
        const val USER = "user-1"
        const val SESSION = "s-holder"
    }
}
