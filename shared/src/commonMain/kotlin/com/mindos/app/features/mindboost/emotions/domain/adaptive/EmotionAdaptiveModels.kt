package com.mindos.app.features.mindboost.emotions.domain.adaptive

import com.mindos.app.features.mindboost.emotions.domain.catalog.AdaptiveZone
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyState

/** The adaptive action chosen for the next block. Safety actions strictly precede performance ones. */
enum class AdaptiveAction { SAFETY_STOP, REDUCE_INTENSITY, CHECK_IN, COOLDOWN, STEP_DOWN, HOLD, STEP_UP }

/**
 * Everything the engine needs to decide the next block. [intensity] is the discrete 1..5 safety scale
 * (the engine never raises it; difficulty is what rises). [selfReport] is the latest before/check-in
 * reading (null if none).
 */
data class EmotionAdaptiveInput(
    val zone: AdaptiveZone,
    val governingAxis: EmotionAxis,
    /**
     * Ola 0 · S2 — the metric THIS exercise is adapted on (`EmotionExerciseDefinition.governingMetric`), which the
     * definition already guarantees is one of the exercise's `primaryMetrics`. Deliberately has NO default: a
     * default would let a call site silently fall back to a key its paradigm never emits, which is the exact defect
     * this field exists to remove.
     */
    val governingMetric: EmotionMetricKey,
    val metrics: EmotionMetricSet,
    val currentDifficulty: EmotionDifficultyVector,
    val intensity: InductionIntensity,
    val safety: EmotionSafetyState,
    val consecutiveErrors: Int,
    val blocksSinceChange: Int,
    val selfReport: SelfReport? = null,
)

/** The engine's decision: an action plus the next difficulty/intensity. */
data class EmotionAdaptiveDecision(
    val action: AdaptiveAction,
    val nextDifficulty: EmotionDifficultyVector,
    val nextIntensity: InductionIntensity,
)
