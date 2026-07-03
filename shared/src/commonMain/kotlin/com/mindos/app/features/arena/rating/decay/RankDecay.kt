@file:Suppress("MagicNumber") // Decay-per-day and max-sigma defaults are documented tuning constants.

package com.mindos.app.features.arena.rating.decay

import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.rating.model.Sigma

/** Phase 03: Rank-decay thresholds. */
data class RankDecayConfig(
    val configVersion: String = "DECAY_CONFIG_V1.0",
    /** Sigma increase per inactive day (rating units). */
    val sigmaIncreasePerDay: Double = 2.0,
    val maxSigma: Double = 350.0,
) {
    init {
        require(sigmaIncreasePerDay >= 0.0) { "sigmaIncreasePerDay must be >= 0" }
        require(maxSigma > 0.0) { "maxSigma must be > 0" }
    }

    companion object {
        val DEFAULT: RankDecayConfig = RankDecayConfig()
    }
}

/**
 * Phase 03: Rank decay (rank_decay.md). Inactivity gradually increases sigma (uncertainty) but NEVER
 * punitively reduces mu. Pure — returns a new [PlayerRatingState].
 */
class RankDecay(
    private val config: RankDecayConfig = RankDecayConfig.DEFAULT,
) {
    fun applyRankDecay(player: PlayerRatingState, daysSinceLastSession: Int): PlayerRatingState {
        require(daysSinceLastSession >= 0) { "daysSinceLastSession must be >= 0" }
        val inflated = minOf(
            config.maxSigma,
            player.globalRatingSigma.value + config.sigmaIncreasePerDay * daysSinceLastSession,
        )
        return player.copy(globalRatingSigma = Sigma(inflated))
    }
}
