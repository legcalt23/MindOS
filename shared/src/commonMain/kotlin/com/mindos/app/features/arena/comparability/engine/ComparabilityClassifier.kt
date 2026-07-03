@file:Suppress("MagicNumber") // 1.0/0.0 are the COMPARABLE / NOT_COMPARABLE multiplier endpoints.

package com.mindos.app.features.arena.comparability.engine

import com.mindos.app.features.arena.comparability.model.AccessibilityComparability
import com.mindos.app.features.arena.comparability.model.ComparabilityConfig
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.rating.model.ComparabilityDecision
import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.session.model.SessionEnvironmentSnapshot

/**
 * Phase 04: Offline, deterministic comparability classifier.
 *
 * Classifies each measurement dimension (latency, jitter, network instability, accessibility) into a
 * comparability tier and combines them conservatively (worst dimension wins) into a
 * [ComparabilityDecision] that the Bayesian rating engine consumes. Pure: no clock, randomness, or I/O.
 * Thresholds come from [ComparabilityConfig] so the engine carries no magic numbers.
 */
class ComparabilityClassifier(
    private val config: ComparabilityConfig = ComparabilityConfig.DEFAULT,
) {

    fun classifyLatency(latency: LatencyMs): ComparabilityStateEnum = when {
        latency.value <= config.latencyComparableMaxMs -> ComparabilityStateEnum.COMPARABLE
        latency.value <= config.latencyPartialMaxMs -> ComparabilityStateEnum.PARTIALLY_COMPARABLE
        else -> ComparabilityStateEnum.NOT_COMPARABLE
    }

    fun classifyJitter(jitter: JitterMs): ComparabilityStateEnum = when {
        jitter.value <= config.jitterComparableMaxMs -> ComparabilityStateEnum.COMPARABLE
        jitter.value <= config.jitterPartialMaxMs -> ComparabilityStateEnum.PARTIALLY_COMPARABLE
        else -> ComparabilityStateEnum.NOT_COMPARABLE
    }

    /** [networkInstability] is the composite latency/jitter/packet-loss score in [0,1]. */
    fun classifyNetwork(networkInstability: Double): ComparabilityStateEnum = when {
        networkInstability <= config.networkComparableMax -> ComparabilityStateEnum.COMPARABLE
        networkInstability <= config.networkPartialMax -> ComparabilityStateEnum.PARTIALLY_COMPARABLE
        else -> ComparabilityStateEnum.NOT_COMPARABLE
    }

    fun accessibilityTier(accessibility: AccessibilityComparability): ComparabilityStateEnum =
        when (accessibility) {
            AccessibilityComparability.COMPARABLE -> ComparabilityStateEnum.COMPARABLE
            AccessibilityComparability.COMPARABLE_OWN_NORM -> ComparabilityStateEnum.PARTIALLY_COMPARABLE
            AccessibilityComparability.TRAINING_ONLY -> ComparabilityStateEnum.NOT_COMPARABLE
            AccessibilityComparability.NOT_COMPARABLE -> ComparabilityStateEnum.NOT_COMPARABLE
        }

    fun classify(env: SessionEnvironmentSnapshot): ComparabilityDecision {
        val dimensions = dimensionTiers(env)
        val overall = worst(dimensions.map { it.second })
        return ComparabilityDecision(
            state = overall,
            updateMultiplier = multiplierFor(overall),
            reasonCode = reasonCode(dimensions, overall),
            sourceEngine = SOURCE_ENGINE,
        )
    }

    fun auditLog(
        env: SessionEnvironmentSnapshot,
        decision: ComparabilityDecision,
        syntheticId: String,
        syntheticReferenceId: String,
        timestampMs: Long,
    ): ComparabilityAuditLog {
        val dimensions = dimensionTiers(env).toMap()
        return ComparabilityAuditLog(
            syntheticId = syntheticId,
            syntheticReferenceId = syntheticReferenceId,
            timestampMs = timestampMs,
            configVersion = config.configVersion,
            decisionState = decision.state.name,
            reasonCode = decision.reasonCode,
            sourceEngine = SOURCE_ENGINE,
            latencyTier = dimensions.getValue(LATENCY).name,
            jitterTier = dimensions.getValue(JITTER).name,
            networkTier = dimensions.getValue(NETWORK).name,
            accessibilityTier = dimensions.getValue(ACCESSIBILITY).name,
        )
    }

    private fun dimensionTiers(
        env: SessionEnvironmentSnapshot,
    ): List<Pair<String, ComparabilityStateEnum>> = listOf(
        LATENCY to classifyLatency(env.latencyMs),
        JITTER to classifyJitter(env.jitterMs),
        NETWORK to classifyNetwork(env.networkInstability),
        ACCESSIBILITY to accessibilityTier(AccessibilityComparability.fromRaw(env.accessibilityClass)),
    )

    private fun worst(tiers: List<ComparabilityStateEnum>): ComparabilityStateEnum = when {
        tiers.contains(ComparabilityStateEnum.NOT_COMPARABLE) -> ComparabilityStateEnum.NOT_COMPARABLE
        tiers.contains(ComparabilityStateEnum.PARTIALLY_COMPARABLE) ->
            ComparabilityStateEnum.PARTIALLY_COMPARABLE
        else -> ComparabilityStateEnum.COMPARABLE
    }

    private fun multiplierFor(state: ComparabilityStateEnum): Double = when (state) {
        ComparabilityStateEnum.COMPARABLE -> 1.0
        ComparabilityStateEnum.PARTIALLY_COMPARABLE -> config.partialMultiplier
        ComparabilityStateEnum.NOT_COMPARABLE -> 0.0
    }

    private fun reasonCode(
        dimensions: List<Pair<String, ComparabilityStateEnum>>,
        overall: ComparabilityStateEnum,
    ): String = "${overall.name}:" +
        dimensions.filter { it.second == overall }.joinToString("+") { it.first }

    companion object {
        private const val SOURCE_ENGINE = "ComparabilityClassifier"
        private const val LATENCY = "LATENCY"
        private const val JITTER = "JITTER"
        private const val NETWORK = "NETWORK"
        private const val ACCESSIBILITY = "ACCESSIBILITY"
    }
}
