@file:Suppress("MagicNumber") // Golden inputs/outputs are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden-value parity for the copied robust statistics (median over mean, MAD over SD), so the
 * emotions copy stays faithful to the focus original it mirrors.
 */
class EmotionRtStatisticsParityTest {

    private val eps = 1e-9

    @Test
    fun median_handles_odd_even_and_empty() {
        assertEquals(2.0, EmotionRtStatistics.median(listOf(3.0, 1.0, 2.0)), eps)
        assertEquals(2.5, EmotionRtStatistics.median(listOf(1.0, 2.0, 3.0, 4.0)), eps)
        assertEquals(0.0, EmotionRtStatistics.median(emptyList()), eps)
    }

    @Test
    fun standard_deviation_and_cv() {
        assertEquals(1.0, EmotionRtStatistics.standardDeviation(listOf(1.0, 2.0, 3.0)), eps)
        assertEquals(0.5, EmotionRtStatistics.coefficientOfVariation(listOf(1.0, 2.0, 3.0)), eps)
        assertEquals(0.0, EmotionRtStatistics.coefficientOfVariation(listOf(2.0, 2.0, 2.0)), eps)
    }

    @Test
    fun scaled_mad() {
        assertEquals(1.4826, EmotionRtStatistics.scaledMad(listOf(1.0, 2.0, 3.0, 4.0, 5.0)), 1e-4)
        assertEquals(0.0, EmotionRtStatistics.scaledMad(emptyList()), eps)
    }

    @Test
    fun reject_outliers_drops_extreme_keeps_small_n_and_handles_zero_mad() {
        val filtered = EmotionRtStatistics.rejectOutliers(listOf(10.0, 11.0, 12.0, 13.0, 100.0))
        assertEquals(4, filtered.size)
        assertTrue(100.0 !in filtered)
        // n < minForFiltering → unchanged.
        assertEquals(3, EmotionRtStatistics.rejectOutliers(listOf(1.0, 100.0, 2.0)).size)
        // MAD == 0 (majority identical) → cannot scale, returns input unchanged.
        assertEquals(5, EmotionRtStatistics.rejectOutliers(listOf(10.0, 10.0, 10.0, 10.0, 100.0)).size)
    }
}
