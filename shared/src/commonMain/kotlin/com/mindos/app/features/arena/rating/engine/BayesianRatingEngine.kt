@file:Suppress("MagicNumber") // Logistic base (10.0) and identity (1.0) are intrinsic to the formula.

package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingEngineConfig
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Phase 03: Offline, deterministic Bayesian Competitive Rating engine.
 *
 * Properties enforced by construction and design:
 * - Pure & immutable: [update] returns a NEW [RatingPosterior]; the prior is never mutated.
 * - Reproducible: output depends only on the immutable [RatingPosterior] + [TrialEvidence] + config.
 *   No clock, no randomness, no I/O.
 * - No external infrastructure: no Firebase, analytics, or telemetry imports.
 * - Integrity-gated: a rating delta is applied ONLY when integrity is VERIFIED and all hard gates
 *   pass; otherwise the update is [RatingUpdateResult.Rejected] and the rating is unchanged.
 * - No pay-to-win: there is no premium/subscription input anywhere in this module.
 */
class BayesianRatingEngine(
    private val config: RatingEngineConfig = RatingEngineConfig.DEFAULT,
) {

    /** Opponent-independent expected performance of [blockMu] against [taskDifficulty], in (0,1). */
    fun expectedTaskPerformance(blockMu: Double, taskDifficulty: Double): Double =
        1.0 / (1.0 + 10.0.pow((taskDifficulty - blockMu) / config.performanceScale))

    /** The objective observed motor outcome carried by the evidence. No self-report/fatigue. */
    fun observedTaskPerformance(evidence: TrialEvidence): Double = evidence.observedPerformance

    /** Delta = observed - expected. Positive means the player exceeded expectation. */
    fun taskPerformanceDelta(observed: Double, expected: Double): Double = observed - expected

    /**
     * Reduces sigma after a valid update, but KEEPS sigma high / inflates it when smurf suspicion is
     * HIGH (rapid recalibration), and never lets it fall below the configured floor.
     */
    fun updateSigma(sigma: Double, finalMultiplier: Double, smurf: SmurfSuspicionLevel): Double =
        when (smurf) {
            SmurfSuspicionLevel.HIGH -> min(config.maxSigma, sigma * config.sigmaInflateOnSmurf)
            SmurfSuspicionLevel.LOW, SmurfSuspicionLevel.NONE ->
                max(config.minSigma, sigma * (1.0 - config.sigmaDecayRate * finalMultiplier))
        }

    /**
     * Applies one rating update. Returns [RatingUpdateResult.Rejected] (rating unchanged) if any
     * hard gate fails; otherwise [RatingUpdateResult.Applied] with the new posterior and the delta.
     */
    fun update(prior: RatingPosterior, evidence: TrialEvidence): RatingUpdateResult {
        val rejection = firstFailingGate(evidence)
        if (rejection != null) return RatingUpdateResult.Rejected(rejection)
        return applyVerifiedUpdate(prior, evidence)
    }

    private fun firstFailingGate(evidence: TrialEvidence): RatingUpdateRejectionReason? = when {
        evidence.integrityState != IntegrityStateEnum.VERIFIED ->
            RatingUpdateRejectionReason.INTEGRITY_NOT_VERIFIED
        !evidence.ledgerApprovedCurrent ->
            RatingUpdateRejectionReason.LEDGER_NOT_APPROVED_CURRENT
        evidence.comparability == ComparabilityStateEnum.NOT_COMPARABLE ->
            RatingUpdateRejectionReason.NOT_COMPARABLE
        evidence.measurementConfidence == ConfidenceStateEnum.INSUFFICIENT ->
            RatingUpdateRejectionReason.INSUFFICIENT_MEASUREMENT_CONFIDENCE
        evidence.sessionValidity == SessionValidityStatusEnum.INVALID ->
            RatingUpdateRejectionReason.SESSION_INVALID
        else -> null
    }

    private fun applyVerifiedUpdate(
        prior: RatingPosterior,
        evidence: TrialEvidence,
    ): RatingUpdateResult.Applied {
        val expected = expectedTaskPerformance(prior.mean.value, evidence.taskDifficulty.value)
        val observed = observedTaskPerformance(evidence)
        val delta = taskPerformanceDelta(observed, expected)
        val finalMultiplier = evidence.measurementMultiplier.value *
            evidence.comparabilityMultiplier.value *
            evidence.modeScalingFactor
        val muDelta = config.muDamping * prior.uncertainty.value * delta * finalMultiplier
        val newMu = prior.mean.value + muDelta
        val newSigmaValue =
            updateSigma(prior.uncertainty.value, finalMultiplier, evidence.smurfSuspicion)
        return RatingUpdateResult.Applied(
            posterior = RatingPosterior(Rating(newMu), Sigma(newSigmaValue)),
            delta = RatingDelta(muDelta = muDelta, sigmaDelta = newSigmaValue - prior.uncertainty.value),
            expectedTaskPerformance = expected,
            taskPerformanceDelta = delta,
        )
    }
}
