@file:Suppress("MagicNumber") // Threshold defaults ARE the tuning data; all are TBD_BY_SIMULATION.

package com.mindos.app.features.arena.comparability.model

/**
 * Phase 04: Comparability thresholds. Per the plan, concrete values are TBD_BY_SIMULATION and
 * BLOCKING before live Shadow Ranked — these are documented, conservative offline defaults derived
 * from the spec examples (latency_threshold_model.md: <50ms COMPARABLE, 50-100ms PARTIALLY, >100ms
 * NOT). They are named here so the classifier itself carries no magic numbers.
 */
data class ComparabilityConfig(
    val configVersion: String = "COMPARABILITY_CONFIG_V1.0",
    val latencyComparableMaxMs: Long = 50,
    val latencyPartialMaxMs: Long = 100,
    val jitterComparableMaxMs: Long = 20,
    val jitterPartialMaxMs: Long = 50,
    val networkComparableMax: Double = 0.30,
    val networkPartialMax: Double = 0.60,
    val partialMultiplier: Double = 0.5,
) {
    init {
        require(latencyComparableMaxMs in 0..latencyPartialMaxMs) { "latency thresholds out of order" }
        require(jitterComparableMaxMs in 0..jitterPartialMaxMs) { "jitter thresholds out of order" }
        require(networkComparableMax in 0.0..networkPartialMax) { "network thresholds out of order" }
        require(networkPartialMax <= 1.0) { "networkPartialMax must be <= 1.0" }
        require(partialMultiplier in 0.0..1.0) { "partialMultiplier must be in [0,1]" }
    }

    companion object {
        val DEFAULT: ComparabilityConfig = ComparabilityConfig()
    }
}
