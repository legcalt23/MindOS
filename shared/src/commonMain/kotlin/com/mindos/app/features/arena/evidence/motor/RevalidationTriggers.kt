@file:Suppress("MagicNumber") // The 5% mu-drift default threshold is a documented named default.

package com.mindos.app.features.arena.evidence.motor

/** Phase 06: Conditions that force a motor's evidence ledger to be revalidated. */
enum class RevalidationTrigger {
    SEASONAL_MU_DRIFT,
    NEW_DEVICE_CLASS,
    DIF_ANOMALY,
    ITEM_POOL_CONTAMINATION,
    SOLVER_BREAKTHROUGH,
}

/** Phase 06: Synthetic signals checked against revalidation triggers (ledger_revalidation_triggers.md). */
data class RevalidationSignals(
    /** Seasonal drift of mu as a fraction (0.06 == 6%). */
    val seasonalMuDriftFraction: Double = 0.0,
    val newDeviceClassIntroduced: Boolean = false,
    val difAnomalyDetected: Boolean = false,
    val itemPoolContaminated: Boolean = false,
    val solverBreakthrough: Boolean = false,
) {
    init {
        require(seasonalMuDriftFraction >= 0.0 && !seasonalMuDriftFraction.isNaN()) {
            "seasonalMuDriftFraction must be >= 0"
        }
    }
}

/** Phase 06: Revalidation thresholds. */
data class RevalidationConfig(
    val configVersion: String = "REVALIDATION_CONFIG_V1.0",
    /** Mu drift above this fraction (5%) triggers revalidation. */
    val muDriftThreshold: Double = 0.05,
) {
    init {
        require(muDriftThreshold in 0.0..1.0) { "muDriftThreshold must be in [0,1]" }
    }

    companion object {
        val DEFAULT: RevalidationConfig = RevalidationConfig()
    }
}

/**
 * Phase 06: Offline, deterministic revalidation evaluator. Pure predicate over synthetic signals.
 */
class RevalidationEvaluator(
    private val config: RevalidationConfig = RevalidationConfig.DEFAULT,
) {
    fun triggered(signals: RevalidationSignals): Set<RevalidationTrigger> = buildSet {
        if (signals.seasonalMuDriftFraction > config.muDriftThreshold) add(RevalidationTrigger.SEASONAL_MU_DRIFT)
        if (signals.newDeviceClassIntroduced) add(RevalidationTrigger.NEW_DEVICE_CLASS)
        if (signals.difAnomalyDetected) add(RevalidationTrigger.DIF_ANOMALY)
        if (signals.itemPoolContaminated) add(RevalidationTrigger.ITEM_POOL_CONTAMINATION)
        if (signals.solverBreakthrough) add(RevalidationTrigger.SOLVER_BREAKTHROUGH)
    }

    fun requiresRevalidation(signals: RevalidationSignals): Boolean = triggered(signals).isNotEmpty()
}
