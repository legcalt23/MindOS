package com.mindos.app.features.mindboost.focus2.domain.session

import com.mindos.app.features.mindboost.core.session.SessionStatus

/**
 * Focus V5 · S1 · ADR-FOCUS-SESSION-STATE (decided 2026-07-20, Decision Log §20.2) — the Focus v2 RUNTIME
 * lifecycle state, local to this package.
 *
 * It is deliberately NOT an extension of the global `SessionStatus`. That enum is transversal across 32 files and
 * 7+ features and has no PAUSED; adding one would force every exhaustive `when` in those features to change for a
 * concept that belongs to a running exercise, not to crash recovery. The repo already has this exact precedent in
 * `core.session.cognitive`.
 *
 * **Deviation from the ADR text, declared explicitly:** the ADR enumerated eight states including `Resumed`.
 * `Resumed` is modelled here as an EVENT ([FocusSessionEvent.SessionResumed]), not a state, because a resumed
 * session is indistinguishable from a running one in every respect — same allowed transitions, same global
 * status, same trial handling. Two states that behave identically are a fabricated distinction, and the thing
 * that actually matters about resuming (how many times it happened) is a COUNTER, which the timing-integrity
 * snapshot already carries. Seven states, eight events.
 */
enum class FocusSessionState {
    /** Nothing has begun. The exercise screen is open but no session or tutorial is running. */
    IDLE,

    /** A tutorial or familiarization run is in progress. Never produces evidence, never feeds a score. */
    TUTORIAL,

    /** The session is running and accepting trials. */
    RUNNING,

    /** The session is paused. Trials are not presented and no reaction time is being measured. */
    PAUSED,

    /** Terminal: the session ended without completing (backgrounded, abandoned, killed). */
    INTERRUPTED,

    /** Terminal: the session ran to completion. */
    COMPLETED,

    /** Terminal: the session is disqualified as evidence. Reachable from a COMPLETED run — a quality gate can
     *  fire after sealing — which is why this is a state and not merely a flag on the ending. */
    INVALIDATED,
    ;

    /** True when no event can move this state anywhere except the post-hoc invalidation of a completed run. */
    val isTerminal: Boolean
        get() = this == INTERRUPTED || this == COMPLETED || this == INVALIDATED
}

/**
 * The session-LIFECYCLE events of the 16-event canonical standard. Trial events (TrialPresented, ResponseStarted,
 * ResponseSubmitted, TrialResolved, DifficultyChanged) and infrastructure events (ExerciseOpened, ResultPersisted,
 * ResultSynced) are journal/analytics records, not lifecycle states, and deliberately generate no state here.
 */
enum class FocusSessionEvent {
    TUTORIAL_STARTED,
    TUTORIAL_COMPLETED,
    SESSION_STARTED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SESSION_INTERRUPTED,
    SESSION_COMPLETED,
    SESSION_INVALIDATED,
}

/**
 * The outcome of applying an event. An illegal transition is a TYPED rejection, never a thrown exception and
 * never a silent no-op: a state machine that swallows an illegal event fabricates a lifecycle, and one that
 * throws turns a recoverable UI mis-sequence into a crash. Mirrors `PublisherStateMachine`'s contract.
 */
sealed interface FocusSessionTransition {

    /** The event was legal; [state] is the resulting state. */
    data class Applied(val state: FocusSessionState) : FocusSessionTransition

    /** The event was not legal from [from]. The caller must decide what to do; nothing has changed. */
    data class Rejected(val from: FocusSessionState, val event: FocusSessionEvent) : FocusSessionTransition
}

/**
 * The Focus v2 lifecycle machine. Transitions happen ONLY by lookup in [transitions] — there is no branch that
 * computes a next state, so an unlisted pair cannot accidentally be legal. Deny-by-default.
 */
object FocusSessionStateMachine {

    /** The single source of truth: every legal `(state, event) -> state'`. Anything absent is rejected. */
    private val transitions: Map<Pair<FocusSessionState, FocusSessionEvent>, FocusSessionState> = mapOf(
        // Tutorial is optional and is NOT the session: completing it returns to IDLE, from which the session
        // starts explicitly. Collapsing TUTORIAL_COMPLETED straight into RUNNING would make a familiarization
        // run silently become the measured one.
        (FocusSessionState.IDLE to FocusSessionEvent.TUTORIAL_STARTED) to FocusSessionState.TUTORIAL,
        (FocusSessionState.TUTORIAL to FocusSessionEvent.TUTORIAL_COMPLETED) to FocusSessionState.IDLE,
        (FocusSessionState.TUTORIAL to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,

        (FocusSessionState.IDLE to FocusSessionEvent.SESSION_STARTED) to FocusSessionState.RUNNING,

        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_PAUSED) to FocusSessionState.PAUSED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_COMPLETED) to FocusSessionState.COMPLETED,
        (FocusSessionState.RUNNING to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,

        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_RESUMED) to FocusSessionState.RUNNING,
        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_INTERRUPTED) to FocusSessionState.INTERRUPTED,
        (FocusSessionState.PAUSED to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,

        // Post-hoc invalidation: a completed run can still be disqualified when a quality gate fires after
        // sealing. This is the ONLY transition out of a terminal state, and it only ever moves toward less
        // credit, never toward more.
        (FocusSessionState.COMPLETED to FocusSessionEvent.SESSION_INVALIDATED) to FocusSessionState.INVALIDATED,
    )

    /** The starting state of every Focus v2 session. */
    val initial: FocusSessionState = FocusSessionState.IDLE

    /** Applies [event] to [from], by lookup only. An unlisted pair is [FocusSessionTransition.Rejected]. */
    fun transition(from: FocusSessionState, event: FocusSessionEvent): FocusSessionTransition =
        transitions[from to event]
            ?.let { FocusSessionTransition.Applied(it) }
            ?: FocusSessionTransition.Rejected(from, event)

    /** Every legal pair, for tests and for anyone who needs to reason about the machine without reading it. */
    fun legalTransitions(): Set<Pair<FocusSessionState, FocusSessionEvent>> = transitions.keys
}

/**
 * Maps the local runtime state onto the GLOBAL lifecycle record, at write-ahead points only.
 *
 * `null` means "this state writes nothing" — the previously recorded global status stands. Two cases:
 * - [FocusSessionState.IDLE] / [FocusSessionState.TUTORIAL]: no session record exists yet.
 * - [FocusSessionState.INVALIDATED]: validity is NOT lifecycle. A completed-then-invalidated run really did
 *   finish, and rewriting it to ABANDONED would falsify the lifecycle record to express a scientific verdict
 *   that belongs in the eligibility layer. The run's evidence is denied elsewhere; the history stays true.
 */
fun FocusSessionState.toGlobalStatus(): SessionStatus? = when (this) {
    FocusSessionState.IDLE, FocusSessionState.TUTORIAL -> null
    FocusSessionState.RUNNING, FocusSessionState.PAUSED -> SessionStatus.STARTED
    FocusSessionState.COMPLETED -> SessionStatus.FINALIZED
    FocusSessionState.INTERRUPTED -> SessionStatus.ABANDONED
    FocusSessionState.INVALIDATED -> null
}
