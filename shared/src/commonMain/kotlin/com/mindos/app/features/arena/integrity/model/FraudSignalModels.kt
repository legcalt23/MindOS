@file:Suppress("MagicNumber") // Threshold defaults / range bounds ARE the data; all TBD_BY_SIMULATION.

package com.mindos.app.features.arena.integrity.model

/**
 * Phase 05: The integrity signals the offline detectors can raise. Synthetic-only; never tied to a
 * real user. Anti-cheat detail is internal — user-facing copy must never reveal which signal fired.
 */
enum class IntegritySignal {
    SMURF,
    BOOSTING,
    ACCOUNT_SHARING,
    COLLUSION,
    LAG_SWITCH,
}

/**
 * Phase 05: Pre-extracted, pseudonymized features for one session/account, fed to the detectors.
 * Every field is a synthetic, validated scalar — no raw device ids, IPs, or PII.
 */
@Suppress("LongParameterList") // Irreducible feature record consumed by the detectors.
data class IntegritySignalFeatures(
    /** Win rate during the early-calibration window, in [0,1]. */
    val earlyWinRate: Double = 0.0,
    /** Volatility (std-dev) of mu during early sessions, in rating units (>= 0). */
    val muVolatility: Double = 0.0,
    /** Implied travel speed between consecutive sessions, km/h (>= 0). */
    val geoVelocityKmh: Double = 0.0,
    /** Mismatch between expected and observed input cadence, in [0,1]. */
    val inputCadenceMismatch: Double = 0.0,
    val deviceIdChanged: Boolean = false,
    val deviceFingerprintChanged: Boolean = false,
    /** Shift in input pattern fingerprint, in [0,1]. */
    val inputPatternShift: Double = 0.0,
    /** Fraction of recent matches against the same opponents, in [0,1]. */
    val repeatedOpponentRatio: Double = 0.0,
    val coordinatedQueueTiming: Boolean = false,
    val jitterSpikeDuringLoss: Boolean = false,
    /** Number of sessions played so far (used to scope "early" smurf detection). */
    val earlySessionCount: Int = 0,
) {
    init {
        require(earlyWinRate in 0.0..1.0) { "earlyWinRate must be in [0,1]" }
        require(muVolatility >= 0.0 && !muVolatility.isNaN()) { "muVolatility must be >= 0" }
        require(geoVelocityKmh >= 0.0 && !geoVelocityKmh.isNaN()) { "geoVelocityKmh must be >= 0" }
        require(inputCadenceMismatch in 0.0..1.0) { "inputCadenceMismatch must be in [0,1]" }
        require(inputPatternShift in 0.0..1.0) { "inputPatternShift must be in [0,1]" }
        require(repeatedOpponentRatio in 0.0..1.0) { "repeatedOpponentRatio must be in [0,1]" }
        require(earlySessionCount >= 0) { "earlySessionCount must be >= 0" }
    }
}

/**
 * Phase 05: Which signals fired for a session. [smurf] does not block integrity (it drives sigma
 * recalibration); the others feed the integrity gate.
 */
data class IntegritySignals(
    val smurf: Boolean = false,
    val boosting: Boolean = false,
    val accountSharing: Boolean = false,
    val collusion: Boolean = false,
    val lagSwitch: Boolean = false,
) {
    val any: Boolean get() = smurf || boosting || accountSharing || collusion || lagSwitch

    val triggered: Set<IntegritySignal>
        get() = buildSet {
            if (smurf) add(IntegritySignal.SMURF)
            if (boosting) add(IntegritySignal.BOOSTING)
            if (accountSharing) add(IntegritySignal.ACCOUNT_SHARING)
            if (collusion) add(IntegritySignal.COLLUSION)
            if (lagSwitch) add(IntegritySignal.LAG_SWITCH)
        }
}

/**
 * Phase 05: Detector thresholds. Concrete values are TBD_BY_SIMULATION (blocking before live Shadow
 * Ranked); these are documented offline defaults. Named here so detectors carry no magic numbers.
 */
data class FraudSignalConfig(
    val configVersion: String = "FRAUD_CONFIG_V1.0",
    val smurfWinRateMin: Double = 0.85,
    val smurfMuVolatilityMin: Double = 80.0,
    val smurfEarlySessionMax: Int = 10,
    val boostingGeoVelocityMaxKmh: Double = 800.0,
    val boostingInputCadenceMismatchMin: Double = 0.60,
    val accountSharingGeoVelocityMaxKmh: Double = 800.0,
    val accountSharingInputShiftMin: Double = 0.60,
    val collusionRepeatedOpponentMin: Double = 0.50,
) {
    init {
        require(smurfWinRateMin in 0.0..1.0) { "smurfWinRateMin must be in [0,1]" }
        require(smurfMuVolatilityMin >= 0.0) { "smurfMuVolatilityMin must be >= 0" }
        require(smurfEarlySessionMax >= 0) { "smurfEarlySessionMax must be >= 0" }
        require(boostingGeoVelocityMaxKmh > 0.0) { "boostingGeoVelocityMaxKmh must be > 0" }
        require(boostingInputCadenceMismatchMin in 0.0..1.0) { "boostingInputCadenceMismatchMin in [0,1]" }
        require(accountSharingGeoVelocityMaxKmh > 0.0) { "accountSharingGeoVelocityMaxKmh must be > 0" }
        require(accountSharingInputShiftMin in 0.0..1.0) { "accountSharingInputShiftMin in [0,1]" }
        require(collusionRepeatedOpponentMin in 0.0..1.0) { "collusionRepeatedOpponentMin in [0,1]" }
    }

    companion object {
        val DEFAULT: FraudSignalConfig = FraudSignalConfig()
    }
}
