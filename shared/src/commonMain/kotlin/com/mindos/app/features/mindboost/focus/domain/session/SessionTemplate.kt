package com.mindos.app.features.mindboost.focus.domain.session

/**
 * Session length templates from the functional design (07_session_periodization).
 * Only the durations needed by the E1 vertical slice are modelled here; the full
 * phase blueprints (warmup/core/peak/transfer/cooldown) land in the
 * periodization phase.
 */
@Suppress("MagicNumber") // The per-template durations ARE the data, named by the parameters.
enum class SessionTemplate(
    val displayName: String,
    val totalMinutes: Int,
    val warmupMs: Long,
    val coreMs: Long,
    val cooldownMs: Long,
) {
    MAINTENANCE_5("Mantenimiento", 5, warmupMs = 30_000, coreMs = 210_000, cooldownMs = 30_000),
    STANDARD_10("Estándar", 10, warmupMs = 60_000, coreMs = 360_000, cooldownMs = 90_000),
    DEVELOPMENT_20("Desarrollo", 20, warmupMs = 120_000, coreMs = 600_000, cooldownMs = 120_000),
    ELITE_30("Élite", 30, warmupMs = 180_000, coreMs = 840_000, cooldownMs = 120_000),
    ;

    /**
     * Approximate number of E1 trials that fit the core duration, given an average
     * per-trial budget (inter-stimulus interval + response window). Bounded so a
     * 5-minute session is still meaningful and a 30-minute one stays manageable.
     */
    fun estimatedTrialCount(averageTrialMs: Long = 3_800): Int =
        (coreMs / averageTrialMs).toInt().coerceIn(MIN_TRIALS, MAX_TRIALS)

    companion object {
        private const val MIN_TRIALS = 24
        private const val MAX_TRIALS = 160

        /** Template whose length is closest to the available [minutes]. */
        fun forMinutes(minutes: Int): SessionTemplate =
            entries.minByOrNull { kotlin.math.abs(it.totalMinutes - minutes) } ?: STANDARD_10
    }
}
