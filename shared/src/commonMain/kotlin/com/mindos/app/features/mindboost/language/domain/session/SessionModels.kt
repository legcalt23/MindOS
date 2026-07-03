package com.mindos.app.features.mindboost.language.domain.session

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import kotlinx.serialization.Serializable

/**
 * Session state machine model (v6 §6.2 Session Orchestrator, §12.4 reducer). The UI only
 * emits [LanguageSessionAction]s and renders [LanguageSessionState] + the returned
 * [TrainingDecision]. It never decides difficulty, module, evidence, repair or claims —
 * those are reduced here, in pure `commonMain` code (the reducer is [LanguageSessionReducer]).
 */

/** The session phases (v6 §6 master cycle). The full LTOS orchestration walks these. */
@Serializable
enum class SessionPhase(val label: String) {
    CALIBRATION("Calibración"),
    TRAINING_BASE("Práctica deliberada"),
    REPAIR_MODE("Reparación"),
    CONSOLIDATION("Consolidación"),
    NEAR_TRANSFER("Transferencia cercana"),
    FUNCTIONAL_TRANSFER("Transferencia funcional"),
    MAINTENANCE("Mantenimiento"),
    RECALIBRATION("Recalibración"),
    ;

    companion object {
        /** Phases where an error pattern should branch into an inserted repair retry (§6.5). */
        val REPAIRABLE: Set<SessionPhase> = setOf(
            TRAINING_BASE, CONSOLIDATION, NEAR_TRANSFER, FUNCTIONAL_TRANSFER,
        )
    }
}

/** Where the session is in the request/response/feedback loop. */
@Serializable
enum class SessionStatus {
    NOT_STARTED,
    AWAITING_RESPONSE,
    AWAITING_FEEDBACK_ACK,
    READY_FOR_NEXT,
    COMPLETED,
}

/**
 * The ordered phase plan for a session (v6 SessionPlan). Built by the adaptation layer
 * (later slice); the reducer only walks it. Repair steps are *inserted* dynamically and are
 * not part of the static plan.
 */
@Serializable
data class SessionPlan(val steps: List<SessionPhase>) {
    init {
        require(steps.isNotEmpty()) { "A SessionPlan must have at least one step." }
    }

    val size: Int get() = steps.size
}

/**
 * Immutable session state. [activePhase] is the phase actually being presented (which may be
 * [SessionPhase.REPAIR_MODE] even while [stepIndex] points at a training step); [stepIndex]
 * is the position within the static [plan].
 */
@Serializable
data class LanguageSessionState(
    val sessionId: LanguageSessionId,
    val plan: SessionPlan,
    val stepIndex: Int,
    val activePhase: SessionPhase,
    val status: SessionStatus,
    val repairPending: Boolean,
) {
    /** The static plan phase at the current position (ignores active repair branching). */
    val plannedPhase: SessionPhase get() = plan.steps[stepIndex]

    companion object {
        /** A not-yet-started state for [sessionId] over [plan]. */
        fun notStarted(sessionId: LanguageSessionId, plan: SessionPlan): LanguageSessionState =
            LanguageSessionState(
                sessionId = sessionId,
                plan = plan,
                stepIndex = 0,
                activePhase = plan.steps[0],
                status = SessionStatus.NOT_STARTED,
                repairPending = false,
            )
    }
}

/**
 * The scored outcome of a response, produced by the ScoringEngine (later slice) — NOT by the
 * UI. The reducer reads it to decide feedback and repair branching. [errorTypes] non-empty in
 * a [SessionPhase.REPAIRABLE] phase triggers an inserted repair retry.
 */
@Serializable
data class ResponseOutcome(
    val isCorrect: Boolean,
    val errorTypes: List<ErrorType> = emptyList(),
    val confidence: Proportion? = null,
)

/** Actions the UI may emit. Sealed so the reducer branches exhaustively. */
@Serializable
sealed interface LanguageSessionAction {
    /** Begin the session over [plan]. */
    @Serializable
    data class Start(val plan: SessionPlan) : LanguageSessionAction

    /** Submit the *scored* outcome of the current task. */
    @Serializable
    data class SubmitResponse(val outcome: ResponseOutcome) : LanguageSessionAction

    /** The user acknowledged the feedback card. */
    @Serializable
    data object FeedbackAcknowledged : LanguageSessionAction

    /** Advance to the next task (or repair retry, or completion). */
    @Serializable
    data object RequestNext : LanguageSessionAction

    /** End the session early. */
    @Serializable
    data object End : LanguageSessionAction
}

/** Stable reason codes for an explainable decision (v6 §1.3, §11.4). */
@Serializable
enum class ReasonCode {
    SESSION_STARTED,
    RESPONSE_SCORED,
    ERROR_PATTERN_DETECTED,
    FEEDBACK_REQUIRED,
    REPAIR_PRESCRIBED,
    ADVANCING,
    PLAN_COMPLETE,
    SESSION_ENDED,
    INVALID_ACTION_IGNORED,
}

/** A claim-safe, user-facing explanation of why the system did what it did (§11.4). */
@Serializable
data class DecisionExplanation(
    val reasonCodes: List<ReasonCode>,
    val userMessage: String,
)

/** The decision the reducer attaches to a transition; the UI renders it. */
@Serializable
sealed interface TrainingDecision {
    val explanation: DecisionExplanation

    /** Present a task for [phase] at [stepIndex]. */
    @Serializable
    data class PresentTask(
        val phase: SessionPhase,
        val stepIndex: Int,
        override val explanation: DecisionExplanation,
    ) : TrainingDecision

    /** Show feedback; [repairTriggered] signals an upcoming repair retry. */
    @Serializable
    data class ShowFeedback(
        val repairTriggered: Boolean,
        override val explanation: DecisionExplanation,
    ) : TrainingDecision

    /** The session is finished. */
    @Serializable
    data class CompleteSession(override val explanation: DecisionExplanation) : TrainingDecision

    /** The action was not valid for the current status and was ignored (state unchanged). */
    @Serializable
    data class NoOp(override val explanation: DecisionExplanation) : TrainingDecision
}

/** Result of reducing one action: the next state plus the decision the UI must render. */
@Serializable
data class SessionTransition(
    val state: LanguageSessionState,
    val decision: TrainingDecision,
)
