@file:Suppress("MagicNumber") // Floor / penalty / confidence defaults are documented tuning constants.

package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.rating.model.BlockRatingState

/**
 * Phase 03: Result of the non-compensatory global rank. [eliteBlocked] is true when any block is
 * below the floor (a weak block cannot be compensated by a strong one).
 */
data class GlobalRankResult(
    val globalMu: Double,
    val eliteBlocked: Boolean,
    val confident: Boolean,
    val weakestBlockId: String,
)

/** Phase 03: Non-compensatory rank thresholds. */
data class NonCompensatoryRankConfig(
    val configVersion: String = "RANK_CONFIG_V1.0",
    /** Minimum acceptable block mu; any block below this blocks Elite. */
    val floor: Double = 1200.0,
    /** Penalty multiplier applied to global mu when a block is below the floor. */
    val floorPenaltyFactor: Double = 0.95,
    /** Block sigma above this means the rank is not confident. */
    val maxConfidentSigma: Double = 120.0,
) {
    init {
        require(floor >= 0.0) { "floor must be >= 0" }
        require(floorPenaltyFactor in 0.0..1.0) { "floorPenaltyFactor must be in [0,1]" }
        require(maxConfidentSigma > 0.0) { "maxConfidentSigma must be > 0" }
    }

    companion object {
        val DEFAULT: NonCompensatoryRankConfig = NonCompensatoryRankConfig()
    }
}

/**
 * Phase 03: Non-compensatory rank (non_compensatory_rank_formula.md).
 *
 * GlobalMu = HarmonicMean(block mus) × FloorPenalty(min block) × ConfidenceGate. The harmonic mean is
 * inherently non-compensatory — a single weak block drags the global rank down more than a strong
 * block lifts it. No premium / self-report inputs.
 */
class NonCompensatoryRank(
    private val config: NonCompensatoryRankConfig = NonCompensatoryRankConfig.DEFAULT,
) {
    fun calculateGlobalRank(blocks: Map<String, BlockRatingState>): GlobalRankResult {
        require(blocks.isNotEmpty()) { "blocks must not be empty" }
        val harmonicMean = blocks.size / blocks.values.sumOf { 1.0 / it.ratingMu.value }
        val weakest = requireNotNull(blocks.entries.minByOrNull { it.value.ratingMu.value })
        val eliteBlocked = weakest.value.ratingMu.value < config.floor
        val penalty = if (eliteBlocked) config.floorPenaltyFactor else 1.0
        val confident = blocks.values.all { it.ratingSigma.value <= config.maxConfidentSigma }
        return GlobalRankResult(
            globalMu = harmonicMean * penalty,
            eliteBlocked = eliteBlocked,
            confident = confident,
            weakestBlockId = weakest.key,
        )
    }
}
