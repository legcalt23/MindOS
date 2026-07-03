@file:Suppress("MaxLineLength") // Detector signatures + threshold expressions are dense.

package com.mindos.app.features.mindboost.emotions.domain.safety

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.metrics.valueIfConfident

/**
 * Inputs to overload detection. Behavioral only — these ARM a safety check, never infer emotion.
 * [rtMedianBaseline]/[rtCvBaseline] are the within-session rolling baselines; null on the first block,
 * so the slowing/variability proxies cannot fire on a guessed constant.
 */
data class OverloadSignals(
    val metrics: EmotionMetricSet,
    val consecutiveErrors: Int,
    val exitAttempts: Int,
    val pauseUses: Int,
    val rtMedianBaseline: Double? = null,
    val rtCvBaseline: Double? = null,
)

interface OverloadDetector {
    fun detect(signals: OverloadSignals, thresholds: OverloadThresholds = OverloadThresholds.DEFAULT): Set<OverloadProxy>
}

/**
 * Threshold-based detector. The proxies only ARM a mid-session check-in; the system never claims to
 * know the user's emotion. Slowing/variability require a baseline (skip the first block).
 */
class DefaultOverloadDetector : OverloadDetector {

    override fun detect(signals: OverloadSignals, thresholds: OverloadThresholds): Set<OverloadProxy> {
        val proxies = mutableSetOf<OverloadProxy>()

        signals.metrics.valueIfConfident(EmotionMetricKey.COMMISSION_RATE)?.let {
            if (it >= thresholds.commissionSpikeRate) proxies.add(OverloadProxy.COMMISSION_SPIKE)
        }
        if (signals.consecutiveErrors >= thresholds.errorStreakLength) {
            proxies.add(OverloadProxy.ERROR_STREAK)
        }
        addSlowingProxy(signals, thresholds, proxies)
        addVariabilityProxy(signals, thresholds, proxies)
        if (signals.exitAttempts >= thresholds.exitAttemptsPerBlock) {
            proxies.add(OverloadProxy.REPEATED_EXIT_ATTEMPTS)
        }
        if (signals.pauseUses >= thresholds.pauseUsesPerBlock) {
            proxies.add(OverloadProxy.REPEATED_PAUSE)
        }
        return proxies
    }

    private fun addSlowingProxy(signals: OverloadSignals, thresholds: OverloadThresholds, into: MutableSet<OverloadProxy>) {
        val baseline = signals.rtMedianBaseline ?: return
        val current = signals.metrics.valueIfConfident(EmotionMetricKey.RT_MEDIAN) ?: return
        if (baseline > 0.0 && current >= baseline * thresholds.sustainedSlowingFactor) {
            into.add(OverloadProxy.SUSTAINED_SLOWING)
        }
    }

    private fun addVariabilityProxy(signals: OverloadSignals, thresholds: OverloadThresholds, into: MutableSet<OverloadProxy>) {
        val baseline = signals.rtCvBaseline ?: return
        val current = signals.metrics.valueIfConfident(EmotionMetricKey.RT_CV) ?: return
        if (baseline > 0.0 && current >= baseline * thresholds.rtVariabilityFactor) {
            into.add(OverloadProxy.EXTREME_RT_VARIABILITY)
        }
    }
}
