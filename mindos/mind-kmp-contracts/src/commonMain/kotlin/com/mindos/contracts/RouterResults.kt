package com.mindos.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouterRequestDto(
    val requestId: String,
    val input: String,
    val localeTag: String = "und",
    val sessionId: String? = null,
    val source: RouterCommandSourceDto = RouterCommandSourceDto.TYPED_TEXT,
    val abuseContext: RouterAbuseContextDto = RouterAbuseContextDto(),
    val explain: Boolean = false,
    val schemaVersion: Int = ContractSchema.Current,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(localeTag.isNotBlank()) { "localeTag must not be blank." }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must be null or non-blank." }
        require(schemaVersion > 0) { "schemaVersion must be positive." }
    }
}

@Serializable
sealed interface RouterResultDto {
    val schemaVersion: Int
    val requestId: String
}

@Serializable
@SerialName("success")
data class RouterSuccessDto(
    override val requestId: String,
    val action: RouterActionDto,
    val warnings: List<RouterWarningDto> = emptyList(),
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterResultDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }
}

@Serializable
@SerialName("failure")
data class RouterFailureResultDto(
    override val requestId: String,
    val reason: RouterFailureDto,
    override val schemaVersion: Int = ContractSchema.Current,
) : RouterResultDto {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }
}

@Serializable
data class RouterWarningDto(
    val code: String,
    val message: String,
) {
    init {
        require(code.isNotBlank()) { "RouterWarningDto.code must not be blank." }
        require(message.isNotBlank()) { "RouterWarningDto.message must not be blank." }
    }
}

@Serializable
sealed interface RouterFailureDto

@Serializable
@SerialName("empty_input")
data object EmptyInputFailureDto : RouterFailureDto

@Serializable
@SerialName("input_too_long")
data class InputTooLongFailureDto(
    val length: Int,
    val maxLength: Int,
) : RouterFailureDto {
    init {
        require(length >= 0) { "length cannot be negative." }
        require(maxLength > 0) { "maxLength must be positive." }
    }
}

@Serializable
@SerialName("unsafe_input")
data class UnsafeInputFailureDto(
    val codes: List<String>,
) : RouterFailureDto {
    init {
        require(codes.isNotEmpty()) { "codes must not be empty." }
        require(codes.all { it.isNotBlank() }) { "codes must not contain blanks." }
    }
}

@Serializable
@SerialName("no_match")
data object NoMatchFailureDto : RouterFailureDto

@Serializable
@SerialName("ambiguous")
data class AmbiguousFailureDto(
    val candidateActionTypes: List<String>,
) : RouterFailureDto {
    init {
        require(candidateActionTypes.size >= 2) { "ambiguous failures require at least two candidates." }
        require(candidateActionTypes.all { it.isNotBlank() }) { "candidateActionTypes must not contain blanks." }
    }
}

@Serializable
@SerialName("validation_failed")
data class ValidationFailedFailureDto(
    val failures: List<ValidationIssueDto>,
) : RouterFailureDto {
    init {
        require(failures.isNotEmpty()) { "validation failures must not be empty." }
    }
}

@Serializable
@SerialName("unsupported_schema_version")
data class UnsupportedSchemaVersionFailureDto(
    val supported: Int,
    val actual: Int,
) : RouterFailureDto {
    init {
        require(supported > 0) { "supported must be positive." }
        require(actual > 0) { "actual must be positive." }
    }
}
