package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.TaskRule
import kotlin.random.Random

/** E6 (task switching) parameters from the relevant axes. */
data class E6Params(val switchFrequency: Double, val responseWindowMs: Long, val preparationMs: Long)

/** Maps difficulty to E6. Reads INTERFERENCE (switch frequency), TEMPORAL_UNCERTAINTY (prep) and PRESSURE. */
@Suppress("MagicNumber")
object E6DifficultyMapping {
    fun map(d: DifficultyVector): E6Params = E6Params(
        switchFrequency = lerp(0.30, 0.60, d.interference.value),
        responseWindowMs = lerp(1500.0, 1000.0, d.pressure.value).toLong(),
        preparationMs = lerp(1000.0, 500.0, d.temporalUncertainty.value).toLong(),
    )
}

/**
 * E6 Cambio generator. Each trial applies the active rule; the rule switches with
 * the configured frequency, and [StimulusPayload.TaskSwitch.isSwitch] marks switch
 * trials so the switch-cost metric can compare them. Deterministic for a seed.
 */
class E6CambioGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E6_CAMBIO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E6DifficultyMapping.map(difficulty)
        val random = Random(seed)
        var rule = if (random.nextBoolean()) TaskRule.RULE_A else TaskRule.RULE_B
        return (0 until spec.targetTrialCount).map { index ->
            val isSwitch = index > 0 && random.nextDouble() < params.switchFrequency
            if (isSwitch) rule = if (rule == TaskRule.RULE_A) TaskRule.RULE_B else TaskRule.RULE_A
            val colorIsRed = random.nextBoolean()
            val shapeIsCircle = random.nextBoolean()
            // The real task-switch operation: apply the ACTIVE rule to the relevant dimension.
            val response = if (correctSideIsLeft(rule, colorIsRed, shapeIsCircle)) {
                ResponseKey.LEFT
            } else {
                ResponseKey.RIGHT
            }
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = response,
                payload = StimulusPayload.TaskSwitch(
                    rule = rule,
                    isSwitch = isSwitch,
                    correctResponse = response,
                    colorIsRed = colorIsRed,
                    shapeIsCircle = shapeIsCircle,
                ),
                interStimulusDelayMs = params.preparationMs,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    /** Color rule (RULE_A) → red answers LEFT; shape rule (RULE_B) → circle answers LEFT. */
    private fun correctSideIsLeft(rule: TaskRule, colorIsRed: Boolean, shapeIsCircle: Boolean): Boolean =
        if (rule == TaskRule.RULE_A) colorIsRed else shapeIsCircle
}
