package com.mindos.app.features.mindboost.memory.domain.adaptive

import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.ActivateMetacognition
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.Hold
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.ProposeRest
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.StepDown
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.StepUp
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.StopSession
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveDecision.WorkSpeed
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricSet
import com.mindos.app.features.mindboost.memory.domain.metrics.rawValueOf
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import kotlin.math.abs

/** Decides how to move difficulty after a block. Pure, deterministic, no I/O. */
interface MemoryAdaptiveEngine {
    fun decide(input: MemoryAdaptiveInput): MemoryAdaptiveResult
}

/**
 * Default rule-based engine (doc §5). The decision order IS the priority: safety
 * ([safetyDecision]: stop / rest / metacognition / step-down) is evaluated before
 * progression ([progressionDecision]: hysteresis / work-speed / step-up).
 * Difficulty moves on exactly ONE relevant axis per decision.
 *
 * Anti-oscillation: hysteresis (hold step-UPs for N blocks after a change),
 * asymmetric steps (down can be bigger and bypasses hysteresis as a safety move),
 * and per-block (not per-trial) decisions.
 */
class DefaultMemoryAdaptiveEngine : MemoryAdaptiveEngine {

    override fun decide(input: MemoryAdaptiveInput): MemoryAdaptiveResult {
        val performance = MemoryPerformance.of(input.recentBlock, input.confidenceBias, input.config)
        val zone = zoneOf(performance, input.config)
        return safetyDecision(input, zone, performance) ?: progressionDecision(input, zone, performance)
    }

    /** Safety moves (stop/rest/metacognition/step-down). Returns null when none applies. */
    private fun safetyDecision(
        input: MemoryAdaptiveInput,
        zone: MemoryZone,
        performance: Double,
    ): MemoryAdaptiveResult? {
        val cfg = input.config
        val current = input.current
        val gov = input.exercise.governingAxis
        fun r(d: MemoryAdaptiveDecision, next: MemoryDifficultyVector, why: String) =
            MemoryAdaptiveResult(d, next, zone, performance, why)
        return when {
            stopRequired(input) -> r(StopSession, current, "Fatiga sostenida o frustración repetida.")
            input.fatigue.emerging -> r(ProposeRest, current, "Fatiga emergente: micro-descanso.")
            abs(input.confidenceBias) > cfg.confidenceBiasThreshold ->
                r(ActivateMetacognition, current, "Calibración pobre: metacognición.")
            zone == MemoryZone.FRUSTRATION -> {
                val axis = input.lastRaisedAxis?.takeIf { it in input.exercise.relevantAxes } ?: gov
                r(StepDown(axis), current.stepDown(axis, cfg.stepBig), "Frustración: bajar fuerte.")
            }
            zone == MemoryZone.SUBOPTIMAL_LOW ->
                r(StepDown(gov), current.stepDown(gov, cfg.stepSmall), "Subóptima baja: bajar suave.")
            else -> null
        }
    }

    /** Progression moves (hysteresis hold / work-speed / step-up). Always returns a decision. */
    private fun progressionDecision(
        input: MemoryAdaptiveInput,
        zone: MemoryZone,
        performance: Double,
    ): MemoryAdaptiveResult {
        val cfg = input.config
        val current = input.current
        val gov = input.exercise.governingAxis
        val speedNorm = MemoryPerformance.speedNorm(input.recentBlock, cfg)
        fun r(d: MemoryAdaptiveDecision, next: MemoryDifficultyVector, why: String) =
            MemoryAdaptiveResult(d, next, zone, performance, why)
        return when {
            input.blocksSinceChange < cfg.cooldownBlocks ->
                r(Hold, current, "Histéresis tras un cambio reciente.")
            speedNorm < cfg.slowSpeedNorm && performance >= cfg.suboptimalLowMax ->
                r(WorkSpeed, current, "Preciso pero lento: trabajar velocidad.")
            zone == MemoryZone.OPTIMAL -> optimalDecision(input, zone, performance)
            zone == MemoryZone.SUBOPTIMAL_HIGH -> {
                val axis = dimensionToRaise(input, gov)
                r(StepUp(axis), current.stepUp(axis, cfg.stepSmall), "Subóptima alta: subir suave.")
            }
            else -> {
                val axis = dimensionToRaise(input, gov)
                r(StepUp(axis), current.stepUp(axis, cfg.stepBig), "Demasiado fácil: subir fuerte.")
            }
        }
    }

    /** In the optimal zone, nudge a specific weak dimension if one exists, else hold. */
    private fun optimalDecision(
        input: MemoryAdaptiveInput,
        zone: MemoryZone,
        performance: Double,
    ): MemoryAdaptiveResult {
        val weak = specificWeakAxis(input)
        val decision = if (weak == null) Hold else StepUp(weak)
        val next = if (weak == null) input.current else input.current.stepUp(weak, input.config.stepSmall)
        val why = if (weak == null) "Zona óptima: mantener." else "Zona óptima: ajustar composición."
        return MemoryAdaptiveResult(decision, next, zone, performance, why)
    }

    private fun stopRequired(input: MemoryAdaptiveInput): Boolean =
        (input.fatigue.level == MemoryFatigueLevel.HIGH && input.fatigue.sustained) ||
            input.consecutiveFrustrationBlocks >= input.config.frustrationBlocksToStop

    /**
     * The specific dimension to address (doc §5.3): if delayed recall lags immediate,
     * raise DELAY; else if intrusions are high, raise INTERFERENCE; else null. Only
     * ever returns an axis the exercise actually uses.
     */
    private fun specificWeakAxis(input: MemoryAdaptiveInput): MemoryDifficultyAxis? {
        val relevant = input.exercise.relevantAxes
        val cfg = input.config
        val intrusion = input.recentBlock.rawValueOf(MemoryMetricKey.INTRUSION_RATE)
        return when {
            MemoryDifficultyAxis.DELAY in relevant && delayedLags(input.recentBlock, cfg) ->
                MemoryDifficultyAxis.DELAY
            MemoryDifficultyAxis.INTERFERENCE in relevant && intrusion > cfg.intrusionRaiseThreshold ->
                MemoryDifficultyAxis.INTERFERENCE
            else -> null
        }
    }

    /** The dimension to raise in a progression zone: the specific weak axis, else [fallback]. */
    private fun dimensionToRaise(input: MemoryAdaptiveInput, fallback: MemoryDifficultyAxis): MemoryDifficultyAxis =
        specificWeakAxis(input) ?: fallback

    private fun delayedLags(block: MemoryMetricSet, cfg: MemoryAdaptiveConfig): Boolean {
        val immediate = block[MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY]?.value
        val delayed = block[MemoryMetricKey.DELAYED_RECALL_ACCURACY]?.value
        return immediate != null && delayed != null && delayed < immediate - cfg.delayedLagMargin
    }
}
