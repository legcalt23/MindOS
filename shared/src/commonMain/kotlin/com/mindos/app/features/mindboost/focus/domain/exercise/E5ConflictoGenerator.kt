package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** Concrete E5 (Stroop/Simon) parameters derived from the relevant difficulty axes. */
data class E5Params(
    val incongruentProportion: Double,
    val responseWindowMs: Long,
    val isiMs: Long,
)

/**
 * Maps difficulty to E5 (Stroop/Simon). Reads ONLY E5's relevant axes:
 * INTERFERENCE (proportion incongruent) and PRESSURE (response window).
 */
@Suppress("MagicNumber") // The literals are this exercise's difficulty curve.
object E5DifficultyMapping {
    fun map(d: DifficultyVector): E5Params = E5Params(
        incongruentProportion = lerp(0.25, 0.65, d.interference.value),
        responseWindowMs = lerp(1600.0, 1000.0, d.pressure.value).toLong(),
        isiMs = lerp(800.0, 600.0, d.pressure.value).toLong(),
    )
}

/**
 * E5 Conflicto generator. Respond to the relevant dimension (LEFT/RIGHT); on
 * incongruent trials the conflicting dimension points the other way. Proportion
 * incongruent scales with INTERFERENCE. Deterministic for a given seed.
 */
class E5ConflictoGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E5_CONFLICTO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E5DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val relevant = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
            val congruent = random.nextDouble() >= params.incongruentProportion
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = relevant,
                payload = StimulusPayload.Conflict(relevantResponse = relevant, congruent = congruent),
                interStimulusDelayMs = params.isiMs,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }
}
