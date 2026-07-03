@file:Suppress("MagicNumber") // Pause-pattern defaults are documented anti-coaching constants.

package com.mindos.app.features.arena.trials

/** Phase 10: Anti-coaching thresholds. */
data class AntiCoachingConfig(
    val longPauseMs: Long = 30_000L,
    val maxLongPauses: Int = 2,
) {
    init {
        require(longPauseMs > 0) { "longPauseMs must be > 0" }
        require(maxLongPauses >= 0) { "maxLongPauses must be >= 0" }
    }

    companion object {
        val DEFAULT: AntiCoachingConfig = AntiCoachingConfig()
    }
}

/**
 * Phase 10 (SIMULATION ONLY): Anti-coaching controls (anti_coaching_controls.md). No repeated trial
 * item family, and suspicious pause patterns flag the session for INTEGRITY_HOLD (the caller applies
 * the hold). Trial structure secrecy is a documentation/policy concern, not exposed here. Pure logic.
 */
class AntiCoachingControls(
    private val config: AntiCoachingConfig = AntiCoachingConfig.DEFAULT,
) {
    /** A trial item family may not be reused for the same player (prevents memorization). */
    fun isItemFamilyRepeated(family: String, exposedHistory: Set<String>): Boolean = family in exposedHistory

    /** Too many long pauses suggests external coaching/lookup -> flag for INTEGRITY_HOLD. */
    fun pausePatternSuspicious(pauseDurationsMs: List<Long>): Boolean =
        pauseDurationsMs.count { it >= config.longPauseMs } > config.maxLongPauses
}
