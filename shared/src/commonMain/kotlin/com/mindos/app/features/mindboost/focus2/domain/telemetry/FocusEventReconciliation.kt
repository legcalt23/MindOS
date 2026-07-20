package com.mindos.app.features.mindboost.focus2.domain.telemetry

/**
 * Focus V5 · S1 — the sealed, versioned reconciliation between the research pack's 13 proposed events and the
 * canonical 16-event standard.
 *
 * Two independent event vocabularies existed on paper with no stated relationship, which is how an
 * implementation ends up quietly emitting a third. This table states, event by event, what happens to each one
 * and why — including the ones that are deliberately NOT implemented. A "not implemented" with a reason is a
 * decision; the same thing without a reason is an omission nobody can audit later.
 *
 * It is data, not behaviour: nothing here emits anything. The emitters land in later slices and must derive
 * their vocabulary from this table.
 */
object FocusEventReconciliation {

    /** Bump when the mapping changes. A change to this table is a change to a contract, not a refactor. */
    const val VERSION: Int = 1
}

/** The 13 events proposed by the research pack. Names kept verbatim so the mapping is auditable against it. */
enum class PackEvent {
    SESSION_CREATED,
    SESSION_COMMITTED,
    TUTORIAL_RESULT,
    STIMULUS_PRESENTED,
    INPUT_RECEIVED,
    TRIAL_EVALUATED,
    ADAPTATION_DECIDED,
    LIFECYCLE_CHANGED,
    QUALITY_FLAGGED,

    /** The pack lists start and end as a single line item; both are kept as one disposition here. */
    BLOCK_STARTED_ENDED,
    TRIAL_PLANNED,
    SCORE_MATERIALIZED,
    CONSENT_CHANGED,
}

/** The canonical 16-event standard. */
enum class CanonEvent {
    EXERCISE_OPENED,
    TUTORIAL_STARTED,
    TUTORIAL_COMPLETED,
    SESSION_STARTED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SESSION_INTERRUPTED,
    SESSION_COMPLETED,
    SESSION_INVALIDATED,
    TRIAL_PRESENTED,
    RESPONSE_STARTED,
    RESPONSE_SUBMITTED,
    TRIAL_RESOLVED,
    DIFFICULTY_CHANGED,
    RESULT_PERSISTED,
    RESULT_SYNCED,
}

/** What becomes of one pack event. */
sealed interface PackEventDisposition {

    /** Maps cleanly onto one or more canonical events. */
    data class MapsTo(val canon: Set<CanonEvent>) : PackEventDisposition

    /** Maps onto a canonical event, but the two are not equivalent; [note] states the gap. */
    data class Partial(val canon: Set<CanonEvent>, val note: String) : PackEventDisposition

    /** Extra local granularity the canon does not model. Permitted, but never a substitute for a canon event. */
    data class LocalGranularity(val note: String) : PackEventDisposition

    /** Deliberately NOT emitted: the information is already recoverable. */
    data class Redundant(val note: String) : PackEventDisposition

    /** Real and needed, but with no counterpart in the canon — a genuine gap in the standard. */
    data class LocalOnly(val note: String) : PackEventDisposition
}

/** Why a canonical event has no pack source. */
data class CanonOnlyEvent(val event: CanonEvent, val note: String)

/** The reconciliation itself: every pack event's disposition, and every canon event without a pack source. */
object FocusEventMapping {

    val dispositions: Map<PackEvent, PackEventDisposition> = mapOf(
        PackEvent.SESSION_CREATED to PackEventDisposition.MapsTo(setOf(CanonEvent.SESSION_STARTED)),
        PackEvent.SESSION_COMMITTED to PackEventDisposition.MapsTo(setOf(CanonEvent.RESULT_PERSISTED)),
        PackEvent.TUTORIAL_RESULT to PackEventDisposition.MapsTo(setOf(CanonEvent.TUTORIAL_COMPLETED)),
        PackEvent.STIMULUS_PRESENTED to PackEventDisposition.MapsTo(setOf(CanonEvent.TRIAL_PRESENTED)),

        // The canon splits what the pack collapses: a response BEGINNING and a response being SUBMITTED are
        // different instants, and the interval between them is the measurement. A vocabulary with only the
        // second cannot express that, so the canon's extra event is kept.
        PackEvent.INPUT_RECEIVED to PackEventDisposition.MapsTo(
            setOf(CanonEvent.RESPONSE_STARTED, CanonEvent.RESPONSE_SUBMITTED),
        ),
        PackEvent.TRIAL_EVALUATED to PackEventDisposition.MapsTo(setOf(CanonEvent.TRIAL_RESOLVED)),
        PackEvent.ADAPTATION_DECIDED to PackEventDisposition.MapsTo(setOf(CanonEvent.DIFFICULTY_CHANGED)),

        // One pack event covering three distinct lifecycle transitions loses which one happened. The canon
        // separates them and so does the local state machine.
        PackEvent.LIFECYCLE_CHANGED to PackEventDisposition.MapsTo(
            setOf(CanonEvent.SESSION_PAUSED, CanonEvent.SESSION_RESUMED, CanonEvent.SESSION_INTERRUPTED),
        ),

        PackEvent.QUALITY_FLAGGED to PackEventDisposition.Partial(
            canon = setOf(CanonEvent.SESSION_INVALIDATED),
            note = "A raised quality FLAG is not an invalidation: most flagged sessions stay valid. Both are " +
                "emitted separately; collapsing them would either hide flags or invalidate honest sessions.",
        ),

        PackEvent.BLOCK_STARTED_ENDED to PackEventDisposition.LocalGranularity(
            note = "Block boundaries are useful locally for recovery and pacing. They are additional detail, " +
                "never a stand-in for a session-level canon event.",
        ),

        PackEvent.TRIAL_PLANNED to PackEventDisposition.Redundant(
            note = "The persisted seed plus its RNG version re-derives the entire trial plan deterministically. " +
                "Persisting the plan as well stores the same facts twice and creates a second source of truth " +
                "that can disagree with the seed.",
        ),

        PackEvent.SCORE_MATERIALIZED to PackEventDisposition.LocalOnly(
            note = "Local-only under the D1 gate. A materialized score is not evidence and never leaves the " +
                "device; emitting it as a shared analytics event would imply a capability claim that does " +
                "not exist.",
        ),

        PackEvent.CONSENT_CHANGED to PackEventDisposition.LocalOnly(
            note = "No canonical counterpart — a genuine gap in the 16-event standard. FIRST to be implemented: " +
                "no other event may be emitted for a purpose whose consent state is not already recorded.",
        ),
    )

    /**
     * Canonical events no pack event produces.
     *
     * [CanonEvent.SESSION_COMPLETED] is on this list as a CORRECTION to the plan's own reconciliation, which
     * listed only three canon-only additions. Walking the table event by event shows a fourth: the pack's
     * `lifecycle_changed` covers paused/resumed/interrupted, and `session_committed` was assigned to
     * RESULT_PERSISTED, so nothing in the 13 emits "the session finished". Finishing and persisting are
     * different facts, and a vocabulary that cannot say the first one cannot distinguish a completed session
     * from an abandoned one whose partial result was written.
     */
    val canonOnly: List<CanonOnlyEvent> = listOf(
        CanonOnlyEvent(
            CanonEvent.EXERCISE_OPENED,
            "Opening an exercise is not starting a session; without it, drop-off before the first trial is invisible.",
        ),
        CanonOnlyEvent(
            CanonEvent.TUTORIAL_STARTED,
            "The pack records only the tutorial RESULT, so an abandoned tutorial leaves no trace at all.",
        ),
        CanonOnlyEvent(
            CanonEvent.SESSION_COMPLETED,
            "No pack event states that the session finished (see the KDoc above — a correction to the plan).",
        ),
        CanonOnlyEvent(
            CanonEvent.RESULT_SYNCED,
            "NOT APPLICABLE while Focus v2 is local-only. Declared, not implemented: sync is blocked, and an " +
                "event asserting a result was synced would be false.",
        ),
    )
}
