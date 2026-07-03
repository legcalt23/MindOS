package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.trials.AntiCoachingControls
import com.mindos.app.features.arena.trials.AscensionCandidate
import com.mindos.app.features.arena.trials.AscensionDenialReason
import com.mindos.app.features.arena.trials.AscensionResult
import com.mindos.app.features.arena.trials.AscensionTrialsAlgorithm
import com.mindos.app.features.arena.trials.Tier
import com.mindos.app.features.arena.trials.TrialCadence
import com.mindos.app.features.arena.trials.TrialCadenceConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 10 / P10.3 (SIMULATION ONLY): Ascension trials algorithm, cadence, anti-coaching. */
class AscensionTrialsTest {

    private val algorithm = AscensionTrialsAlgorithm()

    private fun candidate(
        tier: Tier = Tier.SILVER,
        mu: Double = 1700.0,
        sigma: Double = 60.0,
        blockFloorsMet: Boolean = true,
        integrity: IntegrityStateEnum = IntegrityStateEnum.VERIFIED,
        sessionValid: Boolean = true,
        comparable: Boolean = true,
        replayValid: Boolean = true,
    ) = AscensionCandidate(tier, mu, sigma, blockFloorsMet, integrity, sessionValid, comparable, replayValid)

    @Test
    fun cleanQualifiedCandidateIsPromoted() {
        val result = algorithm.evaluate(candidate(tier = Tier.SILVER, mu = 1700.0))
        assertEquals(Tier.GOLD, (result as AscensionResult.Promoted).toTier)
    }

    @Test
    fun eachUnmetRequirementIsReportedAsADenial() {
        assertDenied(candidate(mu = 1000.0), AscensionDenialReason.MU_BELOW_THRESHOLD)
        assertDenied(candidate(sigma = 200.0), AscensionDenialReason.SIGMA_TOO_HIGH)
        assertDenied(candidate(blockFloorsMet = false), AscensionDenialReason.BLOCK_FLOOR_NOT_MET)
        assertDenied(candidate(integrity = IntegrityStateEnum.HOLD), AscensionDenialReason.INTEGRITY_NOT_VERIFIED)
        assertDenied(candidate(sessionValid = false), AscensionDenialReason.SESSION_INVALID)
        assertDenied(candidate(comparable = false), AscensionDenialReason.NOT_COMPARABLE)
        assertDenied(candidate(replayValid = false), AscensionDenialReason.REPLAY_INVALID)
    }

    @Test
    fun multipleFailuresAccumulate() {
        val result = algorithm.evaluate(candidate(mu = 1000.0, integrity = IntegrityStateEnum.HOLD, replayValid = false))
        val reasons = (result as AscensionResult.Denied).reasons
        assertTrue(reasons.contains(AscensionDenialReason.MU_BELOW_THRESHOLD))
        assertTrue(reasons.contains(AscensionDenialReason.INTEGRITY_NOT_VERIFIED))
        assertTrue(reasons.contains(AscensionDenialReason.REPLAY_INVALID))
    }

    @Test
    fun topTierCannotAscendFurther() {
        val result = algorithm.evaluate(candidate(tier = Tier.ELITE, mu = 3000.0))
        assertEquals(listOf(AscensionDenialReason.ALREADY_TOP_TIER), (result as AscensionResult.Denied).reasons)
    }

    @Test
    fun cadenceEnforcesPerTierCooldown() {
        val config = TrialCadenceConfig.DEFAULT
        // Silver->Gold cadence is 48h; cannot retry after 24h, can after 48h.
        assertTrue(!TrialCadence.canAttempt(Tier.SILVER, lastAttemptMs = 0, nowMs = 86_400_000L, config))
        assertTrue(TrialCadence.canAttempt(Tier.SILVER, lastAttemptMs = 0, nowMs = 172_800_000L, config))
    }

    @Test
    fun antiCoachingFlagsRepeatedFamiliesAndSuspiciousPauses() {
        val controls = AntiCoachingControls()
        assertTrue(controls.isItemFamilyRepeated("FAMILY_A", setOf("FAMILY_A", "FAMILY_B")))
        assertTrue(!controls.isItemFamilyRepeated("FAMILY_C", setOf("FAMILY_A")))
        assertTrue(controls.pausePatternSuspicious(listOf(40_000L, 50_000L, 60_000L)))
        assertTrue(!controls.pausePatternSuspicious(listOf(1_000L, 40_000L)))
    }

    private fun assertDenied(candidate: AscensionCandidate, expected: AscensionDenialReason) {
        val result = algorithm.evaluate(candidate)
        assertTrue(result is AscensionResult.Denied && result.reasons.contains(expected), "missing $expected")
    }
}
