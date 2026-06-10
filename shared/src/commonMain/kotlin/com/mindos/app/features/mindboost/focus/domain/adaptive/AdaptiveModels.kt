package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/** The decision the adaptive engine makes after a block. Safety outranks progression. */
sealed interface AdaptiveDecision {
    data class StepUp(val axis: DifficultyAxis) : AdaptiveDecision
    data object Hold : AdaptiveDecision
    data class StepDown(val axis: DifficultyAxis) : AdaptiveDecision
    data class SwitchAxis(val from: DifficultyAxis, val to: DifficultyAxis) : AdaptiveDecision
    data object Cooldown : AdaptiveDecision
    data object StopSession : AdaptiveDecision
    data class ContainImpulsivity(val axis: DifficultyAxis) : AdaptiveDecision
}

/** The outcome of a decision: the chosen action plus the next difficulty and a human-readable reason. */
data class AdaptiveResult(
    val decision: AdaptiveDecision,
    val nextDifficulty: DifficultyVector,
    val rationale: String,
)

enum class FatigueLevel { LOW, MODERATE, HIGH }

/** Combined fatigue signal — never a single metric (03_/04_). */
data class FatigueSignal(
    val level: FatigueLevel,
    val emerging: Boolean,
    val sustained: Boolean,
) {
    companion object {
        val NONE = FatigueSignal(FatigueLevel.LOW, emerging = false, sustained = false)
    }
}

/** Per-axis mastery, so the engine can detect saturation and pick a weaker axis. */
data class AxisMastery(
    val axis: DifficultyAxis,
    val level: AxisLevel,
    val accuracyAtLevel: Double,
)

/**
 * Tunable thresholds. The default values are the tuning constants themselves
 * (documented by their parameter names), so MagicNumber is suppressed here.
 */
@Suppress("MagicNumber")
data class AdaptiveConfig(
    val accUpThreshold: Double = 0.85,
    val accHoldLow: Double = 0.72,
    val accDownThreshold: Double = 0.65,
    val consecutiveErrorsDown: Int = 3,
    val rtCvSpikeFactor: Double = 1.30,
    val stepUp: Double = 0.08,
    val stepDown: Double = 0.12,
    val cooldownAfterDecisions: Int = 1,
    val impulsiveRtFloorMs: Long = 250,
    val masterySaturation: Double = 0.92,
    /** Fallback baseline RT-CV when the user has none calibrated yet. */
    val fallbackBaselineRtCv: Double = 0.25,
)

/** Everything a decision needs. Immutable. */
data class AdaptiveInput(
    val exercise: ExerciseDefinition,
    val current: DifficultyVector,
    val recentBlock: MetricSet,
    /** A short trailing window of trials (e.g. last 8–12) for sequential signals. */
    val recentTrials: List<TrialResult>,
    val perAxisMastery: Map<DifficultyAxis, AxisMastery>,
    val fatigue: FatigueSignal,
    /** Blocks elapsed since the last difficulty change — drives hysteresis. */
    val blocksSinceChange: Int,
    val config: AdaptiveConfig = AdaptiveConfig(),
)
