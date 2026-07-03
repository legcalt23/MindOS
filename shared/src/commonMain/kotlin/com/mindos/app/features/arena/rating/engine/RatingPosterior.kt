package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma

/**
 * Phase 03/04: Immutable Bayesian rating posterior — the player's estimated skill ([mean]) and the
 * uncertainty around it ([uncertainty]). Invariants on the components are enforced by the [Rating]
 * and [Sigma] value objects (no NaN/infinite mean, non-negative uncertainty), so an invalid
 * posterior cannot be constructed.
 */
data class RatingPosterior(
    val mean: Rating,
    val uncertainty: Sigma,
)

/**
 * Phase 03: Alias making the domain vocabulary explicit. Rating uncertainty IS modelled by [Sigma],
 * which already rejects negative / NaN values at construction.
 */
typealias RatingUncertainty = Sigma
