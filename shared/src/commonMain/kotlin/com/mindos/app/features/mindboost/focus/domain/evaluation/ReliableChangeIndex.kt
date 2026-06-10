package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Test-retest reliability needed to compute a Reliable Change Index. Until a
 * metric has an estimated reliability (≥2–3 probe administrations), RCI is NOT
 * computed — the system shows the raw delta with an uncertainty caveat instead.
 */
data class ReliabilityEstimate(
    val metric: MetricKey,
    val reliability: Double,
    val sdPre: Double,
)

/** Outcome of an RCI computation. Gated: [NotEnoughData] until reliability exists. */
sealed interface RciResult {
    data object NotEnoughData : RciResult
    data class Value(val rci: Double, val reliable: Boolean) : RciResult
}

/**
 * Reliable Change Index (Jacobson & Truax). A change is "reliable" when |RCI| > 1.96
 * (p < .05). This is the honest gate for claiming real change — and it only fires
 * once reliability is known, never on a single session.
 */
object ReliableChangeIndex {

    const val CRITICAL_Z = 1.96

    fun compute(pre: Double, post: Double, reliability: ReliabilityEstimate): RciResult {
        val usable = reliability.reliability in REL_MIN..REL_MAX && reliability.sdPre > 0.0
        if (!usable) return RciResult.NotEnoughData
        val standardError = reliability.sdPre * sqrt(1.0 - reliability.reliability)
        val standardErrorDiff = sqrt(2.0) * standardError
        return if (standardErrorDiff == 0.0) {
            RciResult.NotEnoughData
        } else {
            val rci = (post - pre) / standardErrorDiff
            RciResult.Value(rci = rci, reliable = abs(rci) > CRITICAL_Z)
        }
    }

    // reliability must be a usable correlation in (0, 1]; 0 or out-of-range → cannot compute.
    private const val REL_MIN = 0.0001
    private const val REL_MAX = 1.0
}
