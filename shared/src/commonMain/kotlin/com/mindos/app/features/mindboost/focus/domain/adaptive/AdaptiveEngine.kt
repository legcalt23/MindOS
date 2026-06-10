package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.valueOf
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/** Decides how to move difficulty after a block. Pure, deterministic, no I/O. */
interface AdaptiveEngine {
    fun decide(input: AdaptiveInput): AdaptiveResult
}

/**
 * Default rule-based engine. The decision order IS the priority: safety
 * (stop / cooldown / impulsivity / step-down) is evaluated before progression
 * (switch-axis / step-up). See 04_adaptive_engine_plan.md §3.
 *
 * Anti-oscillation is enforced three ways: hysteresis (hold for N blocks after a
 * change), asymmetric steps (down faster than up), and a hold dead-zone in the
 * mid accuracy band.
 */
class DefaultAdaptiveEngine : AdaptiveEngine {

    // The decision is a priority-ordered table; its branch count IS the design.
    // Splitting it would scatter the safety-before-progression ordering that must
    // stay readable in one place.
    @Suppress("CyclomaticComplexMethod")
    override fun decide(input: AdaptiveInput): AdaptiveResult {
        val cfg = input.config
        val gov = input.exercise.governingAxis
        val accuracy = input.recentBlock.valueOf(MetricKey.ACCURACY)
        val rtCv = input.recentBlock.valueOf(MetricKey.RT_CV)
        // Baseline RT-CV would come from the calibrated profile; until calibration
        // lands we use the config fallback. Relative spikes (not absolute CV) trigger step-down.
        val baselineCv = cfg.fallbackBaselineRtCv
        val consecutiveErrors = trailingConsecutiveErrors(input.recentTrials)

        return when {
            // 1. Stop the session: sustained high fatigue.
            input.fatigue.level == FatigueLevel.HIGH && input.fatigue.sustained ->
                AdaptiveResult(AdaptiveDecision.StopSession, input.current, "Fatiga alta sostenida.")

            // 2. Cooldown: fatigue emerging but not critical — ease time-on-task.
            input.fatigue.emerging ->
                AdaptiveResult(
                    AdaptiveDecision.Cooldown,
                    input.current.stepDown(DifficultyAxis.TIME_ON_TASK, cfg.stepDown),
                    "Fatiga emergente: micro-pausa y menos tiempo-en-tarea.",
                )

            // 3. Contain impulsivity: fast commission errors.
            isImpulsive(input) -> {
                val axis = if (DifficultyAxis.INHIBITORY_DEMAND in input.exercise.relevantAxes) {
                    DifficultyAxis.INHIBITORY_DEMAND
                } else {
                    gov
                }
                AdaptiveResult(
                    AdaptiveDecision.ContainImpulsivity(axis),
                    input.current.stepUp(axis, cfg.stepUp),
                    "Comisiones con RT muy corto: contener precipitación, no premiar velocidad.",
                )
            }

            // 4. Step down: struggling.
            accuracy < cfg.accDownThreshold ||
                consecutiveErrors >= cfg.consecutiveErrorsDown ||
                (rtCv > cfg.rtCvSpikeFactor * baselineCv && rtCv > 0.0) ->
                AdaptiveResult(
                    AdaptiveDecision.StepDown(gov),
                    input.current.stepDown(gov, cfg.stepDown),
                    "Rendimiento por debajo del umbral o variabilidad alta: bajar dificultad.",
                )

            // 5. Hysteresis: just changed — hold and let it stabilize.
            input.blocksSinceChange < cfg.cooldownAfterDecisions ->
                AdaptiveResult(AdaptiveDecision.Hold, input.current, "Histéresis tras un cambio reciente.")

            // 6. Switch axis: governing axis saturated and a weaker relevant axis exists.
            isSaturated(input, gov) && weakestOtherAxis(input, gov) != null -> {
                val to = requireNotNull(weakestOtherAxis(input, gov))
                AdaptiveResult(
                    AdaptiveDecision.SwitchAxis(gov, to),
                    input.current.stepUp(to, cfg.stepUp),
                    "Eje dominante saturado: trabajar un eje más flojo.",
                )
            }

            // 7. Step up: mastering and stable.
            accuracy >= cfg.accUpThreshold && consecutiveErrors == 0 &&
                input.fatigue.level == FatigueLevel.LOW ->
                AdaptiveResult(
                    AdaptiveDecision.StepUp(gov),
                    input.current.stepUp(gov, cfg.stepUp),
                    "Dominio estable: subir dificultad en el eje dominante.",
                )

            // 8. Hold: the mid band, nothing to change.
            else -> AdaptiveResult(AdaptiveDecision.Hold, input.current, "Zona de mantenimiento.")
        }
    }

    /** Number of error trials at the tail of the window (consecutive most-recent errors). */
    private fun trailingConsecutiveErrors(trials: List<TrialResult>): Int {
        var count = 0
        for (t in trials.asReversed()) {
            val isError = t.outcome == TrialOutcome.FALSE_ALARM || t.outcome == TrialOutcome.MISS
            if (isError) count++ else break
        }
        return count
    }

    private fun isImpulsive(input: AdaptiveInput): Boolean {
        val cfg = input.config
        val fastErrors = input.recentTrials.count {
            it.outcome == TrialOutcome.FALSE_ALARM &&
                (it.reactionTime?.value ?: Long.MAX_VALUE) < cfg.impulsiveRtFloorMs
        }
        val falseAlarms = input.recentBlock.valueOf(MetricKey.FALSE_ALARMS)
        return fastErrors >= 2 && falseAlarms >= 2.0
    }

    private fun isSaturated(input: AdaptiveInput, axis: DifficultyAxis): Boolean {
        val mastery = input.perAxisMastery[axis] ?: return false
        return mastery.level.value >= input.config.masterySaturation &&
            mastery.accuracyAtLevel >= input.config.accUpThreshold
    }

    private fun weakestOtherAxis(input: AdaptiveInput, governing: DifficultyAxis): DifficultyAxis? =
        input.exercise.relevantAxes
            .filter { it != governing }
            .minByOrNull { input.perAxisMastery[it]?.accuracyAtLevel ?: 1.0 }
            ?.takeIf { (input.perAxisMastery[it]?.accuracyAtLevel ?: 1.0) < input.config.accUpThreshold }
}
