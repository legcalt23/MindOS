package com.mindos.app.features.mindboost.creativity.domain.validation

/** Why an offline response failed boundary validation. */
enum class CreativityValidationError {
    EMPTY_RESPONSE,
    BELOW_MIN_IDEAS,
    TIMING_NOT_MONOTONIC,
}

/**
 * Result of validating a local response at the system boundary. Transient (not persisted),
 * so it is intentionally not `@Serializable`.
 */
sealed interface CreativityValidationResult {
    data object Valid : CreativityValidationResult
    data class Invalid(val reason: CreativityValidationError) : CreativityValidationResult

    val isValid: Boolean get() = this is Valid
}
