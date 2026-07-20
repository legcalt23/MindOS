package com.mindos.app.features.mindboost.focus2.domain.telemetry

/**
 * Focus V5 · S3 · tarea (8) — which of the 16 canonical events this tracer emits, and WHO emits each one.
 *
 * The S1 reconciliation table settled what the vocabulary IS. This settles what the implementation DOES with
 * it, event by event, including the ones it deliberately does not emit. The plan asks for "los 16 aplicables",
 * and the only way to make that phrase auditable is to write down all sixteen and say which are applicable and
 * why the rest are not — a list of emitted events cannot be checked against a list nobody wrote down.
 *
 * Everything here is local. Focus v2 is local-only, no event leaves the device, and none carries a payload
 * beyond a trial index: the plan asks for "breadcrumbs neutros ... sin payload", and an event carrying the
 * response would move behavioural data into a stream governed by different retention rules from the session
 * store's.
 */
object FocusEventCatalog {

    /** Bump when ownership changes. This is a contract about who may emit what, not an implementation note. */
    const val VERSION: Int = 1

    /** Every canonical event, with its owner. Exhaustive by test — a missing entry turns the suite red. */
    val ownership: Map<CanonEvent, EventOwnership> = mapOf(
        CanonEvent.EXERCISE_OPENED to EventOwnership(
            EmitterLayer.SHELL_PENDING,
            "Opening the screen happens before any session exists, so the reducer cannot be the one to say it. " +
                "Without it, drop-off before the first trial is invisible. NOT EMITTED YET: it belongs to the " +
                "screen's entry point, and nothing mounts the tracer, so there is no entry point to emit from.",
        ),
        CanonEvent.TUTORIAL_STARTED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.TUTORIAL_COMPLETED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_STARTED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_PAUSED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_RESUMED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_INTERRUPTED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_COMPLETED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.SESSION_INVALIDATED to EventOwnership(EmitterLayer.REDUCER, "Lifecycle transition."),
        CanonEvent.TRIAL_PRESENTED to EventOwnership(EmitterLayer.REDUCER, "The instant the window opens."),

        CanonEvent.RESPONSE_STARTED to EventOwnership(
            EmitterLayer.NOT_EMITTED,
            "NOT APPLICABLE to this mechanic, declared rather than omitted. The canon separates a response " +
                "BEGINNING from its SUBMISSION because for some inputs those are different instants with a " +
                "measurable interval between them. A two-alternative choice resolved by a single activation " +
                "has no such interval, and emitting the two at the same instant would invent a duration of " +
                "zero that nothing measured. A tracer whose input becomes continuous must revisit this.",
        ),

        CanonEvent.RESPONSE_SUBMITTED to EventOwnership(
            EmitterLayer.REDUCER,
            "The answer. Emitted only when there WAS one: an expired window resolves without it, because a " +
                "person who did not answer did not submit anything.",
        ),
        CanonEvent.TRIAL_RESOLVED to EventOwnership(
            EmitterLayer.REDUCER,
            "Every trial resolves, answered or not — this is the one that makes omissions countable.",
        ),

        CanonEvent.DIFFICULTY_CHANGED to EventOwnership(
            EmitterLayer.NOT_EMITTED,
            "Not applicable while difficulty is FIXED within a measured block. The ratified E07 design fixes " +
                "congruency proportion and flanker parameters for the whole block precisely so the cells stay " +
                "comparable; an adaptive staircase would emit this, and this tracer has none. Declared here so " +
                "that adding a staircase later is a visible change to a contract rather than a silent new event.",
        ),

        CanonEvent.RESULT_PERSISTED to EventOwnership(
            EmitterLayer.SHELL_PENDING,
            "The write-ahead store owns this: only the layer that performed the write knows whether it " +
                "actually happened, and the reducer never touches storage. NOT EMITTED YET: there is no " +
                "summary sink to persist a RESULT into, only the session buffer that is purged on completion.",
        ),

        CanonEvent.RESULT_SYNCED to EventOwnership(
            EmitterLayer.NOT_EMITTED,
            "NOT APPLICABLE while Focus v2 is local-only. Emitting it would be a false statement about where " +
                "the data is, and the honest way to record a capability that does not exist is to name it and " +
                "say so.",
        ),
    )

    /**
     * The events Focus v2 CAN emit — those with an owning layer, whether or not that layer emits them yet.
     *
     * Not "the events it actually emits": the `SHELL_PENDING` entries are in here and are, by their own
     * declaration, not emitted at this slice. The distinction matters because the two readings differ by
     * exactly the events whose absence would otherwise look like coverage.
     */
    val applicable: Set<CanonEvent>
        get() = ownership.filterValues { it.layer != EmitterLayer.NOT_EMITTED }.keys

    /** The events the pure reducer emits. Anything else an emitter produces belongs to the shell. */
    val reducerOwned: Set<CanonEvent>
        get() = ownership.filterValues { it.layer == EmitterLayer.REDUCER }.keys
}

/** Which layer is allowed to emit an event. */
enum class EmitterLayer {
    /** The pure reducer, as part of a state transition. Emitted today, and tested. */
    REDUCER,

    /**
     * Belongs to the stateful shell (screen lifecycle, storage) and is NOT emitted yet.
     *
     * A separate value from [REDUCER] rather than a shared "SHELL", because the honest question about an event
     * is not only who owns it but whether it exists. An earlier version of this table listed both shell events
     * as plain owners, which read as "implemented" while the test deliberately checked only the reducer's set —
     * a catalog claiming coverage the code did not provide.
     */
    SHELL_PENDING,

    /** Deliberately not emitted. [EventOwnership.note] must say why, or the entry is an omission in disguise. */
    NOT_EMITTED,
}

/** One event's owner and the reasoning behind it. */
data class EventOwnership(val layer: EmitterLayer, val note: String)
