package com.mindos.app.features.arena.rating.engine

/**
 * Phase 03: The reproducible change produced by a single rating update.
 *
 * [muDelta] and [sigmaDelta] are the signed changes to the posterior mean and uncertainty. A
 * rejected (gated) update yields [ZERO]. Because the delta is derived purely from immutable inputs,
 * it is fully reproducible.
 */
data class RatingDelta(
    val muDelta: Double,
    val sigmaDelta: Double,
) {
    init {
        require(!muDelta.isNaN() && !muDelta.isInfinite()) { "muDelta must be finite" }
        require(!sigmaDelta.isNaN() && !sigmaDelta.isInfinite()) { "sigmaDelta must be finite" }
    }

    companion object {
        val ZERO: RatingDelta = RatingDelta(0.0, 0.0)
    }
}
