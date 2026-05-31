package com.mindos.commandrouter.patterns

class ExactCommandPattern(
    override val id: PatternId,
    val command: String,
    override val priority: PatternPriority = PatternPriority.Default
) : CommandPattern {

    private val normalized = command.trim().lowercase().replace(Regex("\\s+"), " ")
    private val tokenCount = normalized.split(" ").size

    override fun match(normalizedInput: String): PatternMatch? =
        if (normalizedInput == normalized) PatternMatch(id) else null

    override fun specificity() = PatternSpecificity(staticTokenCount = tokenCount, parameterCount = 0)
}
