package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey

/**
 * Supplies per-metric test-retest reliabilities used by the Reliable Change Index. Honest by design:
 * trustworthy reliabilities require an empirical study (repeated probe administrations across users),
 * which this product does not yet have. The production default is therefore [EMPTY], so RCI stays
 * gated ([RciResult.NotEnoughData]) and the UI never claims a "reliable change". A future internal
 * reliability study populates this seam without touching call sites.
 */
fun interface ReliabilitySource {
    fun reliabilities(): Map<MetricKey, ReliabilityEstimate>

    companion object {
        /** No reliabilities known yet — keeps RCI gated. */
        val EMPTY: ReliabilitySource = ReliabilitySource { emptyMap() }
    }
}

/**
 * Decides whether reliabilities are available *yet* for an RCI. Three independent gates must all pass:
 * a populated [source], at least [minAdministrations] prior probe administrations of the construct
 * (06_ §5: never claim reliable change before ≥2–3 administrations), AND a retest on a form the user has
 * NOT been given before. Otherwise it returns an empty map, which forces [RciResult.NotEnoughData]
 * downstream — the honest preliminary state.
 *
 * The third gate is MB-FOCUS-0122 (R13). A test-retest reliability assumes parallel forms; on a repeated
 * form, memory of the exact stimulus sequence is confounded with change, so the estimate does not apply to
 * that administration and it is EXCLUDED — the conservative direction. The fact is tri-state on purpose:
 * `false` is the only value that passes, `true` is a declared repeat, and `null` (the form identity is
 * not known for this row, MB-FOCUS-0121) withholds too. Compared as `== false`, never `!= true`.
 */
class ProbeReliabilityEstimator(
    private val source: ReliabilitySource = ReliabilitySource.EMPTY,
    private val minAdministrations: Int = MIN_ADMINISTRATIONS,
) {
    /**
     * Reliabilities to feed RCI, or empty (gated) when there is not enough independent evidence yet.
     * [repeatsEarlierForm] is the scheduler's own declaration for THIS administration
     * ([ProbeAdministration.repeatsEarlierForm]); `null` means nobody can say, which is not a pass.
     */
    fun estimate(priorAdministrations: Int, repeatsEarlierForm: Boolean?): Map<MetricKey, ReliabilityEstimate> {
        val formIsFresh = repeatsEarlierForm == false
        return if (priorAdministrations < minAdministrations || !formIsFresh) emptyMap() else source.reliabilities()
    }

    companion object {
        /** Probe administrations required before any reliability is trusted. */
        const val MIN_ADMINISTRATIONS = 3
    }
}
