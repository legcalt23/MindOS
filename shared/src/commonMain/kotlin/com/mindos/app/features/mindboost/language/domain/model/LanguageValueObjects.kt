package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Self-validating value objects for the language feature. Immutable, `commonMain`-pure,
 * and serialized transparently as their underlying primitive (inline value classes).
 *
 * Invariants live here so the rest of the domain can assume validity. Note: identity
 * value objects guard *structure* (non-blank, non-negative); semantic content validation
 * of a whole [TaskPassport] is the accountable job of the `ContentValidator` (later
 * slice), which returns actionable errors rather than throwing.
 */

/** Stable identity of a language training/evaluation session. */
@Serializable
@JvmInline
value class LanguageSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "LanguageSessionId cannot be blank." }
    }
}

/** Stable identity of a content task/item. */
@Serializable
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId cannot be blank." }
    }
}

/** Stable identity of a single attempt (one response to one task). */
@Serializable
@JvmInline
value class AttemptId(val value: String) {
    init {
        require(value.isNotBlank()) { "AttemptId cannot be blank." }
    }
}

/**
 * Pseudonymous user reference. The domain never stores a raw user id — only a hash
 * produced in the platform layer (v6 §13 data minimization). Guarded as non-blank here;
 * hashing strength is enforced where the hash is produced.
 */
@Serializable
@JvmInline
value class UserIdHash(val value: String) {
    init {
        require(value.isNotBlank()) { "UserIdHash cannot be blank." }
    }
}

/** Monotonic content version of a task. 1-based; bumped on any scoring-relevant change. */
@Serializable
@JvmInline
value class TaskVersion(val value: Int) {
    init {
        require(value >= 1) { "TaskVersion is 1-based and must be >= 1 (was $value)." }
    }

    companion object {
        val FIRST = TaskVersion(1)
    }
}

/** A proportion in the closed range 0.0..1.0 (correctness, rates, risks, dose axes). */
@Serializable
@JvmInline
value class Proportion(val value: Double) {
    init {
        require(value in 0.0..1.0) { "Proportion must be in 0.0..1.0 (was $value)." }
    }

    val percent: Int get() = (value * PERCENT_SCALE).toInt()

    companion object {
        private const val PERCENT_SCALE = 100

        val ZERO = Proportion(0.0)
        val ONE = Proportion(1.0)

        /** Builds a proportion clamped into range — for derived values that may drift. */
        fun clamped(raw: Double): Proportion = Proportion(raw.coerceIn(0.0, 1.0))
    }
}

/** Response latency in milliseconds. Never negative. */
@Serializable
@JvmInline
value class LatencyMs(val value: Long) {
    init {
        require(value >= 0) { "LatencyMs cannot be negative (was $value)." }
    }

    companion object {
        val ZERO = LatencyMs(0)
    }
}
