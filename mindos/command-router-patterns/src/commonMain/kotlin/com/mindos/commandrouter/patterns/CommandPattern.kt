package com.mindos.commandrouter.patterns

interface CommandPattern {
    val id: PatternId
    val priority: PatternPriority
    fun match(normalizedInput: String): PatternMatch?
    fun specificity(): PatternSpecificity
}
