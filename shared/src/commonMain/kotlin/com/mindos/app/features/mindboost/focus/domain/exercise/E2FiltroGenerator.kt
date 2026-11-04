package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.conditionSchedule
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload

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
class E2FiltroGenerator : VersionedTrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E2_FILTRO

    override val generatorVersion: Int = E2_GENERATOR_VERSION

    companion object {
        /**
         * The version of the form E2 produces. **3** since the congruency split became a quota
         * ([conditionSchedule]) instead of a per-trial coin flip: the same seed now yields a different form, so
         * the two are not comparable and the form identity must carry this to keep them apart. (**2** was the
         * move from `kotlin.random.Random` (XorWow) to the pinned splitmix64.) Bump on any change to the drawn
         * sequence.
         *
         * Declared here independently of `FocusFormContract`, which declares the version the engine authorizes.
         * Two declarations that must agree is a check; one declaration read twice is not.
         */
        const val E2_GENERATOR_VERSION = 3
    }

    /**
     * The versioned path: the seed carries the algorithm it was drawn against, so the sequence is reproducible
     * across Kotlin versions. `kotlin.random.Random(seed)` is deliberately NOT used here — its algorithm is not
     * contractual, so a golden or a replay built on it silently becomes a different session on a stdlib change.
     */
    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: SessionSeed): List<Stimulus> {
        val params = E2DifficultyMapping.map(difficulty)
        val random = DeterministicRng.forSeed(seed)
        val incongruent = conditionSchedule(spec.targetTrialCount, params.incongruentProportion, random)
        return (0 until spec.targetTrialCount).map { index ->
            val direction = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
            val congruent = !incongruent[index]
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
