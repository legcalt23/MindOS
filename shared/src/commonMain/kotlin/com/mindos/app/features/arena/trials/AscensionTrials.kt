@file:Suppress("MagicNumber") // Tier thresholds and cadence durations are documented simulation constants.

package com.mindos.app.features.arena.trials

import com.mindos.app.features.arena.config.IntegrityStateEnum

/** Phase 10 (SIMULATION ONLY): Competitive tiers, lowest to highest. */
enum class Tier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND,
    ELITE;

    fun next(): Tier? = entries.getOrNull(ordinal + 1)
}

/** Phase 10: Per-tier promotion thresholds. */
data class TrialTierConfig(
    val thresholds: Map<Tier, Double> = mapOf(
        Tier.BRONZE to 0.0,
        Tier.SILVER to 1400.0,
        Tier.GOLD to 1600.0,
        Tier.PLATINUM to 1800.0,
        Tier.DIAMOND to 2000.0,
        Tier.ELITE to 2200.0,
    ),
    val maxSigma: Double = 100.0,
) {
    fun thresholdFor(tier: Tier): Double = thresholds.getValue(tier)

    companion object {
        val DEFAULT: TrialTierConfig = TrialTierConfig()
    }
}

/** Phase 10: Synthetic candidate for an ascension trial. */
data class AscensionCandidate(
    val currentTier: Tier,
    val globalMu: Double,
    val globalSigma: Double,
    val blockFloorsMet: Boolean,
    val integrityState: IntegrityStateEnum,
    val sessionValid: Boolean,
    val comparable: Boolean,
    val replayValid: Boolean,
)

/** Phase 10: Reasons a promotion was denied. */
enum class AscensionDenialReason {
    ALREADY_TOP_TIER,
    MU_BELOW_THRESHOLD,
    SIGMA_TOO_HIGH,
    BLOCK_FLOOR_NOT_MET,
    INTEGRITY_NOT_VERIFIED,
    SESSION_INVALID,
    NOT_COMPARABLE,
    REPLAY_INVALID,
}

/** Phase 10: Result of an ascension trial. */
sealed interface AscensionResult {
    data class Promoted(val toTier: Tier) : AscensionResult
    data class Denied(val reasons: List<AscensionDenialReason>) : AscensionResult
}

/**
 * Phase 10 (SIMULATION ONLY): Ascension trials algorithm (ascension_trials_algorithm.md). Promotion to
 * the next tier requires ALL of: global mu above the tier threshold, sigma below max, all block floors
 * met, integrity VERIFIED, a valid+comparable session, and a valid replay. Pure offline logic.
 */
class AscensionTrialsAlgorithm(
    private val config: TrialTierConfig = TrialTierConfig.DEFAULT,
) {
    fun evaluate(candidate: AscensionCandidate): AscensionResult {
        val next = candidate.currentTier.next()
            ?: return AscensionResult.Denied(listOf(AscensionDenialReason.ALREADY_TOP_TIER))
        val reasons = collectDenials(candidate, next)
        return if (reasons.isEmpty()) AscensionResult.Promoted(next) else AscensionResult.Denied(reasons)
    }

    private fun collectDenials(candidate: AscensionCandidate, next: Tier): List<AscensionDenialReason> = buildList {
        if (candidate.globalMu <= config.thresholdFor(next)) add(AscensionDenialReason.MU_BELOW_THRESHOLD)
        if (candidate.globalSigma >= config.maxSigma) add(AscensionDenialReason.SIGMA_TOO_HIGH)
        if (!candidate.blockFloorsMet) add(AscensionDenialReason.BLOCK_FLOOR_NOT_MET)
        if (candidate.integrityState != IntegrityStateEnum.VERIFIED) add(AscensionDenialReason.INTEGRITY_NOT_VERIFIED)
        if (!candidate.sessionValid) add(AscensionDenialReason.SESSION_INVALID)
        if (!candidate.comparable) add(AscensionDenialReason.NOT_COMPARABLE)
        if (!candidate.replayValid) add(AscensionDenialReason.REPLAY_INVALID)
    }
}

/** Phase 10: Trial cadence cooldowns per tier transition (trial_cadence.md). */
data class TrialCadenceConfig(
    val bronzeToSilverMs: Long = 86_400_000L,
    val silverToGoldMs: Long = 172_800_000L,
    val goldToPlatinumMs: Long = 259_200_000L,
    val platinumToDiamondMs: Long = 604_800_000L,
    val diamondToEliteMs: Long = 7_776_000_000L,
) {
    companion object {
        val DEFAULT: TrialCadenceConfig = TrialCadenceConfig()
    }
}

/** Phase 10 (SIMULATION ONLY): Enforces the trial cadence (the caller supplies `nowMs`; no clock). */
object TrialCadence {
    fun cadenceMsFor(from: Tier, config: TrialCadenceConfig): Long = when (from) {
        Tier.BRONZE -> config.bronzeToSilverMs
        Tier.SILVER -> config.silverToGoldMs
        Tier.GOLD -> config.goldToPlatinumMs
        Tier.PLATINUM -> config.platinumToDiamondMs
        Tier.DIAMOND -> config.diamondToEliteMs
        Tier.ELITE -> Long.MAX_VALUE
    }

    fun canAttempt(from: Tier, lastAttemptMs: Long, nowMs: Long, config: TrialCadenceConfig): Boolean =
        nowMs - lastAttemptMs >= cadenceMsFor(from, config)
}
