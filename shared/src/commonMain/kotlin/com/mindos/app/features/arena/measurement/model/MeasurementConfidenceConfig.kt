@file:Suppress("MagicNumber") // Threshold defaults ARE the tuning data; all TBD_BY_SIMULATION.

package com.mindos.app.features.arena.measurement.model

/**
 * Phase 04: Measurement-confidence thresholds. Concrete values are TBD_BY_SIMULATION (blocking before
 * live Shadow Ranked); these are documented offline defaults. Named here so the builder carries no
 * magic numbers.
 */
data class MeasurementConfidenceConfig(
    val configVersion: String = "MEASUREMENT_CONFIG_V1.0",
    /** McDonald Omega (or equivalent) reliability floor; below this -> INSUFFICIENT. */
    val minReliability: Double = 0.70,
    /** Minimum sample size for a confident measurement. */
    val minSampleSize: Int = 20,
    /** SEM above this (rating units) -> INSUFFICIENT. */
    val semSufficientMax: Double = 120.0,
    /** Default conditional SEM when no per-band value is supplied. */
    val defaultSem: Double = 60.0,
    /** z-score for the confidence interval (1.96 = 95%). */
    val ciZ: Double = 1.96,
    /** Floor for a SUFFICIENT update multiplier so a confident session always contributes. */
    val minSufficientMultiplier: Double = 0.5,
) {
    init {
        require(minReliability in 0.0..1.0) { "minReliability must be in [0,1]" }
        require(minSampleSize >= 0) { "minSampleSize must be >= 0" }
        require(semSufficientMax > 0.0) { "semSufficientMax must be > 0" }
        require(defaultSem >= 0.0) { "defaultSem must be >= 0" }
        require(ciZ > 0.0) { "ciZ must be > 0" }
        require(minSufficientMultiplier in 0.0..1.0) { "minSufficientMultiplier must be in [0,1]" }
    }

    companion object {
        val DEFAULT: MeasurementConfidenceConfig = MeasurementConfidenceConfig()
    }
}
