@file:Suppress("MagicNumber") // Statistical constants (MAD scale, default cutoffs) are the data.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Robust statistics primitives for the emotions metrics engine. A deliberate COPY of focus's
 * `RtStatistics` (which is `internal` to focus.metrics) so emotions stays self-contained and does not
 * depend on another feature's internal API. Pure functions, no platform. Honesty encoded: median over
 * mean, MAD over SD for outlier rejection (central tendency only — extreme events count on raw data).
 */
internal object EmotionRtStatistics {

    /** Scale factor making the MAD a consistent estimator of SD under normality. */
    private const val MAD_SCALE = 1.4826

    /** Median of a list. Returns 0.0 for an empty list (callers gate on n). */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /** Sample standard deviation (n-1). Returns 0.0 for fewer than 2 values. */
    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    /** Coefficient of variation (SD / mean). Returns 0.0 when mean is 0 or n < 2. */
    fun coefficientOfVariation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return if (mean == 0.0) 0.0 else standardDeviation(values) / mean
    }

    /** MAD scaled by 1.4826 (consistent SD estimator under normality). Robust to the RT right tail. */
    fun scaledMad(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val med = median(values)
        val deviations = values.map { abs(it - med) }
        return median(deviations) * MAD_SCALE
    }

    /**
     * Removes RT outliers using MAD: |x - median| / scaledMad > [zCutoff] are dropped. With fewer than
     * [minForFiltering] values, returns the input unchanged (too small to filter reliably).
     */
    fun rejectOutliers(values: List<Double>, zCutoff: Double = 3.0, minForFiltering: Int = 4): List<Double> {
        if (values.size < minForFiltering) return values
        val med = median(values)
        val mad = scaledMad(values)
        return if (mad == 0.0) values else values.filter { abs(it - med) / mad <= zCutoff }
    }
}
