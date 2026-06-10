package com.mindos.app.features.mindboost.focus.domain.model

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey

/**
 * Inferred, revisable archetype of the user. NEVER presented as a clinical label
 * (see 08_personalization_plan.md §5). It is a probabilistic, evolving estimate.
 */
enum class UserArchetype {
    BEGINNER,
    LOW_CONCENTRATION,
    IMPULSIVE,
    SLOW_BUT_ACCURATE,
    FAST_BUT_INACCURATE,
    ADVANCED,
    ELITE,
}

/**
 * Per-user calibrated thresholds. Sensible population defaults are provided so the
 * system is usable before calibration; calibration (06_) personalizes them.
 */
data class IndividualThresholds(
    /** RT above which a hit counts as a lapse (PVT convention default 500ms). */
    val lapseRtMs: Long = 500,
    /** RT below which a response is an anticipation (non-physiological). */
    val anticipationFloorMs: Long = 150,
    /** The user's baseline RT-CV, used to detect relative spikes. Null until calibrated. */
    val baselineRtCv: Double? = null,
) {
    init {
        require(anticipationFloorMs in 1..lapseRtMs) {
            "anticipationFloorMs must be >0 and <= lapseRtMs."
        }
    }
}

/** Immutable snapshot of an individual baseline (the pre side of pre/post change). */
data class BaselineSnapshot(
    val userId: String,
    val perMetric: Map<MetricKey, Double>,
    val capturedAtMs: Long,
    val state: SubjectiveState? = null,
    /** Device identity — RT is only comparable within the same device (risk A1/C8). */
    val deviceFingerprint: String? = null,
)

/**
 * The living cognitive profile. Sensitive data: local-first, never shared, never
 * shown as a diagnosis (10_/14_). Updated after each session / retest.
 */
@Suppress("MagicNumber") // 1..8 (cycle weeks) and 0.0..1.0 (confidence) are documented domain ranges.
data class UserCognitiveProfile(
    val userId: String,
    val archetype: UserArchetype = UserArchetype.BEGINNER,
    val archetypeConfidence: Double = 0.0,
    val thresholds: IndividualThresholds = IndividualThresholds(),
    val baseline: BaselineSnapshot? = null,
    val startingDifficulty: Map<ExerciseId, DifficultyVector> = emptyMap(),
    /** Current week of the 8-week periodization cycle (1..8). */
    val cycleWeek: Int = 1,
    val totalSessions: Int = 0,
    val updatedAtMs: Long = 0,
) {
    init {
        require(cycleWeek in 1..8) { "cycleWeek must be in 1..8 (was $cycleWeek)." }
        require(archetypeConfidence in 0.0..1.0) { "archetypeConfidence must be in 0.0..1.0." }
    }

    /** Initial difficulty for an exercise: calibrated value, or the easiest default. */
    fun startingDifficultyFor(id: ExerciseId): DifficultyVector =
        startingDifficulty[id] ?: DifficultyVector.EASIEST
}
