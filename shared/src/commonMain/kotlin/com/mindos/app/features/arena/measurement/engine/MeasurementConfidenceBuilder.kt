@file:Suppress("MagicNumber") // 1.0/0.0 are the multiplier endpoints intrinsic to the formula.

package com.mindos.app.features.arena.measurement.engine

import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.measurement.model.MeasurementConfidenceConfig
import com.mindos.app.features.arena.rating.model.ConfidenceInterval
import com.mindos.app.features.arena.rating.model.MeasurementConfidenceDecision
import com.mindos.app.features.arena.rating.model.SEM

/**
 * Phase 04: Offline, deterministic builder of [MeasurementConfidenceDecision] from SEM, reliability,
 * sample size, and Evidence Ledger status (measurement_confidence_decision_builder.md).
 *
 * The resulting INSUFFICIENT state is hard-gated by the Bayesian rating engine (it rejects evidence
 * whose measurementConfidence is INSUFFICIENT), realizing insufficient_confidence_gate.md. Pure: no
 * clock, randomness, or I/O; thresholds come from [MeasurementConfidenceConfig].
 */
class MeasurementConfidenceBuilder(
    private val config: MeasurementConfidenceConfig = MeasurementConfidenceConfig.DEFAULT,
) {

    /** Reliability (e.g. McDonald Omega) meets the floor. */
    fun checkReliability(reliability: Double): Boolean = reliability >= config.minReliability

    /** Conditional SEM by (motor, rating band); falls back to the configured default. */
    fun computeConditionalSEM(
        ratingBand: String,
        motorId: String,
        semByMotorBand: Map<String, Double> = emptyMap(),
    ): Double = semByMotorBand[conditionalKey(motorId, ratingBand)] ?: config.defaultSem

    private fun conditionalKey(motorId: String, ratingBand: String): String = "$motorId:$ratingBand"

    /** 95% (configurable) confidence interval around the rating estimate. */
    fun computeCI(mu: Double, sigma: Double): ConfidenceInterval =
        ConfidenceInterval(mu - config.ciZ * sigma, mu + config.ciZ * sigma)

    @Suppress("LongParameterList") // Irreducible builder inputs (SEM, reliability, sample, ledger, mu, sigma).
    fun buildMeasurementConfidence(
        sem: Double,
        reliability: Double,
        sampleSize: Int,
        ledgerApprovedCurrent: Boolean,
        mu: Double,
        sigma: Double,
    ): MeasurementConfidenceDecision {
        val insufficiencyReason = firstInsufficiencyReason(sem, reliability, sampleSize, ledgerApprovedCurrent)
        val sufficient = insufficiencyReason == null
        return MeasurementConfidenceDecision(
            state = if (sufficient) ConfidenceStateEnum.SUFFICIENT else ConfidenceStateEnum.INSUFFICIENT,
            updateMultiplier = if (sufficient) sufficientMultiplier(sem) else 0.0,
            reasonCode = insufficiencyReason ?: "SUFFICIENT",
            sourceEngine = SOURCE_ENGINE,
            sem = SEM(sem),
            confidenceInterval = computeCI(mu, sigma),
        )
    }

    private fun firstInsufficiencyReason(
        sem: Double,
        reliability: Double,
        sampleSize: Int,
        ledgerApprovedCurrent: Boolean,
    ): String? = when {
        !ledgerApprovedCurrent -> "LEDGER_NOT_APPROVED_CURRENT"
        reliability < config.minReliability -> "RELIABILITY_BELOW_MIN"
        sampleSize < config.minSampleSize -> "SAMPLE_SIZE_BELOW_MIN"
        sem > config.semSufficientMax -> "SEM_ABOVE_MAX"
        else -> null
    }

    private fun sufficientMultiplier(sem: Double): Double =
        (1.0 - sem / config.semSufficientMax).coerceIn(config.minSufficientMultiplier, 1.0)

    companion object {
        private const val SOURCE_ENGINE = "MeasurementConfidenceBuilder"
    }
}
