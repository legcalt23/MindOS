package com.mindos.app.features.mindboost.memory.domain.model

/**
 * Pure-domain value objects for the MindBoost · Memoria (memory) training system.
 *
 * All are immutable, self-validating, and free of any platform / Compose /
 * Firebase dependency, so they compile in `commonMain` and are exhaustively
 * unit-testable. Mirrors the focus feature's FocusValueObjects.kt convention but
 * is kept self-contained inside the memory feature (no cross-feature coupling).
 */

/** Measured reaction / retrieval time in milliseconds. Never negative. */
@JvmInline
value class ReactionTimeMs(val value: Long) {
    init {
        require(value >= 0) { "ReactionTimeMs cannot be negative (was $value)." }
    }

    companion object {
        val ZERO = ReactionTimeMs(0)
    }
}

/** A proportion in the closed range 0.0..1.0 (accuracy, ratios, rates, d-prime inputs). */
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
 * Stepping always stays in range (copy-on-write, never mutates). This is the
 * normalized representation the adaptive engine moves; exercise generators decode
 * it into concrete parameters (item counts, delays, similarity, …).
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

/**
 * 1-based position of an item within a studied list/sequence. Used for serial
 * position curves and order-error analysis. Always >= 1.
 */
@JvmInline
value class SerialPosition(val value: Int) {
    init {
        require(value >= 1) { "SerialPosition is 1-based and must be >= 1 (was $value)." }
    }
}

/**
 * Granularity of the confidence scale offered to the user (doc §4 Ej.16, §16 Q8).
 * The scale widens as metacognition improves: beginners use [BINARY], elite users
 * use [SIX_POINT].
 */
@Suppress("MagicNumber") // the level counts ARE the data of this enum.
enum class ConfidenceScale(val levels: Int) {
    BINARY(2),
    FOUR_POINT(4),
    SIX_POINT(6),
}

/**
 * A metacognitive confidence judgment attached to a response. [level] is 1-based
 * (1 = least confident / "guessing", [scale].levels = most confident / "sure").
 *
 * [normalized] maps the level onto 0.0..1.0 so calibration math (Brier score,
 * confidence-accuracy alignment, gamma) is scale-agnostic.
 */
data class ConfidenceJudgment(
    val level: Int,
    val scale: ConfidenceScale,
) {
    init {
        require(level in 1..scale.levels) {
            "Confidence level must be in 1..${scale.levels} for $scale (was $level)."
        }
    }

    /** 0.0 (least confident) .. 1.0 (most confident), independent of scale size. */
    fun normalized(): Proportion =
        if (scale.levels == 1) {
            Proportion.ONE
        } else {
            Proportion((level - 1).toDouble() / (scale.levels - 1).toDouble())
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
