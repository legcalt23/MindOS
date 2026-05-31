package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternMatchCandidate
import com.mindos.commandrouter.resolver.ResolutionFailure
import com.mindos.commandrouter.resolver.ResolverId

data class CommandRoutingTrace(
    val rawInput: String,
    val normalizedInput: String?,
    val source: String,
    val patternId: PatternId?,
    val resolverId: ResolverId?,
    val actionId: ActionId?,
    val failureReason: String?,
    val ambiguousCandidates: List<PatternId>?
) {
    class Builder(private val input: CommandInput) {
        private var normalizedInput: String? = null
        private var patternId: PatternId? = null
        private var resolverId: ResolverId? = null
        private var actionId: ActionId? = null
        private var failureReason: String? = null
        private var ambiguousCandidates: List<PatternId>? = null

        fun normalized(text: String) { normalizedInput = text }
        fun noMatch() { failureReason = "no_pattern_matched" }
        fun ambiguous(candidates: List<PatternMatchCandidate>) {
            ambiguousCandidates = candidates.map { it.patternId }
            failureReason = "ambiguous_command"
        }
        fun matched(match: com.mindos.commandrouter.patterns.PatternMatch, pattern: com.mindos.commandrouter.patterns.CommandPattern) {
            patternId = pattern.id
        }
        fun resolved(action: com.mindos.commandrouter.actions.CommandAction, resolver: ResolverId) {
            resolverId = resolver
            actionId = action.actionId
        }
        fun resolutionFailed(failure: ResolutionFailure) {
            failureReason = failure::class.simpleName ?: "resolution_failed"
        }
        fun inputTooLong() { failureReason = "input_too_long" }
        fun unsafeInput(reason: String) { failureReason = reason }
        fun noResolver(pid: PatternId) {
            patternId = pid
            failureReason = "no_resolver_for_pattern"
        }

        fun build() = CommandRoutingTrace(
            rawInput = input.rawText,
            normalizedInput = normalizedInput,
            source = input.source::class.simpleName ?: "unknown",
            patternId = patternId,
            resolverId = resolverId,
            actionId = actionId,
            failureReason = failureReason,
            ambiguousCandidates = ambiguousCandidates
        )
    }
}
