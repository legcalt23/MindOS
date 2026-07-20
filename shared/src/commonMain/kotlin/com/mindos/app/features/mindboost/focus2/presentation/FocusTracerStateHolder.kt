package com.mindos.app.features.mindboost.focus2.presentation

import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.focus2.data.session.FocusWriteOutcome
import com.mindos.app.features.mindboost.focus2.data.session.SecureFocusSessionStore
import com.mindos.app.features.mindboost.focus2.data.session.SessionScan
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusAction
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusClock
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRun
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionRuntime
import com.mindos.app.features.mindboost.focus2.domain.runtime.InputModality
import com.mindos.app.features.mindboost.focus2.domain.runtime.VectorFilterTask
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SessionDescriptive
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import com.mindos.app.features.mindboost.focus2.presentation.a11y.SequentialFocusCursor
import com.mindos.app.features.mindboost.focus2.presentation.ui.FocusTracerUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Focus V5 · S3 · tareas (4)+(6) — the shell.
 *
 * The plan's requirement is a holder that is "NO god-object: reducer puro + efectos", so this class owns as
 * little as it can get away with: it sequences effects and derives a UI state, and delegates every decision.
 * The lifecycle rules live in the state machine, the timing arithmetic in the reducer, the classification in
 * the scorer, and the durability in the store.
 *
 * The four things it does own are the four that no pure component can enforce from the inside:
 *
 *  1. **The write-ahead ORDER, and the drain that precedes it.** The seal must be on disk before the first
 *     trial appears, and any buffer a previous process left behind must be dealt with before this run adds
 *     another. If the seal fails the session still runs — the participant keeps their practice — but it is
 *     marked non-evidence for the rest of its life.
 *  2. **The RESPONSE INSTANT.** The clock is read where the tap arrives, not where the loop resumes; see
 *     [onResponse]. This is the single most load-bearing line in the class.
 *  3. **The TRIAL LOOP, and the fact that a pause SUSPENDS it.** An earlier draft iterated a fixed count with
 *     no regard for state, so a 30-second pause silently burned sixteen trials and the session was scored on a
 *     shorter dose than the one it had sealed.
 *  4. **PURGE ON COMPLETION, with its outcome consumed.** The buffer is deleted when the run reaches a terminal
 *     state, and whether that delete succeeded is reported rather than dropped: a purge whose failure nobody
 *     reads is retention with a comment claiming otherwise.
 */
class FocusTracerStateHolder(
    private val run: FocusRun,
    private val store: SecureFocusSessionStore,
    private val userId: String,
    private val clock: FocusClock,
) {
    private val runtime = FocusSessionRuntime(
        descriptor = run.descriptor,
        trials = run.trialFacts,
        taskSpec = VectorFilterTask.SPEC,
        clock = clock,
    )

    private var writeAheadSealed = false
    private var checkpointLost = false
    private var responseGate: CompletableDeferred<TimedResponse>? = null
    private var resumeGate: CompletableDeferred<Unit>? = null

    var cursor: SequentialFocusCursor<Response> = SequentialFocusCursor(listOf(Response.LEFT, Response.RIGHT))
        private set

    /**
     * Whether this run has not yet been disqualified as evidence.
     *
     * Three independent conditions, all of which must hold: the purpose allows it, the seal reached storage,
     * and no checkpoint has been lost. A `false` here is permanent for the session — evidence status can only
     * ever be withdrawn, never restored, because a gap that was not recorded cannot be un-gapped later.
     *
     * Note what this deliberately does NOT assert: that the session finished, or that it delivered the dose it
     * sealed. Those are only knowable at the end, and they are decided in [complete].
     */
    val evidenceAdmissible: Boolean
        get() = run.descriptor.evidenceDisposition() is ProfileEligibilityDisposition.Eligible &&
            writeAheadSealed &&
            !checkpointLost

    /** Trials this session actually accounted for: resolved into a cell, or discarded as contaminated. */
    private val deliveredTrials: Int
        get() = runtime.snapshot.resolvedTrials + runtime.snapshot.contaminatedCount

    /**
     * Runs the whole session: drain, seal, then trial after trial, then close and purge.
     *
     * Each trial opens its window and waits for either a response or the deadline, whichever comes first. The
     * timeout here and the reducer's deadline check are two enforcements of the same rule, and that redundancy
     * is deliberate: this one ends the WAIT, the reducer's decides the OUTCOME, and only the reducer's is
     * authoritative — because the instant it judges is captured at the tap, not at the resumption.
     *
     * While the session is paused the loop SUSPENDS on [resumeGate] rather than consuming iterations. Burning
     * planned trials during a pause would shorten the delivered dose while the record still claimed the sealed
     * one, which is the same "different instrument" failure the restore path exists to prevent.
     */
    suspend fun runSession(): FocusSessionResult {
        val start = start()
        run.trialFacts.indices.forEach { index ->
            resumeGate?.await()
            presentNext()
            val gate = CompletableDeferred<TimedResponse>().also { responseGate = it }
            // The window comes from the SEALED descriptor (the measurement authority — no call site may choose
            // its own). The inter-stimulus gap comes from the PLAN's per-trial resolved value, not from a
            // session-level spec field: it is presentation pacing, not a measured parameter, and a per-trial gap
            // is what a jittered mechanic like the vigilance tracer needs. For the flanker every gap is the same
            // constant, so this is behaviour-identical there (S4 · MB-FOCUS-0041).
            val answered = withTimeoutOrNull(run.descriptor.spec.responseWindowMs) { gate.await() }
            responseGate = null
            submit(answered?.response ?: Response.NONE, answered?.atElapsedNanos ?: clock.elapsedNanos())
            delay(run.plan.trials[index].interStimulusDelayMs)
        }
        return FocusSessionResult(complete(), start)
    }

    /**
     * Drains any buffer a previous process left behind, then seals this run and starts it.
     *
     * The drain is not housekeeping: without it a session killed by the OS leaves its buffer encrypted on disk
     * with nothing on the device that ever looks at it again, which is silent retention however good the
     * intentions were. A stale buffer is DELETED rather than resumed — resuming across process death would need
     * the plan regenerated from the sealed seed, which is not built; merging a stale snapshot into a
     * freshly-seeded plan would splice two different instruments (that is why the old `adopt` path is gone).
     * A scan that FAILED deletes nothing: refusing to enumerate is not permission to destroy what could not be
     * enumerated.
     *
     * PRECONDITION, declared and not yet enforced (`MB-FOCUS-0035`): at most one holder is alive per user per
     * process. The drain cannot distinguish a dead process's orphan from a second live holder's session, so a
     * concurrent `start()` would delete a sibling's in-flight buffer. This is safe only because the tracer is
     * not mounted; the slice that mounts it owns enforcing single-instance (or a liveness floor) FIRST.
     */
    suspend fun start(): FocusStartOutcome {
        val scan = store.scanSessionIds(userId)
        if (scan is SessionScan.Scanned) {
            scan.sessionIds.forEach { id -> store.discard(userId, id) }
        }
        val sealed = store.sealDescriptor(userId, run.descriptor)
        writeAheadSealed = sealed is FocusWriteOutcome.Stored
        runtime.dispatch(FocusAction.START_SESSION)
        return when (sealed) {
            is FocusWriteOutcome.Stored -> FocusStartOutcome.Measuring
            is FocusWriteOutcome.Failed -> FocusStartOutcome.PracticeOnly(sealed.reason)
        }
    }

    /** Presents the next trial. The window opens here and nowhere else. */
    fun presentNext() {
        runtime.dispatch(FocusAction.PRESENT_TRIAL)
    }

    /**
     * Called by the UI the moment the participant answers.
     *
     * **The clock is read HERE**, and that placement is the whole point. An earlier draft carried only the
     * response through the gate and let the runtime read the clock after `await()` resumed, which timed the
     * dispatcher rather than the person: a tap at 138 ms resuming 18 ms later was read as 156 ms and cleared
     * the 150 ms anticipation floor. The error is not symmetric — scheduling delay only ever pushes a reading
     * later — so it biased systematically towards accepting fast guessing as a genuine hit.
     */
    fun onResponse(response: Response) {
        responseGate?.complete(TimedResponse(response, clock.elapsedNanos()))
    }

    /**
     * Resolves the trial and checkpoints.
     *
     * `Response.NONE` is how an expired window arrives; there is no separate entry point for it, because there
     * is no separate FACT — the reducer records both as a closed window with no answer, and neither produces a
     * latency. A second method would only invite the two to drift apart.
     */
    suspend fun submit(response: Response, atElapsedNanos: Long = clock.elapsedNanos()) {
        if (runtime.respond(response, atElapsedNanos).wasRejected) return
        checkpoint()
    }

    /**
     * Pauses or resumes, and holds the trial loop for the duration.
     *
     * One method rather than two because they are one decision with two values. The pause side has two extra
     * obligations: arm the gate the loop waits on, and checkpoint — pausing may have discarded the open trial,
     * and that discard has to reach disk before the app can be backgrounded. Neither happens if the lifecycle
     * machine rejected the transition, because then nothing was paused.
     */
    suspend fun setPaused(paused: Boolean) {
        if (runtime.dispatch(if (paused) FocusAction.PAUSE else FocusAction.RESUME).wasRejected) return
        if (paused) {
            resumeGate = CompletableDeferred()
            checkpoint()
        } else {
            resumeGate?.complete(Unit)
            resumeGate = null
        }
    }

    /** Moves the switch-access focus. Pure UI state; it never touches the session. */
    fun advanceCursor() {
        cursor = cursor.advance()
    }

    /**
     * Closes the session, scores it from the running tallies, and PURGES the buffer.
     *
     * It REFUSES rather than scoring when `COMPLETE` is not a legal transition — from PAUSED, for instance. An
     * earlier draft discarded the dispatch result and scored anyway, so a paused session that was never
     * completed came back with a full verdict and admissible evidence.
     *
     * The purge is not housekeeping either. The buffer exists to survive process death during the run; once
     * the run is over it is session data with no purpose. Its outcome is carried out in the result: a delete
     * that failed leaves counts on disk, and a caller that cannot see that cannot retry it.
     */
    suspend fun complete(): FocusCompletion {
        if (runtime.dispatch(FocusAction.COMPLETE).wasRejected) {
            return FocusCompletion.Refused(runtime.snapshot.state)
        }
        val descriptive = DescriptiveScorer.scoreCells(runtime.snapshot.cells(), VectorFilterTask.SPEC)
        val delivered = deliveredTrials
        val sealed = run.trialFacts.size
        return FocusCompletion.Scored(
            descriptive = descriptive,
            deliveredTrials = delivered,
            sealedTrials = sealed,
            // The dose actually delivered must equal the dose sealed before the first trial. A short session
            // scored against blocks of the full length is a different instrument wearing the same name.
            evidenceAdmissible = evidenceAdmissible && delivered == sealed,
            bufferPurged = store.discard(userId, run.descriptor.sessionId) is FocusWriteOutcome.Stored,
        )
    }

    /** Abandons the session. Same purge obligation as [complete], and the same duty to report its outcome. */
    suspend fun abandon(): FocusWriteOutcome {
        runtime.dispatch(FocusAction.INTERRUPT)
        return store.discard(userId, run.descriptor.sessionId)
    }

    val uiState: FocusTracerUiState get() = FocusTracerUiState(
        trial = run.plan.trials.getOrNull(runtime.snapshot.trialIndex),
        phase = runtime.snapshot.phase,
        modality = run.descriptor.inputModality,
        trialNumber = (runtime.snapshot.trialIndex + 1).coerceAtMost(run.plan.trials.size),
        totalTrials = run.plan.trials.size,
        cursor = cursor.takeIf { run.descriptor.inputModality == InputModality.SWITCH_SEQUENTIAL },
        // Read from the STATE, not the phase. Deriving it from `phase == IDLE` made the button lie during the
        // inter-stimulus gap — a third of every cycle — where the phase is unchanged by pausing, so it still
        // read "Pause", called `onPause` again, was rejected, and left the participant unable to resume.
        paused = runtime.snapshot.state == FocusSessionState.PAUSED,
    )

    private suspend fun checkpoint() {
        if (store.checkpoint(userId, runtime.snapshot) is FocusWriteOutcome.Failed) {
            // A lost checkpoint means the record on disk no longer matches what happened. The run continues as
            // practice; it does not continue as measurement. Never reset: a gap cannot be un-gapped.
            checkpointLost = true
        }
    }
}

/** A response and the monotonic instant it arrived at, captured together so they cannot drift apart. */
data class TimedResponse(val response: Response, val atElapsedNanos: Long)

/** What starting a session actually achieved. Never a bare Boolean: the reason matters to the disclaimer. */
sealed interface FocusStartOutcome {
    data object Measuring : FocusStartOutcome

    /** The seal did not reach storage. The session runs, and it will never count as evidence. */
    data class PracticeOnly(val reason: String) : FocusStartOutcome
}

/**
 * How a session ended.
 *
 * `Refused` is not an error path for the participant — it means the engine was asked to finish something that
 * was not finishable from its current state, and produced no numbers rather than numbers from a partial run.
 */
sealed interface FocusCompletion {
    data class Scored(
        val descriptive: SessionDescriptive,
        val deliveredTrials: Int,
        val sealedTrials: Int,
        val evidenceAdmissible: Boolean,
        val bufferPurged: Boolean,
    ) : FocusCompletion

    data class Refused(val state: FocusSessionState) : FocusCompletion
}

/** Everything a finished run yields: how it ended, and how it began. */
data class FocusSessionResult(
    val completion: FocusCompletion,
    val startOutcome: FocusStartOutcome,
)
