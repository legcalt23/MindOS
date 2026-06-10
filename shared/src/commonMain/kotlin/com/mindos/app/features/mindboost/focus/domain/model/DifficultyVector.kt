package com.mindos.app.features.mindboost.focus.domain.model

/**
 * The nine independent axes of difficulty. Difficulty in MindBoost · Enfoque is
 * NEVER a single scalar "level" — it is a vector, and the adaptive engine moves
 * exactly one axis per decision (see 04_adaptive_engine_plan.md). Scaling speed
 * alone is explicitly forbidden.
 */
enum class DifficultyAxis {
    PERCEPTUAL_LOAD,
    INTERFERENCE,
    TEMPORAL_UNCERTAINTY,
    INHIBITORY_DEMAND,
    DUAL_TASK_LOAD,
    TIME_ON_TASK,
    PRESSURE,
    DISTRACTOR_SALIENCE,
    FAMILIARITY_PENALTY,
}

/**
 * Multi-axis difficulty vector. Each axis is an [AxisLevel] in 0.0..1.0.
 * Immutable: every adjustment returns a copy. An exercise only ever manipulates
 * the axes listed in its [ExerciseDefinition.relevantAxes]; the rest are inert.
 */
data class DifficultyVector(
    val perceptualLoad: AxisLevel = AxisLevel.MIN,
    val interference: AxisLevel = AxisLevel.MIN,
    val temporalUncertainty: AxisLevel = AxisLevel.MIN,
    val inhibitoryDemand: AxisLevel = AxisLevel.MIN,
    val dualTaskLoad: AxisLevel = AxisLevel.MIN,
    val timeOnTask: AxisLevel = AxisLevel.MIN,
    val pressure: AxisLevel = AxisLevel.MIN,
    val distractorSalience: AxisLevel = AxisLevel.MIN,
    val familiarityPenalty: AxisLevel = AxisLevel.MIN,
) {
    /** Reads the level of a single axis. */
    fun level(axis: DifficultyAxis): AxisLevel = when (axis) {
        DifficultyAxis.PERCEPTUAL_LOAD -> perceptualLoad
        DifficultyAxis.INTERFERENCE -> interference
        DifficultyAxis.TEMPORAL_UNCERTAINTY -> temporalUncertainty
        DifficultyAxis.INHIBITORY_DEMAND -> inhibitoryDemand
        DifficultyAxis.DUAL_TASK_LOAD -> dualTaskLoad
        DifficultyAxis.TIME_ON_TASK -> timeOnTask
        DifficultyAxis.PRESSURE -> pressure
        DifficultyAxis.DISTRACTOR_SALIENCE -> distractorSalience
        DifficultyAxis.FAMILIARITY_PENALTY -> familiarityPenalty
    }

    /** Returns a copy with a single axis set to [newLevel]. */
    fun adjust(axis: DifficultyAxis, newLevel: AxisLevel): DifficultyVector = when (axis) {
        DifficultyAxis.PERCEPTUAL_LOAD -> copy(perceptualLoad = newLevel)
        DifficultyAxis.INTERFERENCE -> copy(interference = newLevel)
        DifficultyAxis.TEMPORAL_UNCERTAINTY -> copy(temporalUncertainty = newLevel)
        DifficultyAxis.INHIBITORY_DEMAND -> copy(inhibitoryDemand = newLevel)
        DifficultyAxis.DUAL_TASK_LOAD -> copy(dualTaskLoad = newLevel)
        DifficultyAxis.TIME_ON_TASK -> copy(timeOnTask = newLevel)
        DifficultyAxis.PRESSURE -> copy(pressure = newLevel)
        DifficultyAxis.DISTRACTOR_SALIENCE -> copy(distractorSalience = newLevel)
        DifficultyAxis.FAMILIARITY_PENALTY -> copy(familiarityPenalty = newLevel)
    }

    /** Steps a single axis up by [delta], clamped to 1.0. */
    fun stepUp(axis: DifficultyAxis, delta: Double): DifficultyVector =
        adjust(axis, level(axis).stepUp(delta))

    /** Steps a single axis down by [delta], clamped to 0.0. */
    fun stepDown(axis: DifficultyAxis, delta: Double): DifficultyVector =
        adjust(axis, level(axis).stepDown(delta))

    /** Mean of all axes — a coarse "overall load" scalar for display only, never for adaptation. */
    fun overallLoad(): Double = DifficultyAxis.entries.sumOf { level(it).value } / DifficultyAxis.entries.size

    companion object {
        val EASIEST = DifficultyVector()
    }
}
