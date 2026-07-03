@file:Suppress("MagicNumber") // Difficulty bounds (0.0/1.0) and the 1..5 intensity scale are the data.

package com.mindos.app.features.mindboost.emotions.domain.adaptive

/**
 * Difficulty axes. The adaptive engine moves EXACTLY one axis per decision (restricted to an
 * exercise's relevant axes). Intensity is tracked separately as a safety variable, never an axis.
 */
enum class EmotionAxis {
    TEMPORAL_PRESSURE,
    AFFECTIVE_INTERFERENCE,
    INHIBITORY_DEMAND,
    POST_ERROR_LOAD,
    CATEGORY_AMBIGUITY,
    TIME_ON_TASK,
}

/** Difficulty as a vector of axis levels in [0,1]. Copy-on-write. */
data class EmotionDifficultyVector(
    val levels: Map<EmotionAxis, Double>,
) {
    fun level(axis: EmotionAxis): Double = (levels[axis] ?: 0.0).coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun with(axis: EmotionAxis, value: Double): EmotionDifficultyVector =
        copy(levels = levels + (axis to value.coerceIn(MIN_LEVEL, MAX_LEVEL)))

    companion object {
        const val MIN_LEVEL = 0.0
        const val MAX_LEVEL = 1.0
        private const val DEFAULT_START = 0.3

        fun initial(axes: Set<EmotionAxis>, start: Double = DEFAULT_START): EmotionDifficultyVector =
            EmotionDifficultyVector(axes.associateWith { start.coerceIn(MIN_LEVEL, MAX_LEVEL) })
    }
}

/**
 * Induction intensity on a discrete 1..5 scale (decision B), modeled as Int to remove float drift.
 * HARD operational ceiling = 3: the engine NEVER auto-induces above 3. Levels 4/5 only arrive from a
 * self-report or guardrail and map to REDUCE / STOP. Mapping: 1-2 normal · 3 operating cap · 4 reduce
 * · 5 stop.
 */
data class InductionIntensity(val value: Int) {
    init { require(value in MIN..MAX) { "intensity must be in $MIN..$MAX" } }

    fun reducedBy(step: Int = 1): InductionIntensity =
        InductionIntensity((value - step).coerceAtLeast(MIN))

    val requiresReduce: Boolean get() = value == REDUCE_AT
    val requiresStop: Boolean get() = value >= STOP_AT

    companion object {
        const val MIN = 1
        const val MAX = 5
        const val OPERATIONAL_CEILING = 3
        const val REDUCE_AT = 4
        const val STOP_AT = 5
        val NORMAL = InductionIntensity(1)
        val CAP = InductionIntensity(OPERATIONAL_CEILING)
    }
}
