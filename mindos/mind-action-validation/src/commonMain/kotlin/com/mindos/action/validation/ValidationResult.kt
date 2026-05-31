package com.mindos.action.validation

import com.mindos.action.model.ReferenceKind

/** Why an action failed validation. Typed — invalid actions never execute, and never throw. */
sealed interface ValidationFailure {
    val detail: String

    data class MissingField(val field: String, override val detail: String) : ValidationFailure
    data class MalformedField(val field: String, override val detail: String) : ValidationFailure
    data class Unauthorized(override val detail: String) : ValidationFailure
    data class ResolverNotReady(val kind: ReferenceKind, override val detail: String) : ValidationFailure
}

/** Outcome of validating an action. */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val failures: List<ValidationFailure>) : ValidationResult
}
