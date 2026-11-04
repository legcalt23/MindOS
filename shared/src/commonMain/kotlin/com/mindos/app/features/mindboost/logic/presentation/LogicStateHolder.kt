@file:Suppress("TooManyFunctions", "LongParameterList")

package com.mindos.app.features.mindboost.logic.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.domain.model.JustificationClaim
import com.mindos.app.features.mindboost.logic.domain.model.LogicAttempt
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.model.LogicDifficulty
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicResponse
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.LogicScoringVersion
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningRule
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.presentation.state.LogicUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Orchestrates the turn-based Logic vertical slice; all scoring/adaptation remains in domain engines. */
class LogicStateHolder(
    private val graph: LogicGraph,
    private val userId: String,
    private val displayName: String,
    private val scope: CoroutineScope,
    // MC2 (Fase 2.2): additive write-ahead session lifecycle. Defaults keep production/tests unchanged until a
    // real SecureSessionRecorder is injected. NEVER touches scoring (only the onStart/onFinalize hooks below).
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
) {
    var uiState: LogicUiState by mutableStateOf<LogicUiState>(LogicUiState.Loading)
        private set

    private var items: List<LogicItem> = emptyList()
    private var attempts: List<LogicAttempt> = emptyList()
    private var skillStates: List<SkillState> = emptyList()
    private var currentIndex: Int = 0
    private var sessionStartedAt: Long = 0
    private var itemStartedAt: Long = 0

    // Opaque write-ahead session id (MC2), generated at startSession; NOT the module's own LogicSession id.
    private var recorderSessionId: String = ""

    fun loadHome() {
        uiState = LogicUiState.Loading
        scope.launch {
            runCatching {
                skillStates = graph.repository.loadSkillStates(userId)
                val sessions = graph.repository.recentSessions(userId, HOME_HISTORY_LIMIT)
                LogicUiState.Home(displayName, sessions.size, skillStates)
            }.onSuccess { uiState = it }
                .onFailure { uiState = LogicUiState.Error("No se pudo cargar Lógica.") }
        }
    }

    fun startSession() {
        scope.launch {
            runCatching {
                val configuration = graph.repository.loadConfiguration(userId)
                skillStates = graph.repository.loadSkillStates(userId)
                items = graph.contentProvider.sessionItems()
                    .filter { it.exerciseType in configuration.unlockedExerciseTypes }
                    .take(configuration.sessionItemCount)
                require(items.isNotEmpty()) { "No logic items available." }
                attempts = emptyList()
                currentIndex = 0
                sessionStartedAt = graph.epochClock.nowMillis()
                recorderSessionId = sessionIdGenerator.newSessionId()
                recorder.onStart(recorderSessionId, ScoreModuleId.LOGIC, sessionStartedAt, items.size)
                showCurrentQuestion()
            }.onFailure { uiState = LogicUiState.Error("No se pudo preparar la sesión.") }
        }
    }

    fun selectAnswer(answerId: String) = updateQuestion { copy(selectedAnswerId = answerId, validationMessage = null) }

    fun togglePremise(premiseId: String) = updateQuestion {
        val next = selectedPremiseIds.toMutableSet().apply {
            if (!add(premiseId)) remove(premiseId)
        }
        copy(selectedPremiseIds = next, validationMessage = null)
    }

    fun selectRule(rule: ReasoningRule) = updateQuestion { copy(selectedRule = rule, validationMessage = null) }

    fun selectClaim(claim: JustificationClaim) =
        updateQuestion { copy(selectedClaim = claim, validationMessage = null) }

    fun selectConfidence(confidence: LogicConfidence) =
        updateQuestion { copy(confidence = confidence, validationMessage = null) }

    fun useHint() = updateQuestion {
        val hint = item.hints.getOrNull(hintsUsed) ?: visibleHint
        copy(hintsUsed = hintsUsed + if (hint == null) 0 else 1, visibleHint = hint)
    }

    fun submitAnswer() {
        val question = uiState as? LogicUiState.Question ?: return
        if (!question.hasCompleteResponse()) {
            uiState = question.copy(
                validationMessage = "Completa respuesta, premisas, regla, justificación y confianza.",
            )
            return
        }
        val answer = requireNotNull(question.selectedAnswerId)
        val rule = requireNotNull(question.selectedRule)
        val claim = requireNotNull(question.selectedClaim)
        val confidence = requireNotNull(question.confidence)

        val now = graph.epochClock.nowMillis()
        val response = LogicResponse(
            selectedAnswerId = answer,
            selectedPremiseIds = question.selectedPremiseIds.toList(),
            reasoningRule = rule,
            justificationInput = claim,
            confidence = confidence,
            hintsUsed = question.hintsUsed,
            timeSpentMs = (now - itemStartedAt).coerceAtLeast(0),
        )
        val transferConsistency = transferConsistencyFor(question.item, answer)
        val scoring = graph.scorer.score(question.item, response, transferConsistency)
        val attempt = LogicAttempt(
            itemId = question.item.id,
            exerciseType = question.item.exerciseType,
            targetSkills = question.item.targetSkills,
            difficulty = question.item.difficultyLevel,
            surfaceForm = question.item.surfaceForm,
            logicalStructureId = question.item.logicalStructureId,
            selectedAnswer = response.selectedAnswerId,
            isCorrect = scoring.isCorrect,
            premisesSelected = response.selectedPremiseIds,
            reasoningRule = response.reasoningRule,
            justificationInput = response.justificationInput,
            confidence = response.confidence,
            hintsUsed = response.hintsUsed,
            timeSpentMs = response.timeSpentMs,
            scoreBreakdown = scoring.score,
            errorTags = scoring.errorTags,
            transferTag = question.item.transferLevel,
            isIsomorphicProbe = question.item.isIsomorphicProbe,
            createdAt = now,
        )
        attempts = attempts + attempt
        uiState = LogicUiState.Feedback(
            attempt = attempt,
            feedback = graph.feedbackEngine.feedback(question.item, response, scoring),
            itemIndex = question.itemIndex,
            itemCount = question.itemCount,
        )
    }

    fun continueSession() {
        if (uiState !is LogicUiState.Feedback) return
        currentIndex += 1
        if (currentIndex < items.size) {
            showCurrentQuestion()
        } else {
            finishSession()
        }
    }

    fun returnHome() {
        items = emptyList()
        attempts = emptyList()
        loadHome()
    }

    private fun showCurrentQuestion() {
        itemStartedAt = graph.epochClock.nowMillis()
        uiState = LogicUiState.Question(
            item = items[currentIndex],
            itemIndex = currentIndex + 1,
            itemCount = items.size,
        )
    }

    private fun finishSession() {
        scope.launch {
            runCatching {
                val result = buildResult(attempts)
                val updatedSkills = graph.difficultyAdapter.updateSkillStates(skillStates, attempts)
                val endedAt = graph.epochClock.nowMillis()
                val session = LogicSession(
                    id = "$userId:$endedAt",
                    userId = userId,
                    startedAt = sessionStartedAt,
                    endedAt = endedAt,
                    attempts = attempts,
                    result = result,
                )
                graph.repository.saveSession(session)
                graph.repository.saveSkillStates(userId, updatedSkills)
                skillStates = updatedSkills
                recorder.onFinalize(recorderSessionId, attempts.size, (endedAt - sessionStartedAt).coerceAtLeast(0))
                LogicUiState.Summary(result, updatedSkills)
            }.onSuccess { uiState = it }
                .onFailure { uiState = LogicUiState.Error("La sesión terminó, pero no se pudo guardar.") }
        }
    }

    private fun buildResult(sessionAttempts: List<LogicAttempt>): LogicSessionResult {
        val decision = graph.difficultyAdapter.decide(
            current = sessionAttempts.lastOrNull()?.difficulty ?: LogicDifficulty.FOUNDATION,
            recentAttempts = sessionAttempts,
        )
        val topErrors = sessionAttempts.flatMap { it.errorTags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(TOP_ERROR_LIMIT)
        val probes = sessionAttempts.filter { it.isIsomorphicProbe }
        return LogicSessionResult(
            averageIcr = sessionAttempts.map { it.scoreBreakdown.totalIcr }.averageOrZero(),
            inferentialAccuracy = sessionAttempts.count { it.isCorrect }.toDouble() / sessionAttempts.size,
            averageJustification = sessionAttempts.map { it.scoreBreakdown.justificationScore }.averageOrZero(),
            averageConfidenceCalibration = sessionAttempts
                .map { it.scoreBreakdown.confidenceCalibrationScore }
                .averageOrZero(),
            transferConsistency = probes.takeIf { it.isNotEmpty() }
                ?.map { if (it.isCorrect) 1.0 else 0.0 }
                ?.averageOrZero(),
            topErrorTags = topErrors,
            recommendation = decision.reason,
            scoringVersion = LogicScoringVersion.CURRENT,
        )
    }

    private fun transferConsistencyFor(item: LogicItem, selectedAnswerId: String): Double? {
        if (!item.isIsomorphicProbe) return null
        val trainedTwin = attempts.lastOrNull { it.logicalStructureId == item.logicalStructureId }
        val probeCorrect = selectedAnswerId == item.correctAnswerId
        return trainedTwin?.let { if (it.isCorrect && probeCorrect) 1.0 else 0.0 }
    }

    private fun updateQuestion(transform: LogicUiState.Question.() -> LogicUiState.Question) {
        val question = uiState as? LogicUiState.Question ?: return
        uiState = question.transform()
    }

    private fun LogicUiState.Question.hasCompleteResponse(): Boolean =
        selectedPremiseIds.isNotEmpty() &&
            listOf(selectedAnswerId, selectedRule, selectedClaim, confidence).none { it == null }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private companion object {
        const val HOME_HISTORY_LIMIT = 30
        const val TOP_ERROR_LIMIT = 3
    }
}
