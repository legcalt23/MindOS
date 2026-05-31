package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.CommandAction
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.resolver.ResolutionFailure

sealed interface CommandRouterFailure {
    data object EmptyInput : CommandRouterFailure
    data class InputTooLong(val length: Int, val maxLength: Int) : CommandRouterFailure
    data class UnsafeInput(val codes: List<String>) : CommandRouterFailure
    data class NoPatternMatched(val normalizedInput: String) : CommandRouterFailure
    data class AmbiguousCommand(val normalizedInput: String, val candidateIds: List<PatternId>) : CommandRouterFailure
    data class NoResolverForPattern(val patternId: PatternId) : CommandRouterFailure
    data class ResolutionFailed(val cause: ResolutionFailure) : CommandRouterFailure
}

sealed interface CommandRouterResult {
    data class Success(
        val action: CommandAction,
        val trace: CommandRoutingTrace
    ) : CommandRouterResult

    data class Failure(
        val failure: CommandRouterFailure,
        val trace: CommandRoutingTrace
    ) : CommandRouterResult
}
