package com.mindos.command.runtime

import com.mindos.action.execution.ExecutionResult
import com.mindos.contracts.RouterAbuseContextDto
import com.mindos.contracts.RouterFailureResultDto

enum class CommandSource {
    TYPED_TEXT,
    VOICE_FINAL_TRANSCRIPT,
    SHORTCUT,
    DEEP_LINK,
    NOTIFICATION,
}

data class TextCommandRequest(
    val input: String,
    val requestId: String,
    val sessionId: String? = null,
    val localeTag: String = "und",
    val source: CommandSource = CommandSource.TYPED_TEXT,
    val abuseContext: RouterAbuseContextDto = RouterAbuseContextDto(),
    val dryRun: Boolean = false,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must be null or non-blank." }
        require(localeTag.isNotBlank()) { "localeTag must not be blank." }
    }
}

data class CommandConfirmationRequest(
    val token: ConfirmationToken,
    val requestId: String,
    val sessionId: String,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }
    }
}

sealed interface CommandConfirmationCancellationResult {
    data object Cancelled : CommandConfirmationCancellationResult
    data class Rejected(val reason: ConfirmationRejectionReason) : CommandConfirmationCancellationResult
    data object RuntimeDisabled : CommandConfirmationCancellationResult
    data object InternalFailure : CommandConfirmationCancellationResult
}

data class CommandRuntimeConfig(
    val enabled: Boolean = false,
)

data class CommandDispatchTimings(
    val routingMs: Long? = null,
    val executionMs: Long? = null,
)

enum class RuntimeStage {
    ROUTING,
    MAPPING,
    SAFETY,
    CONFIRMATION,
    EXECUTION,
}

sealed interface CommandRejectionReason {
    data object RuntimeDisabled : CommandRejectionReason
    data object ResultCorrelationMismatch : CommandRejectionReason
    data class MappingFailed(val reason: String) : CommandRejectionReason
    data class SafetyDenied(val reason: String) : CommandRejectionReason
    data object ConfirmationRequiresSession : CommandRejectionReason
    data class ConfirmationRejected(val reason: ConfirmationRejectionReason) : CommandRejectionReason
    data class InternalFailure(val stage: RuntimeStage) : CommandRejectionReason
}

sealed interface CommandDispatchResult {
    val requestId: String
    val timings: CommandDispatchTimings

    data class Executed(
        override val requestId: String,
        val actionType: String,
        val execution: ExecutionResult.Executed,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult

    data class NeedsConfirmation(
        override val requestId: String,
        val actionType: String,
        val prompt: String,
        val token: ConfirmationToken,
        val expiresAtMillis: Long,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult

    data class RoutingFailed(
        override val requestId: String,
        val failure: RouterFailureResultDto,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult

    data class ExecutionFailed(
        override val requestId: String,
        val actionType: String,
        val execution: ExecutionResult,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult

    data class Rejected(
        override val requestId: String,
        val reason: CommandRejectionReason,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult

    data class DryRun(
        override val requestId: String,
        val actionType: String,
        val summary: String,
        override val timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) : CommandDispatchResult
}

data class ResponseText(val value: String) {
    init {
        require(value.isNotBlank()) { "ResponseText must not be blank." }
    }
}
