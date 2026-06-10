package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** Concrete E3 (go/no-go) parameters derived from the relevant difficulty axes. */
data class E3Params(
    val goProportion: Double,
    val responseWindowMs: Long,
    val isiMs: Long,
)

/**
 * Maps difficulty to E3 (go/no-go). Reads ONLY E3's relevant axes:
 * INHIBITORY_DEMAND (go frequency → prepotency, harder to withhold) and
 * PRESSURE (response window). Other axes are inert.
 */
@Suppress("MagicNumber") // The literals are this exercise's difficulty curve.
object E3DifficultyMapping {
    fun map(d: DifficultyVector): E3Params = E3Params(
        // More frequent go builds a stronger prepotent response → harder no-go inhibition.
        goProportion = lerp(0.70, 0.90, d.inhibitoryDemand.value),
        responseWindowMs = lerp(900.0, 600.0, d.pressure.value).toLong(),
        isiMs = lerp(800.0, 550.0, d.pressure.value).toLong(),
    )
}

/**
 * E3 Gatillo generator. Frequent go targets (respond) and rarer no-go items
 * (withhold). At least one no-go is guaranteed. Deterministic for a given seed.
 */
class E3GatilloGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E3_GATILLO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E3DifficultyMapping.map(difficulty)
        val random = Random(seed)
        val count = spec.targetTrialCount
        val forcedNoGoIndex = random.nextInt(count)
        return (0 until count).map { index ->
            val go = index != forcedNoGoIndex && random.nextDouble() < params.goProportion
            Stimulus(
                index = index,
                isTarget = go,
                correctResponseKey = if (go) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.GoNoGo(go = go),
                interStimulusDelayMs = params.isiMs,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }
}
