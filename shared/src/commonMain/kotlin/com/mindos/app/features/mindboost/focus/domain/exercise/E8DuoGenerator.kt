package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.conditionSchedule
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** E8 (dual task) parameters from the relevant axes. */
data class E8Params(val dualProportion: Double, val goRate: Double, val responseWindowMs: Long)

/** Maps difficulty to E8. Reads DUAL_TASK_LOAD (share of dual trials) and PERCEPTUAL_LOAD. */
@Suppress("MagicNumber")
object E8DifficultyMapping {
    fun map(d: DifficultyVector): E8Params = E8Params(
        dualProportion = lerp(0.40, 0.75, d.dualTaskLoad.value),
        goRate = 0.7,
        responseWindowMs = lerp(1100.0, 800.0, d.perceptualLoad.value).toLong(),
    )
}

/**
 * E8 Dúo generator. Interleaves single- and dual-condition trials; the dual share
 * grows with DUAL_TASK_LOAD. The [StimulusPayload.DualTask.dual] flag lets the
 * metrics engine compute the dual-task cost (single vs dual accuracy).
 */
class E8DuoGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E8_DUO

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E8DifficultyMapping.map(difficulty)
        val random = Random(seed)
        // The dual/single split is the cell of DUAL_TASK_COST, so it is a quota. `go` is not a governed cell
        // (no ratified floor reads it), so it stays an independent draw.
        val duals = conditionSchedule(spec.targetTrialCount, params.dualProportion, random)
        return (0 until spec.targetTrialCount).map { index ->
            val dual = duals[index]
            val go = random.nextDouble() < params.goRate
            Stimulus(
                index = index,
                isTarget = go,
                correctResponseKey = if (go) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.DualTask(dual = dual, go = go),
                interStimulusDelayMs = ISI_MS,
                responseWindowMs = params.responseWindowMs,
            )
        }
    }

    private companion object {
        const val ISI_MS = 700L
    }
}
