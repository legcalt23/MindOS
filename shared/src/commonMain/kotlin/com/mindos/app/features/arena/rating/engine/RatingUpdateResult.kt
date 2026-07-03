package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.config.RatingUpdateRejectionReason

/**
 * Phase 03: Result of a Bayesian rating update.
 *
 * - [Applied]: a gated-and-valid update; carries the new posterior plus the auditable delta and the
 *   expected/observed task-performance terms used to compute it.
 * - [Rejected]: a hard gate failed (e.g. integrity not VERIFIED); the prior posterior is unchanged
 *   and the typed [reason] explains why. Reasons carry no identifiers or rating values.
 */
sealed interface RatingUpdateResult {

    data class Applied(
        val posterior: RatingPosterior,
        val delta: RatingDelta,
        val expectedTaskPerformance: Double,
        val taskPerformanceDelta: Double,
    ) : RatingUpdateResult

    data class Rejected(
        val reason: RatingUpdateRejectionReason,
    ) : RatingUpdateResult
}
