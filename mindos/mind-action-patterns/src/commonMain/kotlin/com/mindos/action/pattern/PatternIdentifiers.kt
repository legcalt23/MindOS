package com.mindos.action.pattern

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Stable, author-assigned id for a pattern. Used for diagnostics, never as a ranking tie-break. */
@Serializable
@JvmInline
value class PatternId(val value: String) {
    init {
        require(value.isNotBlank()) { "PatternId must not be blank" }
    }
}

/** Explicit author priority. Higher wins. Default is 0. */
@Serializable
@JvmInline
value class PatternPriority(val value: Int) : Comparable<PatternPriority> {
    override fun compareTo(other: PatternPriority): Int = value.compareTo(other.value)

    companion object {
        val Default = PatternPriority(0)
    }
}
