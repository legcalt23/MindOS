package com.mindos.app.features.mindboost.creativity.domain.session

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityPromptRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityResponseRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityRubricRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionStore
import com.mindos.app.features.mindboost.creativity.data.persistence.PersistedCreativitySessionDto
import com.mindos.app.features.mindboost.creativity.domain.exercise.CreativityExerciseCatalog
import com.mindos.app.features.mindboost.creativity.domain.exercise.CreativityExerciseEngine
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPrompt
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityResponse
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityRubricResult
import com.mindos.app.features.mindboost.creativity.domain.scoring.CreativityRubricScorer
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityResponseValidator
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityValidationError
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityValidationResult

/** An in-progress local creativity round: prompt presented, awaiting a response. */
data class ActiveCreativitySession(
    val sessionId: String,
    val userId: String,
    val exerciseId: CreativityExerciseId,
    val prompt: CreativityPrompt,
    val createdAtMs: Long,
)

/** Outcome of submitting a response to an active round. */
sealed interface CreativitySubmitResult {
    data class Completed(val session: PersistedCreativitySessionDto) : CreativitySubmitResult
    data class Rejected(val reason: CreativityValidationError) : CreativitySubmitResult
}

/**
 * Deterministic, offline orchestration of one creativity practice round:
 * start (local prompt via [CreativityExerciseEngine]) → submit (validate via
 * [CreativityResponseValidator]) → persist (through [CreativitySessionStore]).
 *
 * No AI, no network. The prompt seed comes from a caller-supplied monotonic start so a round
 * is reproducible; wall-clock timestamps for history come from the injected [EpochClock].
 * Rubric scoring is intentionally NOT wired here — that lands in Phase 5.
 */
class CreativitySessionCoordinator(
    private val store: CreativitySessionStore,
    private val clock: EpochClock = SystemEpochClock,
) {

    fun start(
        userId: String,
        exerciseId: CreativityExerciseId,
        monotonicStartMs: Long,
    ): ActiveCreativitySession = ActiveCreativitySession(
        sessionId = "${exerciseId.name}-$monotonicStartMs",
        userId = userId,
        exerciseId = exerciseId,
        prompt = CreativityExerciseEngine.nextPrompt(exerciseId, seed = monotonicStartMs),
        createdAtMs = clock.nowMillis(),
    )

    suspend fun submit(
        active: ActiveCreativitySession,
        ideas: List<String>,
        elapsedMonotonicMs: Long,
    ): CreativitySubmitResult {
        val definition = CreativityExerciseCatalog.definitionFor(active.exerciseId)
        val response = CreativityResponse(
            id = "${active.sessionId}-r",
            promptId = active.prompt.id,
            ideas = ideas,
            elapsedMonotonicMs = elapsedMonotonicMs,
        )
        return when (val validation = CreativityResponseValidator.validate(definition, response)) {
            is CreativityValidationResult.Invalid -> CreativitySubmitResult.Rejected(validation.reason)
            CreativityValidationResult.Valid -> {
                val rubric = CreativityRubricScorer.score(definition, response)
                CreativitySubmitResult.Completed(persist(active, response, rubric))
            }
        }
    }

    private suspend fun persist(
        active: ActiveCreativitySession,
        response: CreativityResponse,
        rubric: CreativityRubricResult,
    ): PersistedCreativitySessionDto {
        val dto = PersistedCreativitySessionDto(
            session = CreativitySessionRecord(
                sessionId = active.sessionId,
                userId = active.userId,
                exerciseId = active.exerciseId,
                createdAtMs = active.createdAtMs,
                updatedAtMs = clock.nowMillis(),
            ),
            prompts = listOf(
                CreativityPromptRecord(
                    promptId = active.prompt.id,
                    sessionId = active.sessionId,
                    exerciseId = active.exerciseId,
                    text = active.prompt.text,
                ),
            ),
            responses = listOf(
                CreativityResponseRecord(
                    responseId = response.id,
                    sessionId = active.sessionId,
                    promptId = response.promptId,
                    ideas = response.ideas,
                    elapsedMonotonicMs = response.elapsedMonotonicMs,
                ),
            ),
            rubric = CreativityRubricRecord(
                sessionId = active.sessionId,
                rubricVersion = rubric.rubricVersion,
                signals = rubric.signals,
                summary = rubric.summary,
                aiDerived = rubric.aiDerived,
            ),
        )
        store.save(dto)
        return dto
    }
}
