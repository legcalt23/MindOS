@file:Suppress("MagicNumber") // Telemetry defaults are neutral/clean domain values, not magic constants.

package com.mindos.app.features.mindboost.core.antigaming

/** Severity-ordered anti-gaming action. The aggregator picks the most severe TRIGGERED action. */
enum class GamingAction { NONE, FLAG_ONLY, WITHHOLD_PROFILE, INVALIDATE }

/** One detector's output. [strength] is 0..1; [evidence] is a PII-free reason code, never user copy. */
data class DetectorSignal(
    val detectorId: String,
    val triggered: Boolean,
    val strength: Double,
    val action: GamingAction,
    val evidence: String,
)

/** Aggregate verdict: the most severe action across detectors. NEVER writes a low score (invariant). */
data class GamingVerdict(val action: GamingAction, val firedDetectors: List<String>) {
    /** Clean enough to let the profile update (the ScoreResult machine still applies its own gates). */
    val isClean: Boolean get() = action == GamingAction.NONE || action == GamingAction.FLAG_ONLY
}

/**
 * INFRA-03 · Aggregate telemetry a detector inspects. Every field DEFAULTS to a clean/neutral value, so a
 * test or an adversary profile sets only what it exercises. Carries NO raw PII — aggregate signals only.
 */
@Suppress("LongParameterList") // A cohesive, all-default neutral telemetry bundle.
data class TrialTelemetry(
    val interResponseIntervalsMs: List<Long> = emptyList(),
    val rtsMs: List<Long> = emptyList(),
    val difficulties: List<Double> = emptyList(),
    val accuracy: Double = 1.0,
    val chanceAccuracy: Double = 0.5,
    val medianRtMs: Long? = null,
    val chanceFloorRtMs: Long = 250L,
    val anticipationRateInBlock: Double = 0.0,
    val stopRespondRate: Double? = null,
    val responseKeys: List<Int> = emptyList(),
    val reexposedFraction: Double = 0.0,
    val typeTokenRatio: Double = 1.0,
    val keywordDensity: Double = 0.0,
    val revealedBeforeCommitFraction: Double = 0.0,
    val multiTouchFraction: Double = 0.0,
    val repeatedItemRtDropFraction: Double = 0.0,
    val repeatedItemAccuracy: Double = 0.0,
    val isomorphGain: Double = 1.0,
    val difficultyAbandonmentCorrelation: Double = 0.0,
    val isEmotionalOrFrustration: Boolean = false,
)

/** A pure anti-gaming detector. Returns a [DetectorSignal]; NEVER emits user-facing copy. */
interface GamingDetector {
    val id: String
    fun evaluate(telemetry: TrialTelemetry): DetectorSignal
}

/** Aggregates detector signals into the most severe verdict. */
object AntiGamingFramework {
    fun aggregate(signals: List<DetectorSignal>): GamingVerdict {
        val fired = signals.filter { it.triggered }
        val action = fired.maxOfOrNull { it.action } ?: GamingAction.NONE
        return GamingVerdict(action, fired.map { it.detectorId })
    }
}
