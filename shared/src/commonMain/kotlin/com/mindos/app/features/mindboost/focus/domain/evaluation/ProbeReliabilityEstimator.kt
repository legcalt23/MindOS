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
 * Decides whether reliabilities are available *yet* for an RCI. Two independent gates must both pass:
 * a populated [source] AND at least [minAdministrations] prior probe administrations of the construct
 * (06_ §5: never claim reliable change before ≥2–3 administrations). Until then it returns an empty map,
 * which forces [RciResult.NotEnoughData] downstream — the honest preliminary state.
 */
class ProbeReliabilityEstimator(
    private val source: ReliabilitySource = ReliabilitySource.EMPTY,
    private val minAdministrations: Int = MIN_ADMINISTRATIONS,
) {
    /** Reliabilities to feed RCI, or empty (gated) when there is not enough evidence yet. */
    fun estimate(priorAdministrations: Int): Map<MetricKey, ReliabilityEstimate> =
        if (priorAdministrations < minAdministrations) emptyMap() else source.reliabilities()

    companion object {
        /** Probe administrations required before any reliability is trusted. */
        const val MIN_ADMINISTRATIONS = 3
    }
}
