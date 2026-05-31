package com.mindos.core

import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.EmptyInputFailureDto
import com.mindos.contracts.InputTooLongFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.RouterFailureDto
import com.mindos.contracts.UnsafeInputFailureDto
import com.mindos.contracts.ValidationFailedFailureDto
import com.mindos.contracts.ValidationIssueCode
import com.mindos.contracts.ValidationIssueDto

sealed interface RouterError {
    data object UnknownCommand : RouterError
    data object EmptyInput : RouterError
    data class InputTooLong(val length: Int, val maxLength: Int) : RouterError
    data class UnsafeInput(val codes: List<String>) : RouterError
    data class InvalidEntity(val field: String, val detail: String) : RouterError
    data class ValidationFailure(val issues: List<ValidationIssueDto>) : RouterError
    data class ResolverFailure(val detail: String) : RouterError
    data class AmbiguousCommand(val candidateActionTypes: List<String>) : RouterError
    data class ConfigurationFailure(val issues: List<String>) : RouterError
}

fun RouterError.toFailureDto(): RouterFailureDto =
    when (this) {
        RouterError.UnknownCommand -> NoMatchFailureDto
        RouterError.EmptyInput -> EmptyInputFailureDto
        is RouterError.InputTooLong -> InputTooLongFailureDto(length, maxLength)
        is RouterError.UnsafeInput -> UnsafeInputFailureDto(codes)
        is RouterError.InvalidEntity -> ValidationFailedFailureDto(
            listOf(ValidationIssueDto(field, ValidationIssueCode.MALFORMED_FIELD, detail)),
        )
        is RouterError.ValidationFailure -> ValidationFailedFailureDto(issues)
        is RouterError.ResolverFailure -> ValidationFailedFailureDto(
            listOf(ValidationIssueDto("resolver", ValidationIssueCode.UNSUPPORTED_ACTION, detail)),
        )
        is RouterError.AmbiguousCommand -> AmbiguousFailureDto(candidateActionTypes)
        is RouterError.ConfigurationFailure -> ValidationFailedFailureDto(
            issues.map { ValidationIssueDto("registry", ValidationIssueCode.MALFORMED_FIELD, it) },
        )
    }
