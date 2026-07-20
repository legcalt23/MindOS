package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TaskSpec
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionEvent
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionStateMachine
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionTransition
import com.mindos.app.features.mindboost.focus2.domain.telemetry.CanonEvent

/**
 * Focus V5 · S3 · tarea (4) — the session runtime, as a PURE function.
 *
 * The plan asks for a state holder that is not a god object: "reducer puro + efectos". This is the reducer. It
 * reads no clock, touches no storage, renders nothing and emits nothing — it returns the next state and a list
 * of things that ought to happen, and the caller performs them. Everything the redteam matrix asks S3 to prove
 * about the runtime is then provable in plain JVM tests: rotation (#22) is a serialization round-trip, the
 * clock property (#26) is two reductions with different wall clocks, and an interrupted trial (#21) is an
 * assertion about a list.
 *
 * Lifecycle legality is NOT decided here. It is delegated to [FocusSessionStateMachine], whose transitions are a
 * lookup table with no computed branch, so this file cannot accidentally invent a legal transition the machine
 * would refuse.
 */
object FocusSessionReducer {

    fun reduce(
        snapshot: FocusRuntimeSnapshot,
        input: FocusRuntimeInput,
        spec: TaskSpec,
    ): FocusReduction = when (input) {
        is FocusRuntimeInput.StartTutorial -> lifecycle(snapshot, FocusSessionEvent.TUTORIAL_STARTED)
        is FocusRuntimeInput.CompleteTutorial -> lifecycle(snapshot, FocusSessionEvent.TUTORIAL_COMPLETED)
        is FocusRuntimeInput.StartSession -> lifecycle(snapshot, FocusSessionEvent.SESSION_STARTED)
        is FocusRuntimeInput.Complete -> lifecycle(snapshot, FocusSessionEvent.SESSION_COMPLETED)
        is FocusRuntimeInput.Invalidate -> lifecycle(snapshot, FocusSessionEvent.SESSION_INVALIDATED)
        is FocusRuntimeInput.Resume -> lifecycle(snapshot, FocusSessionEvent.SESSION_RESUMED)
        is FocusRuntimeInput.Pause -> lifecycle(snapshot, FocusSessionEvent.SESSION_PAUSED)
        is FocusRuntimeInput.Interrupt -> lifecycle(snapshot, FocusSessionEvent.SESSION_INTERRUPTED)
        is FocusRuntimeInput.PresentTrial -> present(snapshot, input)
        is FocusRuntimeInput.Respond ->
            resolve(snapshot, input.response, input.atElapsedNanos, input.conditionId, input.isSignal, spec)
        is FocusRuntimeInput.WindowExpired ->
            resolve(snapshot, Response.NONE, input.atElapsedNanos, input.conditionId, input.isSignal, spec)
    }

    /**
     * Applies a lifecycle event, and — for the two events that can arrive while a trial is on screen — discards
     * that trial.
     *
     * A response window that was open when the session paused or was interrupted produces a latency that
     * measures the interruption, so the trial is counted in [FocusRuntimeSnapshot.contaminatedCount]
     * instead of being answered (REDTEAM #21). It is recorded as discarded rather than dropped silently:
     * "trial 7 was thrown away" and "trial 7 never happened" must not look the same afterwards.
     */
    private fun lifecycle(snapshot: FocusRuntimeSnapshot, event: FocusSessionEvent): FocusReduction {
        val transition = FocusSessionStateMachine.transition(snapshot.state, event)
        if (transition is FocusSessionTransition.Rejected) {
            return FocusReduction(snapshot, rejection = FocusRejection.IllegalLifecycle(snapshot.state, event))
        }
        val next = (transition as FocusSessionTransition.Applied).state
        val contaminating = snapshot.hasOpenWindow &&
            (event == FocusSessionEvent.SESSION_PAUSED || event == FocusSessionEvent.SESSION_INTERRUPTED)
        val base = if (contaminating) discardOpenTrial(snapshot) else snapshot
        return FocusReduction(
            snapshot = base.copy(
                stateName = next.name,
                resumeCount = base.resumeCount + if (event == FocusSessionEvent.SESSION_RESUMED) 1 else 0,
            ),
            emissions = listOf(FocusEmission(event.toCanon())),
        )
    }

    /**
     * Throws away the trial currently on screen.
     *
     * Public because process-death restore needs exactly this: a snapshot resumed in a NEW process carries
     * monotonic anchors from an epoch that no longer exists, so an open window cannot be continued — it can
     * only be discarded. Leaving that to the restore path to re-implement would be two rules for one fact.
     */
    fun discardOpenTrial(snapshot: FocusRuntimeSnapshot) = snapshot.copy(
        phaseName = TrialPhase.IDLE.name,
        presentedAtElapsedNanos = null,
        deadlineElapsedNanos = null,
        trialIndex = snapshot.trialIndex + 1,
        contaminatedCount = snapshot.contaminatedCount + 1,
    )

    /**
     * Opens a response window.
     *
     * The deadline is computed ONCE, here, from the monotonic reading at presentation. Nothing later re-derives
     * it — which is the whole content of REDTEAM #22's design rule: a recomposition replays no input, so it
     * cannot re-arm a window that is already running.
     */
    private fun present(snapshot: FocusRuntimeSnapshot, input: FocusRuntimeInput.PresentTrial): FocusReduction {
        // The trial count is a bound, not a suggestion. Without it, an index past the end still opened a
        // window; nothing could then resolve it (there is no trial to classify), so the window never closed
        // and the session ended with one dangling open.
        val withinPlan = snapshot.trialIndex < input.plannedTrials
        val presentable = withinPlan &&
            snapshot.state == FocusSessionState.RUNNING &&
            !snapshot.hasOpenWindow
        if (!presentable) {
            return FocusReduction(
                snapshot,
                rejection = FocusRejection.TrialInputOutOfPhase(snapshot.state, snapshot.phase),
            )
        }
        return FocusReduction(
            snapshot = snapshot.copy(
                phaseName = TrialPhase.AWAITING_RESPONSE.name,
                presentedAtElapsedNanos = input.atElapsedNanos,
                deadlineElapsedNanos = input.atElapsedNanos + input.responseWindowNanos,
            ),
            emissions = listOf(FocusEmission(CanonEvent.TRIAL_PRESENTED, snapshot.trialIndex)),
        )
    }

    /**
     * Closes the open window with [response].
     *
     * An expiry is a real outcome, not a missing one: it is recorded as [Response.NONE] with a null latency.
     * Writing a zero or the deadline value there would fabricate a reaction time for someone who never reacted.
     */
    @Suppress("LongParameterList") // Every argument is a distinct fact about the trial being closed; a wrapper
    // object would move the same list one file away and add an indirection to read through.
    private fun resolve(
        snapshot: FocusRuntimeSnapshot,
        response: Response,
        atElapsedNanos: Long,
        conditionId: ConditionId,
        isSignal: Boolean,
        spec: TaskSpec,
    ): FocusReduction {
        val presentedAt = snapshot.presentedAtElapsedNanos
        val deadline = snapshot.deadlineElapsedNanos
        return when {
            !snapshot.hasOpenWindow || presentedAt == null || deadline == null ->
                FocusReduction(
                    snapshot,
                    rejection = FocusRejection.TrialInputOutOfPhase(snapshot.state, snapshot.phase),
                )

            // A reading EARLIER than the presentation means the two came from different monotonic epochs — a
            // snapshot restored into a process that started its own clock at zero. Both guards below fail OPEN
            // when they cannot be evaluated, and they compose: an old-epoch deadline is trivially not exceeded,
            // and an uncomputable latency disables the anticipation floor, so the trial would be credited as a
            // CLEAN HIT to someone who may have deliberated for five minutes in a 1200 ms window. Absence must
            // not fabricate a maximum any more than it may fabricate a zero. The trial is discarded on exactly
            // the same principle PAUSE and INTERRUPT discard it: an interrupted trial measures the interruption.
            atElapsedNanos < presentedAt -> contaminated(snapshot)

            else -> {
                // THE WINDOW IS ENFORCED HERE, and this is the only place it can be. Before this existed, the
                // deadline was written into the snapshot and never compared against anything, so every trial
                // recorded a `windowMs` describing a constraint the engine did not apply. An answer arriving
                // after the deadline is not a late answer, it is no answer.
                val answered = response != Response.NONE && atElapsedNanos <= deadline
                val effective = if (answered) response else Response.NONE
                val rtMs = if (answered) (atElapsedNanos - presentedAt) / NANOS_PER_MILLI else null
                val outcome = spec.outcomeOf(isSignal, effective, rtMs)
                FocusReduction(
                    snapshot = snapshot.copy(
                        phaseName = TrialPhase.INTER_STIMULUS.name,
                        presentedAtElapsedNanos = null,
                        deadlineElapsedNanos = null,
                        trialIndex = snapshot.trialIndex + 1,
                        tallies = snapshot.tallying(conditionId, outcome),
                    ),
                    emissions = buildList {
                        if (answered) add(FocusEmission(CanonEvent.RESPONSE_SUBMITTED, snapshot.trialIndex))
                        add(FocusEmission(CanonEvent.TRIAL_RESOLVED, snapshot.trialIndex))
                    },
                )
            }
        }
    }

    /** Throws the open trial away and records it as contaminated, without crediting any outcome to a cell. */
    private fun contaminated(snapshot: FocusRuntimeSnapshot) = FocusReduction(
        snapshot = discardOpenTrial(snapshot),
        emissions = listOf(FocusEmission(CanonEvent.TRIAL_RESOLVED, snapshot.trialIndex)),
    )

    private const val NANOS_PER_MILLI = 1_000_000L

    /** The lifecycle half of the 16-event canon. One-to-one; the mapping is the reconciliation table's. */
    private fun FocusSessionEvent.toCanon(): CanonEvent = when (this) {
        FocusSessionEvent.TUTORIAL_STARTED -> CanonEvent.TUTORIAL_STARTED
        FocusSessionEvent.TUTORIAL_COMPLETED -> CanonEvent.TUTORIAL_COMPLETED
        FocusSessionEvent.SESSION_STARTED -> CanonEvent.SESSION_STARTED
        FocusSessionEvent.SESSION_PAUSED -> CanonEvent.SESSION_PAUSED
        FocusSessionEvent.SESSION_RESUMED -> CanonEvent.SESSION_RESUMED
        FocusSessionEvent.SESSION_INTERRUPTED -> CanonEvent.SESSION_INTERRUPTED
        FocusSessionEvent.SESSION_COMPLETED -> CanonEvent.SESSION_COMPLETED
        FocusSessionEvent.SESSION_INVALIDATED -> CanonEvent.SESSION_INVALIDATED
    }
}
