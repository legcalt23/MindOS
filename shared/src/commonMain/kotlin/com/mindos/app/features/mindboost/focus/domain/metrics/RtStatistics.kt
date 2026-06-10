package com.mindos.app.features.mindboost.focus.domain.metrics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Robust statistics primitives used across the metrics engine. Pure functions,
 * no platform dependency. The choices here encode the system's honesty: median
 * over mean, MAD over SD for outlier rejection (03_metrics_engine_plan.md §1).
 */
internal object RtStatistics {

    /** Scale factor making the MAD a consistent estimator of SD under normality. */
    private const val MAD_SCALE = 1.4826

    /** Median of a list of doubles. Returns 0.0 for an empty list (callers gate on n). */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }

    /** Sample standard deviation (n-1). Returns 0.0 for fewer than 2 values. */
    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    /**
     * Median Absolute Deviation, scaled by 1.4826 to be a consistent estimator of
     * the SD under normality. Robust to the long right tail typical of RT.
     */
    fun scaledMad(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val med = median(values)
        val deviations = values.map { abs(it - med) }
        return median(deviations) * MAD_SCALE
    }

    /**
     * Removes RT outliers using MAD: |x - median| / scaledMad > [zCutoff] are dropped.
     * With fewer than [minForFiltering] values, returns the input unchanged (the
     * estimate is too small to filter reliably).
     */
    fun rejectOutliers(values: List<Double>, zCutoff: Double = 3.0, minForFiltering: Int = 4): List<Double> {
        if (values.size < minForFiltering) return values
        val med = median(values)
        val mad = scaledMad(values)
        // MAD of 0 (majority identical) cannot scale deviations — keep values as-is.
        return if (mad == 0.0) values else values.filter { abs(it - med) / mad <= zCutoff }
    }

    /**
     * Slope of a simple linear regression of [ys] on [xs] (least squares).
     * Returns 0.0 if fewer than 2 points or zero x-variance.
     */
    fun linearSlope(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size < 2 || xs.size != ys.size) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            num += dx * (ys[i] - meanY)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
    }
}
