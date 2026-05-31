package com.mindos.commandrouter.patterns

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PatternId(val value: String)

@Serializable
@JvmInline
value class PatternPriority(val value: Int) : Comparable<PatternPriority> {
    override fun compareTo(other: PatternPriority): Int = this.value.compareTo(other.value)

    companion object {
        val Default = PatternPriority(0)
        val High = PatternPriority(100)
        val Low = PatternPriority(-100)
    }
}

data class PatternSpecificity(
    val staticTokenCount: Int,
    val parameterCount: Int
) : Comparable<PatternSpecificity> {
    override fun compareTo(other: PatternSpecificity): Int {
        val staticCmp = this.staticTokenCount.compareTo(other.staticTokenCount)
        if (staticCmp != 0) return staticCmp
        // fewer params is more specific
        return other.parameterCount.compareTo(this.parameterCount)
    }
}
