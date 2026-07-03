package com.mindos.app.features.arena

import com.mindos.app.features.arena.rating.engine.BadgeType
import com.mindos.app.features.arena.rating.engine.NonCompensatoryRank
import com.mindos.app.features.arena.rating.engine.SpecialistBadgeAssigner
import com.mindos.app.features.arena.rating.model.BlockRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 03 remainder / P3R.2: Non-compensatory rank + specialist badges. */
class NonCompensatoryRankTest {

    private val rank = NonCompensatoryRank()

    private fun block(id: String, mu: Double, sigma: Double = 50.0) =
        id to BlockRatingState(id, Rating(mu), Sigma(sigma), 0L)

    @Test
    fun balancedBlocksAreNotEliteBlockedAndConfident() {
        val result = rank.calculateGlobalRank(mapOf(block("A", 1500.0), block("B", 1500.0)))
        assertEquals(1500.0, result.globalMu, 1e-6)
        assertTrue(!result.eliteBlocked)
        assertTrue(result.confident)
    }

    @Test
    fun harmonicMeanIsNonCompensatory() {
        // A strong block cannot compensate a weaker one: harmonic < arithmetic mean.
        val result = rank.calculateGlobalRank(mapOf(block("A", 1800.0), block("B", 1300.0)))
        val arithmetic = (1800.0 + 1300.0) / 2
        assertTrue(result.globalMu < arithmetic, "global=${result.globalMu}")
        assertTrue(!result.eliteBlocked)
    }

    @Test
    fun weakBlockBelowFloorBlocksEliteAndAppliesPenalty() {
        val result = rank.calculateGlobalRank(mapOf(block("A", 1800.0), block("Weak", 1100.0)))
        assertTrue(result.eliteBlocked)
        assertEquals("Weak", result.weakestBlockId)
        // Penalty makes global mu strictly below the unpenalized harmonic mean.
        val harmonic = 2 / (1 / 1800.0 + 1 / 1100.0)
        assertTrue(result.globalMu < harmonic)
    }

    @Test
    fun highSigmaBlockMakesRankNotConfident() {
        val result = rank.calculateGlobalRank(mapOf(block("A", 1500.0, sigma = 300.0)))
        assertTrue(!result.confident)
    }

    @Test
    fun emptyBlocksAreRejected() {
        assertFailsWith<IllegalArgumentException> { rank.calculateGlobalRank(emptyMap()) }
    }

    @Test
    fun specialistBadgeAwardedOnlyForExcellentBlocksAndDoesNotElevateRank() {
        val blocks = mapOf(block("Speed", 1750.0), block("Memory", 1400.0))
        val badges = SpecialistBadgeAssigner().assignBadges(blocks)
        assertEquals(listOf("Speed"), badges.map { it.blockId })
        assertEquals(BadgeType.SPECIALIST, badges.first().type)
        // The badge has no effect on the non-compensatory rank (rank still gated by the weak block).
        assertTrue(rank.calculateGlobalRank(blocks).globalMu < 1750.0)
    }
}
