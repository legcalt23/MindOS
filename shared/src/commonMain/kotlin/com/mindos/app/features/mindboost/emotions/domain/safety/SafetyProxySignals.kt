@file:Suppress("MagicNumber") // Threshold defaults ARE the tuning data; named below for clarity.

package com.mindos.app.features.mindboost.emotions.domain.safety

/**
 * Behavioral overload proxies. These ARM a safety check (a mid-session check-in) — they NEVER infer
 * an emotional state. The system observes performance and safety, not the user's inner life.
 */
enum class OverloadProxy {
    COMMISSION_SPIKE,
    ERROR_STREAK,
    SUSTAINED_SLOWING,
    EXTREME_RT_VARIABILITY,
    REPEATED_EXIT_ATTEMPTS,
    REPEATED_PAUSE,
    POST_ERROR_ACCURACY_DROP,
    POST_ERROR_IMPULSIVITY,
}

/**
 * Thresholds that decide when each [OverloadProxy] fires. Defaults are tuning constants. The
 * slowing/variability proxies compare against a within-session rolling baseline and do NOT fire on
 * the first block (no baseline yet) — that gating lives in the detector, not here.
 */
data class OverloadThresholds(
    val commissionSpikeRate: Double = 0.30,
    val errorStreakLength: Int = 3,
    val sustainedSlowingFactor: Double = 1.40,
    val sustainedSlowingBlocks: Int = 2,
    val rtVariabilityFactor: Double = 1.60,
    val exitAttemptsPerBlock: Int = 2,
    val pauseUsesPerBlock: Int = 3,
    val postErrorAccuracyDropFactor: Double = 0.60,
    val postErrorImpulsivityCount: Int = 2,
    val postErrorImpulsivityRtMs: Long = 250L,
) {
    companion object {
        val DEFAULT = OverloadThresholds()
    }
}
