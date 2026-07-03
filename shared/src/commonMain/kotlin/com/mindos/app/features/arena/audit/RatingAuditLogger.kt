package com.mindos.app.features.arena.audit

import com.mindos.app.features.arena.audit.model.RatingMutationAuditLog
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult

/**
 * Phase 14 conformance (R-04 / strict §9): produces a [RatingMutationAuditLog] for every rating
 * update — applied OR rejected — so a mutation always yields an auditable entry even in simulation
 * mode. Pure: no clock or I/O; the caller supplies synthetic ids and the timestamp. An [Applied]
 * update records the new posterior; a [Rejected] update records the unchanged prior plus the typed
 * rejection reason.
 */
class RatingAuditLogger(
    private val configVersion: String = "BLEND_CONFIG_V1.0",
) {

    fun audit(
        prior: RatingPosterior,
        result: RatingUpdateResult,
        syntheticId: String,
        syntheticReferenceId: String,
        timestampMs: Long,
    ): RatingMutationAuditLog {
        val newPosterior = when (result) {
            is RatingUpdateResult.Applied -> result.posterior
            is RatingUpdateResult.Rejected -> prior
        }
        return RatingMutationAuditLog(
            syntheticId = syntheticId,
            syntheticReferenceId = syntheticReferenceId,
            timestampMs = timestampMs,
            configVersion = configVersion,
            decisionState = decisionStateFor(result),
            reasonCode = reasonCodeFor(result),
            sourceEngine = SOURCE_ENGINE,
            previousRatingMu = prior.mean.value,
            newRatingMu = newPosterior.mean.value,
            previousRatingSigma = prior.uncertainty.value,
            newRatingSigma = newPosterior.uncertainty.value,
        )
    }

    private fun decisionStateFor(result: RatingUpdateResult): String = when (result) {
        is RatingUpdateResult.Applied -> "APPLIED"
        is RatingUpdateResult.Rejected -> "REJECTED"
    }

    private fun reasonCodeFor(result: RatingUpdateResult): String = when (result) {
        is RatingUpdateResult.Applied -> "STANDARD_UPDATE"
        is RatingUpdateResult.Rejected -> result.reason.name
    }

    companion object {
        private const val SOURCE_ENGINE = "BayesianRatingEngine"
    }
}
