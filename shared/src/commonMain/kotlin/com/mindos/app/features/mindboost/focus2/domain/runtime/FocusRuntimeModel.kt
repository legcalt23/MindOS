package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SdtCell
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialOutcome
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionEvent
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import com.mindos.app.features.mindboost.focus2.domain.telemetry.CanonEvent
import kotlinx.serialization.Serializable

/**
 * Focus V5 · S3 · tarea (4) — the SERIALIZABLE runtime state and its inputs.
 *
 * Every timestamp in this file is an elapsed reading from a MONOTONIC source, never a wall-clock instant. That
 * is the structural half of REDTEAM #26: there is no field here that a civil-time change could move, so a DST
 * jump or a manual clock change cannot shorten a response window or reorder trials. The type system carries the
 * rule; the property test then demonstrates it end to end rather than asserting the obvious.
 *
 * The state is serializable because process death is ordinary, not exceptional. A snapshot that cannot be
 * written down is a session that dies with the Activity.
 */

/** Where the current trial is in its own small cycle. */
enum class TrialPhase {
    /** No trial is on screen. Either the session has not started or the last one resolved. */
    IDLE,

    /** A trial is displayed and its response window is open. */
    AWAITING_RESPONSE,

    /** The trial resolved; the inter-stimulus gap is running before the next one. */
    INTER_STIMULUS,
    ;

    companion object {
        fun fromWire(raw: String?): TrialPhase = entries.firstOrNull { it.name == raw } ?: IDLE
    }
}

/**
 * The only two things the runtime needs to know about a planned trial in order to classify its outcome.
 *
 * Deliberately NOT `PlannedTrial`: that type belongs to the flanker generator, and the whole point of S4 is to
 * run a different mechanic on this same engine. A vigilance trial has no flankers, no congruency and one
 * response — but it still has a condition and a signal/noise axis, so it fits here untouched.
 */
data class TrialFacts(val conditionId: ConditionId, val isSignal: Boolean)

/**
 * One condition's running counts.
 *
 * The condition is stored by NAME and resolved with a fail-closed lookup, for the same reason every other enum
 * in this package is: an ordinal changes meaning the day someone reorders the enum, and a generated serializer
 * throws on a value written by a newer build instead of denying.
 */
@Serializable
data class ConditionTally(val conditionName: String, val cell: SdtCell) {
    val conditionId: ConditionId? get() = ConditionId.entries.firstOrNull { it.name == conditionName }
}

/**
 * The whole mutable state of a running session, and nothing else.
 *
 * **It holds COUNTS, never trials.** This is not a space optimisation, it is the DEFAULT TIER of
 * `ADR-FOCUS-DATA` made structural. The ratified decision is option (iii) TIERED, not a flat no-raw ban:
 * summaries always, plus a per-trial journal ONLY where the sealed purpose is measurement, under scope-bound
 * consent and a daily dose cap, falling back to summaries whenever the purpose is indeterminable. This type
 * implements the summary tier and nothing else; the journal is a separate writer that does not yet exist, and
 * a runtime that quietly kept the trials would be that writer, built without any of the three gates the ADR
 * requires of it. An earlier draft of this type carried a `List<RecordedResponse>` with a per-trial latency,
 * and because the only purge path had no production call-site, every session — tutorials included — left a
 * complete behavioural record encrypted on disk forever, with no consent, no cap and no retention. The tallies
 * instead means there is no raw stream for a future edit to forget to delete: the engine cannot persist what
 * it does not hold. `DescriptiveScorer.scoreCells` computes exactly the same numbers from these counts as it
 * does from a trial stream, so nothing is lost that the measurement uses.
 *
 * What IS lost, stated plainly rather than sold as a feature: per-trial latency is no longer recoverable after
 * the fact. It is classified at the instant of the response (an anticipation is counted, a latency is not
 * kept) and then discarded. That is consistent with "RT secundario en C0", where latency never governs, but it
 * does mean a future RT-based analysis would need its own ADR and its own consent scope — which is precisely
 * the decision C-06 exists to take, and which this engine must not pre-empt by quietly keeping the data.
 *
 * [contaminatedCount] records trials thrown away because a window was open when the session was interrupted
 * (REDTEAM #21). It is a count, not a silent omission: "trial discarded" and "trial never happened" must not
 * look the same when the cells are checked against the sample floor.
 */
@Serializable
data class FocusRuntimeSnapshot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val stateName: String,
    val phaseName: String,
    /** The trial about to be presented, or currently presented. */
    val trialIndex: Int = 0,
    /** Monotonic elapsed nanos when the current trial appeared. Null when no trial is on screen. */
    val presentedAtElapsedNanos: Long? = null,
    /** Monotonic elapsed nanos when the open window closes. Null when no window is open. */
    val deadlineElapsedNanos: Long? = null,
    val tallies: List<ConditionTally> = emptyList(),
    val resumeCount: Int = 0,
    val contaminatedCount: Int = 0,
) {
    /** The counts, keyed for the scorer. An unrecognised condition name is dropped rather than guessed. */
    fun cells(): Map<ConditionId, SdtCell> =
        tallies.mapNotNull { tally -> tally.conditionId?.let { it to tally.cell } }.toMap()

    /** Every trial this session has actually resolved, discarded ones excluded. */
    val resolvedTrials: Int get() = tallies.sumOf { it.cell.totalTrials }

    /** Accumulates one classified outcome into [conditionId]'s tally. */
    fun tallying(conditionId: ConditionId, outcome: TrialOutcome): List<ConditionTally> {
        val existing = tallies.firstOrNull { it.conditionName == conditionId.name }
        val updated = ConditionTally(conditionId.name, (existing?.cell ?: SdtCell.EMPTY) + outcome)
        return tallies.filterNot { it.conditionName == conditionId.name } + updated
    }

    val state: FocusSessionState
        get() = FocusSessionState.entries.firstOrNull { it.name == stateName } ?: FocusSessionState.IDLE

    val phase: TrialPhase get() = TrialPhase.fromWire(phaseName)

    /** True while a response window is open — the condition that makes an interruption contaminating. */
    val hasOpenWindow: Boolean get() = phase == TrialPhase.AWAITING_RESPONSE && deadlineElapsedNanos != null

    companion object {
        const val SCHEMA_VERSION: Int = 1

        fun initial(sessionId: String) = FocusRuntimeSnapshot(
            sessionId = sessionId,
            stateName = FocusSessionState.IDLE.name,
            phaseName = TrialPhase.IDLE.name,
        )
    }
}

/**
 * What can happen to a session.
 *
 * Each carries the monotonic reading at which it happened, supplied by the caller. The reducer never reads a
 * clock: a pure function of (state, input) is what makes the round-trip and clock property tests possible
 * without an emulator, and what keeps the deadline anchored to the engine rather than to a composable's
 * lifecycle (REDTEAM #22 — recomposition must not re-arm the window).
 */
sealed interface FocusRuntimeInput {
    val atElapsedNanos: Long

    data class StartTutorial(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class CompleteTutorial(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class StartSession(override val atElapsedNanos: Long) : FocusRuntimeInput

    /**
     * Show the trial at [FocusRuntimeSnapshot.trialIndex] and open its window.
     *
     * [plannedTrials] is the size of the run's plan, carried so the reducer can refuse to present past the end
     * — a pure reducer has no other way to know how long the session is.
     */
    data class PresentTrial(
        override val atElapsedNanos: Long,
        val responseWindowNanos: Long,
        val plannedTrials: Int,
    ) : FocusRuntimeInput

    /**
     * A response to the trial on screen.
     *
     * It carries the trial's [conditionId] and [isSignal] because the reducer classifies the outcome the
     * moment it happens and keeps only the count — there is no later join against a stored trial, because
     * there is no stored trial. They are loose facts rather than a `PlannedTrial` so that the reducer stays
     * paradigm-neutral: S4's vigilance tracer has one response and no flankers, and must reuse this untouched.
     */
    data class Respond(
        override val atElapsedNanos: Long,
        val response: Response,
        val conditionId: ConditionId,
        val isSignal: Boolean,
    ) : FocusRuntimeInput

    /** The window closed with no response. Recorded as [Response.NONE] — an omission is data, not a gap. */
    data class WindowExpired(
        override val atElapsedNanos: Long,
        val conditionId: ConditionId,
        val isSignal: Boolean,
    ) : FocusRuntimeInput

    data class Pause(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class Resume(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class Interrupt(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class Complete(override val atElapsedNanos: Long) : FocusRuntimeInput
    data class Invalidate(override val atElapsedNanos: Long) : FocusRuntimeInput
}

/**
 * A neutral lifecycle breadcrumb. Deliberately payload-free beyond a trial index: the plan asks for
 * "breadcrumbs neutros de lifecycle ... sin payload", and an event carrying the response would put behavioural
 * data into a stream whose retention rules are not the session store's.
 */
data class FocusEmission(val event: CanonEvent, val trialIndex: Int? = null)

/**
 * Why an input did not apply. Always TYPED and always returned to the caller: a reducer that silently ignores
 * an out-of-order input fabricates a lifecycle, and one that throws turns a UI mis-sequence into a crash.
 */
sealed interface FocusRejection {

    /** The lifecycle machine has no transition for this pair. */
    data class IllegalLifecycle(val from: FocusSessionState, val event: FocusSessionEvent) : FocusRejection

    /** A trial input arrived when no trial could accept it (window closed, session not running). */
    data class TrialInputOutOfPhase(
        val state: FocusSessionState,
        val phase: TrialPhase,
    ) : FocusRejection
}

/** The result of one reduction. On rejection nothing changed and nothing was emitted. */
data class FocusReduction(
    val snapshot: FocusRuntimeSnapshot,
    val emissions: List<FocusEmission> = emptyList(),
    val rejection: FocusRejection? = null,
) {
    val wasRejected: Boolean get() = rejection != null
}
