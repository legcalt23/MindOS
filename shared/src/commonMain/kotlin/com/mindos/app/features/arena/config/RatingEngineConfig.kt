@file:Suppress("MagicNumber") // Config defaults ARE the tuning constants; each is named and documented.

package com.mindos.app.features.arena.config

/**
 * Phase 03: Bayesian rating engine configuration (the "BlendingConfig" of the algorithm spec).
 *
 * Every numeric tuning value lives here as a named, versioned default so that the engine math
 * itself stays free of magic numbers. There is intentionally NO premium / subscription field:
 * premium status must never be an input to rating math (17_metric_integrity_hardening.md AC #2).
 */
data class RatingEngineConfig(
    val configVersion: String = "BLEND_CONFIG_V1.0",
    /** Logistic scale for expected task performance (larger = flatter curve). */
    val performanceScale: Double = 400.0,
    /** Damping applied to the sigma-scaled mu step; keeps updates stable and convergent. */
    val muDamping: Double = 0.30,
    /** Fraction by which sigma shrinks per fully-confident valid update. */
    val sigmaDecayRate: Double = 0.05,
    /** Growth factor applied to sigma when smurf suspicion is HIGH (rapid recalibration). */
    val sigmaInflateOnSmurf: Double = 1.05,
    /** Floor for sigma so it is never collapsed prematurely (sigma_update.md). */
    val minSigma: Double = 30.0,
    /** Ceiling for sigma (default-profile uncertainty). */
    val maxSigma: Double = 350.0,
) {
    init {
        require(performanceScale > 0.0 && !performanceScale.isNaN()) { "performanceScale must be > 0" }
        require(muDamping in 0.0..1.0) { "muDamping must be in [0,1]" }
        require(sigmaDecayRate in 0.0..1.0) { "sigmaDecayRate must be in [0,1]" }
        require(sigmaInflateOnSmurf >= 1.0) { "sigmaInflateOnSmurf must be >= 1.0" }
        require(minSigma in 0.0..maxSigma) { "minSigma must be in [0, maxSigma]" }
    }

    companion object {
        val DEFAULT: RatingEngineConfig = RatingEngineConfig()
    }
}
