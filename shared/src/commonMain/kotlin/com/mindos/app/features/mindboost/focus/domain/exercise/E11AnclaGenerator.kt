package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E11 (digital distraction) parameters from the relevant axes. */
data class E11Params(val intruderProportion: Double, val responseWindowMs: Long)

/** Maps difficulty to E11. Reads DISTRACTOR_SALIENCE (intruder frequency) and PRESSURE (window). */
@Suppress("MagicNumber")
object E11DifficultyMapping {
    fun map(d: DifficultyVector): E11Params = E11Params(
        intruderProportion = lerp(0.20, 0.55, d.distractorSalience.value),
        responseWindowMs = lerp(1400.0, 1000.0, d.pressure.value).toLong(),
    )
}

/**
 * E11 Ancla generator. The user keeps doing a main choice task; on some trials a
 * banner-like intruder appears that must be ignored. The
 * [StimulusPayload.AnchorTask.intruderPresent] flag lets the metrics engine compute
 * the distraction-resistance index (accuracy during intrusion vs baseline).
 */
class E11AnclaGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E11_ANCLA

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E11DifficultyMapping.map(difficulty)
        val random = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val direction = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
            val intruder = random.nextDouble() < params.intruderProportion
            Stimulus(
                index = index,
                isTarget = true,
                correctResponseKey = direction,
                payload = StimulusPayload.AnchorTask(mainResponse = direction, intruderPresent = intruder),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    private companion object {
        const val ISI_MS = 700L
    }
}
