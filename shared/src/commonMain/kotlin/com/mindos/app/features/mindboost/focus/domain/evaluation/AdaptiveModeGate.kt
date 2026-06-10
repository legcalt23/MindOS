package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveDecision
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveResult
import com.mindos.app.features.mindboost.focus.domain.model.Mode

/**
 * Enforces the critical separation of modes (06_evaluation_and_baseline_plan.md §0):
 * adaptation happens ONLY in [Mode.TRAINING]. In [Mode.EVALUATION] and
 * [Mode.CALIBRATION] the difficulty is fixed for the whole block, so this gate
 * short-circuits the adaptive engine and returns the current difficulty unchanged.
 *
 * Routing every adaptive decision through this gate makes the invariant testable
 * and impossible to bypass by accident: a call in a non-training mode can never
 * move difficulty.
 */
object AdaptiveModeGate {

    fun decide(mode: Mode, engine: AdaptiveEngine, input: AdaptiveInput): AdaptiveResult =
        when (mode) {
            Mode.TRAINING -> engine.decide(input)
            Mode.EVALUATION, Mode.CALIBRATION -> AdaptiveResult(
                decision = AdaptiveDecision.Hold,
                nextDifficulty = input.current,
                rationale = "Dificultad fija: no se adapta en modo $mode.",
            )
        }
}
