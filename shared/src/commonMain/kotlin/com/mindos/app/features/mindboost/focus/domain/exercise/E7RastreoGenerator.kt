package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E7 (visual search) parameters from the relevant axes. */
data class E7Params(val setSizes: List<Int>, val targetPresentRate: Double, val responseWindowMs: Long)

/** Maps difficulty to E7. Reads PERCEPTUAL_LOAD (max set size) and PRESSURE (window). */
@Suppress("MagicNumber")
object E7DifficultyMapping {
    fun map(d: DifficultyVector): E7Params {
        val maxSetSize = lerp(8.0, 32.0, d.perceptualLoad.value).toInt()
        // Vary set size across the block so a set-size slope can be measured.
        val sizes = listOf(4, maxSetSize / 2, maxSetSize).map { it.coerceAtLeast(2) }.distinct()
        return E7Params(
            setSizes = sizes,
            targetPresentRate = 0.5,
            responseWindowMs = lerp(2000.0, 1200.0, d.pressure.value).toLong(),
        )
    }
}

/**
 * E7 Rastreo generator. Each trial is a search display; respond when the target is
 * present, withhold when absent. Set size varies across the block so the metrics
 * engine can fit a set-size slope. Deterministic for a seed.
 */
class E7RastreoGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E7_RASTREO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E7DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val setSize = params.setSizes[index % params.setSizes.size]
            val present = random.nextDouble() < params.targetPresentRate
            Stimulus(
                index = index,
                isTarget = present,
                correctResponseKey = if (present) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.Search(targetPresent = present, setSize = setSize),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    private companion object {
        const val ISI_MS = 600L
    }
}
