package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** Concrete E2 (flanker) parameters derived from the relevant difficulty axes. */
data class E2Params(
    val incongruentProportion: Double,
    val flankerCount: Int,
    val responseWindowMs: Long,
    val isiMs: Long,
)

/**
 * Maps difficulty to E2 (Eriksen flanker). Reads ONLY E2's relevant axes:
 * INTERFERENCE (proportion incongruent), PERCEPTUAL_LOAD (number of flankers),
 * PRESSURE (response window). Other axes are inert.
 */
@Suppress("MagicNumber") // The literals are this exercise's difficulty curve.
object E2DifficultyMapping {
    fun map(d: DifficultyVector): E2Params = E2Params(
        incongruentProportion = lerp(0.20, 0.60, d.interference.value),
        flankerCount = lerp(2.0, 6.0, d.perceptualLoad.value).toInt(),
        responseWindowMs = lerp(1500.0, 900.0, d.pressure.value).toLong(),
        isiMs = lerp(700.0, 500.0, d.pressure.value).toLong(),
    )
}

/**
 * E2 Filtro generator. Each trial shows a central arrow (LEFT/RIGHT) flanked by
 * arrows that point the same way (congruent) or the opposite way (incongruent).
 * The user responds to the CENTER. Deterministic for a given seed.
 */
class E2FiltroGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E2_FILTRO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E2DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val direction = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
            val congruent = random.nextDouble() >= params.incongruentProportion
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = direction,
                payload = StimulusPayload.Flanker(
                    centerDirection = direction,
                    congruent = congruent,
                    flankerCount = params.flankerCount,
                ),
                interStimulusDelayMs = params.isiMs,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }
}
