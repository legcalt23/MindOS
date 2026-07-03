package com.mindos.app.features.arena.integrity.engine

import com.mindos.app.features.arena.integrity.model.FraudSignalConfig
import com.mindos.app.features.arena.integrity.model.IntegritySignalFeatures
import com.mindos.app.features.arena.integrity.model.IntegritySignals

/**
 * Phase 05: Offline, deterministic fraud-signal detectors. Each is a pure predicate over synthetic
 * features; thresholds come from [FraudSignalConfig]. No clock, randomness, or I/O.
 */

/** Abnormal early win rate AND high mu volatility within the early-calibration window. */
object SmurfDetector {
    fun detect(features: IntegritySignalFeatures, config: FraudSignalConfig): Boolean =
        features.earlySessionCount <= config.smurfEarlySessionMax &&
            features.earlyWinRate >= config.smurfWinRateMin &&
            features.muVolatility >= config.smurfMuVolatilityMin
}

/** Impossible travel, or input-cadence mismatch combined with a device change. */
object BoostingDetector {
    fun detect(features: IntegritySignalFeatures, config: FraudSignalConfig): Boolean =
        features.geoVelocityKmh >= config.boostingGeoVelocityMaxKmh ||
            (features.inputCadenceMismatch >= config.boostingInputCadenceMismatchMin && features.deviceIdChanged)
}

/** Impossible travel, or fingerprint change combined with an input-pattern shift. */
object AccountSharingDetector {
    fun detect(features: IntegritySignalFeatures, config: FraudSignalConfig): Boolean =
        features.geoVelocityKmh >= config.accountSharingGeoVelocityMaxKmh ||
            (features.deviceFingerprintChanged && features.inputPatternShift >= config.accountSharingInputShiftMin)
}

/** Repeated matchups against the same opponents with coordinated queue timing. */
object CollusionDetector {
    fun detect(features: IntegritySignalFeatures, config: FraudSignalConfig): Boolean =
        features.repeatedOpponentRatio >= config.collusionRepeatedOpponentMin &&
            features.coordinatedQueueTiming
}

/** Sudden jitter spike during a losing position. */
object LagSwitchDetector {
    fun detect(features: IntegritySignalFeatures): Boolean = features.jitterSpikeDuringLoss
}

/**
 * Phase 05: Runs every detector and aggregates the result into [IntegritySignals].
 */
class FraudSignalEvaluator(
    private val config: FraudSignalConfig = FraudSignalConfig.DEFAULT,
) {
    fun evaluate(features: IntegritySignalFeatures): IntegritySignals = IntegritySignals(
        smurf = SmurfDetector.detect(features, config),
        boosting = BoostingDetector.detect(features, config),
        accountSharing = AccountSharingDetector.detect(features, config),
        collusion = CollusionDetector.detect(features, config),
        lagSwitch = LagSwitchDetector.detect(features),
    )
}
