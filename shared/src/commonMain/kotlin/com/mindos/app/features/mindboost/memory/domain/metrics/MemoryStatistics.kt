package com.mindos.app.features.mindboost.memory.domain.metrics

import kotlin.math.abs

/**
 * Robust statistics primitives for the memory metrics engine. Pure, no platform.
 * Self-contained inside the memory feature (mirrors focus's RtStatistics choices:
 * median over mean, MAD over SD for outlier rejection — doc §6 honest metrics).
 */
internal object MemoryStatistics {

    /** Scale factor making the MAD a consistent estimator of SD under normality. */
    private const val MAD_SCALE = 1.4826

    /** Median of a list of doubles. Returns 0.0 for an empty list (callers gate on n). */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /** Median Absolute Deviation scaled to estimate SD; robust to RT's long right tail. */
    fun scaledMad(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val med = median(values)
        return median(values.map { abs(it - med) }) * MAD_SCALE
    }

    /**
     * Drops outliers using MAD: |x - median| / scaledMad > [zCutoff] are removed.
     * With fewer than [minForFiltering] values, returns the input unchanged.
     *
     * NOTE: this is for central-tendency metrics (RT median). Extreme-event counts
     * (intrusions, lapses) must be computed on RAW data — never filter away the very
     * signal you are counting (project learning 2026-06-04).
     */
    fun rejectOutliers(values: List<Double>, zCutoff: Double = 3.0, minForFiltering: Int = 4): List<Double> {
        if (values.size < minForFiltering) return values
        val med = median(values)
        val mad = scaledMad(values)
        return if (mad == 0.0) values else values.filter { abs(it - med) / mad <= zCutoff }
    }
}
