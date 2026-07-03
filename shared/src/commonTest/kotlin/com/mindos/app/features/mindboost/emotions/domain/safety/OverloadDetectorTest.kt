@file:Suppress("MagicNumber", "MaxLineLength") // Threshold fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.safety

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverloadDetectorTest {

    private val detector = DefaultOverloadDetector()

    private fun confident(key: EmotionMetricKey, value: Double) =
        mapOf(key to EmotionMetricValue(key, value, 20, true, EmotionMetricUnit.RATIO))

    @Test
    fun commission_spike_fires_at_threshold() {
        val proxies = detector.detect(OverloadSignals(confident(EmotionMetricKey.COMMISSION_RATE, 0.30), 0, 0, 0))
        assertTrue(OverloadProxy.COMMISSION_SPIKE in proxies)
    }

    @Test
    fun error_streak_fires_at_three() {
        val proxies = detector.detect(OverloadSignals(emptyMap(), consecutiveErrors = 3, exitAttempts = 0, pauseUses = 0))
        assertTrue(OverloadProxy.ERROR_STREAK in proxies)
    }

    @Test
    fun slowing_never_fires_without_a_baseline() {
        val signals = OverloadSignals(confident(EmotionMetricKey.RT_MEDIAN, 5000.0), 0, 0, 0, rtMedianBaseline = null)
        assertTrue(OverloadProxy.SUSTAINED_SLOWING !in detector.detect(signals))
    }

    @Test
    fun slowing_fires_with_baseline_above_factor() {
        val signals = OverloadSignals(confident(EmotionMetricKey.RT_MEDIAN, 700.0), 0, 0, 0, rtMedianBaseline = 500.0)
        assertTrue(OverloadProxy.SUSTAINED_SLOWING in detector.detect(signals))
    }

    @Test
    fun exit_and_pause_proxies_fire() {
        val proxies = detector.detect(OverloadSignals(emptyMap(), 0, exitAttempts = 2, pauseUses = 3))
        assertTrue(OverloadProxy.REPEATED_EXIT_ATTEMPTS in proxies)
        assertTrue(OverloadProxy.REPEATED_PAUSE in proxies)
    }

    @Test
    fun nothing_fires_on_calm_first_block() {
        val signals = OverloadSignals(confident(EmotionMetricKey.COMMISSION_RATE, 0.05), 0, 0, 0)
        assertEquals(emptySet(), detector.detect(signals))
    }
}
