@file:Suppress("MagicNumber") // Logistic base/scale and the uncertainty attenuation factor are formula intrinsics.

package com.mindos.app.features.arena.rating.engine

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Phase 03 (PvP path): Versioned blend ratio between TaskPerformanceDelta and MatchOutcomeDelta per
 * mode (blending_config.md). Pure task modes use match weight 0; PvP modes blend, and the ratio is
 * auditable via [version]. Weights must sum to 1. No premium / self-report inputs.
 */
data class BlendingConfig(
    val version: String = "BLEND_RATIO_V1.0",
    val taskWeight: Double = 1.0,
    val matchWeight: Double = 0.0,
) {
    init {
        require(taskWeight in 0.0..1.0) { "taskWeight must be in [0,1]" }
        require(matchWeight in 0.0..1.0) { "matchWeight must be in [0,1]" }
        require(abs(taskWeight + matchWeight - 1.0) < 1e-9) { "taskWeight + matchWeight must equal 1" }
    }

    companion object {
        val PURE_TASK: BlendingConfig = BlendingConfig(taskWeight = 1.0, matchWeight = 0.0)
        val BALANCED_PVP: BlendingConfig =
            BlendingConfig(version = "BLEND_RATIO_PVP_V1.0", taskWeight = 0.5, matchWeight = 0.5)
    }
}

/**
 * Phase 03 (PvP path): Offline, deterministic match-outcome math.
 *
 * - [calculateExpectedMatchOutcome]: Bradley-Terry / Glicko-flavored win probability against a
 *   specific opponent, attenuated by combined uncertainty (high uncertainty pulls toward 0.5).
 * - [matchOutcomeDelta]: observed (0 loss / 0.5 draw / 1 win) minus expected.
 * - [blend]: combines task and match deltas per a [BlendingConfig].
 *
 * Pure: no clock, randomness, or I/O; no premium inputs.
 */
class MatchOutcomeCalculator(
    private val scale: Double = 400.0,
    private val uncertaintyFactor: Double = 3.0,
) {

    fun calculateExpectedMatchOutcome(
        playerMu: Double,
        opponentMu: Double,
        playerSigma: Double,
        opponentSigma: Double,
    ): Double {
        require(playerSigma >= 0.0 && opponentSigma >= 0.0) { "sigma must be >= 0" }
        val combinedVariance = playerSigma * playerSigma + opponentSigma * opponentSigma
        val attenuation = 1.0 / sqrt(1.0 + uncertaintyFactor * combinedVariance / (scale * scale))
        return 1.0 / (1.0 + 10.0.pow(-attenuation * (playerMu - opponentMu) / scale))
    }

    fun matchOutcomeDelta(observed: Double, expected: Double): Double {
        require(observed in 0.0..1.0) { "observed must be in [0,1]" }
        return observed - expected
    }

    fun blend(taskDelta: Double, matchDelta: Double, config: BlendingConfig): Double =
        config.taskWeight * taskDelta + config.matchWeight * matchDelta
}
