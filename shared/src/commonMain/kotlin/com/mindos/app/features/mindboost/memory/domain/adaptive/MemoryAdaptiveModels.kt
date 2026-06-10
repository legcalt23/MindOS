package com.mindos.app.features.mindboost.memory.domain.adaptive

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricSet
import com.mindos.app.features.mindboost.memory.domain.metrics.rawValueOf
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseDefinition

/**
 * The desirable-difficulty zone derived from block performance (doc §5). The
 * adaptive engine keeps the user in [OPTIMAL] by moving exactly one CME dimension.
 */
enum class MemoryZone {
    FRUSTRATION,
    SUBOPTIMAL_LOW,
    OPTIMAL,
    SUBOPTIMAL_HIGH,
    TOO_EASY,
}

/**
 * The decision after a block. Safety (stop/rest/metacognition) outranks
 * progression (step). Difficulty is NEVER moved on more than one axis per decision
 * (doc §5.3); speed is trained without changing difficulty (doc §5.3 rule 3).
 */
sealed interface MemoryAdaptiveDecision {
    data class StepUp(val axis: MemoryDifficultyAxis) : MemoryAdaptiveDecision
    data class StepDown(val axis: MemoryDifficultyAxis) : MemoryAdaptiveDecision
    data object Hold : MemoryAdaptiveDecision

    /** High accuracy but slow retrieval: keep difficulty, train speed (doc §5.3). */
    data object WorkSpeed : MemoryAdaptiveDecision

    /** Poor confidence calibration: engage the metacognition overlay (doc §5.3). */
    data object ActivateMetacognition : MemoryAdaptiveDecision

    /** Fatigue emerging: offer a short rest, no difficulty change (doc §5.4). */
    data object ProposeRest : MemoryAdaptiveDecision

    /** Sustained fatigue or repeated frustration: end the session (doc §5.4). */
    data object StopSession : MemoryAdaptiveDecision
}

/** Result of a decision: the action, the next difficulty, the zone and an honest reason. */
data class MemoryAdaptiveResult(
    val decision: MemoryAdaptiveDecision,
    val nextDifficulty: MemoryDifficultyVector,
    val zone: MemoryZone,
    val performance: Double,
    val rationale: String,
)

enum class MemoryFatigueLevel { LOW, MODERATE, HIGH }

/** Combined fatigue signal — never a single metric. */
data class MemoryFatigueSignal(
    val level: MemoryFatigueLevel,
    val emerging: Boolean,
    val sustained: Boolean,
) {
    companion object {
        val NONE = MemoryFatigueSignal(MemoryFatigueLevel.LOW, emerging = false, sustained = false)
    }
}

/**
 * Tunable thresholds and weights (doc §5). The defaults ARE the tuning constants
 * (documented by their names), so MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
data class MemoryAdaptiveConfig(
    // Performance weights (positives sum to 1.0).
    val wAccuracy: Double = 0.40,
    val wDelayed: Double = 0.40,
    val wSpeed: Double = 0.20,
    val wIntrusion: Double = 0.30,
    val wConfidenceBias: Double = 0.30,
    // Zone boundaries (doc §5 table).
    val frustrationMax: Double = 0.40,
    val suboptimalLowMax: Double = 0.55,
    val optimalMax: Double = 0.80,
    val suboptimalHighMax: Double = 0.90,
    // Step sizes: small = 5–10%, big = 15–20% (doc §5 table).
    val stepSmall: Double = 0.08,
    val stepBig: Double = 0.18,
    // Hysteresis: hold this many blocks after a change (anti-oscillation).
    val cooldownBlocks: Int = 1,
    // Dimension-selection thresholds (doc §5.3).
    val confidenceBiasThreshold: Double = 0.20,
    val delayedLagMargin: Double = 0.15,
    val intrusionRaiseThreshold: Double = 0.20,
    val slowSpeedNorm: Double = 0.35,
    // Speed normalization window (ms): floor → fast/1.0, ceiling → slow/0.0.
    val speedFloorMs: Double = 400.0,
    val speedCeilingMs: Double = 4000.0,
    // Repeated frustration blocks that end the session.
    val frustrationBlocksToStop: Int = 3,
) {
    init {
        require(frustrationMax < suboptimalLowMax) { "Zone boundaries must be ascending." }
        require(suboptimalLowMax < optimalMax) { "Zone boundaries must be ascending." }
        require(optimalMax < suboptimalHighMax) { "Zone boundaries must be ascending." }
    }
}

/** Everything a decision needs. Immutable. */
data class MemoryAdaptiveInput(
    val exercise: MemoryExerciseDefinition,
    val current: MemoryDifficultyVector,
    val recentBlock: MemoryMetricSet,
    val fatigue: MemoryFatigueSignal = MemoryFatigueSignal.NONE,
    /** Consecutive blocks in the frustration zone (drives StopSession). */
    val consecutiveFrustrationBlocks: Int = 0,
    /** Blocks since the last difficulty change — drives hysteresis. */
    val blocksSinceChange: Int = Int.MAX_VALUE,
    /** Axis raised most recently — reduced first on an abrupt performance drop (doc §5.3). */
    val lastRaisedAxis: MemoryDifficultyAxis? = null,
    /** Confidence calibration bias (over/under-confidence); 0.0 until EX16 wires it. */
    val confidenceBias: Double = 0.0,
    val config: MemoryAdaptiveConfig = MemoryAdaptiveConfig(),
)

/**
 * Block performance (doc §5):
 * `P = w₁·acc + w₂·delayed + w₃·speedNorm − w₄·intrusion − w₅·bias²`, clamped to [0,1].
 *
 * When a delayed-recall measure is absent (immediate-only block) immediate accuracy
 * stands in for it; when no RT is available speed is neutral (0.5) so it neither
 * rewards nor penalizes.
 */
object MemoryPerformance {

    fun of(block: MemoryMetricSet, confidenceBias: Double, config: MemoryAdaptiveConfig): Double {
        val immediate = block[MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY]?.value
        val delayed = block[MemoryMetricKey.DELAYED_RECALL_ACCURACY]?.value
        val accuracy = immediate ?: delayed ?: 0.0
        val delayedAccuracy = delayed ?: immediate ?: 0.0
        val intrusion = block.rawValueOf(MemoryMetricKey.INTRUSION_RATE, 0.0)
        val speedNorm = speedNorm(block, config)

        val raw = config.wAccuracy * accuracy +
            config.wDelayed * delayedAccuracy +
            config.wSpeed * speedNorm -
            config.wIntrusion * intrusion -
            config.wConfidenceBias * confidenceBias * confidenceBias
        return raw.coerceIn(0.0, 1.0)
    }

    /** Lower RT → higher (1.0). Neutral 0.5 when no RT metric is present. */
    fun speedNorm(block: MemoryMetricSet, config: MemoryAdaptiveConfig): Double {
        val rt = block[MemoryMetricKey.RETRIEVAL_SPEED]?.value ?: return NEUTRAL_SPEED
        val span = config.speedCeilingMs - config.speedFloorMs
        return if (span <= 0.0) NEUTRAL_SPEED else ((config.speedCeilingMs - rt) / span).coerceIn(0.0, 1.0)
    }

    private const val NEUTRAL_SPEED = 0.5
}

/** Maps a performance scalar to its [MemoryZone] using the config boundaries (doc §5). */
fun zoneOf(performance: Double, config: MemoryAdaptiveConfig): MemoryZone = when {
    performance < config.frustrationMax -> MemoryZone.FRUSTRATION
    performance < config.suboptimalLowMax -> MemoryZone.SUBOPTIMAL_LOW
    performance <= config.optimalMax -> MemoryZone.OPTIMAL
    performance <= config.suboptimalHighMax -> MemoryZone.SUBOPTIMAL_HIGH
    else -> MemoryZone.TOO_EASY
}
