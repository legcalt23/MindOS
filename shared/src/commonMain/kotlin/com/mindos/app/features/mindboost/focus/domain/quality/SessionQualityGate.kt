package com.mindos.app.features.mindboost.focus.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.GamingVerdict
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation

/**
 * FOC-P0-05 · The one verdict every Focus session must carry before anything publishable is derived from it.
 *
 * [Admitted] means the session cleared every guard THIS lane can observe today; [NotMeasurable] names the single
 * guard that stopped it. There is deliberately no third state: an unknown is not favourable, so a guard that
 * could not observe DENIES (`LIFECYCLE_NOT_OBSERVED`) instead of being skipped.
 *
 * Declared ceiling — what [Admitted] does NOT assert, because Focus has no producer for it yet: an authoritative
 * timebase, frame timing (jank), an accessibility configuration, or a consent record. Each is registered in the
 * ledger with an owner; none is fabricated here as a passing input.
 */
sealed interface SessionAdmission {
    data object Admitted : SessionAdmission

    data class NotMeasurable(val reason: NotMeasurableReason) : SessionAdmission
}

/** Why a session was not admitted. Ordered by precedence: observed facts first, the unknown last. */
enum class NotMeasurableReason {
    /** The lifecycle sensor OBSERVED the app leaving the foreground during the session. */
    INTERRUPTED,

    /** The user made no response at all: nothing here is about the user (`MB-FOCUS-0091`). */
    NO_RESPONSES,

    /**
     * R15 · An anti-gaming detector fired, so the response pattern does not support reading this session as a
     * measure. It is a statement about what WE can conclude, never an accusation: the session is withheld, no
     * low score is written and no copy blames anyone (`FocusAntiGaming`).
     */
    RESPONSE_PATTERN_NOT_INTERPRETABLE,

    /** No sensor watched the foreground, so an interruption could not have been seen. Unknown is not clean. */
    LIFECYCLE_NOT_OBSERVED,
}

/**
 * Pure and total. Every input is mandatory: a caller that does not know a fact must pass its typed absence
 * (`LifecycleObservation.NotObservable`), never a default that reads as a pass. [gaming] has no default for
 * the same reason — a default clean verdict would silently reinstate the ungated lane R15 closed.
 */
object SessionQualityGate {

    fun admit(
        lifecycle: LifecycleObservation,
        respondedTrials: Int,
        gaming: GamingVerdict,
    ): SessionAdmission {
        require(respondedTrials >= 0) { "respondedTrials is a count, got $respondedTrials" }
        return when {
            lifecycle is LifecycleObservation.Observed && lifecycle.interruptions.isNotEmpty() ->
                SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED)
            respondedTrials == 0 -> SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES)
            !gaming.isClean ->
                SessionAdmission.NotMeasurable(NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE)
            lifecycle is LifecycleObservation.NotObservable ->
                SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED)
            else -> SessionAdmission.Admitted
        }
    }
}
