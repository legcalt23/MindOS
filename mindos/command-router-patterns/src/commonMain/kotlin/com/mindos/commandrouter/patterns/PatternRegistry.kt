package com.mindos.commandrouter.patterns

class PatternRegistry(private val patterns: List<CommandPattern>) {

    fun findMatch(normalizedInput: String): PatternMatchResult {
        data class Candidate(
            val pattern: CommandPattern,
            val match: PatternMatch,
            val specificity: PatternSpecificity
        )

        val candidates = patterns.mapNotNull { pattern ->
            val match = pattern.match(normalizedInput) ?: return@mapNotNull null
            Candidate(pattern, match, pattern.specificity())
        }

        if (candidates.isEmpty()) return PatternMatchResult.NoMatch

        val sorted = candidates.sortedWith(
            compareByDescending<Candidate> { it.pattern.priority.value }
                .thenByDescending { it.specificity }
                .thenBy { it.pattern.id.value }
        )

        val best = sorted.first()
        val tied = sorted.filter {
            it.pattern.priority == best.pattern.priority && it.specificity == best.specificity
        }

        return if (tied.size == 1) {
            PatternMatchResult.Matched(best.match, best.pattern)
        } else {
            PatternMatchResult.Ambiguous(tied.map { PatternMatchCandidate(it.pattern.id, it.match) })
        }
    }
}
