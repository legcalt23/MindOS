package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.confidentValueOf
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
    /** Safety first, always: fatigue outranks every performance signal. Null when there is no fatigue action. */
    private fun fatigueDecision(input: AdaptiveInput): AdaptiveResult? = when {
        // 1. Stop the session: sustained high fatigue.
        input.fatigue.level == FatigueLevel.HIGH && input.fatigue.sustained ->
            AdaptiveResult(AdaptiveDecision.StopSession, input.current, "Fatiga alta sostenida.")

        // 2. Cooldown: fatigue emerging but not critical — ease time-on-task.
        input.fatigue.emerging ->
            AdaptiveResult(
                AdaptiveDecision.Cooldown,
                input.current.stepDown(DifficultyAxis.TIME_ON_TASK, input.config.stepDown),
                "Fatiga emergente: micro-pausa y menos tiempo-en-tarea.",
            )

        else -> null
    }

    /**
     * Contain impulsivity: fast commission errors. Null when not impulsive.
     *
     * NOTE — declared debt, not fixed here: this rule is *outcome*-keyed, so it passes beneath a metric
     * allow-list. Its construct is a go/no-go commission error, which E2 does not have (every flanker trial
     * demands a response), yet it can still `stepUp`. Both reviewers flagged it; it is an exit gate for the
     * gold, and it needs per-family gating of adaptive RULES, not just metric keys.
     */
    private fun impulsivityDecision(input: AdaptiveInput): AdaptiveResult? {
        // Applicability BEFORE evidence: on a task where every trial demands a response there is no commission
        // error to contain, so the rule must not fire even if its outcome pattern happens to match.
        val applicable = FocusAdaptiveRulePolicy.allowsImpulsivityContainment(
            input.exercise.id,
            input.exercise.paradigm,
        )
        return if (!applicable || !isImpulsive(input)) {
            null
        } else {
            val axis = if (DifficultyAxis.INHIBITORY_DEMAND in input.exercise.relevantAxes) {
                DifficultyAxis.INHIBITORY_DEMAND
            } else {
                input.exercise.governingAxis
            }
            AdaptiveResult(
                AdaptiveDecision.ContainImpulsivity(axis),
                input.current.stepUp(axis, input.config.stepUp),
                "Comisiones con RT muy corto: contener precipitación, no premiar velocidad.",
            )
        }
    }

    override fun decide(input: AdaptiveInput): AdaptiveResult =
        fatigueDecision(input) ?: impulsivityDecision(input) ?: performanceDecision(input)

    @Suppress("CyclomaticComplexMethod")
    private fun performanceDecision(input: AdaptiveInput): AdaptiveResult {
        val cfg = input.config
        val gov = input.exercise.governingAxis
        // confidentValueOf, never valueOf: an absent metric must arrive as null, not as a 0.0 that is below
        // every step-down threshold. See the METRIC_UNAVAILABLE branch below.
        val accuracy = input.recentBlock.confidentValueOf(MetricKey.ACCURACY)
        val rtCv = input.recentBlock.confidentValueOf(MetricKey.RT_CV)
        // Baseline RT-CV would come from the calibrated profile; until calibration
        // lands we use the config fallback. Relative spikes (not absolute CV) trigger step-down.
        val baselineCv = cfg.fallbackBaselineRtCv
        val consecutiveErrors = trailingConsecutiveErrors(input.recentTrials)

        return when {
            // 4. No usable evidence: hold, never step. A session that measured nothing is not a session the
            // user failed. This branch sits above every step so absence can reach neither StepDown nor StepUp,
            // and it preserves `input.current` unchanged.
            accuracy == null ->
                AdaptiveResult(
                    AdaptiveDecision.Hold(HoldReason.METRIC_UNAVAILABLE),
                    input.current,
                    "Sin evidencia suficiente en este bloque: se mantiene la dificultad.",
                )

            // 5. Step down: struggling.
            accuracy < cfg.accDownThreshold ||
                consecutiveErrors >= cfg.consecutiveErrorsDown ||
                (rtCv != null && rtCv > cfg.rtCvSpikeFactor * baselineCv && rtCv > 0.0) ->
                AdaptiveResult(
                    AdaptiveDecision.StepDown(gov),
                    input.current.stepDown(gov, cfg.stepDown),
                    "Rendimiento por debajo del umbral o variabilidad alta: bajar dificultad.",
                )

            // 6. Hysteresis: just changed — hold and let it stabilize.
            input.blocksSinceChange < cfg.cooldownAfterDecisions ->
                AdaptiveResult(
                    AdaptiveDecision.Hold(HoldReason.HYSTERESIS_AFTER_CHANGE),
                    input.current,
                    "Histéresis tras un cambio reciente.",
                )

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

            // 9. Hold: the mid band, nothing to change.
            else -> AdaptiveResult(
                AdaptiveDecision.Hold(HoldReason.MAINTENANCE_BAND),
                input.current,
                "Zona de mantenimiento.",
            )
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
        // Absent false-alarm evidence must not accuse: null means "not measured", so impulsivity is not claimed.
        val falseAlarms = input.recentBlock.confidentValueOf(MetricKey.FALSE_ALARMS)
        return fastErrors >= 2 && falseAlarms != null && falseAlarms >= 2.0
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
