package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.actions.CommandAction
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.CorrelationId
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ResolverId(val value: String)

data class ResolutionContext(
    val locale: String = "en",
    val correlationId: CorrelationId? = null,
    val source: CommandSource = CommandSource.UserInput,
    val extra: Map<String, String> = emptyMap()
)

sealed interface ResolutionFailure {
    data class NoPatternMatched(val input: String) : ResolutionFailure
    data class AmbiguousPattern(val input: String, val candidateIds: List<String>) : ResolutionFailure
    data class InvalidParameters(val patternId: String, val reason: String) : ResolutionFailure
    data class UnsupportedCommand(val patternId: String) : ResolutionFailure
    data class Unauthorized(val patternId: String, val reason: String) : ResolutionFailure
    data class InternalResolutionError(val message: String) : ResolutionFailure
}

sealed interface ResolutionResult {
    data class Resolved(val action: CommandAction, val resolverId: ResolverId) : ResolutionResult
    data class Failed(val failure: ResolutionFailure) : ResolutionResult
}
