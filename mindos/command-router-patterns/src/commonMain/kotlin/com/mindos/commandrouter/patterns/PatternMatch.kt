package com.mindos.commandrouter.patterns

data class PatternMatch(
    val patternId: PatternId,
    val extractedParameters: Map<String, String> = emptyMap()
)

data class PatternMatchCandidate(
    val patternId: PatternId,
    val match: PatternMatch
)

sealed interface PatternMatchResult {
    data class Matched(val match: PatternMatch, val pattern: CommandPattern) : PatternMatchResult
    data object NoMatch : PatternMatchResult
    data class Ambiguous(val candidates: List<PatternMatchCandidate>) : PatternMatchResult
}
