package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/** The decision the adaptive engine makes after a block. Safety outranks progression. */
/**
 * Why difficulty did not move.
 *
 * Typed so a Hold is never an unexplained no-op, and so the two very different reasons for holding —
 * "we measured, and this is the maintenance band" vs "we could not measure" — can never be confused by a caller
 * or a reader. The second is the fail-closed direction: absent evidence must not move a user's difficulty in
 * either direction.
 */
enum class HoldReason {
    /** Measured, and inside the dead-zone: nothing to change. */
    MAINTENANCE_BAND,

    /** Difficulty changed recently; let it stabilize before changing again. */
    HYSTERESIS_AFTER_CHANGE,

    /**
     * The governing evidence was absent, or the sample did not support it. Never a step. Covers the block's
     * accuracy AND the three session signals ([AdaptiveInput.fatigue], [AdaptiveInput.perAxisMastery],
     * [AdaptiveInput.blocksSinceChange]): a `null` in any of them is "not measured", and not-measured moves
     * difficulty in neither direction and accuses no one.
     */
    METRIC_UNAVAILABLE,

    /** Evaluation/calibration: difficulty is fixed for the whole block by design, so nothing may adapt. */
    FIXED_DIFFICULTY_MODE,

    /**
     * The rule's construct does not exist in this exercise family, so it has nothing to say. Distinct from
     * MAINTENANCE_BAND (measured, mid-band) and METRIC_UNAVAILABLE (could not measure): here the question
     * itself is meaningless for the task, and answering it would contaminate the construct.
     */
    NOT_APPLICABLE_TO_FAMILY,

    /**
     * FOC-P0-05 · the session was not admitted by the quality gate, so the engine was not consulted at all: its
     * metrics were withdrawn and difficulty holds exactly where the session started.
     */
    SESSION_NOT_MEASURABLE,
}

sealed interface AdaptiveDecision {
    data class StepUp(val axis: DifficultyAxis) : AdaptiveDecision
    data class Hold(val reason: HoldReason) : AdaptiveDecision
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

/**
 * Combined fatigue signal — never a single metric (03_/04_).
 *
 * Deliberately has NO "none" constant: a `FatigueSignal` is a MEASUREMENT, and "we did not measure fatigue"
 * is `null` at [AdaptiveInput.fatigue], never a LOW-level sentinel that reads as a healthy user
 * (`MB-EFFICACY-0005`). Guarded by `AdaptiveContractAbsenceGuardTest`.
 */
data class FatigueSignal(
    val level: FatigueLevel,
    val emerging: Boolean,
    val sustained: Boolean,
)

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

/**
 * Everything a decision needs. Immutable.
 *
 * The three session signals are nullable and carry NO defaults: `null` is the honest "this call-site does not
 * measure it yet", it is visible at the call-site, and the engine answers it with `Hold(METRIC_UNAVAILABLE)`.
 * Until a producer exists (FOC-P1-01) the engine route therefore holds; that is the intended C0 containment,
 * not a regression — the alternative was a fabricated healthy user who could be stepped up.
 */
data class AdaptiveInput(
    val exercise: ExerciseDefinition,
    val current: DifficultyVector,
    val recentBlock: MetricSet,
    /** A short trailing window of trials (e.g. last 8–12) for sequential signals. */
    val recentTrials: List<TrialResult>,
    /** Per-axis mastery, or `null` when no mastery tracking ran. An EMPTY map means "tracked, nothing learned". */
    val perAxisMastery: Map<DifficultyAxis, AxisMastery>?,
    /** Measured fatigue, or `null` when no fatigue signal was produced this session. */
    val fatigue: FatigueSignal?,
    /** Blocks elapsed since the last difficulty change — drives hysteresis. `null` when nobody counted. */
    val blocksSinceChange: Int?,
    val config: AdaptiveConfig = AdaptiveConfig(),
)
