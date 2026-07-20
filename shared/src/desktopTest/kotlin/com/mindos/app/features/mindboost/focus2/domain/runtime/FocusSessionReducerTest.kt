package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionEvent
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import com.mindos.app.features.mindboost.focus2.domain.telemetry.CanonEvent
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 · tarea (4) + (9) — the runtime, and the properties it exists to make provable.
 *
 * These run in plain JVM with no emulator, which is the payoff of a pure reducer: rotation is a serialization
 * round-trip and a DST jump is a second clock.
 */
class FocusSessionReducerTest {

    // --- fixtures ----------------------------------------------------------------------------------------

    private fun descriptor() = FocusSessionDescriptor.seal(
        sessionId = "s-1",
        exerciseId = "foc2-e07-filtro-vectorial",
        purpose = SessionMeasurementPurpose.MEASUREMENT,
        seedSource = SeedSource { SessionSeed(7L, RngVersion.V1) },
        generatorVersion = 1,
        scorerVersion = 1,
        constructModelVersion = 1,
        spec = VectorFilterSpec(),
        inputModality = InputModality.TOUCH_DIRECT,
        screenReaderActive = false,
    )

    /** Alternating signal/noise CONGRUENT trials — enough structure for every assertion in this file. */
    private val facts = List(SPARE_TRIALS) { TrialFacts(ConditionId.CONGRUENT, it % 2 == 0) }

    /** A clock whose monotonic readings come from a script and whose civil time is whatever we say it is. */
    private class ScriptedClock(
        private val steps: List<Long>,
        private val wallClock: List<Long> = emptyList(),
    ) : FocusClock {
        private var i = 0
        private var w = 0
        override fun elapsedNanos(): Long = steps[i++.coerceAtMost(steps.lastIndex)]
        override fun wallClockMillis(): Long =
            if (wallClock.isEmpty()) 0L else wallClock[w++.coerceAtMost(wallClock.lastIndex)]
    }

    /**
     * The BUG, written down so the property test has something to fail against: an engine that derives its
     * "elapsed" reading from civil time. Everything else about it is identical.
     */
    private class WallClockAnchoredClock(private val wallClock: List<Long>) : FocusClock {
        private var w = 0
        private fun tick(): Long = wallClock[w++.coerceAtMost(wallClock.lastIndex)]
        override fun elapsedNanos(): Long = tick() * NANOS_PER_MILLI
        override fun wallClockMillis(): Long = tick()
    }

    private fun runtime(clock: FocusClock, initial: FocusRuntimeSnapshot? = null) = FocusSessionRuntime(
        descriptor = descriptor(),
        trials = facts,
        taskSpec = VectorFilterTask.SPEC,
        clock = clock,
        initial = initial ?: FocusRuntimeSnapshot.initial("s-1"),
    )

    private fun steadyNanos(count: Int, stepMs: Long = 100L) =
        (0 until count).map { it * stepMs * NANOS_PER_MILLI }

    private fun FocusRuntimeSnapshot.congruent() =
        cells()[ConditionId.CONGRUENT] ?: error("no CONGRUENT tally")

    // --- the ordinary path -------------------------------------------------------------------------------

    @Test
    fun a_correct_response_is_counted_as_a_hit_and_advances_to_the_next_trial() {
        val runtime = runtime(ScriptedClock(listOf(0L, 0L, 250L * NANOS_PER_MILLI)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT) // trial 0 is signal; LEFT is the yes-response

        assertEquals(1, runtime.snapshot.congruent().hits)
        assertEquals(1, runtime.snapshot.resolvedTrials)
        assertEquals(1, runtime.snapshot.trialIndex)
        assertEquals(TrialPhase.INTER_STIMULUS, runtime.snapshot.phase)
    }

    @Test
    fun an_expired_window_is_counted_as_an_omission_never_as_a_wrong_answer() {
        val runtime = runtime(ScriptedClock(steadyNanos(3, stepMs = 2_000L)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.dispatch(FocusAction.EXPIRE_WINDOW)

        val cell = runtime.snapshot.congruent()
        assertEquals(1, cell.omissions)
        assertEquals(0, cell.misses, "an absent answer is not a wrong answer")
        assertEquals(0, cell.signalTrials, "an omission stays outside the 2x2")
    }

    /**
     * The window is a CONSTRAINT, not a label (efficacy gate BLOQUEA-2).
     *
     * Before this existed, `deadlineElapsedNanos` was written into the snapshot and never compared with
     * anything, so a response arriving an hour late was recorded as an ordinary answer while the trial's
     * `windowMs` claimed a 1200 ms deadline that nothing enforced.
     */
    @Test
    fun a_response_after_the_deadline_is_an_omission_not_a_late_hit() {
        val windowNanos = VectorFilterSpec().responseWindowMs * NANOS_PER_MILLI
        val runtime = runtime(ScriptedClock(listOf(0L, 0L, windowNanos + 1)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT) // correct side, but one nanosecond too late

        val cell = runtime.snapshot.congruent()
        assertEquals(1, cell.omissions, "past the deadline there is no answer to credit")
        assertEquals(0, cell.hits)
    }

    @Test
    fun a_response_exactly_on_the_deadline_still_counts() {
        val windowNanos = VectorFilterSpec().responseWindowMs * NANOS_PER_MILLI
        val runtime = runtime(ScriptedClock(listOf(0L, 0L, windowNanos)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)
        // The discriminating half of the test above: without it, a reducer that rejected EVERY response would
        // pass there. The boundary is inclusive, so "too late" really means later than the window.
        assertEquals(1, runtime.snapshot.congruent().hits)
    }

    @Test
    fun the_deadline_is_taken_from_the_sealed_spec_not_from_the_caller() {
        val runtime = runtime(ScriptedClock(listOf(0L, 0L)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        assertEquals(
            VectorFilterSpec().responseWindowMs * NANOS_PER_MILLI,
            runtime.snapshot.deadlineElapsedNanos,
        )
    }

    /**
     * An anticipation is a real, rare behavioural claim, so it must come from a real latency (efficacy
     * BLOQUEA-1). A reading BEFORE the presentation cannot happen inside one monotonic epoch; it happens when a
     * snapshot from a dead process is resumed against a fresh clock. The difference is negative, lands under
     * the 150 ms floor, and would be recorded as "answered before perception was possible".
     */
    @Test
    fun a_reading_earlier_than_the_presentation_never_manufactures_an_anticipation() {
        val stale = FocusRuntimeSnapshot(
            sessionId = "s-1",
            stateName = FocusSessionState.RUNNING.name,
            phaseName = TrialPhase.AWAITING_RESPONSE.name,
            trialIndex = 0,
            presentedAtElapsedNanos = 300L * 1_000_000_000L, // 300 s, from a previous process
            deadlineElapsedNanos = 300L * 1_000_000_000L + VectorFilterSpec().responseWindowMs * NANOS_PER_MILLI,
        )
        val fresh = runtime(ScriptedClock(listOf(400L * NANOS_PER_MILLI)), initial = stale)
        fresh.respond(Response.LEFT)

        // Nothing reaches a cell at all — not an anticipation, and not a hit either. An earlier version of
        // this test credited the hit, arguing that only the latency had been lost. That argument does not
        // survive contact with the two guards: with a dead-epoch anchor, `at <= deadline` is trivially true
        // and the anticipation floor is disabled by the null latency, so the trial would be scored as a CLEAN
        // hit for someone who may have deliberated for five minutes in a 1200 ms window. What is missing is
        // not the latency, it is whether there was any answer INSIDE the window — and that is unknowable, so
        // the trial is discarded on the same principle as any other interruption, not resolved in anyone's
        // favour.
        assertEquals(
            emptyMap(),
            fresh.snapshot.cells(),
            "nothing may be credited to any cell from a dead monotonic epoch",
        )
        assertEquals(1, fresh.snapshot.contaminatedCount, "and the loss is recorded, not silent")
        assertEquals(1, fresh.snapshot.trialIndex, "the session moves on rather than stalling on it")
    }

    @Test
    fun a_genuinely_fast_response_is_still_recorded_as_an_anticipation() {
        // The discriminating half: the guard above must not have disabled anticipation detection altogether.
        val runtime = runtime(ScriptedClock(listOf(0L, 0L, 50L * NANOS_PER_MILLI)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)
        assertEquals(1, runtime.snapshot.congruent().anticipations)
    }

    // --- REDTEAM #21: an interrupted trial is discarded, visibly --------------------------------------------

    @Test
    fun pausing_or_interrupting_mid_window_discards_that_trial_instead_of_answering_it() {
        listOf(FocusAction.PAUSE, FocusAction.INTERRUPT).forEach { action ->
            val runtime = runtime(ScriptedClock(steadyNanos(4)))
            runtime.dispatch(FocusAction.START_SESSION)
            runtime.dispatch(FocusAction.PRESENT_TRIAL)
            runtime.dispatch(action)

            assertEquals(1, runtime.snapshot.contaminatedCount, "$action must discard the open trial")
            assertEquals(0, runtime.snapshot.resolvedTrials, "$action must not record an outcome")
            assertEquals(1, runtime.snapshot.trialIndex, "the discarded trial is not re-presented")
            assertEquals(null, runtime.snapshot.deadlineElapsedNanos)
        }
    }

    @Test
    fun pausing_between_trials_discards_nothing() {
        val runtime = runtime(ScriptedClock(steadyNanos(5)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)
        runtime.dispatch(FocusAction.PAUSE)

        // The discriminating half: without it, a reducer that contaminated on EVERY pause would pass above.
        assertEquals(0, runtime.snapshot.contaminatedCount)
        assertEquals(1, runtime.snapshot.resolvedTrials)
    }

    // --- rejections are typed ------------------------------------------------------------------------------

    @Test
    fun an_illegal_lifecycle_event_is_rejected_without_changing_anything() {
        val runtime = runtime(ScriptedClock(steadyNanos(2)))
        val before = runtime.snapshot
        val reduction = runtime.dispatch(FocusAction.PAUSE) // IDLE cannot pause

        assertEquals(
            FocusRejection.IllegalLifecycle(FocusSessionState.IDLE, FocusSessionEvent.SESSION_PAUSED),
            reduction.rejection,
        )
        assertEquals(before, runtime.snapshot, "a rejected event must leave the state untouched")
        assertTrue(runtime.emissions.isEmpty(), "a rejected event must emit nothing")
    }

    @Test
    fun responding_with_no_open_window_is_rejected_rather_than_silently_ignored() {
        val runtime = runtime(ScriptedClock(steadyNanos(3)))
        runtime.dispatch(FocusAction.START_SESSION)
        val reduction = runtime.respond(Response.LEFT)
        assertIs<FocusRejection.TrialInputOutOfPhase>(reduction.rejection)
        assertEquals(0, runtime.snapshot.resolvedTrials)
    }

    @Test
    fun presenting_past_the_end_of_the_plan_is_rejected_instead_of_opening_a_dangling_window() {
        // A snapshot whose index already sits at the plan's length: the next presentation has no trial behind
        // it. Without the bound the reducer opened a window nothing could resolve, and the session ended with
        // it dangling.
        val runtime = runtime(
            ScriptedClock(steadyNanos(2)),
            initial = FocusRuntimeSnapshot(
                sessionId = "s-1",
                stateName = FocusSessionState.RUNNING.name,
                phaseName = TrialPhase.IDLE.name,
                trialIndex = SPARE_TRIALS,
            ),
        )
        val reduction = runtime.dispatch(FocusAction.PRESENT_TRIAL)
        assertIs<FocusRejection.TrialInputOutOfPhase>(reduction.rejection)
        assertFalse(runtime.snapshot.hasOpenWindow, "no window may open past the last planned trial")
    }

    @Test
    fun resuming_counts_the_resumes() {
        val runtime = runtime(ScriptedClock(steadyNanos(6)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PAUSE)
        runtime.dispatch(FocusAction.RESUME)
        runtime.dispatch(FocusAction.PAUSE)
        runtime.dispatch(FocusAction.RESUME)
        assertEquals(2, runtime.snapshot.resumeCount)
        assertEquals(FocusSessionState.RUNNING, runtime.snapshot.state)
    }

    // --- REDTEAM #22: rotation / recomposition (SAME process, same clock) ----------------------------------

    /**
     * Rotation, and ONLY rotation.
     *
     * An earlier version of this test presented at elapsed 0, which made the arithmetic come out right for
     * BOTH rotation and cross-process restore and therefore hid the fact that the second one was broken. The
     * presentation anchor here is deliberately non-zero so the assertion actually depends on it.
     */
    @Test
    fun rotation_preserves_the_running_window_and_measures_from_the_original_presentation() {
        val presentedAt = 5_000L * NANOS_PER_MILLI
        val first = runtime(ScriptedClock(listOf(presentedAt, presentedAt)))
        first.dispatch(FocusAction.START_SESSION)
        first.dispatch(FocusAction.PRESENT_TRIAL)
        val midWindow = first.snapshot
        assertTrue(midWindow.hasOpenWindow, "the fixture must actually be mid-window or this proves nothing")

        val restored = Json.decodeFromString<FocusRuntimeSnapshot>(Json.encodeToString(midWindow))
        assertEquals(midWindow, restored)
        assertEquals(presentedAt, restored.presentedAtElapsedNanos)

        // Same process, same clock: 400 ms after the ORIGINAL presentation, which is inside the window.
        val resumed = runtime(ScriptedClock(listOf(presentedAt + 400L * NANOS_PER_MILLI)), initial = restored)
        resumed.respond(Response.LEFT)
        assertEquals(1, resumed.snapshot.congruent().hits, "recomposition must not re-arm or void the window")
    }

    // --- REDTEAM #26a: civil time cannot touch the engine --------------------------------------------------

    @Test
    fun moving_the_wall_clock_thirteen_hours_changes_nothing_and_would_change_everything_if_anchored_to_it() {
        val monotonic = steadyNanos(6, stepMs = 300L)
        val calmWallClock = listOf(1_000L, 1_300L, 1_600L, 1_900L, 2_200L, 2_500L)
        val jumpedWallClock = calmWallClock.mapIndexed { i, ms ->
            if (i >= JUMP_AT) ms - THIRTEEN_HOURS_MS else ms
        }

        // The mid-window snapshot is captured as well as the final one. That is where the deadline still
        // exists, and the deadline is the quantity a civil-time jump would move — by the end of the trial the
        // anchors have been cleared, so comparing only final states would compare the two engines on a field
        // that no longer holds the evidence.
        data class Played(val midWindow: FocusRuntimeSnapshot, val end: FocusRuntimeSnapshot, val e: List<FocusEmission>)

        fun play(clock: FocusClock): Played {
            val runtime = runtime(clock)
            runtime.dispatch(FocusAction.START_SESSION)
            runtime.dispatch(FocusAction.PRESENT_TRIAL)
            val mid = runtime.snapshot
            runtime.respond(Response.LEFT)
            runtime.dispatch(FocusAction.PRESENT_TRIAL)
            runtime.dispatch(FocusAction.EXPIRE_WINDOW)
            return Played(mid, runtime.snapshot, runtime.emissions)
        }

        val calm = play(ScriptedClock(monotonic, calmWallClock))
        val jumped = play(ScriptedClock(monotonic, jumpedWallClock))
        assertEquals(calm, jumped, "a DST/timezone jump must not alter deadlines, outcomes, order or events")

        // The discriminating half. If the engine derived its elapsed reading from civil time — the defect this
        // property exists to forbid — the same jump moves the deadline by thirteen hours.
        val anchoredCalm = play(WallClockAnchoredClock(calmWallClock))
        val anchoredJumped = play(WallClockAnchoredClock(jumpedWallClock))
        assertNotEquals(
            anchoredCalm.midWindow.deadlineElapsedNanos,
            anchoredJumped.midWindow.deadlineElapsedNanos,
            "the wall-clock-anchored engine must be demonstrably broken, or this property proves nothing",
        )
    }

    // --- REDTEAM #26b: the seed is persisted before the first trial -----------------------------------------

    @Test
    fun the_seed_is_sealed_in_the_descriptor_before_any_trial_is_presented() {
        val runtime = runtime(ScriptedClock(steadyNanos(3)))
        val sealedHex = runtime.descriptor.seedHex
        assertEquals(0, runtime.snapshot.resolvedTrials, "no trial has run yet")

        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)
        assertEquals(sealedHex, runtime.descriptor.seedHex, "the seed must not change once trials have run")
    }

    // --- ADR-FOCUS-DATA option (i): the state cannot hold a raw trial --------------------------------------

    /**
     * The structural half of the NO-RAW rule.
     *
     * The snapshot is what gets written to disk, so the guarantee that no per-trial record is persisted is only
     * as strong as the guarantee that the snapshot cannot hold one. This asserts it on the serialized form,
     * where a stray field would actually show up.
     */
    @Test
    fun the_serialized_state_contains_counts_and_no_per_trial_record() {
        // A 400 ms step keeps the response above the anticipation floor, so this really does produce a HIT.
        val runtime = runtime(ScriptedClock(steadyNanos(6, stepMs = 400L)))
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)

        val encoded = Json.encodeToString(runtime.snapshot)
        listOf("rtNanos", "responseName", "responses", "trialIndices").forEach { forbidden ->
            assertTrue(forbidden !in encoded, "the persisted state must not carry '$forbidden'")
        }
        assertTrue("hits" in encoded, "and it must carry the counts, or it is measuring nothing")
    }

    // --- the emitted vocabulary ----------------------------------------------------------------------------

    @Test
    fun a_full_session_emits_the_canonical_lifecycle_and_trial_events_in_order() {
        val runtime = runtime(ScriptedClock(steadyNanos(8)))
        runtime.dispatch(FocusAction.START_TUTORIAL)
        runtime.dispatch(FocusAction.COMPLETE_TUTORIAL)
        runtime.dispatch(FocusAction.START_SESSION)
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
        runtime.respond(Response.LEFT)
        runtime.dispatch(FocusAction.COMPLETE)

        assertEquals(
            listOf(
                CanonEvent.TUTORIAL_STARTED,
                CanonEvent.TUTORIAL_COMPLETED,
                CanonEvent.SESSION_STARTED,
                CanonEvent.TRIAL_PRESENTED,
                CanonEvent.RESPONSE_SUBMITTED,
                CanonEvent.TRIAL_RESOLVED,
                CanonEvent.SESSION_COMPLETED,
            ),
            runtime.emissions.map { it.event },
        )
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val THIRTEEN_HOURS_MS = 13L * 60L * 60L * 1000L
        /**
         * The jump lands from the SECOND reading on, which is the one that presents the first trial and
         * therefore arms the deadline. Jumping later would leave the deadline untouched and the discriminating
         * half would compare two identical values — passing while proving nothing.
         */
        const val JUMP_AT = 1
        const val SPARE_TRIALS = 16
    }
}
