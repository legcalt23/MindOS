package com.mindos.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ValidationIssueCode {
    MISSING_FIELD,
    MALFORMED_FIELD,
    UNSUPPORTED_ACTION,
    UNSUPPORTED_SCHEMA_VERSION,
}

@Serializable
data class ValidationIssueDto(
    val field: String,
    val code: ValidationIssueCode,
    val message: String,
) {
    init {
        require(field.isNotBlank()) { "field must not be blank." }
        require(message.isNotBlank()) { "message must not be blank." }
    }
}

@Serializable
sealed interface ValidationReportDto {
    val schemaVersion: Int
}

@Serializable
@SerialName("valid")
data class ValidValidationReportDto(
    override val schemaVersion: Int = ContractSchema.Current,
) : ValidationReportDto

@Serializable
@SerialName("invalid")
data class InvalidValidationReportDto(
    val issues: List<ValidationIssueDto>,
    override val schemaVersion: Int = ContractSchema.Current,
) : ValidationReportDto {
    init {
        require(issues.isNotEmpty()) { "invalid validation reports must include at least one issue." }
    }
}

fun interface ActionContractValidator {
    fun validate(action: RouterActionDto): ValidationReportDto
}

object DefaultActionContractValidator : ActionContractValidator {
    override fun validate(action: RouterActionDto): ValidationReportDto {
        val issues = buildList {
            if (action.schemaVersion != ContractSchema.Current) {
                add(
                    ValidationIssueDto(
                        field = "schemaVersion",
                        code = ValidationIssueCode.UNSUPPORTED_SCHEMA_VERSION,
                        message = "Unsupported action schema version ${action.schemaVersion}.",
                    ),
                )
            }
            if (action.requestId.isBlank()) {
                add(ValidationIssueDto("requestId", ValidationIssueCode.MISSING_FIELD, "requestId must not be blank."))
            }
            when (action) {
                is OpenProfileActionDto -> requireNonBlank("username", action.username)
                is OpenStoryActionDto -> requireNonBlank("storyRef", action.storyRef)
                is OpenStoryOfUserActionDto -> requireNonBlank("username", action.username)
                is OpenChatActionDto -> requireNonBlank("chatRef", action.chatRef)
                is OpenMessagesActionDto,
                is OpenSettingsActionDto,
                -> Unit
            }
        }
        return if (issues.isEmpty()) ValidValidationReportDto() else InvalidValidationReportDto(issues)
    }

    private fun MutableList<ValidationIssueDto>.requireNonBlank(field: String, value: String) {
        if (value.isBlank()) {
            add(ValidationIssueDto(field, ValidationIssueCode.MISSING_FIELD, "$field must not be blank."))
        }
    }
}
