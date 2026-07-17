@file:Suppress("MagicNumber") // 0.15 sample-rate + [0,1] bounds are named/domain constants, not magic.

package com.mindos.app.features.mindboost.core.scoring.normalization

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Fase A · PURE fix of the master plan's inter-session confidence (§8.2), which was DEFECTIVE:
 *   `CL = (1 − SD/Mean)·(1 − e^(−0.15·n))`
 * With `CV = SD/Mean` the term `(1 − CV)` goes NEGATIVE when `CV > 1`, and `Mean = 0` divides by zero.
 *
 * This function clamps to `[0,1]` and fails closed. It is a CONFIDENCE / STABILITY quantity and must NEVER
 * be used as a data-sufficiency (sample-size) quantity — they are ORTHOGONAL (learning 2026-06-28,
 * `ModuleContribution.dataSufficiency` vs `confidence`).
 *
 * NOT WIRED into production in this slice: wiring it into a module's `confidence` is Fase C, because it
 * would reclassify existing profiles. Known limitation to resolve in Fase C before wiring: the coefficient
 * of variation is scale-sensitive — a consistently LOW-but-stable performer (small mean) is penalized for a
 * low mean, not for instability. Fase C should evaluate a dispersion normalized by the SCORE SCALE (e.g. SD
 * over the fixed `[0,1]` range) instead of by the mean.
 */
@Suppress("MemberNameEqualsClassName")
object InterSessionConfidence {

    /** Sample-rate constant of the saturation term `1 − e^(−k·n)` (master plan §8.2). */
    const val CONFIDENCE_SAMPLE_RATE_K: Double = 0.15

    /** Below this many sessions, dispersion is undefined ⇒ confidence is an honest 0.0 (never fabricated). */
    const val MIN_SESSIONS_FOR_CONFIDENCE: Int = 2

    /**
     * Confidence in `[0,1]` from the dispersion and count of recent valid session scores. Extremes:
     *  - `n < 2` ⇒ 0.0 (dispersion undefined; honest "no confidence yet").
     *  - `mean <= 0` ⇒ 0.0 (fail closed; no positive signal to be confident about).
     *  - `CV > 1` ⇒ dispersion term clamps to 0 ⇒ 0.0.
     *  - `CV = 0` (perfectly stable) with large `n` ⇒ approaches 1.0.
     */
    fun interSessionConfidence(recentScores: List<Double>): Double {
        require(recentScores.all { it.isFinite() }) { "recentScores must be finite." }
        val n = recentScores.size
        if (n < MIN_SESSIONS_FOR_CONFIDENCE) return 0.0
        val mean = recentScores.average()
        return if (mean <= 0.0) {
            0.0
        } else {
            val cv = sampleStandardDeviation(recentScores, mean) / mean
            val dispersionTerm = (1.0 - cv).coerceIn(0.0, 1.0)
            val sampleTerm = 1.0 - exp(-CONFIDENCE_SAMPLE_RATE_K * n)
            (dispersionTerm * sampleTerm).coerceIn(0.0, 1.0)
        }
    }

    /** Sample standard deviation (n−1 divisor) — unbiased for a sample of sessions; caller guards n >= 2. */
    private fun sampleStandardDeviation(values: List<Double>, mean: Double): Double {
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }
}
