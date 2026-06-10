package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E9 (multiple-object tracking) parameters from the relevant axes. */
data class E9Params(val objectCount: Int, val targetCount: Int, val speed: Double)

/**
 * Maps difficulty to E9 (MOT). Reads PERCEPTUAL_LOAD (object count), DUAL_TASK_LOAD
 * (number of targets to track) and DISTRACTOR_SALIENCE (speed).
 */
@Suppress("MagicNumber")
object E9DifficultyMapping {
    fun map(d: DifficultyVector): E9Params {
        val objectCount = lerp(6.0, 16.0, d.perceptualLoad.value).toInt()
        val targetCount = lerp(2.0, 5.0, d.dualTaskLoad.value).toInt().coerceAtMost(objectCount)
        return E9Params(
            objectCount = objectCount,
            targetCount = targetCount,
            speed = lerp(1.0, 3.0, d.distractorSalience.value),
        )
    }
}

/**
 * E9 Enjambre generator. Each trial is one MOT display: [E9Params.targetCount]
 * targets to keep track of among identical moving objects. The user later confirms
 * whether a probed object was a target ([Stimulus.isTarget]). Object trajectories
 * are a render concern; the domain fixes the deterministic target set.
 */
class E9EnjambreGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E9_ENJAMBRE

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E9DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val targetIndices = pickTargets(random, params.objectCount, params.targetCount)
            val probed = random.nextInt(params.objectCount)
            val probedIsTarget = probed in targetIndices
            Stimulus(
                index = index,
                isTarget = probedIsTarget,
                correctResponseKey = if (probedIsTarget) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.MultiObjectTracking(
                    objectCount = params.objectCount,
                    targetCount = params.targetCount,
                    targetIndices = targetIndices,
                    speed = params.speed,
                ),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = RESPONSE_WINDOW_MS,
            )
        }
    }

    private fun pickTargets(random: Random, objectCount: Int, targetCount: Int): Set<Int> {
        val indices = (0 until objectCount).toMutableList()
        val targets = mutableSetOf<Int>()
        repeat(targetCount.coerceAtMost(objectCount)) {
            targets.add(indices.removeAt(random.nextInt(indices.size)))
        }
        return targets
    }

    private companion object {
        const val ISI_MS = 900L
        const val RESPONSE_WINDOW_MS = 2_000L
    }
}
