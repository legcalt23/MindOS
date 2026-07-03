package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.rating.decay.RankDecay
import com.mindos.app.features.arena.rating.decay.SeasonConfig
import com.mindos.app.features.arena.rating.decay.SeasonReset
import com.mindos.app.features.arena.rating.engine.RatingFreeze
import com.mindos.app.features.arena.rating.rollback.RollbackEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 03 remainder / P3R.3: Decay, season reset, rating freeze, rollback. */
class RatingLifecycleTest {

    @Test
    fun decayInflatesSigmaButNeverReducesMu() {
        val player = ArenaTestFixtures.playerRatingState(mu = 1600.0, sigma = 100.0)
        val decayed = RankDecay().applyRankDecay(player, daysSinceLastSession = 10)
        assertEquals(120.0, decayed.globalRatingSigma.value, 1e-9)
        assertEquals(1600.0, decayed.globalRatingMu.value, 1e-9)
    }

    @Test
    fun decayIsCappedAndRejectsNegativeDays() {
        val player = ArenaTestFixtures.playerRatingState(sigma = 349.0)
        assertEquals(350.0, RankDecay().applyRankDecay(player, 100).globalRatingSigma.value, 1e-9)
        assertFailsWith<IllegalArgumentException> { RankDecay().applyRankDecay(player, -1) }
    }

    @Test
    fun seasonResetPullsTowardMeanInflatesSigmaAndPreservesOrdering() {
        val reset = SeasonReset()
        val config = SeasonConfig(seasonId = "SEASON_2")
        val strong = reset.applySeasonReset(ArenaTestFixtures.playerRatingState(mu = 1800.0, sigma = 100.0), config)
        val weak = reset.applySeasonReset(ArenaTestFixtures.playerRatingState(mu = 1200.0, sigma = 100.0), config)
        assertEquals(1650.0, strong.globalRatingMu.value, 1e-9)
        assertEquals(1350.0, weak.globalRatingMu.value, 1e-9)
        assertTrue(strong.globalRatingMu.value > weak.globalRatingMu.value) // ordering preserved
        assertEquals(150.0, strong.globalRatingSigma.value, 1e-9) // 100 * 1.5
        assertEquals("SEASON_2", strong.seasonId)
    }

    @Test
    fun freezeSetsNonVerifiedStateAndIsDetectable() {
        val freeze = RatingFreeze()
        val frozen = freeze.freezeRating(ArenaTestFixtures.playerRatingState(), "integrity review")
        assertEquals(IntegrityStateEnum.REVIEW_REQUIRED, frozen.integrityState)
        assertTrue(freeze.isFrozen(frozen))
        assertTrue(!freeze.isFrozen(ArenaTestFixtures.playerRatingState(integrityState = IntegrityStateEnum.VERIFIED)))
        assertFailsWith<IllegalArgumentException> { freeze.freezeRating(frozen, " ") }
    }

    @Test
    fun rollbackInvertsDeltaWithAuditTrail() {
        val engine = RollbackEngine()
        val result = engine.rollback("SESSION_9", originalDelta = 30.0, syntheticPlayerId = "SYNTH_1", 100L, 200L)
        assertEquals(-30.0, result.invertedDelta, 1e-9)
        assertEquals("INVERT_UPDATE", result.reference.reasonCode)
        assertTrue(result.reference.rollbackId.contains("SESSION_9"))

        val player = ArenaTestFixtures.playerRatingState(mu = 1530.0)
        val restored = engine.applyRollback(player, result.invertedDelta)
        assertEquals(1500.0, restored.globalRatingMu.value, 1e-9)
    }
}
