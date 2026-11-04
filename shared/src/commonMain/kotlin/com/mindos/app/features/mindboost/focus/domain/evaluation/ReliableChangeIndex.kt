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
 *
 * DECLARED CEILING — two corrections this formula does NOT apply. Both are owned by whoever first
 * wires a non-EMPTY `ReliabilitySource`; until then every result here is `NotEnoughData`
 * (`MB-FOCUS-0102`).
 *
 *  - NO expected-retest-change term. The numerator is the raw difference, so a second administration
 *    is compared against a first as if repeating the task moved nothing. Correcting it needs a
 *    normative retest change from a control sample, and this repo has no source for one — a field
 *    defaulting to `0.0` would ASSERT "repeating it changes nothing", which fabricates the flattering
 *    answer out of an absence.
 *  - NO correction for judging several metrics at once. Each one is compared against the same 1.96.
 *    Today that is a no-op and not a leak: on the probe the protocol serves, exactly one metric can
 *    clear the central floor (see `TransferReportBuilder.label`), so the family has one member. It
 *    stops being a no-op the day the probe is lengthened (`MB-FOCUS-0130`) or a second probe is served.
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
