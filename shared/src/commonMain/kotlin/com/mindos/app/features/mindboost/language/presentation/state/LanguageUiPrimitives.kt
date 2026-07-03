package com.mindos.app.features.mindboost.language.presentation.state

/**
 * Shared presentation primitives for the language feature UI models (v6 §11). Pure view data:
 * no Compose, no platform — so the *contracts that make the UI correct* are testable in
 * `commonMain` and the actual screens just render them.
 */

/** A claim-safe reliability band (v6 §11.3). Thresholds mirror §5.3 reliability tiers. */
enum class ReliabilityBand(val label: String) {
    LOW("baja"),
    MEDIUM("media"),
    MEDIUM_HIGH("media-alta"),
    HIGH("alta"),
    ;

    companion object {
        const val MEDIUM_THRESHOLD = 0.5
        const val MEDIUM_HIGH_THRESHOLD = 0.7
        const val HIGH_THRESHOLD = 0.8

        fun fromReliability(reliability: Double): ReliabilityBand = when {
            reliability < MEDIUM_THRESHOLD -> LOW
            reliability < MEDIUM_HIGH_THRESHOLD -> MEDIUM
            reliability < HIGH_THRESHOLD -> MEDIUM_HIGH
            else -> HIGH
        }
    }
}

/**
 * A claim-safe *probable range* (never an exact score, §11.6). Only ever produced when there is
 * enough real evidence; otherwise the UI shows the state + reliability, not a fabricated number.
 */
data class ScoreRange(val low: Int, val high: Int) {
    init {
        require(low <= high) { "ScoreRange low ($low) must be <= high ($high)." }
    }
}
