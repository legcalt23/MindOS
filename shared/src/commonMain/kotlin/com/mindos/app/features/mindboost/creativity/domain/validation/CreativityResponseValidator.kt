package com.mindos.app.features.mindboost.creativity.domain.validation

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseDefinition
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityResponse

/**
 * Deterministic, offline validation of a local response against its exercise definition.
 *
 * Pure rules at the system boundary — no network, no AI. This is the single authority on
 * whether a response's timing and content are acceptable, so models stay lenient and this
 * validator owns the checks (including the monotonic-timing anti-cheat rule).
 */
object CreativityResponseValidator {

    fun validate(
        definition: CreativityExerciseDefinition,
        response: CreativityResponse,
    ): CreativityValidationResult = when {
        response.elapsedMonotonicMs < 0 ->
            invalid(CreativityValidationError.TIMING_NOT_MONOTONIC)
        response.normalizedIdeas.isEmpty() ->
            invalid(CreativityValidationError.EMPTY_RESPONSE)
        response.distinctIdeaCount < definition.minIdeas ->
            invalid(CreativityValidationError.BELOW_MIN_IDEAS)
        else -> CreativityValidationResult.Valid
    }

    private fun invalid(reason: CreativityValidationError) = CreativityValidationResult.Invalid(reason)
}
