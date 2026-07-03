package com.mindos.app.features.mindboost.language.domain.session

/**
 * Pure, deterministic reducer for the language session (v6 §12.4). `(state, action) →
 * transition`. No I/O, no time, no platform — fully unit-testable and replayable.
 *
 * It is *total*: every (status, action) pair has a defined result. Actions that are invalid
 * for the current status leave the state unchanged and return a [TrainingDecision.NoOp], so a
 * stray UI event can never crash or corrupt the session.
 *
 * Repair branching: an incorrect/erroneous response in a [SessionPhase.REPAIRABLE] phase sets
 * [LanguageSessionState.repairPending]; the next [LanguageSessionAction.RequestNext] then
 * presents an inserted [SessionPhase.REPAIR_MODE] retry for the *same* plan step before the
 * plan advances (Detectar → … → Reintentar, §6.5).
 */
object LanguageSessionReducer {

    fun reduce(state: LanguageSessionState, action: LanguageSessionAction): SessionTransition =
        when (action) {
            is LanguageSessionAction.Start -> handleStart(state, action)
            is LanguageSessionAction.SubmitResponse -> handleSubmit(state, action)
            is LanguageSessionAction.FeedbackAcknowledged -> handleFeedbackAck(state)
            is LanguageSessionAction.RequestNext -> handleRequestNext(state)
            is LanguageSessionAction.End -> handleEnd(state)
        }

    private fun handleStart(
        state: LanguageSessionState,
        action: LanguageSessionAction.Start,
    ): SessionTransition {
        if (state.status != SessionStatus.NOT_STARTED) return noOp(state)
        val started = LanguageSessionState.notStarted(state.sessionId, action.plan)
            .copy(status = SessionStatus.AWAITING_RESPONSE)
        return SessionTransition(
            state = started,
            decision = TrainingDecision.PresentTask(
                phase = started.activePhase,
                stepIndex = started.stepIndex,
                explanation = explanation(
                    listOf(ReasonCode.SESSION_STARTED),
                    "Empezamos con ${started.activePhase.label.lowercase()}.",
                ),
            ),
        )
    }

    private fun handleSubmit(
        state: LanguageSessionState,
        action: LanguageSessionAction.SubmitResponse,
    ): SessionTransition {
        if (state.status != SessionStatus.AWAITING_RESPONSE) return noOp(state)
        val repairTriggered = action.outcome.errorTypes.isNotEmpty() &&
            state.activePhase in SessionPhase.REPAIRABLE
        val reasons = buildList {
            add(ReasonCode.RESPONSE_SCORED)
            if (repairTriggered) add(ReasonCode.ERROR_PATTERN_DETECTED)
            add(ReasonCode.FEEDBACK_REQUIRED)
        }
        val message = if (repairTriggered) {
            "Detecté un patrón reparable; lo trabajaremos antes de seguir."
        } else {
            "Respuesta registrada."
        }
        return SessionTransition(
            state = state.copy(
                status = SessionStatus.AWAITING_FEEDBACK_ACK,
                repairPending = repairTriggered,
            ),
            decision = TrainingDecision.ShowFeedback(repairTriggered, explanation(reasons, message)),
        )
    }

    private fun handleFeedbackAck(state: LanguageSessionState): SessionTransition {
        if (state.status != SessionStatus.AWAITING_FEEDBACK_ACK) return noOp(state)
        return SessionTransition(
            state = state.copy(status = SessionStatus.READY_FOR_NEXT),
            decision = TrainingDecision.NoOp(
                explanation(listOf(ReasonCode.FEEDBACK_REQUIRED), "Feedback revisado."),
            ),
        )
    }

    private fun handleRequestNext(state: LanguageSessionState): SessionTransition {
        if (state.status != SessionStatus.READY_FOR_NEXT) return noOp(state)
        val nextIndex = state.stepIndex + 1
        return when {
            state.repairPending -> presentRepair(state)
            nextIndex >= state.plan.size -> SessionTransition(
                state = state.copy(status = SessionStatus.COMPLETED),
                decision = TrainingDecision.CompleteSession(
                    explanation(listOf(ReasonCode.PLAN_COMPLETE), "Completaste el plan de la sesión."),
                ),
            )
            else -> advance(state, nextIndex)
        }
    }

    private fun presentRepair(state: LanguageSessionState): SessionTransition {
        val next = state.copy(
            status = SessionStatus.AWAITING_RESPONSE,
            activePhase = SessionPhase.REPAIR_MODE,
            repairPending = false,
        )
        return SessionTransition(
            state = next,
            decision = TrainingDecision.PresentTask(
                phase = SessionPhase.REPAIR_MODE,
                stepIndex = next.stepIndex,
                explanation = explanation(
                    listOf(ReasonCode.REPAIR_PRESCRIBED, ReasonCode.ADVANCING),
                    "Reintento isomórfico para fijar el patrón.",
                ),
            ),
        )
    }

    private fun advance(state: LanguageSessionState, nextIndex: Int): SessionTransition {
        val phase = state.plan.steps[nextIndex]
        val next = state.copy(
            status = SessionStatus.AWAITING_RESPONSE,
            stepIndex = nextIndex,
            activePhase = phase,
        )
        return SessionTransition(
            state = next,
            decision = TrainingDecision.PresentTask(
                phase = phase,
                stepIndex = nextIndex,
                explanation = explanation(
                    listOf(ReasonCode.ADVANCING),
                    "Continuamos con ${phase.label.lowercase()}.",
                ),
            ),
        )
    }

    private fun handleEnd(state: LanguageSessionState): SessionTransition {
        if (state.status == SessionStatus.COMPLETED) return noOp(state)
        return SessionTransition(
            state = state.copy(status = SessionStatus.COMPLETED),
            decision = TrainingDecision.CompleteSession(
                explanation(listOf(ReasonCode.SESSION_ENDED), "Sesión finalizada."),
            ),
        )
    }

    private fun noOp(state: LanguageSessionState): SessionTransition = SessionTransition(
        state = state,
        decision = TrainingDecision.NoOp(
            explanation(listOf(ReasonCode.INVALID_ACTION_IGNORED), "Acción ignorada."),
        ),
    )

    private fun explanation(reasons: List<ReasonCode>, message: String): DecisionExplanation =
        DecisionExplanation(reasonCodes = reasons, userMessage = message)
}
