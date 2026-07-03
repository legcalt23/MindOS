@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.decisiones.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.decisiones.presentation.DecisionsFlowStage
import com.mindos.app.features.mindboost.decisiones.presentation.DecisionsFlowStateHolder
import com.mindos.app.features.mindboost.decisiones.presentation.reversibilityTrialsOrEmpty
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.session.DecisionRound

/** Composition-scoped factory for [DecisionsFlowStateHolder] (one session per id/timestamp). */
@Composable
fun rememberDecisionsFlowStateHolder(
    sessionId: String,
    timestampMs: Long,
): DecisionsFlowStateHolder = remember(sessionId, timestampMs) {
    DecisionsFlowStateHolder(sessionId = sessionId, timestampMs = timestampMs)
}

/**
 * Hosts a full Decisiones session, swapping the child screen as the [flow] advances through
 * check-in -> Bayes Arena -> Reversibility -> result. Each child reports completion into the
 * coordinator; the result stage assembles the collected check-in and rounds. [onExit] leaves the
 * feature (back from any stage, or after the session is sealed).
 */
@Composable
fun DecisionsRootScreen(
    flow: DecisionsFlowStateHolder,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onReversibilityTrials: (List<ReversibilityTrial>) -> Unit = {},
) {
    when (flow.stage) {
        DecisionsFlowStage.CheckIn -> DecisionCheckInScreen(
            holder = rememberDecisionCheckInStateHolder(),
            onFinished = flow::onCheckInComplete,
            onBack = onExit,
            modifier = modifier,
        )

        DecisionsFlowStage.BayesArena -> BayesArenaScreen(
            holder = rememberBayesArenaStateHolder(seed = flow.resultTimestampMs),
            onFinished = flow::onBayesComplete,
            onBack = onExit,
            modifier = modifier,
        )

        DecisionsFlowStage.Reversibility -> {
            val reversibilityHolder = rememberReversibilityTriageStateHolder(seed = flow.resultTimestampMs)
            ReversibilityTriageScreen(
                holder = reversibilityHolder,
                onFinished = { rounds ->
                    handleReversibilityFinish(
                        reversibilityHolder.uiState,
                        rounds,
                        onReversibilityTrials,
                        flow::onReversibilityComplete,
                    )
                },
                onBack = onExit,
                modifier = modifier,
            )
        }

        DecisionsFlowStage.Result -> DecisionResultScreen(
            holder = rememberDecisionResultStateHolder(
                sessionId = flow.resultSessionId,
                timestampMs = flow.resultTimestampMs,
                checkIn = flow.checkInInput(),
                rounds = flow.rounds,
            ),
            onSealed = { _, _ -> onExit() },
            onBack = onExit,
            modifier = modifier,
        )
    }
}

/**
 * Pure orchestration of a finished reversibility stage: persist the captured trials, THEN advance the flow.
 * Extracted from the composable so the persist-on-finish wiring is unit-testable without driving the Compose
 * tree. Only a [ReversibilityTriageUiState.Finished] yields trials ([reversibilityTrialsOrEmpty]); any other
 * state persists nothing. The flow advances regardless (the screen only fires this from the Finished state).
 */
internal fun handleReversibilityFinish(
    finishedState: ReversibilityTriageUiState,
    rounds: List<DecisionRound>,
    persist: (List<ReversibilityTrial>) -> Unit,
    advance: (List<DecisionRound>) -> Unit,
) {
    persist(finishedState.reversibilityTrialsOrEmpty())
    advance(rounds)
}
