package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.timing.TemporalWindowVerdict
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TaskSpec
import kotlin.time.TimeSource

/**
 * Focus V5 · S3 · tarea (4) + REDTEAM #26 — the two clocks a session needs, kept deliberately apart.
 *
 * [elapsedNanos] is MONOTONIC and is the only thing timing may ever be derived from. [wallClockMillis] exists
 * solely to label a record with "when did this happen" for a human reading a history; nothing in the engine may
 * compute a duration, a deadline or an ordering from it. Splitting them into two named methods is what makes a
 * violation visible in review: `deadline = wallClockMillis() + window` reads wrong at a glance, whereas a single
 * `now()` used for both purposes reads fine and is the bug.
 */
interface FocusClock {
    /** Monotonic elapsed time. Unaffected by time-zone, DST or a user changing the device clock. */
    fun elapsedNanos(): Long

    /** Civil time, for labelling only. Never an input to a duration. */
    fun wallClockMillis(): Long
}

/**
 * The real clock: `TimeSource.Monotonic` for elapsed time, and the injected [epochMillis] for labels.
 *
 * The epoch reader is a parameter rather than a direct `Clock.System.now()` call because holders and engines in
 * this repo must not reach for the system clock themselves (charter §5, and kotlinx-datetime 0.7.1 removed
 * `Clock.System` from the desktop test runtime — learning 2026-05-29).
 *
 * Note what this class makes true and what it therefore forbids: its `mark` is taken when the object is
 * CONSTRUCTED, so a new process starts a new monotonic epoch at zero. Elapsed readings from a previous process
 * are meaningless here, which is why `FocusSessionRestore` discards any window that was open when the process
 * died rather than resuming it.
 */
class MonotonicFocusClock(private val epochMillis: () -> Long) : FocusClock {
    private val mark = TimeSource.Monotonic.markNow()
    override fun elapsedNanos(): Long = mark.elapsedNow().inWholeNanoseconds
    override fun wallClockMillis(): Long = epochMillis()
}

/** The actions a UI can take. Everything that needs a time reading gets it from the clock, never from a caller. */
enum class FocusAction {
    START_TUTORIAL,
    COMPLETE_TUTORIAL,
    START_SESSION,
    PRESENT_TRIAL,
    EXPIRE_WINDOW,
    PAUSE,
    RESUME,
    INTERRUPT,
    COMPLETE,
    INVALIDATE,
}

/**
 * The thin, stateful shell around [FocusSessionReducer].
 *
 * It owns exactly four things the reducer must not: the current snapshot, the clock, the sealed descriptor that
 * supplies the response window, and the planned trials' classification facts. Everything else is delegated.
 *
 * The response window comes from `descriptor.spec`, so no call site can present a trial with a window of its
 * own choosing — the sealed spec is the authority, which is the point of sealing it before the first trial.
 */
class FocusSessionRuntime(
    val descriptor: FocusSessionDescriptor,
    private val trials: List<TrialFacts>,
    private val taskSpec: TaskSpec,
    private val clock: FocusClock,
    initial: FocusRuntimeSnapshot = FocusRuntimeSnapshot.initial(descriptor.sessionId),
) {
    var snapshot: FocusRuntimeSnapshot = initial
        private set

    private val recorded = mutableListOf<FocusEmission>()

    /** Every emission so far, in order. The caller journals these; the runtime only remembers them. */
    val emissions: List<FocusEmission> get() = recorded.toList()

    /** The trial currently on screen, or the one about to be. Null once the plan is exhausted. */
    val currentTrial: TrialFacts? get() = trials.getOrNull(snapshot.trialIndex)

    fun dispatch(
        action: FocusAction,
        temporalVerdict: TemporalWindowVerdict? = null,
        temporalSampledFrames: Int = 0,
    ): FocusReduction = apply(inputFor(action, temporalVerdict, temporalSampledFrames))

    /**
     * Resolves the open trial with [response], as observed at [atElapsedNanos].
     *
     * The instant is a PARAMETER, and defaults to reading the clock only for callers that resolve synchronously
     * with the event. It has to be, because the shell receives a tap on one dispatcher and resumes its loop on
     * another: reading the clock here would time the SCHEDULER, not the person. That error is not symmetric —
     * every millisecond of scheduling delay pushes a response further from the anticipation floor, so it biases
     * systematically towards accepting a fast guess as a genuine hit, on the one number that governs a
     * behavioural classification.
     */
    fun respond(
        response: Response,
        atElapsedNanos: Long = clock.elapsedNanos(),
        temporalVerdict: TemporalWindowVerdict? = null,
        temporalSampledFrames: Int = 0,
    ): FocusReduction {
        val facts = currentTrial ?: return FocusReduction(
            snapshot,
            rejection = FocusRejection.TrialInputOutOfPhase(snapshot.state, snapshot.phase),
        )
        return apply(
            FocusRuntimeInput.Respond(
                atElapsedNanos, response, facts.conditionId, facts.isSignal,
                temporalVerdict, temporalSampledFrames,
            ),
        )
    }

    // REMOVED: `adopt(restored)`. It let a snapshot from a dead process be installed into a runtime whose plan
    // had been generated from a DIFFERENT seed — `FocusRunPlanner.prepare` mints a new seed on every call — so
    // an adopted `trialIndex` indexed a different permutation and the tallies of two distinct instruments were
    // merged. That destroys the exact per-cell quota `MB-FOCUS-0021` was closed to guarantee, and the re-seal
    // that followed left a descriptor on disk claiming a seed that had not generated its own trials. Real
    // resume needs the plan regenerated from the SEALED seed; until that exists, a buffer from a dead process
    // is drained, not adopted — which is what the ledger already said this slice delivered.

    private fun apply(input: FocusRuntimeInput): FocusReduction {
        val reduction = FocusSessionReducer.reduce(snapshot, input, taskSpec)
        if (!reduction.wasRejected) {
            snapshot = reduction.snapshot
            recorded += reduction.emissions
        }
        return reduction
    }

    private fun inputFor(
        action: FocusAction,
        temporalVerdict: TemporalWindowVerdict?,
        temporalSampledFrames: Int,
    ): FocusRuntimeInput {
        val at = clock.elapsedNanos()
        val facts = currentTrial
        return when (action) {
            FocusAction.START_TUTORIAL -> FocusRuntimeInput.StartTutorial(at)
            FocusAction.COMPLETE_TUTORIAL -> FocusRuntimeInput.CompleteTutorial(at)
            FocusAction.START_SESSION -> FocusRuntimeInput.StartSession(at)
            FocusAction.PRESENT_TRIAL -> FocusRuntimeInput.PresentTrial(
                at,
                responseWindowNanos,
                trials.size,
                facts?.conditionId ?: taskSpec.primaryCondition,
            )
            FocusAction.EXPIRE_WINDOW -> FocusRuntimeInput.WindowExpired(
                at,
                facts?.conditionId ?: taskSpec.primaryCondition,
                facts?.isSignal ?: false,
                temporalVerdict,
                temporalSampledFrames,
            )
            FocusAction.PAUSE -> FocusRuntimeInput.Pause(at)
            FocusAction.RESUME -> FocusRuntimeInput.Resume(at)
            FocusAction.INTERRUPT -> FocusRuntimeInput.Interrupt(at)
            FocusAction.COMPLETE -> FocusRuntimeInput.Complete(at)
            FocusAction.INVALIDATE -> FocusRuntimeInput.Invalidate(at)
        }
    }

    private val responseWindowNanos: Long
        get() = descriptor.spec.responseWindowMs * NANOS_PER_MILLI

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
