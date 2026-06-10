package com.mindos.app.features.mindboost.focus.domain.model

/**
 * Pure-domain value objects for the MindBoost · Enfoque (focus) training system.
 *
 * All are immutable, self-validating, and free of any platform / Compose /
 * Firebase dependency, so they compile in `commonMain` and are exhaustively
 * unit-testable. See docs/mindboost_enfoque_implementation_plan/02_domain_model_plan.md.
 */

/** Measured reaction time in milliseconds. Never negative. */
@JvmInline
value class ReactionTimeMs(val value: Long) {
    init {
        require(value >= 0) { "ReactionTimeMs cannot be negative (was $value)." }
    }

    companion object {
        val ZERO = ReactionTimeMs(0)
    }
}

/** A proportion in the closed range 0.0..1.0 (accuracy, ratios, rates). */
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

/**
 * Normalized level of a single difficulty axis: 0.0 (easiest) .. 1.0 (hardest).
 * Stepping always stays in range (copy-on-write, never mutates).
 */
@JvmInline
value class AxisLevel(val value: Double) {
    init {
        require(value in 0.0..1.0) { "AxisLevel must be in 0.0..1.0 (was $value)." }
    }

    fun stepUp(delta: Double): AxisLevel = AxisLevel((value + delta).coerceIn(0.0, 1.0))

    fun stepDown(delta: Double): AxisLevel = AxisLevel((value - delta).coerceIn(0.0, 1.0))

    companion object {
        val MIN = AxisLevel(0.0)
        val MAX = AxisLevel(1.0)

        fun clamped(raw: Double): AxisLevel = AxisLevel(raw.coerceIn(0.0, 1.0))
    }
}

/** Stable identity of a training/evaluation session. */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank." }
    }
}

/** Stable identity of a block within a session. */
@JvmInline
value class BlockId(val value: String) {
    init {
        require(value.isNotBlank()) { "BlockId cannot be blank." }
    }
}

/** Stable identity of a single trial. */
@JvmInline
value class TrialId(val value: String) {
    init {
        require(value.isNotBlank()) { "TrialId cannot be blank." }
    }
}
