@file:Suppress("MagicNumber") // Population mean / pull / inflation defaults are documented constants.

package com.mindos.app.features.arena.rating.decay

import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma

/** Phase 03: Season-reset configuration. */
data class SeasonConfig(
    val seasonId: String,
    val populationMean: Double = 1500.0,
    /** How far mu is pulled toward the population mean (0..1). < 1 preserves relative ordering. */
    val muPullFactor: Double = 0.5,
    val sigmaInflationFactor: Double = 1.5,
    val maxSigma: Double = 350.0,
) {
    init {
        require(seasonId.isNotBlank()) { "seasonId must not be blank" }
        require(muPullFactor in 0.0..1.0) { "muPullFactor must be in [0,1]" }
        require(sigmaInflationFactor >= 1.0) { "sigmaInflationFactor must be >= 1" }
        require(maxSigma > 0.0) { "maxSigma must be > 0" }
    }
}

/**
 * Phase 03: Season reset (season_reset.md). A soft reset: mu is pulled toward the population mean and
 * sigma is increased. Because the pull is a fraction < 1, relative ordering is preserved while the
 * distribution is compressed. Pure — returns a new [PlayerRatingState].
 */
class SeasonReset {
    fun applySeasonReset(player: PlayerRatingState, seasonConfig: SeasonConfig): PlayerRatingState {
        val mu = player.globalRatingMu.value
        val newMu = mu + seasonConfig.muPullFactor * (seasonConfig.populationMean - mu)
        val newSigma = minOf(
            seasonConfig.maxSigma,
            player.globalRatingSigma.value * seasonConfig.sigmaInflationFactor,
        )
        return player.copy(
            globalRatingMu = Rating(newMu),
            globalRatingSigma = Sigma(newSigma),
            seasonId = seasonConfig.seasonId,
        )
    }
}
