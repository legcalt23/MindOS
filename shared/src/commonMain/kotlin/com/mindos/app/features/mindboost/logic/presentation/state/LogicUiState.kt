package com.mindos.app.features.mindboost.logic.presentation.state

import com.mindos.app.features.mindboost.logic.domain.model.JustificationClaim
import com.mindos.app.features.mindboost.logic.domain.model.LogicAttempt
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.model.LogicFeedback
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningRule
import com.mindos.app.features.mindboost.logic.domain.model.SkillState

sealed interface LogicUiState {
    data object Loading : LogicUiState

    data class Home(
        val displayName: String,
        val completedSessions: Int,
        val skillStates: List<SkillState>,
    ) : LogicUiState

    data class Question(
        val item: LogicItem,
        val itemIndex: Int,
        val itemCount: Int,
        val selectedAnswerId: String? = null,
        val selectedPremiseIds: Set<String> = emptySet(),
        val selectedRule: ReasoningRule? = null,
        val selectedClaim: JustificationClaim? = null,
        val confidence: LogicConfidence? = null,
        val hintsUsed: Int = 0,
        val visibleHint: String? = null,
        val validationMessage: String? = null,
    ) : LogicUiState

    data class Feedback(
        val attempt: LogicAttempt,
        val feedback: LogicFeedback,
        val itemIndex: Int,
        val itemCount: Int,
    ) : LogicUiState

    data class Summary(
        val result: LogicSessionResult,
        val skillStates: List<SkillState>,
    ) : LogicUiState

    data class Error(val message: String) : LogicUiState
}

