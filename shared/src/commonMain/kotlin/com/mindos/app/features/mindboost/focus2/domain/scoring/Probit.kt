package com.mindos.app.features.mindboost.focus2.domain.scoring

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * The inverse standard normal CDF and its density.
 *
 * Extracted from [DescriptiveScorer] rather than suppressed there: a probit is general-purpose mathematics, it
 * is reusable by any future signal-detection lane, and it is independently pinnable. Leaving it inside the
 * scorer made that object carry twelve functions and made the golden test look like a test of the scorer when
 * it is really a test of this approximation.
 *
 * No probit existed anywhere else in this repo (verified by grep across every `.kt`), which is why it is
 * written rather than imported.
 */
object Probit {

    /**
     * Acklam's rational approximation of the inverse standard normal CDF (relative error ~1.15e-9).
     *
     * `DescriptiveScorerGoldenTest` pins it against values obtained from an INDEPENDENT source — published
     * quantiles cross-checked by a bisection in another language — never read off this implementation, because
     * a golden read from its own subject is a tautology, not a test.
     */
    @Suppress("MagicNumber") // The coefficients ARE the algorithm; naming them would obscure, not clarify.
    fun zScore(p: Double): Double {
        require(p > 0.0 && p < 1.0) { "z(p) needs p in (0,1), was $p" }
        val a = doubleArrayOf(
            -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
            1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00,
        )
        val b = doubleArrayOf(
            -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
            6.680131188771972e+01, -1.328068155288572e+01,
        )
        val c = doubleArrayOf(
            -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
            -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00,
        )
        val d = doubleArrayOf(
            7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00,
        )
        val pLow = 0.02425
        val pHigh = 1.0 - pLow
        return when {
            p < pLow -> {
                val q = sqrt(-2.0 * ln(p))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
            p <= pHigh -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - p))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0)
            }
        }
    }

    /** Standard normal density, needed by the standard error of d'. */
    fun density(x: Double): Double = exp(-HALF * x * x) / sqrt(2.0 * PI)

    private const val HALF = 0.5
}
