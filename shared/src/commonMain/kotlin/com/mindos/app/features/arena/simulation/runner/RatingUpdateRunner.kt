@file:Suppress("MagicNumber") // Synthetic measurement inputs are documented simulation constants.

package com.mindos.app.features.arena.simulation.runner

import com.mindos.app.features.arena.comparability.engine.ComparabilityClassifier
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.integrity.engine.IntegrityGateBuilder
import com.mindos.app.features.arena.measurement.engine.MeasurementConfidenceBuilder
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.engine.TrialEvidence
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.MatchQuality
import com.mindos.app.features.arena.rating.model.MeasurementQuality
import com.mindos.app.features.arena.rating.model.Rating

/** Phase 07: Outcome of running one session through the full gate pipeline. */
data class RunnerResult(
    val newPosterior: RatingPosterior,
    val update: RatingUpdateResult,
    val integrityState: IntegrityStateEnum,
    val comparability: ComparabilityStateEnum,
    val measurement: ConfidenceStateEnum,
)

/**
 * Phase 07: Rating-update runner (rating_update_runner.md). Pipes one [GeneratedSession] through the
 * complete offline gate stack — comparability, measurement confidence, integrity gate — and then the
 * Bayesian rating engine, returning the new posterior (unchanged if any gate rejects). Pure: no clock
 * or I/O; the ledger-approved signal is supplied by the caller (from the Phase 06 ledger gate).
 */
class RatingUpdateRunner(
    private val engine: BayesianRatingEngine = BayesianRatingEngine(),
    private val comparabilityClassifier: ComparabilityClassifier = ComparabilityClassifier(),
    private val measurementBuilder: MeasurementConfidenceBuilder = MeasurementConfidenceBuilder(),
    private val integrityGateBuilder: IntegrityGateBuilder = IntegrityGateBuilder(),
) {

    fun runRatingUpdate(
        prior: RatingPosterior,
        session: GeneratedSession,
        ledgerApprovedCurrent: Boolean,
    ): RunnerResult {
        val comparability = comparabilityClassifier.classify(session.environment)
        val measurement = measurementBuilder.buildMeasurementConfidence(
            sem = SYNTHETIC_SEM,
            reliability = SYNTHETIC_RELIABILITY,
            sampleSize = SYNTHETIC_SAMPLE,
            ledgerApprovedCurrent = ledgerApprovedCurrent,
            mu = prior.mean.value,
            sigma = prior.uncertainty.value,
        )
        val gate = integrityGateBuilder.buildIntegrityGate(session.abuseSignals)
        val evidence = buildEvidence(session, ledgerApprovedCurrent, comparability, measurement, gate.state)
        val update = engine.update(prior, evidence)
        val newPosterior = if (update is RatingUpdateResult.Applied) update.posterior else prior
        return RunnerResult(newPosterior, update, gate.state, comparability.state, measurement.state)
    }

    @Suppress("LongParameterList") // Threads the pipeline outputs into the auditable evidence record.
    private fun buildEvidence(
        session: GeneratedSession,
        ledgerApprovedCurrent: Boolean,
        comparability: com.mindos.app.features.arena.rating.model.ComparabilityDecision,
        measurement: com.mindos.app.features.arena.rating.model.MeasurementConfidenceDecision,
        integrityState: IntegrityStateEnum,
    ): TrialEvidence = TrialEvidence(
        motorType = session.motorType,
        taskDifficulty = Rating(session.taskDifficulty),
        observedPerformance = session.observedPerformance,
        accuracy = Confidence(session.observedPerformance),
        latency = session.environment.latencyMs,
        integrityState = integrityState,
        ledgerApprovedCurrent = ledgerApprovedCurrent,
        comparability = comparability.state,
        measurementConfidence = measurement.state,
        sessionValidity = SessionValidityStatusEnum.VALID,
        measurementMultiplier = MeasurementQuality(measurement.updateMultiplier),
        comparabilityMultiplier = MatchQuality(comparability.updateMultiplier),
        modeScalingFactor = 1.0,
        smurfSuspicion = integrityGateBuilder.smurfSuspicionFrom(session.abuseSignals),
        hardwareClass = session.hardwareClass,
    )

    companion object {
        private const val SYNTHETIC_SEM = 30.0
        private const val SYNTHETIC_RELIABILITY = 0.85
        private const val SYNTHETIC_SAMPLE = 50
    }
}
