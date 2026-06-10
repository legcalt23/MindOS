package com.mindos.app.features.mindboost.memory.domain.model

/**
 * The 8 independent axes of difficulty in MindBoost · Memoria (doc §5). Difficulty
 * is NEVER a single scalar "level" — it is a multi-dimensional vector, and the
 * adaptive engine moves exactly one axis per decision (doc §5.3). "More items =
 * harder" as the only lever is explicitly forbidden (doc §13.11).
 *
 * Letters match the CME formula symbols in doc §5.
 */
enum class MemoryDifficultyAxis {
    /** L — number of items to remember (load). */
    LOAD,

    /** I — similarity/overlap between items or between blocks (interference). */
    INTERFERENCE,

    /** D — time between encoding and recall (delay). */
    DELAY,

    /** S — resemblance between targets and distractors (similarity). */
    SIMILARITY,

    /** P — depth of encoding required (rote → elaborative). */
    DEPTH,

    /** R — type of retrieval demanded (recognition → free recall). */
    RETRIEVAL,

    /** C — contextual variation between study and test. */
    CONTEXT,

    /** K — secondary (concurrent) task load. */
    CONCURRENCY,
}

/**
 * Per-user sensitivity parameters (α, β, γ, δ, ε of doc §5). They scale how much
 * each multiplicative axis contributes to perceived difficulty for a given user.
 * Defaults to 1.0 (population prior) until calibration estimates them.
 */
data class CmeSensitivity(
    val alpha: Double = 1.0,
    val beta: Double = 1.0,
    val gamma: Double = 1.0,
    val delta: Double = 1.0,
    val epsilon: Double = 1.0,
) {
    init {
        require(alpha >= 0 && beta >= 0 && gamma >= 0 && delta >= 0 && epsilon >= 0) {
            "CME sensitivity parameters must be non-negative."
        }
    }

    companion object {
        /** Population prior — all sensitivities at 1.0. */
        val DEFAULT = CmeSensitivity()
    }
}

/**
 * Multi-axis difficulty vector. Every axis is a normalized [AxisLevel] (0.0..1.0);
 * exercise generators decode each level into concrete parameters using the axis
 * ranges declared in the catalogue (e.g. LOAD 0.0..1.0 → 2..30 items).
 *
 * Immutable: every adjustment returns a copy. An exercise only ever manipulates
 * the axes listed in its [MemoryExerciseDefinition.relevantAxes]; the rest are inert.
 */
data class MemoryDifficultyVector(
    val load: AxisLevel = AxisLevel.MIN,
    val interference: AxisLevel = AxisLevel.MIN,
    val delay: AxisLevel = AxisLevel.MIN,
    val similarity: AxisLevel = AxisLevel.MIN,
    val depth: AxisLevel = AxisLevel.MIN,
    val retrieval: AxisLevel = AxisLevel.MIN,
    val context: AxisLevel = AxisLevel.MIN,
    val concurrency: AxisLevel = AxisLevel.MIN,
) {
    /** Reads the normalized level of a single axis. */
    fun level(axis: MemoryDifficultyAxis): AxisLevel = when (axis) {
        MemoryDifficultyAxis.LOAD -> load
        MemoryDifficultyAxis.INTERFERENCE -> interference
        MemoryDifficultyAxis.DELAY -> delay
        MemoryDifficultyAxis.SIMILARITY -> similarity
        MemoryDifficultyAxis.DEPTH -> depth
        MemoryDifficultyAxis.RETRIEVAL -> retrieval
        MemoryDifficultyAxis.CONTEXT -> context
        MemoryDifficultyAxis.CONCURRENCY -> concurrency
    }

    /** Returns a copy with a single axis set to [newLevel]. */
    fun adjust(axis: MemoryDifficultyAxis, newLevel: AxisLevel): MemoryDifficultyVector = when (axis) {
        MemoryDifficultyAxis.LOAD -> copy(load = newLevel)
        MemoryDifficultyAxis.INTERFERENCE -> copy(interference = newLevel)
        MemoryDifficultyAxis.DELAY -> copy(delay = newLevel)
        MemoryDifficultyAxis.SIMILARITY -> copy(similarity = newLevel)
        MemoryDifficultyAxis.DEPTH -> copy(depth = newLevel)
        MemoryDifficultyAxis.RETRIEVAL -> copy(retrieval = newLevel)
        MemoryDifficultyAxis.CONTEXT -> copy(context = newLevel)
        MemoryDifficultyAxis.CONCURRENCY -> copy(concurrency = newLevel)
    }

    /** Steps a single axis up by [delta], clamped to 1.0. */
    fun stepUp(axis: MemoryDifficultyAxis, delta: Double): MemoryDifficultyVector =
        adjust(axis, level(axis).stepUp(delta))

    /** Steps a single axis down by [delta], clamped to 0.0. */
    fun stepDown(axis: MemoryDifficultyAxis, delta: Double): MemoryDifficultyVector =
        adjust(axis, level(axis).stepDown(delta))

    /**
     * Carga Mnésica Efectiva (doc §5). The conceptual difficulty of a trial as a
     * single positive scalar, used to keep the user in the desirable-difficulty
     * zone. NOT used to drive adaptation directly (the engine adjusts individual
     * axes); it is a comparison/telemetry quantity.
     *
     * Faithful to:
     * ```
     * CME = L × (1+α·I) × (1+β·D) × (1+γ·S) × P_weight × R_weight × (1+δ·C) × (1+ε·K)
     * ```
     * with normalized axes:
     * - L  → loadFactor = 1 + load                  (1.0 .. 2.0)
     * - P_weight = 1.0 + DEPTH_RANGE·depth          (1.0 .. 1.5, rote → elaborative)
     * - R_weight = R_FLOOR + R_RANGE·retrieval      (0.7 .. 1.5, recognition → free)
     * - I, D, S, C, K used directly as their 0..1 levels.
     */
    fun cme(sensitivity: CmeSensitivity = CmeSensitivity.DEFAULT): Double {
        val loadFactor = 1.0 + load.value
        val pWeight = P_FLOOR + DEPTH_RANGE * depth.value
        val rWeight = R_FLOOR + R_RANGE * retrieval.value
        return loadFactor *
            (1.0 + sensitivity.alpha * interference.value) *
            (1.0 + sensitivity.beta * delay.value) *
            (1.0 + sensitivity.gamma * similarity.value) *
            pWeight *
            rWeight *
            (1.0 + sensitivity.delta * context.value) *
            (1.0 + sensitivity.epsilon * concurrency.value)
    }

    /** Mean of all axes — a coarse "overall load" scalar for display only, never for adaptation. */
    fun overallLoad(): Double =
        MemoryDifficultyAxis.entries.sumOf { level(it).value } / MemoryDifficultyAxis.entries.size

    companion object {
        /** P_weight floor (rote encoding) and span up to elaborative (1.0 → 1.5). */
        private const val P_FLOOR = 1.0
        private const val DEPTH_RANGE = 0.5

        /** R_weight floor (recognition) and span up to free recall (0.7 → 1.5). */
        private const val R_FLOOR = 0.7
        private const val R_RANGE = 0.8

        val EASIEST = MemoryDifficultyVector()
    }
}
