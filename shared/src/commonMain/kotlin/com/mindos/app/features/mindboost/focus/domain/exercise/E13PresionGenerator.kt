package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E13 (deadline pressure) parameters from the relevant axes. */
data class E13Params(val pressureProportion: Double, val deadlineMs: Long, val responseWindowMs: Long)

/**
 * Maps difficulty to E13. Reads PRESSURE (share of deadline trials and how tight
 * the deadline is). Calm trials and deadline trials are interleaved so the engine
 * can compute the pressure delta (accuracy calm − accuracy under deadline).
 */
@Suppress("MagicNumber")
object E13DifficultyMapping {
    fun map(d: DifficultyVector): E13Params = E13Params(
        pressureProportion = lerp(0.40, 0.60, d.pressure.value),
        deadlineMs = lerp(800.0, 450.0, d.pressure.value).toLong(),
        responseWindowMs = lerp(1600.0, 1100.0, d.pressure.value).toLong(),
    )
}

/**
 * E13 Presión generator. Interleaves calm and deadline (pressure) choice trials.
 * The [StimulusPayload.Pressure.underPressure] flag lets the metrics engine compute
 * the pressure delta. Deterministic for a seed.
 */
class E13PresionGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E13_PRESION

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E13DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val direction = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
            val underPressure = random.nextDouble() < params.pressureProportion
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = direction,
                payload = StimulusPayload.Pressure(
                    correctResponse = direction,
                    underPressure = underPressure,
                    deadlineMs = if (underPressure) params.deadlineMs else 0L,
                ),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    private companion object {
        const val ISI_MS = 700L
    }
}
